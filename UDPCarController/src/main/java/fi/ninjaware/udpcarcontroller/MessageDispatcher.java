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

/**
 * Created by miku on 1/26/14.
 */
public class MessageDispatcher {

    private static final String TAG = MessageDispatcher.class.getName();

    private InetAddress address;

    private DatagramSocket socket;

    private static final int PORT = 18169;

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

    }

    public void close() {
        socket.close();
    }

    public void sendMessage(ControlType type, byte magnitude) {
        new SendMessageTask(socket, address, PORT).execute(new ControlEvent(type, magnitude));
    }

}
