package info.reflets.app.dao;


import info.reflets.app.R;
import info.reflets.app.model.Article;
import info.reflets.app.parsing.ArticleParser;
import info.reflets.app.utils.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class ArticleTask extends AsyncTask<Void, Void, Boolean> {

	public interface OnHeaderTaskListener {
		public void onDownloaded(boolean result, List<Article> headers);
	}
	
	Context 		mContext;
	
	boolean 		mShowDialog = false;
	ProgressDialog 	mDialog;
	List<Article> 	mArticles;
	
	OnHeaderTaskListener		mCallback;
	
	public ArticleTask(Context context, boolean showDialog, OnHeaderTaskListener callback){
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
			
			HttpGet httpGet = new HttpGet(Tools.RSS_URL);
			HttpClient httpclient = new DefaultHttpClient();

			// Execute HTTP Get Request
			HttpResponse response = httpclient.execute(httpGet);
			InputStream stream = response.getEntity().getContent();
		
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			
			ArticleParser handler = new ArticleParser();
			parser.parse(stream, handler);
			
			mArticles = handler.getArticles();
			mArticles = DataCache.getMergedList(mContext, mArticles);
			
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
			mCallback.onDownloaded(result, mArticles);
	}

}
