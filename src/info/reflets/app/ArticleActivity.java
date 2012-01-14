package info.reflets.app;

import info.reflets.app.model.Article;
import info.reflets.app.utils.ImageDownloader;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/***
 * Article activity
 *
 */
public class ArticleActivity extends Activity implements ImageGetter {

	final static String LOG_TAG = ArticleActivity.class.getSimpleName();
	
	public final static String EXTRA_ARTICLE = "EXTRA_ARTICLE";
		
	Article mArticle;	
	
	TextView 	mContentView;
	ImageView	mImageView;
	
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
		
		mImageView = (ImageView) findViewById(R.id.article_image);
		
		mContentView = (TextView) findViewById(R.id.article_content);
		progressBar = (ProgressBar) findViewById(R.id.article_progress);
		
		ImageDownloader.downloadImage(this, mImageView, mArticle.getImage());
		
		// Setting text content and retrieving images in a thread 
		new Thread(new Runnable() {
			
			public void run() {
				
				spannedContent = Html.fromHtml(mArticle.getContent(), ArticleActivity.this, null); 			
				
				runOnUiThread(new Runnable() {
					public void run() {
						// Setting text content
						mContentView.setText(spannedContent);
						mContentView.setMovementMethod(LinkMovementMethod.getInstance());

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
		return getResources().getDrawable(R.drawable.empty);
	}
	
	
}
