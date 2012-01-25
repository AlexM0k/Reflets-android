package info.reflets.app.adapters;

import info.reflets.app.R;
import info.reflets.app.model.Article;
import info.reflets.app.utils.ImageAdvancedView;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/***
 * Adapter for the main list
 * @author Alexandre
 *
 */
public class ArticleListAdapter extends ArrayAdapter<Article> {

	private LayoutInflater	mLayoutInflater;
	Activity mActivity;
	
	public ArticleListAdapter(Activity activity, int textViewResourceId) {
		super(activity, textViewResourceId);
		mActivity = activity;
		mLayoutInflater = LayoutInflater.from(activity);
	}
	
	/***
	 * Adding articles
	 * @param headers
	 */
	public void addHeaders(List<Article> headers){
		clear();
		
		for (Article h : headers)
			add(h);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewWrapper holder = null;
		Article article = getItem(position);
		
		if (convertView != null) {
			// Recycling view	
			holder = (ViewWrapper) convertView.getTag();
			
		} else {
			// Creating new view
			holder = new ViewWrapper();
			
			convertView = mLayoutInflater.inflate( R.layout.header_item, null);
			
			holder.title 		= (TextView) convertView.findViewById(R.id.header_title);
			holder.date 		= (TextView) convertView.findViewById(R.id.header_date);
			holder.author 		= (TextView) convertView.findViewById(R.id.header_author);
			holder.description 	= (TextView) convertView.findViewById(R.id.header_description);
			holder.image		= (ImageAdvancedView) convertView.findViewById(R.id.header_image);
						
			convertView.setTag(holder);
			
		}
		
		// Setting article information
		if (article != null){
			
			holder.title.setText(article.getTitle());
			holder.date.setText( article.getDate());
			holder.author.setText(article.getAuthor());
			holder.description.setText(article.getDescription());
			holder.image.setImage(article.getImage());
		}
		
		return convertView;
	}
	
	// Wrapper
	class ViewWrapper {
		TextView title;
		TextView author;
		TextView date;
		TextView description;
		ImageAdvancedView image;
	}
}
