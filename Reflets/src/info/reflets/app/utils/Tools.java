package info.reflets.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.WindowManager;


public class Tools {

	public static final String RSS_URL = "http://feeds.feedburner.com/refletsinfo?format=xml";
	
	private static final String RSS_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";
	
	public static int DEVICE_WIDTH;

	public static void init(Context context){
		WindowManager mWinMgr = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		DEVICE_WIDTH = mWinMgr.getDefaultDisplay().getWidth();
	}
	
	
	/***
	 * Parse date from the rss format
	 * @param dateStr
	 * @return
	 */
	public static Calendar parseDate(String dateStr){
		SimpleDateFormat dFormat = new SimpleDateFormat(RSS_DATE_FORMAT, Locale.US);					
		Calendar calendar = Calendar.getInstance();
		
		try {
			// Parsing date
			Date date = dFormat.parse(dateStr);
			calendar.setTime(date);			
		} catch (Exception e) {
			Log.d("Tools", e.getMessage());
		}
		
		return calendar;
	}
	
	/***
	 * True if network is available, otherwise false
	 * @param context
	 * @return
	 */
	public static boolean isConnected(Context context) {

	    NetworkInfo info = (NetworkInfo) ((ConnectivityManager) context
	            .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

	    return (info != null && info.isConnected());
	}
}
