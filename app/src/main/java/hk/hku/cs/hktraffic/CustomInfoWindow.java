package hk.hku.cs.hktraffic;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import java.util.HashMap;

/**
 * specify how the info windows of the markers will be displayed
 */
public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {

    private Context context;
    private ImageHandler imageHandler;
    private ViewGroup infoWindow;
    private TextView title;
    private ImageView image;

    /**
     * constructor for this InfoWindowAdapter
     * @param ctx the view calling this
     * @param imageHandler imageHandler to get images from
     */
    public CustomInfoWindow(Context ctx, ImageHandler imageHandler){
        infoWindow = (ViewGroup) ((Activity)ctx).getLayoutInflater()
                .inflate(R.layout.marker_info_window, null);
        title = infoWindow.findViewById(R.id.marker_title);
        image = infoWindow.findViewById(R.id.marker_image);
        this.context = ctx;
        this.imageHandler = imageHandler;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    /**
     * customize the info window to the marker that has been clicked
     * @param marker the marker that is clicked
     * @return the view to display in the info window
     */
    @Override
    public View getInfoContents(Marker marker) {
        //get views and marker info
        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.marker_info_window, null);
        title = view.findViewById(R.id.marker_title);
        image = view.findViewById(R.id.marker_image);
        HashMap<String, String> markerInfo = (HashMap<String, String>) marker.getTag();

        // set the title
        title.setText(formatTitle(markerInfo.get("description")));

        // set the image
        image.setImageBitmap(imageHandler.getImage(markerInfo.get("url")));
        return view;
    }

    /**
     * format the title to better fit the info window
     * @param s the title to format
     * @return the formatted title
     */
    private String formatTitle(String s) {
        String title = s.substring(0, s.indexOf('['));
        if (title.length() >= 30) {
            title = title.substring(0, 29);
            title += "...";
        }
        return title;
    }

}
