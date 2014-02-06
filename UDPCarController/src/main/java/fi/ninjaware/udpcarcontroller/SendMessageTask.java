package fi.ninjaware.udpcarcontroller;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by miku on 1/26/14.
 */
public class SendMessageTask extends AsyncTask<ControlEvent, Void, Void> {

    private DatagramSocket socket;

    private InetAddress address;

    private int port;

    public SendMessageTask(DatagramSocket socket, InetAddress address, int port) {
        this.socket = socket;
        this.address = address;
        this.port = port;
    }

    @Override
    protected Void doInBackground(ControlEvent... controlEvents) {

        byte[] buffer = new byte[] { controlEvents[0].getControlCode(),
                controlEvents[0].getMagnitude() };

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            Log.e(getClass().getName(), "Failed to send a UDP packet.", e);
        }

        /*Log.d(getClass().getName(),
                String.format("Sent message. Control: %d, magnitude: %d.",
                        controlEvents[0].getControlCode(),
                        controlEvents[0].getMagnitude()));*/

        return null;
    }

}
