package info.reflets.app;

import info.reflets.app.model.Article;
import info.reflets.app.utils.HorizontalPager;
import info.reflets.app.utils.HorizontalPager.OnScreenSwitchListener;
import info.reflets.app.utils.ImageAdvancedView;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/***
 * Article activity
 *
 */
public class ArticleActivity extends Activity implements OnScreenSwitchListener, ImageGetter  {

	final static String LOG_TAG = ArticleActivity.class.getSimpleName();
	
	public final static String EXTRA_ARTICLE_POSITION = "EXTRA_ARTICLE_POSITON";
	
	HorizontalPager mPager;
	ImageView		mArrowLeft;
	ImageView		mArrowRight;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_view);
		
		mArrowLeft = (ImageView) findViewById(R.id.arrow_left);
		mArrowRight = (ImageView) findViewById(R.id.arrow_right);
		
		mPager = (HorizontalPager) findViewById(R.id.pager);
		mPager.setNbChild(0, StartActivity.mArticles.size());
		mPager.setOnScreenSwitchListener(this, getIntent().getIntExtra(EXTRA_ARTICLE_POSITION, 0));
	}

	public void onExitView(int position, View ConvertView) {
		
	}

	public void onDisplayView(int position, View ConvertView) {
		
		mArrowLeft.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
		mArrowRight.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
		mArrowLeft.setVisibility(View.INVISIBLE);
		mArrowRight.setVisibility(View.INVISIBLE);

	}

	public View getView(int position, View ConvertView) {
		final Article article = StartActivity.mArticles.get(position);
		
		View articleView  = View.inflate(this, R.layout.article_item, null);
		
		// Setting article title, date, author
		TextView titleView = (TextView) articleView.findViewById(R.id.article_title);
		titleView.setText(article.getTitle());
		
		TextView dateView = (TextView) articleView.findViewById(R.id.article_date);
		dateView.setText(article.getDate());
		
		TextView authorView = (TextView) articleView.findViewById(R.id.article_author);
		authorView.setText(article.getAuthor());
		
		ImageAdvancedView mImageView = (ImageAdvancedView) articleView.findViewById(R.id.article_image);
		mImageView.setImage(article.getImage());
		
		TextView mContentView = (TextView) articleView.findViewById(R.id.article_content);
		ProgressBar progressBar = (ProgressBar) articleView.findViewById(R.id.article_progress);
		
		
		// Setting text content and retrieving images in a thread 
		Spanned spannedContent = Html.fromHtml(article.getContent(), ArticleActivity.this, null); 	

		// Setting text content
		mContentView.setText(spannedContent);
		mContentView.setMovementMethod(LinkMovementMethod.getInstance());

		// Hiding progress bar
		progressBar.setVisibility(View.GONE);

		return articleView;
	}
	
	
	/***
	 * Downloading image
	 */
	public Drawable getDrawable(String source) {
		return getResources().getDrawable(R.drawable.empty);
	}
	
	
}
