package com.mainak.walkbuddy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class Buddies extends ListActivity {

	public static final String PREFS_NAME = "MyPrefsFile";
	public static ArrayAdapter<Model> adapter;

	@Override
	public void onCreate(Bundle icicle) 
	{
		super.onCreate(icicle);
		setContentView(R.layout.header);
		// Create an array of Strings, that will be put to our ListActivity
		adapter = new InteractiveArrayAdapter(this,getModel());
		setListAdapter(adapter);

	}

	
	public void buttonClick(View v) 
	{
		InteractiveArrayAdapter ad = (InteractiveArrayAdapter)adapter;
		List<Model> list = ad.list;
		String[] str = {"",""};
		int count =0;
		for(int i=0; i<list.size(); i++)
		{
			Model m  = list.get(i);
			if(m.isSelected())
				str[count++] = m.getName();
		}
		Toast.makeText(this, str[0]+str[1], Toast.LENGTH_LONG).show();
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("buddy1", str[0]);
		editor.putString("buddy2", str[1]);
		// Commit the edits!
		editor.commit();
	}

	private List<Model> getModel() {
		List<Model> list = new ArrayList<Model>();
		list.add(get("yry3"));
		list.add(get("cs434"));
		// Initially select one of the items
		//list.get(0).setSelected(true);
		return list;
	}

	private Model get(String s) {
		return new Model(s);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
	}

} 