package com.mainak.walkbuddy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class MapsActivity extends MapActivity 
{

	private LocationManager mLocationManager;
	private Handler mHandler;
	private Handler tHandler;

	private static final int ENABLE_GPS_REQUEST_CODE = 1001;
	private long mStartTime = 0L;
	public static final String PREFS_NAME = "MyPrefsFile";

	private String taddress, netid;
	private int update_interval;
	private String[] buddies;

	public static final String TAG = "CS434";
	// get lat/long from http://itouchmap.com/latlong.html
	public static final GeoPoint ZOO = new GeoPoint(41313104, -72925218); // GeoPoint uses microdegree as unit
	public static final int DEFAULT_ZOOM = 16;

	private MapView m_wbMap;
	private List<Overlay> m_mapOverlays;
	private MyLocationOverlay m_myLocationOverlay;
	private BuddyOverlay itemizedOverlay;
	private PathOverlay m_pathOverlay;

	private MapController m_mapController;

	int[][] prevCoordinates;

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		taddress = settings.getString("taddress", "");
		netid = settings.getString("netid", "");
		update_interval = settings.getInt("update_interval", 20)*1000;
		buddies = new String[2];
		buddies[0] = settings.getString("buddy1", "");
		buddies[1] = settings.getString("buddy2", "");


		m_wbMap = (MapView) findViewById(R.id.m_wbMap);
		m_wbMap.setBuiltInZoomControls(true);

		m_mapController = m_wbMap.getController();
		m_mapController.setCenter(ZOO);
		m_mapController.setZoom(DEFAULT_ZOOM);
		Log.d(TAG, "" + m_wbMap);

		m_mapOverlays = m_wbMap.getOverlays();

		// add self overlay
		m_myLocationOverlay = new MyLocationOverlay(this, m_wbMap);
		m_myLocationOverlay.enableCompass();
		m_myLocationOverlay.enableMyLocation();
		m_mapOverlays.add(m_myLocationOverlay);

		// add path overlay
		m_pathOverlay = new PathOverlay();
		m_mapOverlays.add(m_pathOverlay);
		Drawable drawable = this.getResources().getDrawable(R.drawable.buddymarker);
		itemizedOverlay = new BuddyOverlay(drawable, this);


		int[][] pc = {{41313104, -72925218}, {41313104, -72925218}, {41313104, -72925218}, {41313104, -72925218}, {41313104, -72925218}};
		prevCoordinates = pc;
	}

	public void buttonClick(View v)
	{
		Button b = (Button)findViewById(R.id.control_btn);
		if (b.getText().toString().equalsIgnoreCase("Start"))
		{
			mHandler = new Handler() {
				public void handleMessage(Message msg) 
				{
					String str = "";
					for(int i=3; i>=0; i--)
					{
						prevCoordinates[i+1][0] = prevCoordinates[i][0];	 
						prevCoordinates[i+1][1] = prevCoordinates[i][1];
					}
					prevCoordinates[0][0] = ((GeoPoint)msg.obj).getLatitudeE6();
					prevCoordinates[0][1] = ((GeoPoint)msg.obj).getLongitudeE6();
					for(int i=0; i<5; i++)
						str = str + prevCoordinates[i][0] +","+ prevCoordinates[i][1] + "\n";
					//Toast.makeText(MapsActivity.this, str, Toast.LENGTH_LONG).show();
				}
			};

			tHandler = new Handler();
			if (mStartTime == 0L) 
			{
				tHandler.removeCallbacks(mUpdateTimeTask);
				tHandler.postDelayed(mUpdateTimeTask, update_interval);
			}

			mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

			if (!gpsEnabled) 
			{
				Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(settingsIntent);
			}
			displayUpdates();
		}
		else
		{
			b.setText(getResources().getString(R.string.start));
			mLocationManager.removeUpdates(listener);
			tHandler.removeCallbacks(mUpdateTimeTask);
		}
	}

	protected void displayUpdates()
	{
		Button b = (Button)findViewById(R.id.control_btn);
		b.setText(getResources().getString(R.string.stop));
		updateUILocation(requestUpdatesFromProvider());
	}

	/*@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		if (requestCode == ENABLE_GPS_REQUEST_CODE && gpsEnabled)
			displayUpdates();
		else if (requestCode == ENABLE_GPS_REQUEST_CODE && !gpsEnabled && data.getBundleExtra("tries").getInt("tries")<3)
		{
			Toast.makeText(this, data.getBundleExtra("tries").getInt("tries"), Toast.LENGTH_LONG).show();
			Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			settingsIntent.putExtra("tries", data.getBundleExtra("tries").getInt("tries")+1);
			startActivityForResult(settingsIntent, ENABLE_GPS_REQUEST_CODE);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}*/


	private class PathOverlay extends Overlay {

		private Paint m_paint;
		Projection m_projection;

		public PathOverlay() {
			super();
			m_paint = new Paint();
			m_paint.setColor(Color.RED);
			m_paint.setStrokeWidth(4);
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {

			m_projection = mapView.getProjection();
			for(int i=0; i<4; i++)
			{
				GeoPoint gp1  = new GeoPoint(prevCoordinates[i][0], prevCoordinates[i][1]);
				GeoPoint gp2  = new GeoPoint(prevCoordinates[i+1][0], prevCoordinates[i+1][1]);
				myDrawLine(canvas, gp1, gp2);
			}

		}

		public void myDrawLine(Canvas canvas, GeoPoint gp1, GeoPoint gp2) {

			Point p1 = new Point();
			Point p2 = new Point();

			m_projection.toPixels(gp1, p1);
			m_projection.toPixels(gp2, p2);

			canvas.drawLine(p1.x, p1.y, p2.x, p2.y, m_paint);

		}
	}


	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() 
		{            
			updateUILocation(requestUpdatesFromProvider());
			try 
			{
				new serverTask().execute();
			} 
			catch (Exception e) 
			{
				Log.v("Exception on google search","Exception:" + e.getMessage());
			}
			tHandler.postDelayed(this, update_interval);
		}
	};

	private Location requestUpdatesFromProvider()
	{
		Location location = null;
		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) 
		{
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, update_interval, 0, listener);
			location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} 
		else 
		{
			Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
		}
		return location;
	}

	private void updateUILocation(Location location) {
		// We're sending the update to a handler which then updates the UI with the new
		// location.
		if (location != null)
			Message.obtain(mHandler, 1, new GeoPoint((int)(location.getLatitude()*1e6), (int)(location.getLongitude()*1e6))).sendToTarget();
	}

	private final LocationListener listener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// A new location update is received.  Do something useful with it.  Update the UI with
			// the location update.
			updateUILocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	private class serverTask extends AsyncTask<String, Integer, String> 
	{

		protected String doInBackground(String... searchKey) 
		{
			try 
			{
				return searchRequest(buddies);
			} 
			catch (Exception e) 
			{
				Log.v("Exception in google search", "Exception:" + e.getMessage());
				return "";
			}
		}

		protected void onPostExecute(String result)
		{
			try 
			{
				processResponse(result);
			} 
			catch (Exception e) 
			{
				Log.v("Exception of google search", "Exception:" + e.getMessage());
			}

		}

		// func to construct search
		public String searchRequest(String[] netList) throws MalformedURLException, IOException 
		{
			String listOfnetids = netList[0];
			for(int i=1; i< netList.length; i++)
				listOfnetids = listOfnetids + "," + netList[i];
			String newFeed = "http://"+taddress+"/wb?me=" + netid + "&meV=" + prevCoordinates[0][0]+":"+ prevCoordinates[0][1] + "&buddies=" + listOfnetids;
			StringBuilder response = new StringBuilder();
			Log.v("gsearch", "gsearch url:" + newFeed);
			URL url = new URL(newFeed);

			HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();

			if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) 
			{
				BufferedReader input = new BufferedReader(new InputStreamReader(httpconn.getInputStream()), 8192);
				String strLine = null;
				while ((strLine = input.readLine()) != null) 
				{
					response.append(strLine);
				}
				input.close();
			}
			return response.toString();
		}

		public void processResponse(String resp) throws IllegalStateException, IOException, JSONException, NoSuchAlgorithmException 
		{
			StringBuilder sb = new StringBuilder();
			Log.v("gsearch", "gsearch result:" + resp);
			JSONObject jobj  = new JSONObject(resp);
			Log.v("buddy", "b1 " + buddies[0]);
			Log.v("buddy", "b2 " + buddies[1]);
			OverlayItem overlayitem1= null, overlayitem2=null;
			if(!buddies[0].equals(""))
			{
				Object obj = jobj.get(buddies[0]);
				Log.v("buddy", "jsonobject " + obj.toString());
				String[] request = obj.toString().split(":");
				GeoPoint point = new GeoPoint(Integer.parseInt(request[0]), Integer.parseInt(request[1]));
				Log.v("gsearch", "json result:" );
				overlayitem1 = new OverlayItem(point, buddies[0], point.toString());
				itemizedOverlay.removeOverlay(0);
			}
			if(!buddies[1].equals(""))
			{
				Object obj = jobj.get(buddies[1]);
				Log.v("buddy", "request2 " + obj.toString());
				String[] request = obj.toString().split(":");
				Log.v("buddy", "request2 result:" + request[0]+","+request[1]);
				GeoPoint point = new GeoPoint(Integer.parseInt(request[0]), Integer.parseInt(request[1]));
				overlayitem2 = new OverlayItem(point, buddies[1], point.toString());
				itemizedOverlay.removeOverlay(0);
			}
			if (overlayitem1!=null ) itemizedOverlay.addOverlay(overlayitem1);
			if (overlayitem2!=null ) itemizedOverlay.addOverlay(overlayitem2);
			if (itemizedOverlay.size()!=0)
				m_mapOverlays.add(itemizedOverlay);
			m_wbMap.postInvalidate();
		}
	}
}
