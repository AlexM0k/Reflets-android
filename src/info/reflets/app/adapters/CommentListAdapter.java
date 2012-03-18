package info.reflets.app.adapters;

import info.reflets.app.R;
import info.reflets.app.model.Comment;

import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CommentListAdapter extends ArrayAdapter<Comment> implements ImageGetter{

	private LayoutInflater	mLayoutInflater;
	Activity mActivity;
	
	public CommentListAdapter(Activity activity, int textViewResourceId) {
		super(activity, textViewResourceId);
		mActivity = activity;
		mLayoutInflater = LayoutInflater.from(activity);
	}

	public void addComments(List<Comment> comments){
		clear();
		
		for (Comment c : comments)
			add(c);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewWrapper holder = null;
		Comment comment = getItem(position);
		
		if (convertView != null) {
			// Recycling view	
			holder = (ViewWrapper) convertView.getTag();
			
		} else {
			// Creating new view
			holder = new ViewWrapper();
			
			convertView = mLayoutInflater.inflate( R.layout.comment_item, null);
			
			holder.title 		= (TextView) convertView.findViewById(R.id.comment_title);
			holder.date 		= (TextView) convertView.findViewById(R.id.comment_date);
			holder.content 	= (TextView) convertView.findViewById(R.id.comment_content);
						
			convertView.setTag(holder);
			
		}
		
		// Setting article information
		if (comment != null){
			
			holder.title.setText(comment.getTitle());
			holder.date.setText( comment.getDate());
			
			holder.content.setText(Html.fromHtml(comment.getContent(), this, null));
		}
		
		return convertView;
	}
	
	// Wrapper
		class ViewWrapper {
			TextView title;
			TextView date;
			TextView content;
		}

		public Drawable getDrawable(String arg0) {
			return mActivity.getResources().getDrawable(R.drawable.empty);
		}
}
