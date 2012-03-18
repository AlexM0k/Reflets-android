package info.reflets.app;

import info.reflets.app.adapters.CommentListAdapter;
import info.reflets.app.model.Comment;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

public class CommentActivity extends Activity {

	public final static String EXTRA_TITLE = "EXTRA_TITLE";
	
	public static List<Comment> comments;
	
	String articleTile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.comment_list);
		
		articleTile = getIntent().getStringExtra(EXTRA_TITLE);
		
		if (comments != null){
			TextView title = (TextView) findViewById(R.id.article_title);
			title.setText(articleTile);
			
			
			ListView list = (ListView) findViewById(R.id.comment_list);
			
			CommentListAdapter adapter = new CommentListAdapter(this, 0);
			
			list.setAdapter(adapter);
			adapter.addComments(comments);
		}
	}
}
