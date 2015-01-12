package com.example.myfirstglassapp.sensor;

import edu.mit.media.funf.json.IJsonObject;

public class LightSensorDataCollector extends AbstractSensorDataCollector {

	private ISensorData SensorData = new ISensorData() {
		private double timestamp = 0;
		private double lux = 0;

		@Override
		public void setJsonData(IJsonObject data) {
			timestamp = data.get("timestamp").getAsDouble();
			lux = data.get("lux").getAsDouble();
		}

		@Override
		public String getCvsValue() {
			return timestamp + "," + lux;
		}
	};

	@Override
	public ISensorData getNewSensoreData() {
		return SensorData;
	}
}
