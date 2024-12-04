package com.onvif.camera;

public class camview {
    public static void main(String[] args) {
        OnvifCameraManager cameraManager = new OnvifCameraManager();
        cameraManager.discoverCameras();
        cameraManager.printDiscoveredCameras();
    }
}

