package com.example.myfirstglassapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


// The activity for the livecard menu.
// Ref: https://developers.google.com/glass/develop/gdk/ui/live-card-menus
public class LiveCardMenuActivity extends Activity
{
	private static final String TAG = "LiveCardMenuActivity";

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.live_card_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Log.d(TAG,"onOptionsItemSelected() called.");

        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.live_card_open_app:
                gotoMain();
                return true;
            case R.id.live_card_reset:
				SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
				Editor editor = sharedPref.edit();
				editor.remove(getString(R.string.cloudlet_ip));
				editor.commit();
                return true;
            case R.id.live_card_stop:
            	stopService(new Intent(this, MainService.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // Opens the "main" (voice launcher) activity.
    // TBD: Does this work?
    // This seems to open the activity "on top of the live card"
    //    (based on the behavior when the activity is dismissed).
    // How to open an activity "on top of the clock screen" ????
    private void gotoMain()
    {
        Intent intent = new Intent(this, ActionSelectionActivity.class);
        // ???
        startActivity(intent.setFlags(    // Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        // Nothing else to do, closing the activity.
        finish();
    }
}
