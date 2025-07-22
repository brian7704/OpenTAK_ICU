package io.opentakserver.opentakicu;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import io.opentakserver.opentakicu.contants.Preferences;
import io.opentakserver.opentakicu.cot.Contact;
import io.opentakserver.opentakicu.cot.Detail;
import io.opentakserver.opentakicu.cot.Point;
import io.opentakserver.opentakicu.cot.Status;
import io.opentakserver.opentakicu.cot.Takv;
import io.opentakserver.opentakicu.cot.auth;
import io.opentakserver.opentakicu.cot.Cot;
import io.opentakserver.opentakicu.cot.event;
import io.opentakserver.opentakicu.cot.uid;

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
    private String atak_trust_store;
    private String atak_trust_store_password;
    private String atak_client_cert;
    private String atak_client_cert_password;
    private String uid;
    private String path;

    private Socket socket;
    private SSLSocket sslSocket;
    private Context context;
    private Intent batteryStatus;

    private String mServerMessage;
    private OnMessageReceived mMessageListener;
    public boolean mRun = false;
    private PrintWriter mBufferOut;
    private BufferedReader mBufferIn;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(Context context, String serverAddress, int port, OnMessageReceived listener) {
        this.context = context;
        this.serverAddress = serverAddress;
        this.port = port;
        mMessageListener = listener;

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = context.registerReceiver(null, ifilter);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        getSettings();
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(final String message) {
        try {
            Runnable runnable = () -> {
                if (mBufferOut != null) {
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        } catch (Exception e) {
            Log.e(TAG ,"Error sending message", e);
        }
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
                Log.d(TAG, "Connected via TCP");
            }

            // Janky way to try to prevent a race condition where we send the initial CoT before the socket is connected
            Log.d(TAG, "Sleeping");
            Thread.sleep(1000);
            Log.d(TAG, "awake");

            XmlFactory xmlFactory = XmlFactory.builder()
                    .xmlInputFactory(new WstxInputFactory())
                    .xmlOutputFactory(new WstxOutputFactory())
                    .build();

            XmlMapper xmlMapper = XmlMapper.builder(xmlFactory).build();
            xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            if (atak_auth) {
                Cot cot = new Cot(atak_username, atak_password, uid);
                auth atakAuth = new auth(cot);
                sendMessage(xmlMapper.writeValueAsString(atakAuth));
            }

            // FTS requires this CoT to be sent before any others
            event event = new event();
            event.setUid(uid);

            Point point = new Point(9999999, 9999999, 9999999);
            event.setPoint(point);

            Contact contact = new Contact(path);

            Takv takv = new Takv(context);

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level * 100 / (float)scale;

            Detail detail = new Detail(contact, null, null, null, takv, new uid(path), null, new Status(batteryPct));
            event.setDetail(detail);

            sendMessage(xmlMapper.writeValueAsString(event));

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
        serverAddress = prefs.getString(Preferences.ATAK_SERVER_ADDRESS, Preferences.ATAK_SERVER_ADDRESS_DEFAULT);
        port = Integer.parseInt(prefs.getString(Preferences.ATAK_SERVER_PORT, Preferences.ATAK_SERVER_PORT_DEFAULT));
        atak_auth = prefs.getBoolean(Preferences.ATAK_SERVER_AUTHENTICATION, Preferences.ATAK_SERVER_AUTHENTICATION_DEFAULT);
        atak_username = prefs.getString(Preferences.ATAK_SERVER_USERNAME, Preferences.ATAK_SERVER_USERNAME_DEFAULT);
        atak_password = prefs.getString(Preferences.ATAK_SERVER_PASSWORD, Preferences.ATAK_SERVER_PASSWORD_DEFAULT);
        atak_ssl = prefs.getBoolean(Preferences.ATAK_SERVER_SSL, Preferences.ATAK_SERVER_SSL_DEFAULT);
        atak_trust_store = prefs.getString(Preferences.ATAK_SERVER_SSL_TRUST_STORE, Preferences.ATAK_SERVER_SSL_TRUST_STORE_DEFAULT);
        atak_trust_store_password = prefs.getString(Preferences.ATAK_SERVER_SSL_TRUST_STORE_PASSWORD, Preferences.ATAK_SERVER_SSL_TRUST_STORE_PASSWORD_DEFAULT);
        atak_client_cert = prefs.getString(Preferences.ATAK_SERVER_SSL_CLIENT_CERTIFICATE, Preferences.ATAK_SERVER_SSL_CLIENT_CERTIFICATE_DEFAULT);
        atak_client_cert_password = prefs.getString(Preferences.ATAK_SERVER_SSL_CLIENT_CERTIFICATE_PASSWORD, Preferences.ATAK_SERVER_SSL_CLIENT_CERTIFICATE_PASSWORD_DEFAULT);
        uid = prefs.getString(Preferences.UID, Preferences.UID_DEFAULT);
        path = prefs.getString(Preferences.STREAM_PATH, Preferences.STREAM_PATH_DEFAULT);
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