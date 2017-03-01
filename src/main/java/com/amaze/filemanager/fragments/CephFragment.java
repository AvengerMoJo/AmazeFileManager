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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.services.cephservice.CephService;
import com.amaze.filemanager.utils.theme.AppTheme;

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

    TextView statusText, warningText, cephAddrText;
    Button cephButton;
    private MainActivity mainActivity;
    private View rootView;

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                warningText.setText("");
            } else {
                //stopServer();
                statusText.setText(getResources().getString(R.string.ceph_status_not_running));
                warningText.setText(getResources().getString(R.string.ceph_no_wifi));
                cephAddrText.setText("Empty");
                //ftpBtn.setText(getResources().getString(R.string.start_ftp));
                cephButton.setText(getResources().getString(R.string.start_ftp));
            }
        }
    };
    /*
    private BroadcastReceiver ftpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == FTPService.ACTION_STARTED) {
                statusText.setText(getResources().getString(R.string.ftp_status_running));
                warningText.setText("");
                cephAddrText.setText(getFTPAddressString());
                ftpBtn.setText(getResources().getString(R.string.stop_ftp));
            } else if (action == FTPService.ACTION_FAILEDTOSTART) {
                statusText.setText(getResources().getString(R.string.ftp_status_not_running));
                warningText.setText("Oops! Something went wrong");
                cephAddrText.setText("");
                ftpBtn.setText(getResources().getString(R.string.start_ftp));
            } else if (action == FTPService.ACTION_STOPPED) {
                statusText.setText(getResources().getString(R.string.ftp_status_not_running));
                cephAddrText.setText("");
                ftpBtn.setText(getResources().getString(R.string.start_ftp));
            }
        }
    };
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        switch (item.getItemId()) {
            case R.id.choose_ftp_port:
                int currentFtpPort = FTPService.getDefaultPortFromPreferences(preferences);

                new MaterialDialog.Builder(getActivity())
                        .input(getString(R.string.ftp_port_edit_menu_title), Integer.toString(currentFtpPort), true, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                            }
                        })
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                EditText editText = dialog.getInputEditText();
                                if (editText != null) {
                                    String name = editText.getText().toString();

                                    int portNumber = Integer.parseInt(name);
                                    if (portNumber < 1024) {
                                        Toast.makeText(getActivity(), R.string.ftp_port_change_error_invalid, Toast.LENGTH_SHORT)
                                             .show();
                                    } else {
                                        FTPService.changeFTPServerPort(preferences, portNumber);
                                        Toast.makeText(getActivity(), R.string.ftp_port_change_success, Toast.LENGTH_SHORT)
                                             .show();
                                    }
                                }
                            }
                        })
                        .positiveText("CHANGE")
                        .negativeText(R.string.cancel)
                        .build()
                        .show();

                return true;
        }
        */
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
        statusText =(TextView) rootView.findViewById(R.id.statusText);
        warningText = (TextView) rootView.findViewById(R.id.warningText);
        cephAddrText = (TextView) rootView.findViewById(R.id.cephAddressText);
        cephButton = (Button) rootView.findViewById(R.id.connectButton);

        ImageView cephImage = (ImageView)rootView.findViewById(R.id.ceph_image);

        //light theme
        if (mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
            cephImage.setImageResource(R.drawable.ic_ceph_light);
        } else {
            //dark
            cephImage.setImageResource(R.drawable.ic_ceph_dark);
        }

        cephButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CephService.isConnected()) {
                    if (CephService.isConnectedToWifi(getContext()))
                        connectServer();
                    else
                        warningText.setText(getResources().getString(R.string.ftp_no_wifi));
                } else {
                    stopServer();
                }
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        mainActivity.setActionBarTitle(getResources().getString(R.string.ceph));
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
        getContext().sendBroadcast(new Intent(CephService.ACTION_CONNECT_CEPHSERVER));
    }

    /**
     * Sends a broadcast to stop ftp server
     */
    private void stopServer() {
        //getContext().sendBroadcast(new Intent(FTPService.ACTION_STOP_FTPSERVER));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatus();
        //IntentFilter wifiFilter = new IntentFilter();
        //wifiFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        //getContext().registerReceiver(mWifiReceiver, wifiFilter);
        //IntentFilter ftpFilter = new IntentFilter();
        //ftpFilter.addAction(FTPService.ACTION_STARTED);
        //ftpFilter.addAction(FTPService.ACTION_STOPPED);
        //ftpFilter.addAction(FTPService.ACTION_FAILEDTOSTART);
        //getContext().registerReceiver(ftpReceiver, ftpFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        //getContext().unregisterReceiver(mWifiReceiver);
        //getContext().unregisterReceiver(ftpReceiver);
    }

    /**
     * Update UI widgets based on connection status
     */
    private void updateStatus() {
        if( CephService.isConnected() )
            cephAddrText.setText(CephService.getOwnerId() + " is now connected to Ceph" );
        else 
            cephAddrText.setText("Not connected ..." );
    }

    /**
     * @return address at which server is running
     */
    //private String getFTPAddressString() {
     //   return "ftp://" + FTPService.getLocalInetAddress(getContext()).getHostAddress() + ":" + FTPService.getPort();
    //}
}
