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
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;

import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
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
import com.amaze.filemanager.exceptions.CryptException;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.CryptUtil;
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

    private int accentColor;
    private TextView statusText, warningText, cephAddr, cephPort;
    private TextView username, password;
    private AppCompatEditText usernameEditText, passwordEditText;
    private TextInputLayout usernameTextInput, passwordTextInput;

    private Button cephButton;
    private ImageButton passwordButton;
    private MainActivity mainActivity;
    private View rootView, startDividerView, statusDividerView;
    private Spanned spannedStatusConnected;

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
                cephButton.setText(getResources().getString(R.string.ceph_start));
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
        switch(item.getItemId()) {
            case R.id.choose_ceph_port:
                int cur_port = getDefaultPortFromPreferences(); 

                new MaterialDialog.Builder(getActivity())
                    .input(getString(R.string.ceph_port_edit_menu_title),
                            Integer.toString(cur_port), true, (dialog, input) -> {}) 
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .onPositive((dialog, which) -> {
                        EditText editText = dialog.getInputEditText();
                        if( editText != null ) {
                            String name = editText.getText().toString();
                            int portNumber = Integer.parseInt(name);
                            changeCephServerPort(portNumber);
                            Toast.makeText(getActivity(), R.string.ceph_port_change_success,
                                Toast.LENGTH_SHORT).show();
                        }
                    })
                    .positiveText("Change")
                    .negativeText("Cancel")
                    .build()
                    .show();
                return true;
            case R.id.ceph_login:
                MaterialDialog.Builder loginDialogBuilder = new MaterialDialog.Builder(getActivity());
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View rootView = inflater.inflate(R.layout.dialog_ceph_login, null);
                initLoginDialogViews(rootView);
                loginDialogBuilder.customView(rootView, true);
                loginDialogBuilder.title(getString(R.string.ceph_login));

                if (passwordEditText.getText().toString().equals("")) {
                    passwordTextInput.setError(getResources().getString(R.string.field_empty));
                } else if (usernameEditText.getText().toString().equals("")) {
                    usernameTextInput.setError(getResources().getString(R.string.field_empty));
                } else { 
                    // password and username field not empty, let's set them to preferences
                    setUsername(usernameEditText.getText().toString());
                    setPassword(passwordEditText.getText().toString());
                }
                loginDialogBuilder.positiveText(getResources().getString(R.string.set).toUpperCase())
                    .negativeText(getResources().getString(R.string.cancel))
                    .build()
                    .show();
                return true;
            case R.id.ceph_address:
                MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(getActivity());
                dialogBuilder.title(getString(R.string.ceph_address));
                dialogBuilder.input(getString(R.string.ceph_address_hint),
                        getDefaultAddressFromPreferences(),
                        false, (dialog, input) -> {});
                dialogBuilder.onPositive((dialog, which) -> {
                    EditText editText = dialog.getInputEditText();
                    if (editText != null) {
                        setAddress(editText.getText().toString());
                        Toast.makeText(getActivity(), R.string.ceph_address_change_success, Toast.LENGTH_SHORT).show();
                    }
                });

                dialogBuilder.positiveText(getResources().getString(R.string.change).toUpperCase())
                    .negativeText(R.string.cancel)
                    .build()
                    .show();
                return true;
        }
        return false;
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

        accentColor =  mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);

        //ImageView cephImage = (ImageView)rootView.findViewById(R.id.ceph_image);

        updateSpans();
        updateStatus();

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
     * Create the menu to config the ceph server
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mainActivity.getMenuInflater().inflate(R.menu.ceph_server_menu, menu);
    }


    /**
     * Sends a broadcast to connect to ceph server
     */
    private void connectServer() {
        // getContext().sendBroadcast(new Intent(CephService.ACTION_CONNECT_CEPHSERVER));
    }

    /**
     * Sends a broadcast to stop server
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

        statusText.setText(spannedStatusConnected);
        cephButton.setEnabled(true);
        cephButton.setText(getResources().getString(R.string.ceph_stop).toUpperCase());

        final String passwordDecrypted = getPasswordFromPreferences();
        final String passwordBulleted = passwordDecrypted.replaceAll(".", "\u25CF");
        username.setText(getResources().getString(R.string.username) + ": " + getUsernameFromPreferences());
        password.setText(getResources().getString(R.string.password) + ": " + passwordBulleted);

        passwordButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_grey600_24dp));
        if (passwordDecrypted.equals("")) {
            passwordButton.setVisibility(View.GONE);
        } else {
            passwordButton.setVisibility(View.VISIBLE);
        }
        passwordButton.setOnClickListener(v -> {
            if (password.getText().toString().contains("\u25CF")) {
                // password was not visible, let's make it visible
                password.setText(getResources().getString(R.string.password) + ": " + passwordDecrypted);
                passwordButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_off_grey600_24dp));
            } else {
                // password was visible, let's hide it
                password.setText(getResources().getString(R.string.password) + ": " + passwordBulleted);
                passwordButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_grey600_24dp));
            }
        });
        cephPort.setText(getResources().getString(R.string.ceph_port) + ": " + getDefaultPortFromPreferences());
        cephAddr.setText(getResources().getString(R.string.ceph_address) + ": " + getDefaultAddressFromPreferences());
    }


    /**
     * Updates the status spans
     */
    private void updateSpans() {
        String statusHead = getResources().getString(R.string.ceph_status_title) + ": ";
        spannedStatusConnected = Html.fromHtml(statusHead + "<b>&nbsp;&nbsp;" +
                "<font color='" + accentColor + "'>" + getResources().getString(R.string.ceph_status_running) + 
                "</font></b>" + "&nbsp;<i>(" + cephAddr + ")</i>");

    }

    /**
     * create the login Dialog for the S3 service 
     */
    private void initLoginDialogViews(View loginDialogView) {
        usernameEditText = (AppCompatEditText) loginDialogView.findViewById(R.id.edit_text_dialog_ceph_username);
        passwordEditText = (AppCompatEditText) loginDialogView.findViewById(R.id.edit_text_dialog_ceph_password);
        usernameTextInput = (TextInputLayout) loginDialogView.findViewById(R.id.text_input_dialog_ceph_username);
        passwordTextInput = (TextInputLayout) loginDialogView.findViewById(R.id.text_input_dialog_ceph_password);

        if( usernameEditText.setText(getUsernameFromPreferences());
        usernameEditText.setEnabled(true);
        passwordEditText.setText(getPasswordFromPreferences());
        passwordEditText.setEnabled(true);
    }


    /**
     * @return port number at which S3 Server is running
     */
    private int getDefaultPortFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // return preferences.getInt(CephService.PORT_PREFERENCE_KEY, CephService.DEFAULT_PORT);
        return preferences.getInt("CephPort", 7480);
    }

    /**
     * Update port number at S3 Service is running
     */
    private void changeCephServerPort(int port) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.edit()
            // .putInt(CephService.PORT_PREFERENCE_KEY, port)
            .putInt("CephPort", port)
            .apply();
        updateStatus();
    }

    /**
     * @return default username for S3 Service 
     */
    private String getUsernameFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // return preferences.getString(CephService.KEY_PREFERENCE_USERNAME, CephService.DEFAULT_USERNAME);
        return preferences.getString("ceph_username", "ceph_user");
    }

    /**
     * setup the username for S3 service 
     */
    private void setUsername(String username) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //preferences.edit().putString(CephService.KEY_PREFERENCE_USERNAME, username).apply();
        preferences.edit().putString("ceph_username", username).apply();
        updateStatus();
    }

    /**
     * @return deafult password for user in S3 Service
     */
    private String getPasswordFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        try {
            //String encryptedPassword = preferences.getString(CephService.KEY_PREFERENCE_PASSWORD, "");
            String encryptedPassword = preferences.getString("ceph_password_encrypted", "");
            if (encryptedPassword.equals("")) {
                return "";
            } else {
                return CryptUtil.decryptPassword(getContext(), encryptedPassword);
            }
        } catch (CryptException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            // preferences.edit().putString(CephService.KEY_PREFERENCE_PASSWORD, "").apply();
            preferences.edit().putString("ceph_password_encrypted", "").apply();
            return "";
        }
    }

    /**
     * setup the user's password for S3 service 
     */
    private void setPassword(String password) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        try {
            //preferences.edit().putString(CephService.KEY_PREFERENCE_PASSWORD, CryptUtil.encryptPassword(getContext(), password)).apply();
            preferences.edit().putString("ceph_password_encrypted", CryptUtil.encryptPassword(getContext(), password)).apply();
        } catch (CryptException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), getResources().getString(R.string.error), Toast.LENGTH_LONG).show();
        }
        updateStatus();
    }

    /**
     * setup the user's address for S3 service 
     */
    private void setAddress(String address) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //preferences.edit().putString(CephService.KEY_PREFERENCE_PASSWORD, CryptUtil.encryptPassword(getContext(), password)).apply();
        preferences.edit().putString("ceph_address", address).apply();
        Toast.makeText(getContext(), getResources().getString(R.string.ceph_address_change_success), Toast.LENGTH_LONG).show();
        updateStatus();
    }


    /**
     * @return deafult address for the S3 Service
     */
    private String getDefaultAddressFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //return preferences.getString(CephService.KEY_PREFERENCE_ADDRESS, CephService.DEFAULT_ADDRESS);
        return preferences.getString("ceph_address", "");
    }

    /**
     * @return address at which server is running
     */
    private String getCephAddressString() {
     //   return "ceph://" + CephService.getLocalInetAddress(getContext()).getHostAddress() + ":" + CephService.getPort();
     return "";
    }
}
