package fi.ninjaware.udpcarcontroller;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by miku on 1/26/14.
 */
public class MessageDispatcher {

    private static final String TAG = MessageDispatcher.class.getName();

    private final Timer timer;

    private InetAddress address;

    private DatagramSocket socket;

    private static final int PORT = 18169;

    private static final int UPKEEP_TIME = 500; // ms

    private ControlEvent previousControlEvent;

    private long previousDispatchTime;

    public MessageDispatcher(WifiManager wifiManager) {

        // Create the UDP socket.
        try {
            socket = new DatagramSocket();
            Log.i(TAG, "Created a UDP socket.");
        } catch (SocketException e) {
            Log.e(TAG, "Failed to create a UDP socket.", e);
        }

        // Get the IP address of the router / Raspberry Pi.
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        int hostAddress = dhcpInfo.gateway;
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };
        try {

            address = InetAddress.getByAddress(addressBytes);
            Log.i(TAG, "Router IP address is " + address.getHostAddress());
        } catch (UnknownHostException e) {
            Log.e(TAG, "Failed to get router IP address.", e);
        }

        // Create a timer for sending the UDP packets on a fixed rate.
        timer = new Timer();
        timer.scheduleAtFixedRate(new ControlEventTimerTask(), 0, UPKEEP_TIME);
    }

    public void close() {
        timer.cancel();
        socket.close();
    }

    public void sendMessage(ControlEvent controlEvent) {
        sendMessage(controlEvent, false);
    }

    public void sendMessage(ControlEvent controlEvent, boolean scheduled) {
        new SendMessageTask(socket, address, PORT).execute(controlEvent);
        if(!scheduled) {
            previousControlEvent = controlEvent;
            previousDispatchTime = System.currentTimeMillis();
        }
    }

    private class ControlEventTimerTask extends TimerTask {

        @Override
        public void run() {
            long period = System.currentTimeMillis() - previousDispatchTime;
            if(period > UPKEEP_TIME && previousControlEvent != null) {
                Log.d(TAG, "Scheduled dispatch");
                sendMessage(previousControlEvent, true);
            }
        }
    }
}
