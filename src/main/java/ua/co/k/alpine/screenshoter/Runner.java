package ua.co.k.alpine.screenshoter;

import java.io.IOException;
import java.net.InetAddress;

public class Runner {

    public static void main(String[] args) throws IOException {
        String hostName = InetAddress.getLocalHost().getHostName();
        String driverPath = null;
        String targetDirectory = "/opt/volume";
        // specific rule for running locally, not in container 
        // my PC name is 'nb7'
        if ("nb7".equals(hostName)) {
            driverPath = "src/main/docker/chromedriver/90/linux/chromedriver";
            targetDirectory = "volume";
        }
        ScreenShotService service = new ScreenShotService(true, driverPath, targetDirectory);
        service.createImage("http://example.com/", System.currentTimeMillis()+"sample.png", 500, 500);
    }
}
