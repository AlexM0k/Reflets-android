package info.reflets.app;

import info.reflets.app.model.Article;
import info.reflets.app.utils.Tools;

import java.io.InputStream;
import java.net.URL;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/***
 * Article activity
 *
 */
public class ArticleActivity extends Activity implements ImageGetter {

	final static String LOG_TAG = ArticleActivity.class.getSimpleName();
	
	private final static int IMAGE_MARGIN = 20;
	
	public final static String EXTRA_ARTICLE = "EXTRA_ARTICLE";
		
	Article mArticle;	
	
	TextView 	contentView;
	Spanned 	spannedContent;
	ProgressBar progressBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_item);
		
		mArticle = getIntent().getParcelableExtra(EXTRA_ARTICLE);
		
		// Setting article title, date, author
		TextView titleView = (TextView) findViewById(R.id.article_title);
		titleView.setText(mArticle.getTitle());
		
		TextView dateView = (TextView) findViewById(R.id.article_date);
		dateView.setText(mArticle.getDate());
		
		TextView authorView = (TextView) findViewById(R.id.article_author);
		authorView.setText(mArticle.getAuthor());
		
		contentView = (TextView) findViewById(R.id.article_content);
		progressBar = (ProgressBar) findViewById(R.id.article_progress);
		
		// Setting text content and retrieving images in a thread 
		new Thread(new Runnable() {
			
			public void run() {
				spannedContent = Html.fromHtml(mArticle.getContent(), ArticleActivity.this, null); 			
				
				runOnUiThread(new Runnable() {
					public void run() {
						// Setting text content
						contentView.setText(spannedContent);
						contentView.setMovementMethod(LinkMovementMethod.getInstance());

						// Hiding progress bar
						progressBar.setVisibility(View.GONE);
					}
				});
					
			}
		}).start();
		
	}

	/***
	 * Downloading image
	 */
	public Drawable getDrawable(String source) {
		try{
			Log.d(LOG_TAG, "Loading image : "+ source);		
			
			InputStream stream = (InputStream) new URL(source).getContent();
			
			Drawable d = Drawable.createFromStream(stream, "src");
			
			if (d.getIntrinsicWidth() > Tools.DEVICE_WIDTH - IMAGE_MARGIN){
				
			}
			else
				d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
	
			return d;
	
		}catch (Exception e){
			Log.d(LOG_TAG, "error:" +e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}
