package com.mainak.walkbuddy;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity implements OnClickListener {

	public static final String PREFS_NAME = "MyPrefsFile";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        Button submit = (Button)findViewById(R.id.submit);
        submit.setOnClickListener(this);
    }
    
    @Override
	public void onClick(View arg0) 
    {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		
		EditText et1 = (EditText)findViewById(R.id.EditText1);
		EditText et2 = (EditText)findViewById(R.id.EditText2);
		EditText et3 = (EditText)findViewById(R.id.EditText3);
		
		editor.putString("taddress", et1.getText().toString());
		editor.putString("netid", et2.getText().toString());
		editor.putInt("update_interval", Integer.parseInt(et3.getText().toString()));
		
		editor.commit();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_settings, menu);
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

}
