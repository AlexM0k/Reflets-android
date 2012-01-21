package info.reflets.app;

import info.reflets.app.adapters.ArticleListAdapter;
import info.reflets.app.dao.ArticleTask;
import info.reflets.app.dao.ArticleTask.OnHeaderTaskListener;
import info.reflets.app.dao.DataCache;
import info.reflets.app.model.Article;
import info.reflets.app.utils.Tools;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class StartActivity extends Activity implements OnHeaderTaskListener, OnItemClickListener {
    
	final static String LOG_TAG = StartActivity.class.getSimpleName();
	public static List<Article> mArticles;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setting splash screen
        setContentView(R.layout.splash);
        
        // Initialization
        Tools.init(this);
        
        new ArticleTask(this, false, this).execute();
    }

	/***
	 * Listener triggered when articles have been downloaded
	 */
	public void onDownloaded(boolean result, List<Article> headers) {
		
		// If success
		if (result){
			mArticles = headers;
			
			new Thread( new Runnable() {
				
				public void run() {
					// Saving in cache
					DataCache.save(StartActivity.this, mArticles);		
				}
			}).start();
			
		}
		else {
			// Setting error message wheter there is connectivity or not
			String message = getString(Tools.isConnected(this) ? R.string.error_generic : R.string.error_no_network);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			
			mArticles = DataCache.load(this);
		}
		
		// Loading articles list in view
		loadArticles();
	}
	
	/***
	 * Setting the menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menu_inflater = getMenuInflater();
		menu_inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/***
	 * Defining menu behaviour
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {

		// Refreshing
		case R.id.menu_refresh :
			new ArticleTask(this, true, this).execute();
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	/***
	 * Setting the list of articles
	 */
	private void loadArticles(){
		setContentView(R.layout.header_list);
		
		ListView listView = (ListView) findViewById(R.id.header_list);
		
		ArticleListAdapter adapter = new ArticleListAdapter(this, 0);
		adapter.addHeaders(mArticles);
		listView.setAdapter( adapter );
		listView.setOnItemClickListener(this);
	}

	/***
	 * Listener triggered when user click on an item in the list
	 */
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		
		Intent articleIntent = new Intent(this, ArticleActivity.class);
		articleIntent.putExtra(ArticleActivity.EXTRA_ARTICLE_POSITION, position);
		
		startActivity(articleIntent);
	}
}