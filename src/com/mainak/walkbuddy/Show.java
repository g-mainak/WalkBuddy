package com.mainak.walkbuddy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Show extends Activity implements OnClickListener {

	private TextView self_location_text;
	private TextView b1_location_text;
	private TextView b2_location_text;
	
	private LocationManager mLocationManager;
	private Handler mHandler;
	private Handler tHandler;

	private long mStartTime = 0L;
	public static final String PREFS_NAME = "MyPrefsFile";
	
	private String taddress, netid;
	private int update_interval;
	private String[] buddies;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		taddress = settings.getString("taddress", "");
		netid = settings.getString("netid", "");
		update_interval = settings.getInt("update_interval", 20)*1000;
		buddies = new String[2];
		buddies[0] = settings.getString("buddy1", "");
		buddies[1] = settings.getString("buddy2", "");
		
		b1_location_text = (TextView)findViewById(R.id.b1_location);
		b2_location_text = (TextView)findViewById(R.id.b2_location);
		
		Button button = (Button) findViewById(R.id.start);
        button.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_show, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View arg0) {
		Button b = (Button)findViewById(R.id.start);
		if (b.getText().toString().equalsIgnoreCase("Start"))
		{
			self_location_text = (TextView)findViewById(R.id.self_location);

			mHandler = new Handler() {
				public void handleMessage(Message msg) 
				{
					self_location_text.setText((String) msg.obj);
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
			Toast.makeText(this, gpsEnabled?"GPSENabled":"GPSDisabled", Toast.LENGTH_LONG).show();
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
				Log.v("Exception google search","Exception:" + e.getMessage());
			}
			tHandler.postDelayed(this, update_interval);
		}
	};

	/*@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		Toast.makeText(this, gpsEnabled?"GPSENabled":"GPSDisabled", Toast.LENGTH_LONG).show();
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


	protected void displayUpdates()
	{
		Button b = (Button)findViewById(R.id.start);
		b.setText(getResources().getString(R.string.stop));
		self_location_text.setText("Initializing...");
		updateUILocation(requestUpdatesFromProvider());
	}


	private Location requestUpdatesFromProvider()
	{
		Location location = null;
		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) 
		{
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, listener);
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
			Message.obtain(mHandler, 1, location.getLatitude()*1e6 + "," + location.getLongitude()*1e6).sendToTarget();
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
				Log.v("Exception google search", "Exception:" + e.getMessage());
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
				Log.v("Exception google search", "Exception:" + e.getMessage());
			}

		}

		// func to construct search
		public String searchRequest(String[] netList) throws MalformedURLException, IOException 
		{
			String listOfnetids = netList[0];
			for(int i=1; i< netList.length; i++)
				listOfnetids = listOfnetids + "," + netList[i];
			String newFeed = "http://"+taddress+"/wb?me=" + netid + "&meV=" + self_location_text.getText().toString() + "&buddies=" + listOfnetids;
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
			Object obj = jobj.get(buddies[0]);
			b1_location_text.setText(obj.toString());
			obj = jobj.get(buddies[1]);
			b2_location_text.setText(obj.toString());			
		}
	}
}	
