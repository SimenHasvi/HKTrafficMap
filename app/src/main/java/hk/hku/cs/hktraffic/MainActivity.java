package hk.hku.cs.hktraffic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map; //the google map object to be displayed
    private View layout; // the activity main layout
    private ImageHandler imageHandler = new ImageHandler(this); //image handler for the map camera images
    private ArrayList<Marker> markerList = new ArrayList<>(); //list of all the markers on the map
    private Boolean markersVisible = false; //is the markers visible?
    private LocationManager locationManager; //manage the current location
    private DisplayMetrics displayMetrics = new DisplayMetrics(); //used to get the dimensions of the screen for scaling

    /**
     * Specify actions to happen as this activity is created
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // display the layout
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.activity_main);

        // set up the map
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fmMap);
        supportMapFragment.getMapAsync(this);

    }

    /**
     * Specify actions to do when the map fragment is ready
     * @param googleMap the map object representing the map
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // set up the map with styling
        map = googleMap;
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        map.setTrafficEnabled(true);
        setMapIdleListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setAllGesturesEnabled(true);

        // move the map camera to HK, or current location
        Location currentLocation = getCurrentLocation();
        if (currentLocation != null) {
            LatLng gps = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(gps, 12));
        }
        else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(22.3193, 114.1694), 12));
        }

        // add all the markers
        addTrafficCameraMarkers();

        // set the info window of all the markers
        CustomInfoWindow infoWindow = new CustomInfoWindow(this, imageHandler);
        map.setInfoWindowAdapter(infoWindow);
    }

    /**
     * Get the location of the user
     * @return the location of the user, null if it can not be obtained
     */
    private Location getCurrentLocation() {
        // which methods can we use to find the location?
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Location location = null;

        // check that necessary permissions are in place
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Location Update", "missing permissions");
            checkAndRequestPermissions();
            return null;
        }

        // location listener, we don't really need it to do anything
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        // get location from network
        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener);
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        // get location from GPS
        if (isNetworkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        return location;
    }

    /**
     * Set a listener which will remove the markers if zoomed out too far
     */
    private void setMapIdleListener() {
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                int zoom = (int) map.getCameraPosition().zoom;
                if (zoom >= 12 && !markersVisible) {
                    markersVisible = true;
                    for (Marker m : markerList) {
                        m.setVisible(true);
                    }
                }
                else if (zoom < 12 && markersVisible) {
                    markersVisible = false;
                    Snackbar.make(layout, "Zoom in to see the cameras", Snackbar.LENGTH_INDEFINITE).show();
                    for (Marker m : markerList) {
                        m.setVisible(false);
                    }
                }
            }
        });
    }

    /**
     * add all the camera markers from the online database
     * will also provide the imageHandler a target so that it can start buffering the images
     */
    private void addTrafficCameraMarkers() {
        // run on a separate thread since we are downloading from internet.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Retrieve and parse the XML file
                    URL TrafficCameraURL = new URL(getResources().getString(R.string.traffic_camera_url));
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document TrafficCameraDoc = db.parse(TrafficCameraURL.openStream());

                    // Add the items in the document to the map
                    NodeList imageList = TrafficCameraDoc.getDocumentElement().getElementsByTagName("image");

                    // Get the pin icon and resize it
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int height = displayMetrics.heightPixels;
                    int width = displayMetrics.widthPixels;
                    BitmapDrawable icon = (BitmapDrawable)getResources().getDrawable(R.drawable.camera_pin);
                    Bitmap iconBitmap = icon.getBitmap();
                    final Bitmap smallIconBitmap = Bitmap.createScaledBitmap(iconBitmap, width/12, width/12, false);

                    for (int i = 0; i < imageList.getLength(); i++) {
                        Node image = imageList.item(i);

                        // fill in camera info hashmap which will be the tag of the marker
                        final HashMap<String, String> cameraInfo = new HashMap<>();
                        cameraInfo.put("key", ((Element) image).getElementsByTagName("key").item(0).getTextContent());
                        cameraInfo.put("region", ((Element) image).getElementsByTagName("region").item(0).getTextContent());
                        cameraInfo.put("description", ((Element) image).getElementsByTagName("description").item(0).getTextContent());
                        cameraInfo.put("latitude", ((Element) image).getElementsByTagName("latitude").item(0).getTextContent());
                        cameraInfo.put("longitude", ((Element) image).getElementsByTagName("longitude").item(0).getTextContent());
                        cameraInfo.put("url", ((Element) image).getElementsByTagName("url").item(0).getTextContent());

                        // add the marker on the UI thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LatLng cameraPos = new LatLng(Double.parseDouble(cameraInfo.get("latitude")), Double.parseDouble(cameraInfo.get("longitude")));
                                Marker marker = map.addMarker(new MarkerOptions()
                                        .position(cameraPos)
                                        .title(cameraInfo.get("description"))
                                        .snippet("Loading image failed!")
                                        .icon(BitmapDescriptorFactory.fromBitmap(smallIconBitmap))
                                        .visible(markersVisible));
                                marker.setTag(cameraInfo);
                                markerList.add(marker);

                                // buffer the image in the imageHandler
                                imageHandler.addNewTarget(cameraInfo.get("url"));
                                imageHandler.bufferTarget(cameraInfo.get("url"));
                            }
                        });
                    }
                }
                catch (Exception e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * check if the necessary permissions are in place, if not, ask for them
     * @return are the permissions in place or not?
     */
    private boolean checkAndRequestPermissions() {
        int internet = ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET);
        int loc = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        int loc2 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        ArrayList<String> listPermissionsNeeded = new ArrayList<>();

        if (internet != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.INTERNET);
        }
        if (loc != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (loc2 != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions((Activity) this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), 1);
            return false;
        }
        return true;
    }
}
