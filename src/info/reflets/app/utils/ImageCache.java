package info.reflets.app.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public final class ImageCache extends AbstractMap<String, SoftReference<Bitmap>> implements Runnable
{
	private static final String 		LOG_TAG 						= ImageCache.class.getSimpleName();

	public	static final int			HTTP_TIMEOUT					= 15 * 1000;
	private	static final int			NB_RETRY_TO_DOWNLOAD			= 3;
	private	static final int			NB_THREAD_MAX					= 10;

	private static final String			FILE_EXTENSION					= ".img";
	private static final String			STORAGE_DIR						= "images/";
	private static final SortFileByDate	FILE_SORTER						= new SortFileByDate();
	
	public static final String			EXTRA_BITMAP					= "extra_bitmap";
	public static final String			EXTRA_IMAGE_URL					= "extra_image_url";
	
	private	static boolean				mCacheFileDisable				= false;
	private	static int					mCacheFileLimit					= 150;
	public  static Bitmap				mBitmapEmpty					= null;
	private static ConnectivityManager	mConnectivityManager			= null;

	private final List<Handler> 			LOADERS						= new ArrayList<Handler>();
	private final HashMap<String, String>	URLS						= new HashMap<String, String>();

	private File							SAVE_DIR					= null;
	private LinkedHashMap<String, File>		CACHE_FILE					= null;
	private boolean							mThreadRunning				= true;

	private final Map<String, SoftReference<Bitmap>> hash 				= new HashMap<String, SoftReference<Bitmap>>();
	private final int						mRamCapacity;
	private final LinkedList<Bitmap> 		hardCache 					= new LinkedList<Bitmap>();
	private final ReferenceQueue<Bitmap> 	queue 						= new ReferenceQueue<Bitmap>();
	
	static class SortFileByDate implements Comparator<File>{
	    public int compare(File file1, File file2){
	        long comparateur = file1.lastModified() - file2.lastModified();
	        return (comparateur > 0) ? -1 : 1;
	    }
	}


    public void disableCacheFile(){
    	mCacheFileDisable = true;
    }
    
    public void setCacheFileLimit(int limit){
    	mCacheFileLimit = limit;
    }


	//*************************************************************************
	// CONSTRUCTOR
	//*************************************************************************
    public ImageCache(Context context, String folderName){
        this(context, folderName, 100);
    }
    
    public ImageCache(Context context, String folderName, int ramSize)
    {
    	mRamCapacity = ramSize;

    	CACHE_FILE = new LinkedHashMap<String, File>(mCacheFileLimit, 1.1f, true);
   		
		createEmptyBitmap();
		
		if (mConnectivityManager == null){
			mConnectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		}

		// Get cache folder
		File storageDir = new File(context.getCacheDir(), STORAGE_DIR);

		if (!storageDir.exists()) {
			if (storageDir.mkdir()){
				Log.d(LOG_TAG, storageDir + " created!");
			}
			else{
				Log.d(LOG_TAG, storageDir + " not created!");
			}
		}

		SAVE_DIR = new File(storageDir, folderName);

		if (!SAVE_DIR.exists()) {
			if (SAVE_DIR.mkdir()){
				Log.d(LOG_TAG, SAVE_DIR + " created!");
			}
			else{
				Log.d(LOG_TAG, SAVE_DIR + " not created!");
			}
		}

		// Load and clean files
		loadFile();

		// Launch thread
		Thread thread = new Thread(this);
		thread.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		thread.setDaemon(true);
		thread.start();
	}

	public void clearAll()
	{
		gcFile();

		clear();

		mBitmapEmpty			= null;
	}

	// Create empty bitmap with like white rectangle => use to replace null value when no bitmap to cache 
	private void createEmptyBitmap()
	{
		if (mBitmapEmpty != null){
			return;
		}

		int		width	= 10;
		int 	height	= 10;
		int[]	colors	= new int[width * height];

		for (int c = 0 ; c < colors.length ; c++){
			colors[c] = 0xFFFFFF;
		}
		
		mBitmapEmpty = Bitmap.createBitmap(colors, width, height, Config.ARGB_8888);
	}

	
	//*************************************************************************
	// THREAD
	//*************************************************************************
	public void run()
	{
		while (mThreadRunning)
		{
			synchronized(URLS){
				if (URLS.size() == 0 || URLS.size() > NB_THREAD_MAX){
					try {
						URLS.wait();
					}
					catch (InterruptedException e) {
				}
				}

				final String imageUrl = (String)URLS.keySet().iterator().next();

				URLS.remove(imageUrl);

				if (imageUrl != null && !imageUrl.equals("")){
					
					new Thread(new Runnable() {
						public void run() {
							Bitmap bitmap = get(imageUrl);

							if (bitmap == null){
								boolean fromInternet = false;

								bitmap = loadBitmapFromFile(imageUrl);

								// Download from internet
								if (bitmap == null){
									if (isConnected()){
										try{
											fromInternet 	= true;
											bitmap			= downloadBitmap(imageUrl);
	
											if (bitmap == null){
												String mAlternativeUrl = URLS.get(imageUrl);
	
												if (mAlternativeUrl != null){
													bitmap = downloadBitmap(mAlternativeUrl);
													mAlternativeUrl = null;
												}
											}
										}
										catch(OutOfMemoryError e) {
											clear();
											System.gc();
										}
									}
								}

								if (bitmap == null){
									Log.d(LOG_TAG, "No bitmap for " + imageUrl);
								}
								else{
									putBitmap(imageUrl, bitmap, fromInternet);
								}
							}
	
							synchronized(LOADERS){
								for (Handler h : LOADERS){
									Message message = new Message();
							        message.what = ImageAdvancedView.MSG_DISPLAY_IMAGE;
							        
							        Bundle data = new Bundle();
							        data.putString(ImageCache.EXTRA_IMAGE_URL, imageUrl);
	
							        Bitmap image = bitmap;
							        data.putParcelable(ImageCache.EXTRA_BITMAP, image);
	
							        message.setData(data);
							        
							        h.sendMessage(message);
								}
							}
						}
					}).start();
				}
			}
		}
    }
	
	public static Map<String, Bitmap> loadBitmaps(final List<String> listUrls, final String cache) {
		Map<String, Bitmap> res = new HashMap<String, Bitmap>();
		
		for (String url : listUrls){
			res.put(url, getBitmapFromUrl(url));
		}
		
		return res;
	}


	//*************************************************************************
	// RAM
	//*************************************************************************
    public Bitmap get(String key){
    	Bitmap 					result		= null;
        SoftReference<Bitmap>	soft_ref	= (SoftReference<Bitmap>)hash.get(key);

        if (soft_ref != null){
            result = soft_ref.get();

            if (result == null){
            	hash.remove(key);
            }
            else{
                hardCache.addFirst(result);

                if (hardCache.size() > mRamCapacity){
                    hardCache.removeLast();
                }
            }
        }

        return result;
    }

    private static class SoftValue extends SoftReference<Bitmap>
    {
        private final Object key;

        public SoftValue(Bitmap k, String key, ReferenceQueue<Bitmap> q) {
            super(k, q);
            this.key = key;
        }
    }

    private void processQueue(){
        SoftValue sv;
        while((sv = (SoftValue)queue.poll()) != null){
            hash.remove(sv.key);
        }
    }

    public SoftReference<Bitmap> put(String key, Bitmap value){
        processQueue();
        return hash.put(key, new SoftValue(value, key, queue));
    }

    public void clear(){
        hardCache.clear();
        processQueue();
        hash.clear();
    }
    
    public void recycle(String url){
		Bitmap bitmap = get(url);

		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
    }
    
    public int size(){
        processQueue();
        return hash.size();
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Set entrySet() {
        throw new UnsupportedOperationException();
    }

	public Bitmap getBitmap(String imageUrl, String alternativeUrl, Handler handler)
	{
		Bitmap bitmap = get(imageUrl);

		if (bitmap != null) {
			return bitmap;
		}

		synchronized(LOADERS){
			if (!LOADERS.contains(handler)){
				LOADERS.add(handler);
			}
		}
		
		synchronized(URLS){
			if (!URLS.containsKey(imageUrl)){
				URLS.put(imageUrl, alternativeUrl);
				URLS.notify();
			}
		}

		return null;
	}

	public int getNbHandler(){
		return LOADERS.size();
	}
	
	public void removeHandler(Handler handler)
	{
		synchronized(LOADERS){
			LOADERS.remove(handler);
		}
	}

	public void putBitmap(final String paramString, Bitmap paramBitmap)
	{
		putBitmap(paramString, paramBitmap, true);
	}
	
	public void putBitmap(final String paramString, Bitmap paramBitmap, boolean saveInFile)
	{
		if (paramBitmap == null){
			return;
		}

		put(paramString, paramBitmap);

		if (saveInFile){
			if (paramBitmap != mBitmapEmpty){
				final Bitmap bitmapToSave = paramBitmap;	
				new Thread(new Runnable() {
					public void run() {
						saveBitmapInFile(paramString, bitmapToSave);
					}
				}).start();
			}
		}
	}
	
	public void removeCache(final String paramString)
	{
		if (paramString == null){
			return;
		}

		Bitmap bitmap = get(paramString);

		if (bitmap != null) {
			remove(paramString);
			
		}

		synchronized(CACHE_FILE){
			File localFile = CACHE_FILE.get(paramString + FILE_EXTENSION);
	
			Log.d(LOG_TAG, "GET paramString = " + paramString + FILE_EXTENSION + " = " + (localFile==null));

			if (localFile != null) {
				if (localFile.exists()){
					localFile.delete();
					Log.d(LOG_TAG, "deleted!!");
				}

				CACHE_FILE.remove(paramString + FILE_EXTENSION);
			}
		}
	}
	
	
	//*************************************************************************
	// FILES
	//*************************************************************************
	private void loadFile()
	{
		
		File[] files = SAVE_DIR.listFiles( new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				if (filename.endsWith(FILE_EXTENSION)){
					return true;
				}
				return false;
			}
		});
		
		if (files == null){
			return;
		}
		
		for (File f : files){
			CACHE_FILE.put(f.getName(), f);
		}
		
		Log.d(LOG_TAG, "CACHE_FILE = " + CACHE_FILE.size());
	}
	
	public void gcFile()
	{
		
		File[] files = SAVE_DIR.listFiles( new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				if (filename.endsWith(FILE_EXTENSION)){
					return true;
				}
				return false;
			}
		});
		
		if (files == null){
			return;
		}

		try {
			Arrays.sort(files, FILE_SORTER);
		}
		catch (Exception e) {
	//		e.printStackTrace();
			Log.d(LOG_TAG, "Exception when sorting => deleted all files!");
		}

		synchronized(CACHE_FILE){
			long size = 0;
			int nbFiles = 0;

			for (File f : files){

				if (nbFiles > mCacheFileLimit){
					if (f.delete()){
						Log.d(LOG_TAG, f.getPath() + " deleted!");
					}
					else{
						size += f.length();
					}
				}
				else{
					size += f.length();	
				}

				nbFiles++;
			}

			Log.d(LOG_TAG, "Nb files total = " + files.length + " nbFiles in cache = " + CACHE_FILE.size() + " total length = " + size);
		}
	}
	


	private Bitmap loadBitmapFromFile(String paramString)
	{
		if (mCacheFileDisable){
			return null;
		}
		
		paramString = Integer.toString(paramString.hashCode());

		Bitmap localBitmap = null;
		
		synchronized(CACHE_FILE){
			File localFile = CACHE_FILE.get(paramString + FILE_EXTENSION);
	
			if (localFile != null) {
				localBitmap = getBitmapFromFile(localFile);
				if (localBitmap == null){
					CACHE_FILE.remove(paramString + FILE_EXTENSION);
				}

				return localBitmap;
			}
		}

		return localBitmap;
	}
	
	public void saveBitmapInFile(String paramString, Bitmap paramBitmap)
	{
		if (mCacheFileDisable){
			return;
		}
		
		paramString = Integer.toString(paramString.hashCode());
	
		synchronized(CACHE_FILE){
			if (!CACHE_FILE.containsKey(paramString + FILE_EXTENSION)){
				File				file = new File(SAVE_DIR, paramString + FILE_EXTENSION);
				FileOutputStream	fOut = null;
	
				try {
					if (!file.exists()){
						file.createNewFile();
					}
	
					fOut = new FileOutputStream(file);
					if (paramBitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)){
						fOut.flush();

						CACHE_FILE.put(paramString + FILE_EXTENSION, file);
	
					}
				}
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				finally{
					if (fOut != null){
						try {
							fOut.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
    // Decodes image and scales it to reduce memory consumption
	private Bitmap getBitmapFromFile(File f)
	{
		try {
			return BitmapFactory.decodeStream(new FileInputStream(f), null, null);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch(OutOfMemoryError oome) {
			System.gc();
		}

		return null;
	}
	
	
	//*************************************************************************
	// NETWORK
	//*************************************************************************
	private static boolean isConnected() {
		NetworkInfo info = (NetworkInfo)mConnectivityManager.getActiveNetworkInfo();
		return (info != null && info.isConnected());
	}


	public static Bitmap getBitmapFromUrl(String imageUrl)
	{
		Bitmap bmp = null;

		try{
			bmp =  downloadBitmap(imageUrl);
		}
	    catch(OutOfMemoryError e) {
	    }

		return bmp;
	}
	
	public static Bitmap downloadBitmap(String imageUrl) throws OutOfMemoryError
	{
		if (imageUrl == null || imageUrl.equals("")){
			return null;
		}
		
	    URL 				url			= null;
	    HttpURLConnection	connection	= null;
	    InputStream			inputStream	= null;
	    Bitmap				bmp			= null;
	    
	    try {
						
	        url = new URL(imageUrl);
			connection = (HttpURLConnection)url.openConnection();
	        connection.setDoInput(true);
	        connection.connect();

	        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
		        inputStream = new FlushedInputStream(connection.getInputStream());
	
		        byte[] arrayBmp = readBytes(inputStream);
		        bmp = BitmapFactory.decodeByteArray(arrayBmp, 0, arrayBmp.length);
	
		        int nbRetry = NB_RETRY_TO_DOWNLOAD;
	
		        while (bmp == null && nbRetry > 0){
		        	Thread.sleep(100);  
		        	bmp = BitmapFactory.decodeByteArray(arrayBmp, 0, arrayBmp.length);
		        	nbRetry--;
		        }
	        }
	        else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND){
	        	bmp = mBitmapEmpty;
	        }
	        else{
	        	Log.i(LOG_TAG, "Response code : " + connection.getResponseCode() + " for " + imageUrl);
	        }
	    }
	    catch (Exception e) {
	        Log.w(LOG_TAG, "Error while retrieving bitmap from " + imageUrl, e);
	    }
	    catch(OutOfMemoryError e) {
	    	throw new OutOfMemoryError();
	    }
	    finally {
	        if (connection != null) {
	            try {
					connection.disconnect();
				} catch (Exception e) {
					e.printStackTrace();
				}
	        }
	        
	        if (inputStream != null) {
	        	try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	    }
	    
	    return bmp;
	}

	static class FlushedInputStream extends FilterInputStream
	{
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException
		{
			long totalBytesSkipped = 0L;
			
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) {
					int octet = read();
					if (octet < 0) {
						break; // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}

	public static byte[] readBytes(InputStream inputStream) throws IOException
	{
		// this dynamically extends to take the bytes you read
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

		// this is storage overwritten on each iteration with bytes
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		// we need to know how may bytes were read to write them to the
		// byteBuffer
		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			byteBuffer.write(buffer, 0, len);
		}

		// and then we can return your byte array.
		return byteBuffer.toByteArray();
	}
}