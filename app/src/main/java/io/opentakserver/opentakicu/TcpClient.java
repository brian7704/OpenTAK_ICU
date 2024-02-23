package io.opentakserver.opentakicu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.cot.auth;
import io.opentakserver.opentakicu.cot.Cot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class TcpClient extends Thread implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = TcpClient.class.getSimpleName();
    public static final String TAK_SERVER_CONNECTED = "tak_server_connected";
    public static final String TAK_SERVER_DISCONNECTED = "tak_server_disconnected";

    private SharedPreferences prefs;

    public String serverAddress;
    public int port;
    private boolean atak_auth = false;
    private String atak_username;
    private String atak_password;
    private boolean atak_ssl = false;
    private boolean atak_ssl_self_signed = true;
    private String atak_trust_store;
    private String atak_trust_store_password;
    private String atak_client_cert;
    private String atak_client_cert_password;
    private String uid;

    private Socket socket;
    private SSLSocket sslSocket;
    private Context context;

    private String mServerMessage;
    private OnMessageReceived mMessageListener;
    public boolean mRun = false;
    private PrintWriter mBufferOut;
    private BufferedReader mBufferIn;

    private boolean connected = false;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(Context context, String serverAddress, int port, OnMessageReceived listener) {
        this.context = context;
        this.serverAddress = serverAddress;
        this.port = port;
        mMessageListener = listener;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        getSettings();
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        Runnable runnable = () -> {
            if (mBufferOut != null) {
                mBufferOut.println(message);
                mBufferOut.flush();
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

            try {
                mBufferOut.close();
                if (sslSocket != null)
                    sslSocket.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close buffer", e);
            }
        }

        prefs.unregisterOnSharedPreferenceChangeListener(this);
        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
        socket = null;
        sslSocket = null;
        Log.d(TAG, "Sending Broadcast " + TAK_SERVER_DISCONNECTED);
        context.sendBroadcast(new Intent(TAK_SERVER_DISCONNECTED).setPackage(context.getPackageName()));
    }

    public void run() {
        mRun = true;

        try {
            InetAddress serverAddr = InetAddress.getByName(serverAddress);

            Log.d(TAG, "Connecting...");
            getSettings();

            if (atak_ssl) {
                if (atak_ssl_self_signed) {
                    Log.d(TAG, "Connecting via SSL to a server with a self signed cert");
                    KeyStore trusted = KeyStore.getInstance("PKCS12");
                    FileInputStream trust_store = new FileInputStream(atak_trust_store);

                    KeyStore client_cert_keystore = KeyStore.getInstance("PKCS12");
                    FileInputStream client_cert = new FileInputStream(atak_client_cert);

                    trusted.load(trust_store, atak_trust_store_password.toCharArray());
                    trust_store.close();

                    client_cert_keystore.load(client_cert, atak_client_cert_password.toCharArray());
                    client_cert.close();

                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(trusted);


                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(client_cert_keystore, atak_trust_store_password.toCharArray());
                    SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
                    sslContext.init(kmf.getKeyManagers(),trustManagerFactory.getTrustManagers(),null);

                    SSLSocketFactory factory = sslContext.getSocketFactory();
                    sslSocket = (SSLSocket) factory.createSocket(serverAddr, port);
                    sslSocket.setSoTimeout(1000);
                } else {
                    Log.d(TAG, "Connecting vi SSL to a server with a signed cert");
                    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    sslSocket = (SSLSocket) factory.createSocket(serverAddr, port);
                    sslSocket.setSoTimeout(1000);
                }
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream())), true);
                mBufferIn = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                context.sendBroadcast(new Intent(TAK_SERVER_CONNECTED).setPackage(context.getPackageName()));
            } else {
                Log.d(TAG, "Connecting via TCP");
                socket = new Socket(serverAddr, port);
                socket.setSoTimeout(1000);
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                context.sendBroadcast(new Intent(TAK_SERVER_CONNECTED).setPackage(context.getPackageName()));
            }

            if (atak_auth) {
                XmlFactory xmlFactory = XmlFactory.builder()
                        .xmlInputFactory(new WstxInputFactory())
                        .xmlOutputFactory(new WstxOutputFactory())
                        .build();

                XmlMapper xmlMapper = XmlMapper.builder(xmlFactory).build();

                Cot cot = new Cot(atak_username, atak_password, uid);
                auth atakAuth = new auth(cot);
                sendMessage(xmlMapper.writeValueAsString(atakAuth));
            }

            try {
                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    try {
                        mServerMessage = mBufferIn.readLine();
                        if (mServerMessage != null && mMessageListener != null) {
                            mMessageListener.messageReceived(mServerMessage);
                        }
                    } catch (SocketException e) {
                        Log.e(TAG, "Socket closed");
                        stopClient();
                    } catch (SocketTimeoutException e) {}
                }
                stopClient();
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
                stopClient();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error", e);
            stopClient();
        }
    }

    public void setmRun(boolean mRun) {
        if (!mRun)
            Log.d(TAG, "Stopping thread");
        this.mRun = mRun;
    }

    private void getSettings() {
        serverAddress = prefs.getString("atak_address", null);
        port = Integer.parseInt(prefs.getString("atak_port", "8088"));
        atak_auth = prefs.getBoolean("atak_auth", false);
        atak_username = prefs.getString("atak_username", null);
        atak_password = prefs.getString("atak_password", null);
        atak_ssl = prefs.getBoolean("atak_ssl", false);
        atak_ssl_self_signed = prefs.getBoolean("atak_ssl_self_signed", true);
        atak_trust_store = prefs.getString("trust_store_certificate", null);
        atak_trust_store_password = prefs.getString("trust_store_cert_password", "atakatak");
        atak_client_cert = prefs.getString("client_certificate", null);
        atak_client_cert_password = prefs.getString("client_cert_password", "atakatak");
        uid = prefs.getString("uid", "OpenTAK-ICU-" + UUID.randomUUID().toString());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        getSettings();
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

}