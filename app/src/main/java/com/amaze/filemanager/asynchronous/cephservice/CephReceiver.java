package com.amaze.filemanager.asynchronous.cephservice;

/**
 * Created by AvengerMoJo on 02-23-2017.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.amaze.filemanager.fragments.CephFragment;

public class CephReceiver extends BroadcastReceiver {

    static final String TAG = CephReceiver.class.getSimpleName();
    static CephFragment myFragment = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received: " + intent.getAction());

        try {
            if (intent.getAction().equals(CephService.ACTION_CONNECT)) {
                Intent serverService = new Intent(context, CephService.class);
                if (!CephService.isConnected()) {
                    Log.d(TAG, "Connecting : " );
                    context.startService(serverService);
                }
                myFragment.updateStatus();
            } else if (intent.getAction().equals(CephService.ACTION_DISCONNECT)) {
                Intent serverService = new Intent(context, CephService.class);
                if (CephService.isConnected()) {
                    Log.d(TAG, "Disconnecting : " );
                    context.stopService(serverService);
                }
                myFragment.updateStatus();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to ceph on intent " + e.getMessage());
        }
    }

    public void setFragment(CephFragment fragment) {
        Log.d(TAG, "Setting Fragment");
        myFragment = fragment; 
    }


}
