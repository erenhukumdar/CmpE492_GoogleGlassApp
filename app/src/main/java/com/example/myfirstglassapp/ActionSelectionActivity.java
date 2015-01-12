package com.example.myfirstglassapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by Tekron on 10.11.2014.
 */
public class ActionSelectionActivity extends Activity
{
    private static final String TAG = "ActionSelectionActivity";

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_type_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Log.d(TAG, "onOptionsItemSelected() called.");
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("Activity",item.getTitle().toString());
        int mode;
        if (item.getTitle().toString()==getResources().getString(R.string.calibre))
            mode=0;
        else
            mode=1;
        Log.d("item_id_Selected",String.valueOf(mode));
        editor.putInt("mode",mode);
        editor.commit();
        gotoMain();
        return super.onOptionsItemSelected(item);

    }


    // Opens the "main" (voice launcher) activity.
    // TBD: Does this work?
    // This seems to open the activity "on top of the live card"
    //    (based on the behavior when the activity is dismissed).
    // How to open an activity "on top of the clock screen" ????
    private void gotoMain()
    {
        Intent intent = new Intent(this, MainActivity.class);
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
