package com.example.myfirstglassapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.widget.TextView;

import com.example.myfirstglassapp.sensor.AbstractSensorDataCollector;
import com.example.myfirstglassapp.sensor.AccelerometerDataCollector;
import com.example.myfirstglassapp.sensor.GyroscopeDataCollector;
import com.example.myfirstglassapp.sensor.LightSensorDataCollector;
import com.example.myfirstglassapp.sensor.MagnetometerDataCollector;
import com.example.myfirstglassapp.sensor.SoundMeter;
import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;
import com.google.android.glass.eye.EyeGestureManager.Listener;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.AccelerometerSensorProbe;
import edu.mit.media.funf.probe.builtin.GyroscopeSensorProbe;
import edu.mit.media.funf.probe.builtin.LightSensorProbe;
import edu.mit.media.funf.probe.builtin.MagneticFieldSensorProbe;
import edu.mit.media.funf.probe.builtin.ProximitySensorProbe;

public class MainActivity extends Activity implements DataListener {
    private static final String TAG = "AttentionDetectorMain";
    private static final int interval = 10000;
    private TextureView mVideoCaptureView = null;
    private Camera mCamera = null;
    private Handler handler = new Handler();
    private String cloudletIp = "";
    private boolean isSurfaceReady = false;
    private boolean isFunfServiceConnected = false;
    private String macAdress="";
    Map<String, AbstractSensorDataCollector> sensorDataCollectorMap = null;
    public static final String PIPELINE_NAME = "default";
    private FunfManager funfManager;
    private BasicPipeline pipeline;
    private GyroscopeSensorProbe gyroscopeProbe;
    private AccelerometerSensorProbe accelerometerProbe;
    private MagneticFieldSensorProbe magnetometerProbe;
    private ProximitySensorProbe proximityProbe;
    private LightSensorProbe lightProbe;
    private EyeGestureManager mEyeGestureManager;
    private EyeGestureListener mEyeGestureListener;
    private int blinkCounter = 0;
    private SoundMeter mSensor;
    private  ArrayList <String> amplitudeList=new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        macAdress=info.getMacAddress();
        Log.d(TAG, ">>>>>>>> onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //___________________________________ Services is starting _______________________________


        if (!isMyServiceRunning(MainService.class)) {
            startService(new Intent(this, MainService.class));
            return;
        }

        //____________________________________ Sensors are setting ________________________________
        mSensor = new SoundMeter();
        mEyeGestureManager = EyeGestureManager.from(this);
        for (EyeGesture eg : EyeGesture.values()) {
            boolean supported = mEyeGestureManager.isSupported(eg);
            Log.d("WinkDetector", eg.name() + ":" + supported);
        }
        mEyeGestureManager = EyeGestureManager.from(getApplicationContext());

        mEyeGestureListener = new EyeGestureListener();
        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
               /* mCamera.startPreview();
                mCamera.takePicture(null, null, mPicture);*/
            }
        }, 0, 1000);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        cloudletIp = sharedPref.getString(getString(R.string.cloudlet_ip), "");

        if (cloudletIp != "") {
            TextView title = (TextView) findViewById(R.id.Title);
            TextView subTitle = (TextView) findViewById(R.id.SubTitle);
            title.setText("Server IP: " + cloudletIp);
            subTitle.setText("Tab to select another...");
            startAllSecure();
            mVideoCaptureView = (TextureView) findViewById(R.id.scan_preview);
            mVideoCaptureView.setKeepScreenOn(true);
            mVideoCaptureView.setSurfaceTextureListener(new VideoCaptureTextureListener());
        }

        // Bind to the service, to create the connection with FunfManager

        bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();



    }

    @Override
    protected void onStop() {
        super.onStop();



    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, ">>>>>>>> onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, ">>>>>>>> onPause");
        super.onPause();
        handler.removeCallbacks(dataSender);
        handler.removeCallbacks(soundOperation);
        stopVideo();
        stopFunf();
        stopMicrophone();
        stopGestureManager();
        isSurfaceReady = false;
        isFunfServiceConnected = false;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, ">>>>>>>> onResume");
        super.onResume();
        startAllSecure();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            Intent intent = new Intent(this, CloudletSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("keep", false);
            startActivity(intent);
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return false;
    }


    private synchronized void startAllSecure() {
        Log.d(TAG, ">>>>>>>> startAllSecure");
        if ( isFunfServiceConnected && cloudletIp != "") {
            startVideo();
            startFunf();
            startMicrophone();
            startGestureManager();
            dataSender.run();
            soundOperation.run();
        }
    }
    private void startMicrophone()
    {

        mSensor.start();

    }
    private void stopMicrophone()
    {
        mSensor.stop();

    }
    private void stopGestureManager()
    {
        mEyeGestureManager.unregister(EyeGesture.BLINK, mEyeGestureListener);
        mEyeGestureManager.unregister(EyeGesture.DOUBLE_BLINK, mEyeGestureListener);
        mEyeGestureManager.unregister(EyeGesture.WINK, mEyeGestureListener);

    }
    private void startGestureManager()
    {

        mEyeGestureManager.register(EyeGesture.BLINK, mEyeGestureListener);
        mEyeGestureManager.register(EyeGesture.DOUBLE_BLINK, mEyeGestureListener);
        //mEyeGestureManager.register(EyeGesture.DOFF, mEyeGestureListener);
        mEyeGestureManager.register(EyeGesture.WINK, mEyeGestureListener);
        //mEyeGestureManager.register(EyeGesture.DOUBLE_WINK, mEyeGestureListener);

    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * ***************************************************************
     * FUNF RELATED CODES IS IN THIS SECTION
     * ****************************************************************
     */
    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, ">>>>>>>> onServiceConnected");
            funfManager = ((FunfManager.LocalBinder) service).getManager();

            Gson gson = funfManager.getGson();
            gyroscopeProbe = gson.fromJson(new JsonObject(), GyroscopeSensorProbe.class);
            accelerometerProbe = gson.fromJson(new JsonObject(), AccelerometerSensorProbe.class);
            magnetometerProbe = gson.fromJson(new JsonObject(), MagneticFieldSensorProbe.class);
            proximityProbe = gson.fromJson(new JsonObject(), ProximitySensorProbe.class);
            lightProbe = gson.fromJson(new JsonObject(), LightSensorProbe.class);

            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);
            gyroscopeProbe.registerPassiveListener(MainActivity.this);
            accelerometerProbe.registerPassiveListener(MainActivity.this);
            magnetometerProbe.registerPassiveListener(MainActivity.this);
            proximityProbe.registerPassiveListener(MainActivity.this);
            lightProbe.registerPassiveListener(MainActivity.this);

            sensorDataCollectorMap = new HashMap<String, AbstractSensorDataCollector>();
            sensorDataCollectorMap.put(gyroscopeProbe.getClass().getSimpleName(), new GyroscopeDataCollector());
            sensorDataCollectorMap.put(accelerometerProbe.getClass().getSimpleName(), new AccelerometerDataCollector());
            sensorDataCollectorMap.put(magnetometerProbe.getClass().getSimpleName(), new MagnetometerDataCollector());
            //sensorDataCollectorMap.put(proximityProbe.getClass().getName(), new ProximitySensorDataCollector());
            sensorDataCollectorMap.put(lightProbe.getClass().getSimpleName(), new LightSensorDataCollector());

            isFunfServiceConnected = true;
            startAllSecure();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, ">>>>>>>> onServiceDisconnected");
            funfManager = null;
        }
    };

    @Override
    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        Log.d(TAG, ">>>>>>>> onDataCompleted");
    }

    @Override
    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
        String probeType = probeConfig.get("@type").getAsString();
        probeType = probeType.substring(probeType.lastIndexOf(".") + 1);
        //get proper sensor data collector from the map, and use it!

        AbstractSensorDataCollector datacollector = sensorDataCollectorMap.get(probeType);
        if (datacollector != null) {
            datacollector.onDataReceived(data);
          //  Log.e(TAG, ">>>>>>>> " + probeType + " >>> " + data.toString());
        }
        else
            Log.e(TAG, ">>>>>>>> " + probeType + "is not defined in sensor data collector MAP");
    }

    private void startFunf() {
        if (funfManager != null) {
            funfManager.enablePipeline(PIPELINE_NAME);
            pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(PIPELINE_NAME);

            gyroscopeProbe.registerListener(pipeline);
            accelerometerProbe.registerListener(pipeline);
            magnetometerProbe.registerListener(pipeline);
            proximityProbe.registerListener(pipeline);
            lightProbe.registerListener(pipeline);
        }
    }

    private void stopFunf() {
        if (funfManager != null) {
            gyroscopeProbe.unregisterListener(pipeline);
            accelerometerProbe.unregisterListener(pipeline);
            magnetometerProbe.unregisterListener(pipeline);
            proximityProbe.unregisterListener(pipeline);
            lightProbe.unregisterListener(pipeline);

            funfManager.disablePipeline(PIPELINE_NAME);

            unbindService(funfManagerConn);

            //Log.e(TAG, ">>>>>>>> " + probeType + "is not defined in sensor data collector MAP");.clear();
            sensorDataCollectorMap = null;
            funfManager = null;
        }
        isFunfServiceConnected = false;
    }

    /**
     * ***************************************************************
     * CAMERA RELATED CODES IS IN THIS SECTION
     * ****************************************************************
     */
    private void startVideo() {
        Log.d(TAG, ">>>>>>>> startVideo");
        //if camera is not null, release it!
        if (mCamera != null)
            mCamera.release();

        if (null != mVideoCaptureView) {
            try {
                mCamera = Camera.open();
                mCamera.setPreviewTexture(mVideoCaptureView.getSurfaceTexture());
            } catch (IOException e) {
                Log.e(TAG, "Error opening camera or setting preview texture", e);
                return;
            }
        }
        mCamera.startPreview();
    }

    private void stopVideo() {
        Log.d(TAG, ">>>>>>>> stopVideo");
        if (null == mCamera)
            return;

        try {
            mCamera.stopPreview();
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewTexture(null);
            mCamera.release();
        } catch (IOException e) {
            Log.e(TAG, "error occured during releasing camera:" + e.getMessage());
        }
        mCamera = null;
    }

    private final class VideoCaptureTextureListener implements TextureView.SurfaceTextureListener {

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            stopVideo();
            isSurfaceReady = false;
            return true;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            isSurfaceReady = true;
            startAllSecure();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //TODO: apply image processing to find face on the video
        }
    }

    /**
     * ***************************************************************
     * HTTP POST RELATED CODES IS IN THIS SECTION
     * ****************************************************************
     */
    public Runnable soundOperation = new Runnable() {

        private long time = 200;

        @Override
        public void run() {
            /*if (null == mCamera)
                return;*/




            saveAmplitude();
            handler.postDelayed(this, 200);
        }
    };

    private void saveAmplitude()
    {
        double now=new Date().getTime();

        double amplitude= mSensor.getAmplitude();
        String sample=String.valueOf(now)+","+String.valueOf(amplitude);
        amplitudeList.add(sample);
        Log.e("SoundOperation", "Getting Samples="+String.valueOf(amplitude)  );
    }
    private String getCVSAplitudeData()
    {
        int numberofElements=amplitudeList.size();
        String resultString="";
        for (int i=0;i<numberofElements;i++)
        {
            resultString+=String.valueOf(amplitudeList.get(i))+","+macAdress+"\n";
        }
        amplitudeList.clear();
        Log.e("AmpTest",resultString);
    return resultString;
    }
    public Runnable dataSender = new Runnable() {
        private long time = 0;

        @Override
        public void run() {
            if (null == mCamera)
                return;

            time += interval;
            Log.d(TAG, "Posting image for time " + time);

            new DataSenderTask().execute(mVideoCaptureView.getBitmap());
            handler.postDelayed(this, interval);
        }
    };

    class DataSenderTask extends AsyncTask<Bitmap, Void, Boolean> {
        protected Boolean doInBackground(Bitmap... images) {
            try {
                postImage(images[0]);
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                int mode= sharedPref.getInt("mode",0);

                postSensorData(mode);
                postSoundData(mode);
                postBlinkData();
                mSensor.stop();
                mSensor.start();

            } catch (Exception e) {
                Log.e(TAG, "error occured during running AsyncTask:" + e.getMessage());
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }

    private void postImage(Bitmap image) {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        String urlServer = "http://" + cloudletIp + ":8080/uploadphoto/";
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        try {
            URL url = new URL(urlServer);
            connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs & Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");

            //Set timeout
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(2500);

            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"image\";type=\"file\"" + lineEnd);
            outputStream.writeBytes(lineEnd);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] buffer = baos.toByteArray();
            outputStream.write(buffer, 0, buffer.length);

            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();

            Log.d(TAG, "POST response code" + serverResponseCode);
            Log.d(TAG, "POST response message" + serverResponseMessage);

            outputStream.flush();
            outputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "POST error" + ex.getMessage());
        }
    }

    private void postBlinkData()
    {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String urlServer = "http://" + cloudletIp + ":8080/uploadblink/";
        String urlParameters = "BlinkCount="+sharedPref.getInt("blinkCounter",0);
        try {
            URL url = new URL(urlServer);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs & Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");

            //Set timeout
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(2500);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();

            Log.d(TAG, "POST response code" + serverResponseCode);
            Log.d(TAG, "POST response message" + serverResponseMessage);

            wr.flush();
            wr.close();
            connection.disconnect();
            sharedPref.edit().putInt("blinkCounter",0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "POST error" + ex.getMessage());
        }

    }
    private void postSoundData(int mode)
    {

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String urlServer = "http://" + cloudletIp + ":8080/uploadmicrophone/";
        String urlParameters = "Amplitude="+getCVSAplitudeData()+"&Mode="+String.valueOf(mode)+"&Activity="+sharedPref.getString("Activity","");
        try {
            URL url = new URL(urlServer);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs & Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");

            //Set timeout
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(2500);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();

            Log.d(TAG, "POST response code" + serverResponseCode);
            Log.d(TAG, "POST response message" + serverResponseMessage);

            wr.flush();
            wr.close();
            connection.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "POST error" + ex.getMessage());
        }

    }
    private void postSensorData(int mode) {
        String urlServer = "http://" + cloudletIp + ":8080/uploadsensor/";
        String urlParameters = "";
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        for (Entry<String, AbstractSensorDataCollector> entry : sensorDataCollectorMap.entrySet()) {
            if (!urlParameters.equals(""))
                urlParameters += "&";
            Log.e("PostedData", entry.getValue().getStringForPost());
            urlParameters += entry.getKey() + "=" + entry.getValue().getStringForPost()+"&Activity="+sharedPref.getString("Activity","")+"&Mode="+String.valueOf(mode)+"&MacAdress="+macAdress;
            entry.getValue().clearResults();
        }

        try {
            URL url = new URL(urlServer);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs & Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");

            //Set timeout
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(2500);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();

            Log.d(TAG, "POST response code" + serverResponseCode);
            Log.d(TAG, "POST response message" + serverResponseMessage);

            wr.flush();
            wr.close();
            connection.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "POST error" + ex.getMessage());
        }
    }

    private class EyeGestureListener implements Listener {

        @Override
        public void onEnableStateChange(EyeGesture eyeGesture, boolean paramBoolean) {
            Log.i(TAG, eyeGesture + " state changed:" + paramBoolean);
        }

        @Override
        public void onDetected(final EyeGesture eyeGesture) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    blinkCounter++;
                    sharedPref.edit().putInt("blinkCounter",blinkCounter);
                    sharedPref.edit().commit();
                    Log.i("BlinkCounter", "Counter Value:" + String.valueOf(blinkCounter));

                }
            });
        }
    }
}
