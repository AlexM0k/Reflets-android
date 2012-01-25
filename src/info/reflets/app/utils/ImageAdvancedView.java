package info.reflets.app.utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;

public class ImageAdvancedView extends FrameLayout
{
	private static final String 				LOG_TAG					= ImageAdvancedView.class.getSimpleName();
	public	static final int					MSG_DISPLAY_IMAGE		= 0;
	private static final Map<Integer, Bitmap>	BITMAP_NOT_FOUND		= new HashMap<Integer, Bitmap>();

	private	static ImageCache	mCACHE					= null;
	
	private static long 		mRAM 					= -1;
	public	static boolean		mLowMemoryHandset		= false;
	public  static Bitmap		mBitmapEmpty;

	private String				mDownloadingUrl			= null;
	private String				mAlternativeUrl			= null;
	private ImageCache 			mImageCache			= null;
	private Bitmap				mBitmapNotFound 		= null;
	private boolean				mBitmapGone				= false;
	private boolean				mBitmapInvisible		= false;
	private Bitmap				mBitmapLoading			= null;
	private int					mSpinnerSize			= 0;
	private int					mProgressBarStyle		= android.R.attr.progressBarStyle;
	private ProgressBar			mSpinner;
	private ImageView			mImage;
	private ImageListener		mImageListener			= null;
	private boolean				mDisplayLoadingSection	= true;

	
	public static void clearCacheImage(){
		if (mCACHE != null){
			mCACHE.clearAll();
			mCACHE = null;
		}

		for (Integer key : BITMAP_NOT_FOUND.keySet()){
			Bitmap bmp = BITMAP_NOT_FOUND.get(key);
			bmp.recycle();
			bmp = null;
		}
		BITMAP_NOT_FOUND.clear();
	}
	
	public static ImageCache getImageCache(Context ctx){
		return getImageCache(ctx, null);
	}
	
