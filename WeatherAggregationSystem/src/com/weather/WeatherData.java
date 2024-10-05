package com.weather;

import org.json.JSONObject;

public class WeatherData {
    private JSONObject data;

    public WeatherData(JSONObject data) {
        this.data = data;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    // Optional: Static method to create a WeatherData instance from a JSON string
    public static WeatherData fromJson(String json) {
        JSONObject jsonObject = new JSONObject(json);
        return new WeatherData(jsonObject);
    }
}
