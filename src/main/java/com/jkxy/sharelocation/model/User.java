package com.jkxy.sharelocation.model;

/**
 * Created by X on 2016/5/14.
 */
public class User {

    private String userName;//用户名
    private double latitude = 0.000f;//纬度
    private double longitude = 0.000f;//经度
    private boolean isOnLine = false;//是否在线标志

    public boolean isOnLine() {
        return isOnLine;
    }

    public void setIsOnLine(boolean isOnLine) {
        this.isOnLine = isOnLine;
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "userName=" + userName + "," + "latitude=" + latitude + "," + "longitude" + longitude + "," +
                "isOnLine=" + isOnLine;

    }


}
