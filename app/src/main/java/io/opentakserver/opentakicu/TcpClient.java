package io.opentakserver.opentakicu;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = TcpClient.class.getSimpleName();
    public String serverAddress;
    public int port;
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(String serverAddress, int port, OnMessageReceived listener) {
        this.serverAddress = serverAddress;
        this.port = port;
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.d(TAG, "Sending: " + message);
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {
        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void run() {
        mRun = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(serverAddress);

            Log.d("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, port);

            try {

                //sends the message to the server
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                //in this while the client listens for the messages sent by the server
                while (mRun) {

                    mServerMessage = mBufferIn.readLine();

                    if (mServerMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(mServerMessage);
                    }

                }

                Log.d("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        serverAddress = sharedPreferences.getString("atak_address", "192.168.1.10");
        port = Integer.parseInt(sharedPreferences.getString("atak_port", "8088"));
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}