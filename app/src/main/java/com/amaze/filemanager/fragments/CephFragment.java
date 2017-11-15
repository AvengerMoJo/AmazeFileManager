package com.amaze.filemanager.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
// import com.amaze.filemanager.services.cephservice.CephService;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.amaze.filemanager.utils.Utils;

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


/**
 * Created by AvengerMoJo on 02-23-2017
 */
public class CephFragment extends Fragment {

    TextView statusText, warningText, cephAddr, cephPort;
    TextView username, password;
    Button cephButton;
    private MainActivity mainActivity;
    private View rootView, startDividerView, statusDividerView;
    ImageButton passwordButton;

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // warningText.setText("");
            } else {
                statusText.setText(getResources().getString(R.string.ceph_status_not_running));
                // warningText.setText(getResources().getString(R.string.ceph_no_wifi));
                cephAddr.setText("Empty");
                cephButton.setText(getResources().getString(R.string.start_ceph));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //mainActivity.getMenuInflater().inflate(R.menu.ftp_server_menu, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_ceph, container, false);
        statusText =(TextView) rootView.findViewById(R.id.text_view_ceph_status);
        //warningText = (TextView) rootView.findViewById(R.id.warningText);
        username = (TextView) rootView.findViewById(R.id.text_view_ceph_username);
        password = (TextView) rootView.findViewById(R.id.text_view_ceph_password);
        cephPort = (TextView) rootView.findViewById(R.id.text_view_ceph_port);
        cephAddr = (TextView) rootView.findViewById(R.id.text_view_ceph_address);
        startDividerView = rootView.findViewById(R.id.divider_ceph_start);
        statusDividerView = rootView.findViewById(R.id.divider_ceph_status);     
        cephButton = (Button) rootView.findViewById(R.id.cephStartStopButton);
        passwordButton = (ImageButton) rootView.findViewById(R.id.ceph_password_visible);

        //ImageView cephImage = (ImageView)rootView.findViewById(R.id.ceph_image);

        //light theme
        if (mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
            startDividerView.setBackgroundColor(Utils.getColor(getContext(), R.color.divider));
            statusDividerView.setBackgroundColor(Utils.getColor(getContext(), R.color.divider));
        } else {
            startDividerView.setBackgroundColor(Utils.getColor(getContext(), R.color.divider_dark_card));
            statusDividerView.setBackgroundColor(Utils.getColor(getContext(), R.color.divider_dark_card));
        }
        cephButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                if (!CephService.isConnected()) {
                    if (CephService.isConnectedToWifi(getContext()))
                        connectServer();
                    else
                        warningText.setText(getResources().getString(R.string.ceph_no_wifi));
                } else {
                    stopServer();
                }
                */
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        mainActivity.getAppbar().setTitle(R.string.ceph);
        mainActivity.floatingActionButton.hideMenuButton(true);
        mainActivity.buttonBarFrame.setVisibility(View.GONE);
        mainActivity.supportInvalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Sends a broadcast to connect to ceph server
     */
    private void connectServer() {
        // getContext().sendBroadcast(new Intent(CephService.ACTION_CONNECT_CEPHSERVER));
    }

    /**
     * Sends a broadcast to stop ftp server
     */
    private void stopServer() {
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Update UI widgets based on connection status
     */
    private void updateStatus() {
        /*
        if( CephService.isConnected() )
            cephAddrText.setText(CephService.getOwnerId() + " is now connected to Ceph" );
        else 
            cephAddrText.setText("Not connected ..." );
            */
    }

    /**
     * @return address at which server is running
     */
    private String getCephAddressString() {
     //   return "ceph://" + CephService.getLocalInetAddress(getContext()).getHostAddress() + ":" + CephService.getPort();
     return "";
    }
}
