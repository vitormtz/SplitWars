package com.example.splitwars;

import java.io.Serializable;

public class NetworkMessage implements Serializable {

    private int originalShipType = 1;
    public static final int PROJECTILE = 1;
    public static final int HIT = 2;
    public static final int DISCONNECT = 3;
    public static final int SCREEN_SIZE = 4;
    private int type;
    private float x, y;
    private float speedX, speedY;
    private int screenWidth;
    private int screenHeight;
    private float relativeX;

    public NetworkMessage(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setY(float y) {

        this.y = y;
    }

    public float getSpeedX() {

        return speedX;
    }

    public void setSpeedX(float speedX) {
        this.speedX = speedX;
    }

    public float getSpeedY() {

        return speedY;
    }

    public void setSpeedY(float speedY) {
        this.speedY = speedY;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public float getRelativeX() {
        return relativeX;
    }

    public void setRelativeX(float relativeX) {
        this.relativeX = relativeX;
    }

    public void setOriginalShipType(int originalShipType) {
        this.originalShipType = originalShipType;
    }
}