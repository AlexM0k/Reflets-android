package info.reflets.app;

import info.reflets.app.dao.CommentTask;
import info.reflets.app.dao.CommentTask.OnCommentTaskListener;
import info.reflets.app.model.Article;
import info.reflets.app.model.Comment;
import info.reflets.app.utils.HorizontalPager;
import info.reflets.app.utils.HorizontalPager.OnScreenSwitchListener;
import info.reflets.app.utils.ImageAdvancedView;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/***
 * Article activity
 *
 */
public class ArticleActivity extends Activity implements OnScreenSwitchListener, ImageGetter, OnCommentTaskListener  {

	final static String LOG_TAG = ArticleActivity.class.getSimpleName();
	
	public final static String EXTRA_ARTICLE_POSITION 	= "EXTRA_ARTICLE_POSITON";
	public final static String EXTRA_ARTICLE_LIST		= "EXTRA_ARTICLE_LIST";
	
	List<Article>	mArticles;
	int				mCurrentPosition;
	HorizontalPager mPager;
	ImageView		mArrowLeft;
	ImageView		mArrowRight;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_view);
		
		mArticles = getIntent().getParcelableArrayListExtra(EXTRA_ARTICLE_LIST);
		
		mArrowLeft = (ImageView) findViewById(R.id.arrow_left);
		mArrowRight = (ImageView) findViewById(R.id.arrow_right);
		
		mCurrentPosition = getIntent().getIntExtra(EXTRA_ARTICLE_POSITION, 0);
		
		mPager = (HorizontalPager) findViewById(R.id.pager);
		mPager.setNbChild(0, mArticles.size());
		mPager.setOnScreenSwitchListener(this, mCurrentPosition);
	}

	@Override  
    public void startManagingCursor(Cursor c) {  
     if (c == null) {  
      throw new IllegalStateException("cannot manage cursor: cursor == null");  
     }  
     super.startManagingCursor(c);  
    }  
	
	/***
	 * Setting the menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menu_inflater = getMenuInflater();
		menu_inflater.inflate(R.menu.article_menu, menu);
		return true;
	}

	/***
	 * Defining menu behaviour
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {

		// Refreshing
		case R.id.menu_share :
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + mArticles.get(mCurrentPosition).getTitle());
			
			intent.putExtra(Intent.EXTRA_TEXT   , mArticles.get(mCurrentPosition).getShareDescription());
			
			try {
			    startActivity(Intent.createChooser(intent, getString(R.string.share)));
			} catch (android.content.ActivityNotFoundException ex) {
			   
			}
			break;
			
		case R.id.menu_comments : 
			new CommentTask(this, mArticles.get(mCurrentPosition).getCommentUrl(), true, this).execute();
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	public void onExitView(int position, View ConvertView) {
		
	}

	public void onDisplayView(int position, View ConvertView) {
		mCurrentPosition = position;
		
		if (position < mArticles.size() - 1) 
			mArrowRight.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));		
		
		if (position > 0)
			mArrowLeft.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));

	}

	public View getView(int position, View ConvertView) {
		final Article article = mArticles.get(position);
		
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

	public void onCommentsDownloaded(boolean result, ArrayList<Comment> comments) {
		CommentActivity.comments = comments;
		
		Intent intent = new Intent(this, CommentActivity.class);
		intent.putExtra(CommentActivity.EXTRA_TITLE, mArticles.get(mCurrentPosition).getTitle());
		startActivity(intent);
	}
	
	
}
