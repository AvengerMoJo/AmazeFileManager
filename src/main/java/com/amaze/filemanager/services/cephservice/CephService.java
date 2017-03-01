package com.amaze.filemanager.services.cephservice;

/**
 * Created by AvengerMoJo on 23-02-2017.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

/*
import org.apache.ftpserver.ConnectionConfigFactory;

*/

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import java.util.Date;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.util.StringUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
//import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.HttpMethod;


import java.net.URL;


// for http only 
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;

public class CephService extends Service implements Runnable {


    public static final int DEFAULT_PORT = 7480;
    public static final String PORT_PREFERENCE_KEY = "RADOS_Port";

    public static int getDefaultPortFromPreferences(SharedPreferences preferences) {
        try {
            return preferences.getInt(PORT_PREFERENCE_KEY, DEFAULT_PORT);
        } catch (ClassCastException ex) {
            Log.e("CephService", "Default RADOS port preference is not an int. Resetting to default.");
            changeCephServerPort(preferences, DEFAULT_PORT);

            return DEFAULT_PORT;
        }
    }
    public interface CephConnectionListener{
        void addCeph(String name, String path, String port, String access_key, String secret_key);
    }

    private static CephConnectionListener cephConnectionListener=null;

    public static void changeCephServerPort(SharedPreferences preferences, int port) {
        preferences.edit()
                   .putInt(PORT_PREFERENCE_KEY, port)
                   .apply();
    }

    private static final String TAG = CephService.class.getSimpleName();

    // Service will (global) broadcast when server start/stop
    //static public final String ACTION_STARTED = "com.amaze.filemanager.services.cephservice.CephReceiver.FTPSERVER_STARTED";
    //static public final String ACTION_STOPPED = "com.amaze.filemanager.services.cephservice.CephReceiver.FTPSERVER_STOPPED";
    //static public final String ACTION_FAILEDTOSTART = "com.amaze.filemanager.services.cephservice.CephReceiver.FTPSERVER_FAILEDTOSTART";

    // RequestStartStopReceiver listens for these actions to start/stop this server
    //static public final String ACTION_START_FTPSERVER = "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_START_FTPSERVER";
    //static public final String ACTION_STOP_FTPSERVER = "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_STOP_FTPSERVER";

    static public final String ACTION_CONNECT_CEPHSERVER = "com.amaze.filemanager.services.cephservice.CephReceiver.ACTION_CONNECT_CEPHSERVER"; 

    private static AWSCredentials credentials = null;
    private static ClientConfiguration clientConfig = null;
    private static AmazonS3 conn = null;

    private static String ceph_host = null;
    private static int port = 7480;

    private String access_key = "GDI7SZ36Z7CF7PY2WZB7";
    private String secret_key = "evHf7mG5PpOB4dQzUk7BdeEjb0FB92K3562PQrk5";

    private static Owner me = null;
    private static List<Bucket> root_buckets = null;
    private static String current_bucket = null;

    protected boolean shouldExit = false;

    protected static Thread serverThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldExit = false;
        int attempts = 10;
        while (serverThread != null) {
            if (attempts > 0) {
                attempts--;
            } else {
                return START_STICKY;
            }
        }

        if (intent != null && intent.getStringExtra("access_key") != null && intent.getStringExtra("secret_key") != null) {
            access_key = intent.getStringExtra("access_key");
            secret_key = intent.getStringExtra("secret_key");
        }
        serverThread = new Thread(this);
        serverThread.start();
        Log.d(TAG, "CephServerService.startCommand() started");

