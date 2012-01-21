package info.reflets.app.dao;

import info.reflets.app.model.Article;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class DataCache {

	private final static String CACHE_FILE = "reflets.cache";

	/***
	 * Saving articles to cache
	 * @param context
	 * @param data
	 */
	public static void save(Context context, List<Article> data){
	
		try {
			
			FileOutputStream stream = context.openFileOutput(CACHE_FILE, Context.MODE_PRIVATE);
			
			// Saving in json
			GsonBuilder gsonb = new GsonBuilder();
			Gson gson = gsonb.create();
				
			stream.write(gson.toJson(data).getBytes());
			
			stream.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public static List<Article> getMergedList(Context context, List<Article> list){
			// Loading actual cache
			List<Article> cache = load(context);
			
			// If cache is not empty merging new data with cache
			if (cache.size() > 0)
				merge(cache, list);
			else
				cache = list;
			
			return cache;
	}
	/***
	 * Merging a list into another
	 * @param initialList
	 * @param additionalList
	 */
	private static void merge(List<Article> initialList, List<Article> additionalList){
	
		for (Article article : additionalList){
			
			if (! isArticleInList(article, initialList))
			{
				initialList.add(article);
			}
		}
	}
	
	private static boolean isArticleInList(Article article, List<Article> list){
	
		for (Article a : list)
			if (a.getDate().equals(article.getDate()))
				return true;
		return false;
	}
	
	
	/***
	 * Loading all article from cache
	 * @param context
	 * @return
	 */
	public static List<Article> load(Context context){
		List<Article> list = new ArrayList<Article>();
		
		try {
			FileInputStream stream = context.openFileInput(CACHE_FILE);
			
			// Reading json string
			String json = IOUtils.toString(stream);
			
			// Parsing json to list of articles
			Gson gson = new Gson();

			Type collectionType = new TypeToken<List<Article>>(){}.getType();
			List<Article> articleList = gson.fromJson(json, collectionType);
			list.addAll(articleList);


		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return list;
	}
}
