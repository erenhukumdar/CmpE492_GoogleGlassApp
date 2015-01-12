package com.example.myfirstglassapp;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class MainService extends Service {

	private static final String TAG = "MainService";

	// For live card
	private static final String LIVE_CARD_TAG = "menudemo_card";
    private LiveCard mLiveCard;
    
    private RemoteViews mLiveCardView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	//publish live card if it is not created before
        if (mLiveCard == null) {
        	
        	Log.d(TAG, "publishing my live card...");
        	
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            
            mLiveCardView = new RemoteViews(getPackageName(),
                    R.layout.activity_live_card);
            
            Intent menuIntent = new Intent(this, LiveCardMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            //mLiveCard.attach(this);
            mLiveCard.publish(PublishMode.REVEAL);
            
            //Always call setViews() to update the live card's RemoteViews.
            mLiveCard.setViews(mLiveCardView);
            
            /*
            final Handler h = new Handler();
            h.postDelayed(new Runnable()
            {
                private long time = 0;

                @Override
                public void run()
                {
                    new MdnsTask().execute();	
                }
            }, 1000); // 1 second delay (takes millis)
            */
        } else {
            mLiveCard.navigate();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }
    
    /*
    class MdnsTask extends AsyncTask<Void, Void, Boolean> {

        private Exception exception;

        protected Boolean doInBackground(Void...voids) {
            try {
            	startMdnsSearch();
            } catch (Exception e) {
                this.exception = e;
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            // TODO: check this.exception 
            // TODO: do something with the feed
        }
    }
    
    public void stopMdnsSearch(){
    	try {
            Log.d(TAG, "Stopping mDNS search...");
			jmdns.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void startMdnsSearch(){
        try {
            Log.d(TAG, "Starting mDNS search...");
            WifiManager wifiManager=(WifiManager)getSystemService(android.content.Context.WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            InetAddress _bindingAddress = InetAddress.getByName(ip);

            Log.d(TAG, "Binding address: " + ip);
            jmdns = JmDNS.create(_bindingAddress);
            jmdns.addServiceListener("_http._tcp.local.", new SampleListener());            
        } catch (IOException e) {
        	Log.d(TAG, "Cannot Start mDNS search!");
            e.printStackTrace();
        }
    }

    static class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
        	Log.d(TAG, "Service added   : " + event.getName() + "." + event.getType());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
        	Log.d(TAG, "Service removed : " + event.getName() + "." + event.getType());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
        	Log.d(TAG, "Service resolved: " + event.getInfo());
        }
    }
    */
}