        return START_STICKY;
    }

    public class CephServiceBinder extends Binder {
        public CephService getService() {
            return CephService.this;
        }
    }

    private IBinder cephServiceBinder = new CephServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind Ceph Service being binded");
        return cephServiceBinder;
    }

    public void setConnectionListener(CephConnectionListener client) {
        Log.d(TAG, "Ceph Service set listener " );
        cephConnectionListener = client;
    }

    // this should not need to update until multiple account setup 
    public void setupS3() { 
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //System.setProperty(SDKGlobalConfiguration.ENABLE_S3_SIGV4_SYSTEM_PROPERTY, "true");
        credentials = new BasicAWSCredentials(access_key, secret_key);
        clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);
        //clientConfig.setSignerOverride("S3SignerType");
        ceph_host = "ceph://avengermojo.ddns.net";
        port = getDefaultPortFromPreferences(preferences);
    }
    public void connectCephServer() { 
        Log.d(TAG, "Ceph connectCephServer");
        if( serverThread == null ) { 
            Log.d(TAG, " I should be inside the serverThread ... but it is NULL");
            serverThread = new Thread(this);
            serverThread.start();
        }
        if( conn != null ) { 
            Log.d(TAG, " I am not empty ... should I be reconnecting?");
            conn = null;
        }
        // Another way to setup client 
        //conn = new AmazonS3Client(credentials);
        // AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        conn = new AmazonS3Client(credentials, clientConfig);  // for http 
        conn.setEndpoint( getEndPoint() );
    }

    @Override
    public void run() {
        Log.d(TAG, "CephServerService.run() started");
        setupS3(); 
        //ceph_host = ceph_host.toLowerCase();
        //if( ceph_host.startsWith( "ceph://" ) ) { 
            //String ceph_host_endpoint  = ceph_host.replaceFirst("^ceph://", "");
            //conn.setEndpoint(ceph_host_endpoint + ":" + port);
            //System.out.println("Connecting to Server : " + ceph_host_endpoint + ":" + port);
        //}
        connectCephServer(); 
        me =  conn.getS3AccountOwner(); 
        if( me != null ) {
            System.out.println("Owner :" + me.toString() );
            if( cephConnectionListener != null ) { 
                //String s_port = new String(port);
                cephConnectionListener.addCeph( me.getId(), ceph_host, String.valueOf(port), access_key, secret_key);
            }
        }
        for (Bucket bucket : getRootBucket() ) {
            System.out.println("CEPH Bucket list " + bucket.getName() + "\t" +
                    StringUtils.fromDate(bucket.getCreationDate()));
        } 
        
        //sendBroadcast(new Intent(CephService.ACTION_STARTED));
        //sendBroadcast(new Intent(CephService.ACTION_FAILEDTOSTART));
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy() Stopping server");
        shouldExit = true;
        if (serverThread == null) {
            conn = null;
            Log.w(TAG, "Stopping with null serverThread");
            return;
        }
        serverThread.interrupt();
        try {
            serverThread.join(10000); // wait 10 sec for server thread to finish
        } catch (InterruptedException e) {
        }
        if (serverThread.isAlive()) {
            Log.w(TAG, "Server thread failed to exit");
        } else {
            Log.d(TAG, "serverThread join()ed ok");
            serverThread = null;
            conn = null;
        }
        Log.d(TAG, "CephServerService.onDestroy() finished");
    }

    //Restart the service if the app is closed from the recent list
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    public static boolean isConnectedToWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static boolean isConnected() {
        // return true if and only if a server Thread is running
        if (serverThread == null) {
            Log.d(TAG, "Server is not running (null serverThread)");
            return false;
        }
        if (conn != null) {
            Log.d(TAG, "Ceph server is alive");
        } else {
            Log.d(TAG, "Ceph is not connected!....should I be reconnecting? ");
        }
        return true;
    }

    public static void sleepIgnoreInterupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public static int getPort() {
        return port;
    }

    public static String getRoot() { 
        return ceph_host;
    }

    public static boolean isRoot(String path) { 
        return Objects.equals(ceph_host, path) || Objects.equals(ceph_host+"/", path);
    }

    public static String getEndPoint() {
        String endpoint  = ceph_host.replaceFirst("^ceph://", "");
        endpoint += ":" + port; 
        Log.d(TAG, "getEndPoint ->" + endpoint ); 
        return endpoint; 
    }

    public static String getOwnerId() { 
        if( me != null ) { 
            return me.getId(); 
        } else 
            return "Unknown"; 
    }

    public static List<Bucket> getRootBucket() { 
        if( root_buckets != null ) {
            return root_buckets;
        }
        if( conn != null ) { 
            root_buckets = conn.listBuckets();
            return root_buckets;
        }
        return null;
    }

    public static List<S3ObjectSummary> getObjectsListSummary() { 
        Log.d("CephService", "getObjects with default current_bucket");
        if( conn != null ) { 
            ObjectListing objects = conn.listObjects(current_bucket);
            List<S3ObjectSummary> summaries = objects.getObjectSummaries();
            int i=0;
            while (objects.isTruncated()) {
                   objects = conn.listNextBatchOfObjects(objects);
                   summaries.addAll(objects.getObjectSummaries());
            }
            /*for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                System.out.println(objectSummary.getKey() + "\t" +
                        objectSummary.getSize() + "\t" +
                        StringUtils.fromDate(objectSummary.getLastModified()));
            }
            */
            return summaries;
        }
        Log.d("CephService", "ceph server is not connected");
        return null;
    }

    public static List<S3ObjectSummary> getObjectsListSummaryByBucket( String bucket_name ) {
        Log.d("CephService", "getObjects with bucket name " + bucket_name );
        setBucketName( bucket_name );
        return getObjectsListSummary(); 
    }

    public static URL getObjectURL( String key ) {
        //String b_name = current_bucket.replaceFirst( "^/", "" );
        String b_name = current_bucket;
        Log.d("CephService", "getObjectURL with bucket name " + b_name + " key " + key );
        URL url=null;
        if( conn != null ) {
            //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            //StrictMode.setThreadPolicy(policy);

            // conn.setObjectAcl(b_name, key, CannedAccessControlList.PublicRead);
            //String region = conn.getRegionName();
            Log.d("CephService", "conn.getRegionName() " + region ); 
            Date expiration = new Date((new Date().getTime()) + 36000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(b_name, key);
            request.setMethod(HttpMethod.GET);
            request.setExpiration( expiration );
            //request.setSSEAlgorithm(new SSEAlgorithm.fromString("aws:kms"));
            //request.setSSEAlgorithm(SSEAlgorithm.KMS.getAlgorithm());
            url = conn.generatePresignedUrl(request);
            Log.d("CephService", "conn.generatePresignedUrl" + url ); 
            // url = conn.getUrl( b_name, key ); 
            url = conn.generatePresignedUrl(b_name, key, expiration );
            Log.d("CephService", "conn.generatePresignedUrl" + url ); 
        }
        return url;
    }

    public static void setBucketName( String new_bucket_name ) {
        current_bucket = new_bucket_name;
    }
}
