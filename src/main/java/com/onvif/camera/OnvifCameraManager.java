package com.onvif.camera;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class OnvifCameraManager {
    private static final int WS_DISCOVERY_PORT = 3702;
    private static final String WS_DISCOVERY_ADDRESS = "239.255.255.250";
    private List<String> discoveredCameras;

    public OnvifCameraManager() {
        this.discoveredCameras = new ArrayList<>();
    }

    public List<String> discoverCameras() {
        try {
            // Create multicast socket for WS-Discovery
            MulticastSocket socket = new MulticastSocket(WS_DISCOVERY_PORT);
            InetAddress multicastAddress = InetAddress.getByName(WS_DISCOVERY_ADDRESS);
            socket.joinGroup(multicastAddress);

            // Prepare ONVIF probe message
            String probeMessage =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\" " +
                "xmlns:w=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" " +
                "xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\" " +
                "xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\">" +
                "<e:Header>" +
                "<w:MessageID>uuid:" + java.util.UUID.randomUUID() + "</w:MessageID>" +
                "<w:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>" +
                "<w:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>" +
                "</e:Header>" +
                "<e:Body>" +
                "<d:Probe>" +
                "<d:Types>dn:NetworkVideoTransmitter</d:Types>" +
                "</d:Probe>" +
                "</e:Body>" +
                "</e:Envelope>";

            // Send probe message
            byte[] probeBytes = probeMessage.getBytes();
            DatagramPacket probe = new DatagramPacket(
                probeBytes,
                probeBytes.length,
                multicastAddress,
                WS_DISCOVERY_PORT
            );
            socket.send(probe);

            // Listen for responses
            byte[] receiveBuffer = new byte[4096];
            DatagramPacket response = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            // Set timeout for discovery (3 seconds)
            socket.setSoTimeout(3000);

            try {
                while (true) {
                    socket.receive(response);
                    String deviceResponse = new String(
                        response.getData(),
                        0,
                        response.getLength()
                    );

                    // Add discovered camera IP to the list
                    String deviceIP = response.getAddress().getHostAddress();
                    if (!discoveredCameras.contains(deviceIP)) {
                        discoveredCameras.add(deviceIP);
                    }
                }
            } catch (SocketTimeoutException e) {
                // Discovery timeout reached
            }

            socket.leaveGroup(multicastAddress);
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return discoveredCameras;
    }

    public void printDiscoveredCameras() {
        if (discoveredCameras.isEmpty()) {
            System.out.println("No cameras discovered");
            return;
        }

        System.out.println("Discovered ONVIF cameras:");
        for (String camera : discoveredCameras) {
            System.out.println("Camera IP: " + camera);
        }
    }
}
