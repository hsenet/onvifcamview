package util;

 public class OnvifDevice {
    private final Device device;
    private final Media media;

    public OnvifDevice(String address, String username, String password) throws ConnectException {
        // Initialize ONVIF device connection
        if (!address.startsWith("http")) {
            address = "http://" + address;
        }

        try {
            // Create device and media services
            device = new Device();
            device.createSOAPDeviceClient(address, username, password);

            media = new Media();
            media.createSOAPMediaClient(address, username, password);
        } catch (Exception e) {
            throw new ConnectException("Failed to connect to ONVIF device: " + e.getMessage());
        }
    }

    public Device getDevice() {
        return device;
    }

    public Media getMedia() {
        return media;
    }
}
