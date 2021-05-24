package hk.hku.cs.hktraffic;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.util.HashMap;


/**
 * Handle all the images so that we can retrieve them later
 */
public class ImageHandler {


    private final HashMap<String, Bitmap> images = new HashMap<>(); // images with url as key
    private final HashMap<String, Target> imageTargets = new HashMap<>(); // targets with url as key
    private DisplayMetrics displayMetrics = new DisplayMetrics(); // for scaling purposes
    private Context context; // the view creating the instance, needed for screen size

    /**
     * Simple constructior
     * @param context the view calling this
     */
    ImageHandler(Context context) {
        this.context = context;
    }

    /**
     * add a new target which we need to download the images
     * @param url the url of the image for this target
     */
    public void addNewTarget(final String url) {
        imageTargets.put(url, new Target() {
            @Override
            //success
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                //scale the image to the screen size
                ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int height = displayMetrics.heightPixels;
                int width = displayMetrics.widthPixels;
                images.put(url, Bitmap.createScaledBitmap(bitmap, width/2, width/2, false));

                Log.d("Image Loaded", url);
            }
            //failed
            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Log.d("Image Failed", String.valueOf(e));
            }
            //loading
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Log.d("Preparing Image", url);
            }
        });
    }

    /**
     * buffer all the targets
     */
    public void bufferTarget(){
        for (String url : imageTargets.keySet()) {
                Picasso.get().load(url).into(imageTargets.get(url));
        }
    }

    /**
     * buffer the target of the provided url
     * @param url the url for the image to buffer
     */
    public void bufferTarget(String url){
        for (String targetUrl : imageTargets.keySet()) {
            if (url == targetUrl) {
                Picasso.get().load(url).into(imageTargets.get(url));
            }
        }
    }

    /**
     * get the buffered image of the provided url
     * @param url the url for the image to retrieve
     * @return the image
     */
    public Bitmap getImage(String url) {
        return images.get(url);
    }

}
