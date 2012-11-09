package com.mainak.walkbuddy;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity 
{
	public static final String PREFS_NAME = "MyPrefsFile";

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void buddiesContent(View view) 
	{
		Intent intent = new Intent(this, Buddies.class);
		startActivity(intent);
	}
	
	public void showContent(View view) 
	{
		Intent intent = new Intent(this, Show.class);
		startActivity(intent);
	}
	
	public void settingsContent(View view) 
	{
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
	public void mapsContent(View view) 
	{
		Intent intent = new Intent(this, MapsActivity.class);
		startActivity(intent);
	}
}
