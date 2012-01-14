package info.reflets.app.utils;

import java.io.InputStream;
import java.net.URL;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

public class ImageDownloader {

	public static void downloadImage(final Activity activity, final ImageView view, final String url){

		new Thread( new Runnable() {

			public void run() {
				try {

					InputStream stream = (InputStream) new URL(url).getContent();

					final Drawable drawable = Drawable.createFromStream(stream, "src");

					activity.runOnUiThread(new Runnable() {

						public void run() {
							if (view != null && drawable != null)
							{
								view.setImageDrawable(drawable);
								
								if (view.getVisibility() != View.VISIBLE)
									view.setVisibility(View.VISIBLE);
							}
						}
					});

				} catch (Exception e) {

				}		
			}
		}).start();

	}
}
