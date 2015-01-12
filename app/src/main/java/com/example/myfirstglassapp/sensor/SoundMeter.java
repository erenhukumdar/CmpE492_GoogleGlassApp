package com.example.myfirstglassapp.sensor;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class SoundMeter {

    private AudioRecord ar = null;
    private int minSize;

    public void start() {
        minSize= AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,minSize);
        ar.startRecording();
        Log.d("Microphone","___________________started______________");
    }

    public void stop() {
        if (ar != null) {
            ar.stop();
        }
    }

    public double getAmplitude() {
        short[] buffer = new short[minSize];

        ar.read(buffer, 0, minSize);
        int max = 0;
        for (short s : buffer)
        {
          /*  Log.d("MicBuffer",String.valueOf(s) );*/
            if (Math.abs(s) > max)
            {
                max = Math.abs(s);

            }
        }

        Log.d("Microphone",String.valueOf(max) );
        return max;

    }

}