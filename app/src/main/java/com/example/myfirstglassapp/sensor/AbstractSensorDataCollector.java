package com.example.myfirstglassapp.sensor;

import java.util.LinkedList;

import edu.mit.media.funf.json.IJsonObject;

abstract public class AbstractSensorDataCollector {
	private Object lock = new Object();
	LinkedList<ISensorData> linkedlist = new LinkedList<ISensorData>();
    String result="";
	//use lock to provide thread safe linked list
	public void onDataReceived(IJsonObject data){
		synchronized(lock) {
			ISensorData properSensorData = getNewSensoreData();

			properSensorData.setJsonData(data);
            result+= properSensorData.getCvsValue() + "\n";
			/*linkedlist.add(properSensorData);
            Log.e("LinkList2",linkedlist.toString() );*/
		}
	}

	//use lock to provide thread safe linked list
	public String getStringForPost(){

		synchronized(lock) {
           /* Log.e("LinkList",linkedlist.toString() );
			for (ISensorData myData : linkedlist) {
                Log.e("DataCollector", myData.getCvsValue());
                result += myData.getCvsValue() + "\n";
            }
			linkedlist.clear();*/
		}
     //   Log.e("LinkList",result );



		return result;
	}
    public void clearResults(){

        synchronized(lock) {
           /* Log.e("LinkList",linkedlist.toString() );
			for (ISensorData myData : linkedlist) {
                Log.e("DataCollector", myData.getCvsValue());
                result += myData.getCvsValue() + "\n";
            }
			linkedlist.clear();*/
            result="";
        }
        //   Log.e("LinkList",result );


    }
	abstract public ISensorData getNewSensoreData();
}
