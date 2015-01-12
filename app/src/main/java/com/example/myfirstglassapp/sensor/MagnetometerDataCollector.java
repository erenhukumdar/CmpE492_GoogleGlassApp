package com.example.myfirstglassapp.sensor;

import edu.mit.media.funf.json.IJsonObject;

public class MagnetometerDataCollector extends AbstractSensorDataCollector {

	private ISensorData SensorData = new ISensorData() {
		private double timestamp = 0;
		private double x = 0;
		private double y = 0;
		private double z = 0;

		@Override
		public void setJsonData(IJsonObject data) {
			timestamp = data.get("timestamp").getAsDouble();
			x = data.get("x").getAsDouble();
			y = data.get("y").getAsDouble();
			z = data.get("z").getAsDouble();
		}

		@Override
		public String getCvsValue() {
			return timestamp + "," + x + "," + y + "," + z;
		}
	};

	@Override
	public ISensorData getNewSensoreData() {
		return SensorData;
	}
}
