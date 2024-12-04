package service;

import org.onvif.ver10.device.wsdl.GetDeviceInformation;
import org.onvif.ver10.device.wsdl.GetDeviceInformationResponse;
import org.onvif.ver10.media.wsdl.GetStreamUri;
import org.onvif.ver10.media.wsdl.GetStreamUriResponse;
import org.onvif.ver10.schema.Profile;
import org.onvif.ver10.schema.StreamSetup;
import org.onvif.ver10.schema.StreamType;
import org.onvif.ver10.schema.Transport;
import org.onvif.ver10.schema.TransportProtocol;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class OnvifCameraManager {
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final int DISCOVERY_TIMEOUT = 2000; // 2 seconds

    public static class CameraDevice {
        private String address;
        private String username;
        private String password;
        private OnvifDevice device;
        private String manufacturer;
        private String model;
        private String serialNumber;

        public CameraDevice(String address, String username, String password) {
            this.address = address;
            this.username = username;
            this.password = password;
        }

        // Getters and setters
        public String getAddress() { return address; }
        public String getManufacturer() { return manufacturer; }
        public String getModel() { return model; }
        public String getSerialNumber() { return serialNumber; }
    }

    private List<CameraDevice> discoveredCameras = new ArrayList<>();

    public List<CameraDevice> discoverCameras() {
        discoveredCameras.clear();
        try {
            // Get all network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback and disabled interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                // Get all IP addresses for this interface
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Skip IPv6 addresses
                    if (addr.getHostAddress().contains(":")) {
                        continue;
                    }

                    // Create WS-Discovery message
                    String probe = createWSDiscoveryProbe();
                    // Send probe and process responses
                    sendProbe(probe, addr);
                }
            }
        } catch (SocketException e) {
            System.err.println("Error accessing network interfaces: " + e.getMessage());
        }

        return discoveredCameras;
    }

    public void connectToCamera(CameraDevice camera) {
        try {
            // Create ONVIF device connection
            camera.device = new OnvifDevice(camera.getAddress(), camera.username, camera.password);

            // Get device information
            GetDeviceInformation getDeviceInfo = new GetDeviceInformation();
            GetDeviceInformationResponse response = camera.device.getDevice().getDeviceInformation(getDeviceInfo);

            // Store device information
            camera.manufacturer = response.getManufacturer();
            camera.model = response.getModel();
            camera.serialNumber = response.getSerialNumber();

            System.out.println("Successfully connected to camera at " + camera.getAddress());
            System.out.println("Manufacturer: " + camera.getManufacturer());
            System.out.println("Model: " + camera.getModel());
            System.out.println("Serial Number: " + camera.getSerialNumber());

        } catch (ConnectException e) {
            System.err.println("Failed to connect to camera at " + camera.getAddress() + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error while connecting to camera: " + e.getMessage());
        }
    }

    public String getStreamUri(CameraDevice camera) {
        try {
            // Get media profiles
            List<Profile> profiles = camera.device.getMedia().getProfiles();
            if (profiles.isEmpty()) {
                throw new RuntimeException("No media profiles available");
            }

            // Use the first profile
            Profile profile = profiles.get(0);

            // Setup stream configuration
            StreamSetup streamSetup = new StreamSetup();
            streamSetup.setStream(StreamType.RTP_UNICAST);
            Transport transport = new Transport();
            transport.setProtocol(TransportProtocol.RTSP);
            streamSetup.setTransport(transport);

            // Get stream URI
            GetStreamUri request = new GetStreamUri();
            request.setProfileToken(profile.getToken());
            request.setStreamSetup(streamSetup);

            GetStreamUriResponse response = camera.device.getMedia().getStreamUri(request);
            return response.getMediaUri().getUri();

        } catch (Exception e) {
            System.err.println("Error getting stream URI: " + e.getMessage());
            return null;
        }
    }

    private String createWSDiscoveryProbe() {
        // WS-Discovery probe message template
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\" " +
               "xmlns:w=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" " +
               "xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\" " +
               "xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\">" +
               "<e:Header>" +
               "<w:MessageID>uuid:84ede3de-7dec-11d0-c360-f01234567890</w:MessageID>" +
               "<w:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>" +
               "<w:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>" +
               "</e:Header>" +
               "<e:Body>" +
               "<d:Probe>" +
               "<d:Types>dn:NetworkVideoTransmitter</d:Types>" +
               "</d:Probe>" +
               "</e:Body>" +
               "</e:Envelope>";
    }

    private void sendProbe(String probe, InetAddress addr) {
        // Implementation of sending WS-Discovery probe and processing responses
        // This would involve creating a UDP socket, sending the probe message,
        // and listening for responses from ONVIF cameras
        // When a camera is discovered, create a new CameraDevice and add it to discoveredCameras
    }

    // Example usage
    public static void main(String[] args) {
        OnvifCameraManager manager = new OnvifCameraManager();

        // Discover cameras
        List<CameraDevice> cameras = manager.discoverCameras();
        System.out.println("Found " + cameras.size() + " cameras");

        // Connect to each camera
        for (CameraDevice camera : cameras) {
            manager.connectToCamera(camera);
            String streamUri = manager.getStreamUri(camera);
            if (streamUri != null) {
                System.out.println("Stream URI for camera " + camera.getAddress() + ": " + streamUri);
            }
        }
    }
}
