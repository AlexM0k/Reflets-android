package info.reflets.app.dao;

import info.reflets.app.R;
import info.reflets.app.model.Comment;
import info.reflets.app.parsing.CommentParser;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.ClientProtocolException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class CommentTask extends AsyncTask<Void, Void, Boolean>{

	public interface OnCommentTaskListener {
		public void onCommentsDownloaded(boolean result, ArrayList<Comment> comments);
	}
	
	Context 		mContext;
	
	// A waiting dialog is shown
	boolean 		mShowDialog = false;
	ProgressDialog 	mDialog;
	
	// Comments list
	ArrayList<Comment> 	mComments;
	
	// Comments Url
	String mUrl;
	
	OnCommentTaskListener		mCallback;
	
	public CommentTask(Context context, String url, boolean showDialog, OnCommentTaskListener callback){
		mUrl		= url;
		mContext 	= context;
		mShowDialog = showDialog;
		mCallback	= callback;
	}
	
	@Override
	protected void onPreExecute() {

		if (mShowDialog){
			mDialog = new ProgressDialog(mContext);
			mDialog.setCancelable(false);
			mDialog.setMessage(mContext.getString(R.string.loading));
			mDialog.show();
		}
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {
		
		
		try {
			URL serverAddress = new URL(mUrl);
			URLConnection connection = serverAddress.openConnection();
			connection.connect();
		
			// Parsing
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			
			CommentParser handler = new CommentParser();
			parser.parse(connection.getInputStream(), handler);
			
			mComments = handler.getComments();			
			
		}
		catch (ClientProtocolException e){
			return false;
		}
		catch (IOException e){
			return false;
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		if (mShowDialog)
			mDialog.dismiss();
		
		if (mCallback != null)
			mCallback.onCommentsDownloaded(result, mComments);
	}

}
