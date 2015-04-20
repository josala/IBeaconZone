package com.josala.ibeaconzone;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import com.josala.service.ScanService;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String TAG = "iBeacon TEST";
    BluetoothAdapter mBluetoothAdapter;
    TextView tvNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvNotification = (TextView) findViewById(R.id.tvNotification);

        //get the bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //check for bluetooth connection.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            showBluetoothAlert();
        }

        //set background flag before service start.
        ScanService.getSingleton().setIsBackground(false);
        ScanService.actionStart(getApplicationContext(), ScanService.class);

        //register to receive messages.
        IntentFilter mIntentFilter = new IntentFilter(ScanService.EVENT_NAME);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver, mIntentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScanService.getSingleton().setIsBackground(false);
        Log.d(TAG, "app onResume");
        //check for bluetooth connection.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            showBluetoothAlert();
        }
        refreshScreenData();
    }

    //this will be called when intent is broadcasted from service.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, android.content.Intent intent) {
            refreshScreenData();
        }
    };

    private void refreshScreenData() {
        Log.d(TAG, "REFRESH SCREEN DATA");
        //retreive iBeaconList from service.

        String msg = ScanService.getSingleton().getMessage();
        tvNotification.setText(msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ScanService.getSingleton().setIsBackground(true);
        Log.w(TAG, "SWIPE CLOSE!!!");

        //unregister receiver.
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);

    }

    private void showBluetoothAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bluetooth not enabled");
        builder.setMessage("Please enable bluetooth in settings.");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
                System.exit(0);
            }
        });
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);
        ScanService.getSingleton().setIsBackground(true);
        Log.d(TAG, "app onSaveInstanceState");

    }

}
