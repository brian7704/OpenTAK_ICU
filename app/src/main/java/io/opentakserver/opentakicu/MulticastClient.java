package io.opentakserver.opentakicu;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

import androidx.preference.PreferenceManager;

public class MulticastClient {
    private static final String LOGTAG = "MulticastClient";
    private SharedPreferences preferences;
    private Context context;
    private InetAddress ip_address;
    private int port;
    private MulticastSocket socket;

    public MulticastClient(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private void join_group() {
        try {
            socket = new MulticastSocket(port);
            //ip_address = InetAddress.getByName(preferences.getString("address", "239.1.1.1"));
            ip_address = InetAddress.getByName("239.2.3.1");
            port = 6969; //Integer.parseInt(preferences.getString("port", "1234"));
            Log.d(LOGTAG, "Joined group");
            socket.joinGroup(ip_address);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to join multicast group", e);
        }
    }

    public void send_cot(String cot) {
        try {
            Runnable runnable = () -> {
                try {
                    join_group();
                    byte[] bytes = cot.getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(bytes, bytes.length, ip_address, port));
                    Log.d(LOGTAG, "Sent cot");
                    leave_group();
                } catch (IOException e) {
                    Log.e(LOGTAG, "Failed to send multicast CoT", e);
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to run multicast thread", e);
        }
    }

    public void leave_group() {
        try {
            socket.leaveGroup(ip_address);
            Log.d(LOGTAG, "Left group");
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to leave multicast group", e);
        }
    }
}
