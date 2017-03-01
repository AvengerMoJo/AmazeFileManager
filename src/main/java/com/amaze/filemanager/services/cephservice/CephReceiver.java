package com.amaze.filemanager.services.cephservice;

/**
 * Created by AvengerMoJo on 02-23-2017.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CephReceiver extends BroadcastReceiver {

    static final String TAG = CephReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received: " + intent.getAction());

        try {
            if (intent.getAction().equals(CephService.ACTION_CONNECT_CEPHSERVER)) {
                Intent serverService = new Intent(context, CephService.class);
                if (!CephService.isConnected()) {
                    Log.d(TAG, "Connecting : " );
                    context.startService(serverService);
                }
            }
           /* else if (intent.getAction().equals(FTPService.ACTION_STOP_FTPSERVER)) {
                Intent serverService = new Intent(context, FTPService.class);
                context.stopService(serverService);
            }
            */
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to ceph on intent " + e.getMessage());
        }
    }


}