	public static ImageCache getImageCache(Context ctx, String cache){
		if (cache == null){
			cache = "cacheDefault";
		}

		if (mCACHE == null){
			if (mRAM == -1){
				MemoryInfo mi = new MemoryInfo();
				((ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);

				mRAM = mi.availMem / 1048576L;				
				mLowMemoryHandset = (mRAM <= 100);

			}

			int cacheMax = mLowMemoryHandset ? 0 : (int)mRAM;

			mCACHE = new ImageCache(ctx, cache, cacheMax);
		}
		
		return mCACHE;
	}


    /**
     * Constructor.  This version is only needed if you will be instantiating
     * the object manually (not from a layout XML file).
     * @param context
     */
    public ImageAdvancedView(Context context) {
        super(context);
        instantiate(context, null, null, null, null, null, null);
    }
    
    public ImageAdvancedView(Context context, int spinnerStyle) {
        super(context);
        mProgressBarStyle = spinnerStyle;
        instantiate(context, null, null, null, null, null, null);
    }

	/**
	 * This is used when creating the view in XML
	 * To have an image load in XML use the tag 'image="http://developer.android.com/images/dialog_buttons.png"'
	 * Replacing the url with your desired image
	 * Once you have instantiated the XML view you can call
	 * setImageDrawable(url) to change the image
	 * @param context
	 * @param attrSet
	 */
	public ImageAdvancedView(final Context context, final AttributeSet attrSet)
	{
		super(context, attrSet);

		// Spinner size
		mSpinnerSize = attrSet.getAttributeIntValue(null, "spinnerSize", 0);

		// Spinner style
		String spinnerStyle = attrSet.getAttributeValue(null, "progressBarStyle");
				
		if (spinnerStyle != null){
			if (spinnerStyle.equalsIgnoreCase("Widget_ProgressBar_Inverse"))			mProgressBarStyle = android.R.attr.progressBarStyleInverse;
			else if (spinnerStyle.equalsIgnoreCase("Widget_ProgressBar_Large"))			mProgressBarStyle = android.R.attr.progressBarStyleLarge;
			else if (spinnerStyle.equalsIgnoreCase("Widget_ProgressBar_Large_Inverse"))	mProgressBarStyle = android.R.attr.progressBarStyleLargeInverse;
			else if (spinnerStyle.equalsIgnoreCase("Widget_ProgressBar_Small"))			mProgressBarStyle = android.R.attr.progressBarStyleSmall;
			else if (spinnerStyle.equalsIgnoreCase("Widget_ProgressBar_Small_Inverse"))	mProgressBarStyle = android.R.attr.progressBarStyleSmallInverse;
		}

		// Init
		instantiate(
			context,
			attrSet.getAttributeValue(null, "image"),
			attrSet.getAttributeValue(null, "imageLoading"),
			attrSet.getAttributeValue(null, "imageNotFound"),
			attrSet.getAttributeValue(null, "cacheTag"),
			attrSet.getAttributeValue(null, "scaleType"),
			attrSet.getAttributeValue(null, "gravity")
		);
	}

	/**
	 * This is used when creating the view programatically
	 * Once you have instantiated the view you can call
	 * setImage(url) to change the image
	 * @param context the Activity context
	 * @param imageUrl the Image URL you wish to load
	 */
	public ImageAdvancedView(final Context context, final String imageUrl,
			final String imageLoading, final String imageNotFound,
			final String cacheTag, final String scaleType, final String gravity)
	{
		super(context);
		instantiate(context, imageUrl, imageLoading, imageNotFound, cacheTag, scaleType, gravity);		
	}

	/**
	 *  First time loading of the LoaderImageView
	 *  Sets up the LayoutParams of the view, you can change these to
	 *  get the required effects you want
	 */
	private synchronized void instantiate(final Context context,
			final String imageUrl, final String imageLoading, final String imageNotFound,
			final String cacheTag, final String scaleType, final String gravity)
	{
		mImageCache = getImageCache(context, cacheTag);
		
		if (imageLoading != null){
			int resLoading = context.getResources().getIdentifier(imageLoading, "drawable", context.getPackageName());

			if (resLoading != 0){
				mBitmapLoading = BitmapFactory.decodeResource(context.getResources(), resLoading);
			}
		}

		if (imageNotFound != null){
			if (imageNotFound.equals("gone")){
				mBitmapGone		= true;
				mBitmapNotFound = ImageCache.mBitmapEmpty;
			}
			else if (imageNotFound.equals("invisible")){
				mBitmapInvisible	= true;
				mBitmapNotFound 	= ImageCache.mBitmapEmpty;
			}
			else{
				int resNotFound = context.getResources().getIdentifier(imageNotFound, "drawable", context.getPackageName());

				if (resNotFound != 0){
					Bitmap bitmap = BITMAP_NOT_FOUND.get(resNotFound);
					if (bitmap != null) {
						mBitmapNotFound = bitmap;
					} else {
						try {
							mBitmapNotFound = BitmapFactory.decodeResource(context.getResources(), resNotFound);
							BITMAP_NOT_FOUND.put(resNotFound, mBitmapNotFound);
						} catch (Exception e) {
							Log.e(LOG_TAG, "decode BitmapNotFound", e);
						}
					}					
				}
			}
		}

		mImage = new ImageView(context);
		if (gravity == null){
			mImage.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));	
		}
		else{
			int gravityValue = Gravity.CENTER;
			
			if (gravity.equalsIgnoreCase("bottom"))					gravityValue = Gravity.BOTTOM;
			else if (gravity.equalsIgnoreCase("left"))				gravityValue = Gravity.LEFT;
			else if (gravity.equalsIgnoreCase("center_horizontal"))	gravityValue = Gravity.CENTER_HORIZONTAL;
			else if (gravity.equalsIgnoreCase("center_vertical"))	gravityValue = Gravity.CENTER_VERTICAL;
			else if (gravity.equalsIgnoreCase("right"))				gravityValue = Gravity.RIGHT;
			else if (gravity.equalsIgnoreCase("top"))				gravityValue = Gravity.TOP;
			else if (gravity.equalsIgnoreCase("fill_horizontal"))	gravityValue = Gravity.FILL_HORIZONTAL;
			else if (gravity.equalsIgnoreCase("fill_vertical"))		gravityValue = Gravity.FILL_VERTICAL;
			else if (gravity.equalsIgnoreCase("fill"))				gravityValue = Gravity.FILL;
			else if (gravity.equalsIgnoreCase("clip_horizontal"))	gravityValue = Gravity.CLIP_HORIZONTAL;
			else if (gravity.equalsIgnoreCase("clip_vertical"))		gravityValue = Gravity.CLIP_VERTICAL;

			mImage.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, gravityValue));
		}
		
		// Resets Scaletype, so needs to be done before reading scaletype value
		mImage.setAdjustViewBounds(true);

		mImage.setScaleType(ScaleType.FIT_CENTER);
		
		if (scaleType != null){
			if (scaleType.equalsIgnoreCase("center"))				mImage.setScaleType(ScaleType.CENTER);
			else if (scaleType.equalsIgnoreCase("centerCrop"))		mImage.setScaleType(ScaleType.CENTER_CROP);
			else if (scaleType.equalsIgnoreCase("centerInside"))	mImage.setScaleType(ScaleType.CENTER_INSIDE);
			else if (scaleType.equalsIgnoreCase("fitCenter"))		mImage.setScaleType(ScaleType.FIT_CENTER);
			else if (scaleType.equalsIgnoreCase("fitEnd"))			mImage.setScaleType(ScaleType.FIT_END);
			else if (scaleType.equalsIgnoreCase("fitStart"))		mImage.setScaleType(ScaleType.FIT_START);
			else if (scaleType.equalsIgnoreCase("fitXY"))			mImage.setScaleType(ScaleType.FIT_XY);
			else if (scaleType.equalsIgnoreCase("matrix"))			mImage.setScaleType(ScaleType.MATRIX);			
		} 
			
		// Spinner
		mSpinner = new ProgressBar(context, null, mProgressBarStyle);
		if (mSpinnerSize > 0){
			mSpinner.setLayoutParams(new FrameLayout.LayoutParams(mSpinnerSize, mSpinnerSize, Gravity.CENTER));
		}
		else{
			mSpinner.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		}
		mSpinner.setIndeterminate(true);
		mSpinner.setVisibility(View.INVISIBLE);

		addView(mImage);
		addView(mSpinner);
	}
	
	public void setSpinnerSize(int size) {
		mSpinnerSize = size;
		mSpinner.setLayoutParams(new FrameLayout.LayoutParams(mSpinnerSize, mSpinnerSize, Gravity.CENTER));
	}

	public void setImageLoading(Bitmap res) {
		mBitmapLoading = res;
	}

	public void setImageNotFound(final Context context, final int res) {
		mBitmapNotFound = BitmapFactory.decodeResource(context.getResources(), res);
	}
	
	public ImageView getImage(){
		return mImage;
	}
	
	public Bitmap getImageNotFound(){
		return mBitmapNotFound;
	}
	
	private boolean checkUrl(String imageUrl)
	{		
		// No url
		if (imageUrl == null){
			displayImage(mBitmapNotFound);
			return false;
		}
		
		
		if (imageUrl.startsWith("file:///")) {
			
			Bitmap bmp = mImageCache.get(imageUrl);
			
			if (bmp ==  null){
				bmp = BitmapFactory.decodeFile(imageUrl.replace("file:///", "/"));

				if (bmp == null){
					bmp = mBitmapNotFound;
				}
				else{
					mImageCache.put(imageUrl, bmp);
				}
				
			}
			
			
			displayImage(bmp);
			
			return false;
		}
		else if (imageUrl.startsWith("android.resource://")){
			int resId = -1;
			try {
				resId = Integer.parseInt(imageUrl.substring(imageUrl.lastIndexOf("/") + 1));
				mImage.setImageResource(resId);
				displayImage(null);
			}
			catch (NumberFormatException e) {
				e.printStackTrace();
			}
			finally{
				if (resId == -1){
					displayImage(mBitmapNotFound);
					return false;
				}
			}

			return false;
		}
		else if (imageUrl.startsWith("content://")){
			int resId = -1;
			try {
				resId = Integer.parseInt(imageUrl.substring(imageUrl.lastIndexOf("/") + 1));
			}
			catch (NumberFormatException e) {
				e.printStackTrace();
			}
			finally{
				if (resId == -1){
					displayImage(mBitmapNotFound);
					return false;
				}
			}
			
			Uri contactUri = Uri.parse(imageUrl);
			try {
				InputStream is = null;

				try{
					Bitmap bmp = Tools.openContactPhotoInputStream(getContext(), contactUri);
					displayImage(bmp != null ? bmp : mBitmapNotFound);
				}
				catch (OutOfMemoryError oome) {
					System.gc();
				}
				finally {
					try {
						if (is != null)
							is.close();
					}
					catch (Exception e) {}
					
					is = null;
				}
			}
			catch (Exception e){
				Log.e(LOG_TAG, e.getMessage());
				e.printStackTrace();
			}

			return false;
		}
		
		return true;
	}
	
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
        	switch(msg.what){
        	case MSG_DISPLAY_IMAGE:        		
        		Bundle data = msg.getData();

        		if (mDownloadingUrl == null){
        			displayImage(null);
        		}
        		else if (mDownloadingUrl.equals(data.getString(ImageCache.EXTRA_IMAGE_URL))){
        			Bitmap bitmap = data.getParcelable(ImageCache.EXTRA_BITMAP);
        			displayImage(bitmap == null ? mBitmapNotFound : bitmap);
        		}
        		
				break;
        	}
        }
    };

	private void displayImage(Bitmap bitmap){

		mSpinner.setVisibility(View.INVISIBLE);
		mDownloadingUrl = null;
		mImageCache.removeHandler(handler);

		if (bitmap == null){
			return;
		}
		
		if (mBitmapGone && bitmap == mBitmapNotFound){
			setVisibility(View.GONE);
		}
		else if (mBitmapInvisible && bitmap == mBitmapNotFound){
			setVisibility(View.INVISIBLE);
		}
		else{
			if (mImageListener != null){
				mImageListener.onImageDisplay(mImage, bitmap);
			}
			else{
				mImage.setImageBitmap(bitmap);
			}
		}
	}
	
	
	public void setImage(final CharSequence imageUrl){
		if (imageUrl == null) {
			displayImage(mBitmapNotFound);
			return;
		}

		setImage(imageUrl.toString(), null, true);
	}
	
	public void setImage(final String imageUrl, String alternativeUrl){
		setImage(imageUrl, alternativeUrl, true);
	}

	
	/**
	 * Set's the view's bitmap, this uses the internet to retrieve the image
	 * don't forget to add the correct permissions to your manifest
	 * @param imageUrl the url of the image you wish to load
	 */
	public void setImage(final String imageUrl, String alternativeUrl, boolean displaySpinner)
	{
		if (imageUrl == null) {
			displayImage(mBitmapNotFound);
			return;
		}
		
		if (!checkUrl(imageUrl)) {
			return;
		}
		
		mDownloadingUrl			= imageUrl;
		mAlternativeUrl			= alternativeUrl;
		mDisplayLoadingSection	= displaySpinner;

		// FrameLayout is visible by default
		setVisibility(View.VISIBLE);

		// No spinner before download
		mSpinner.setVisibility(View.INVISIBLE);

		// No image in bg by default
		mImage.setImageDrawable(null);

		if (mDisplayLoadingSection){
			mSpinner.setVisibility(View.VISIBLE);
			
			if (mBitmapLoading != null){
				mImage.setImageBitmap(mBitmapLoading);
			}
		}
		
		Bitmap bitmapCached = mImageCache.getBitmap(imageUrl, mAlternativeUrl, handler);
		if (bitmapCached != null){
			displayImage(bitmapCached);
		}
	}
	
	public void setImageListener(ImageListener imgListener){
		mImageListener = imgListener;
	}
	
	public static interface ImageListener {
		public void onImageDisplay(ImageView iv, Bitmap bmp);
	}
	
	public void setScaleType(ScaleType type){
		mImage.setScaleType(type);
	}
}