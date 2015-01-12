package com.example.myfirstglassapp.sensor;

import edu.mit.media.funf.json.IJsonObject;

public interface ISensorData{
	public void setJsonData(IJsonObject data);
	public String getCvsValue();
}