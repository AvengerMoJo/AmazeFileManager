package com.amaze.filemanager.asynchronous.cephservice;

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
import com.amazonaws.services.s3.model.Owner;

// for http only 
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
public class CephObject { 

    private String name; 
    private String path;
    private long size;
    private long modify;
    private boolean isBucket;

    public CephObject( String obj_name, String obj_path, long obj_size, long obj_modify ) { 
        name = obj_name;
        path = obj_path;
        size = obj_size;
        modify = obj_modify;
        isBucket = false;
    }

    public CephObject( String obj_name, String obj_path, long obj_size, long obj_modify, boolean obj_is_bucket) { 
        name = obj_name;
        path = obj_path;
        size = obj_size;
        modify = obj_modify;
        isBucket = obj_is_bucket;
    }

    public String getName() {
        return name;
    }

    public String getPath() { 
        return path; 
    }

    public long getSize() {
        return size; 
    }

    public boolean isBucket() { 
        return isBucket;
    }


    public long length() {
        return size; 
    }

    public long lastModified() {
        return modify;
    }
}
