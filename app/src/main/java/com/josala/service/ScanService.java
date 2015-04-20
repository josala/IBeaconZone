package com.josala.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.josala.business.MessageGenerator;
import com.josala.ibeacon.IBeacon;
import com.josala.ibeaconzone.MainActivity;
import com.josala.ibeaconzone.R;

/**
 * Launches a loop that enables bluetooth scans for a certain period of time.
 */
public class ScanService extends Service {

    private static final String TAG = "iBeacon TEST";

    private static ScanService instance = null;

    private Handler mHandler;
    private ArrayList<IBeacon> scannedIBeacons;
    private ArrayList<IBeacon> previousScannedIBeacons;
    private HashMap<String, IBeacon> scannedMap;
    private HashMap<String, IBeacon> auxMap;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private NotificationManager mNotificationManager;
    private String message = "";
    public static final int NOTIFICATION_ID = 0;
    private static final long SCAN_PERIOD = 5000;
    private static final long IDLE_PERIOD = 1000;
    private Timer timer;
    private TimerTask scanTimerTask;
    private boolean isScanning;
    //set at true by default to avoid app swipe close data reset.
    private boolean isOnBackground = true;
    public static final String EVENT_NAME = "refresh-list";
    private final static String BusinessUUID = "46a7518a-8052-4c96-8671-aa51efee3f57";

    public ScanService() {
        super();
        this.instance = this;
    }

    public static ScanService getSingleton() {
        if (instance == null) {
            instance = new ScanService();
        }
        return instance;
    }

    public static void actionStart(Context ctx, Class<?> cls) {
        Intent scanIntent = new Intent(ctx, cls);
        ctx.startService(scanIntent);

    }

    @Override
    public IBinder onBind(Intent intent) {
        // No need to bind service
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ONSTARTCOMMAND");

        long TIMER_PERIOD = SCAN_PERIOD + IDLE_PERIOD;

        if (timer != null) {
            timer.cancel();
        }

        mHandler = new Handler();
        timer = new Timer();
        scannedIBeacons = new ArrayList<IBeacon>();
        scannedMap = new HashMap<String, IBeacon>();
        auxMap = new HashMap<String, IBeacon>();
        previousScannedIBeacons = new ArrayList<IBeacon>();

        scanTimerTask = new TimerTask() {

            @Override
            public void run() {

                //Check if radio is enabled before every loop
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {

                    isScanning = mBluetoothAdapter.startLeScan(mLeScanCallback);
                    Log.d(TAG, "isScanning: " + isScanning);

                    final Runnable r = new Runnable() {

                        @Override
                        public void run() {

                            isScanning = false;
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            Log.d(TAG, "stop scan");

                            //generate a HashMap with current cycle scanned ibeacons.
                            Log.e(TAG, "ARRAYLIST BEACONS TO HASH SIZE: " + scannedIBeacons.size());
                            scannedMap = generateHashMap(scannedIBeacons);

                            //TEST
                            Log.d(TAG, "SCANMAP SIZE: " + scannedMap.size());
                            Log.d(TAG, "AUXMAP SIZE: " + auxMap.size());

                            zoneComparator();
                            auxMap = generateHashMap(scannedIBeacons);

                            Log.d(TAG, "AUXMAP 2 SIZE: " + auxMap.size());
                            previousScannedIBeacons = scannedIBeacons;
                            scannedIBeacons.clear();
                        }
                    };
                    mHandler.postDelayed(r, SCAN_PERIOD);
                }

            }
        };

        timer.scheduleAtFixedRate(scanTimerTask, 0, TIMER_PERIOD);

        return super.onStartCommand(intent, flags, startId);
    }

    private HashMap<String, IBeacon> generateHashMap(ArrayList<IBeacon> ibeaconList) {

        HashMap<String, IBeacon> result = new HashMap<String, IBeacon>();
        String key = "";

        if (ibeaconList.isEmpty()) {
            return result;
        }

        for (IBeacon b : ibeaconList) {
            key = b.getProximityUuid() + b.getMajor() + b.getMinor();
            Log.d(TAG, "KEY: " + key);
            result.put(key, b);
        }

        return result;
    }

    /**
     * Compares scans results with previous cycle scan results.
     */
    private void zoneComparator() {

        Set<String> keySet = scannedMap.keySet();
        Iterator<String> iterator = keySet.iterator();
        Log.d(TAG, "KEYSET ITERATOR");
        while (iterator.hasNext()) {
            String key = iterator.next();
            //remove same beacons
            if (auxMap.containsKey(key)) {
                Log.d(TAG, "SAME ZONE beacon removed: " + auxMap.get(key).getMinor());
                auxMap.remove(key);
            } else {
                //if previous scans don't contain key user has entered a new zone.
                Log.d(TAG, "NEW ZONE beacon: " + scannedMap.get(key).getMinor());
                String msg = "You have entered " + MessageGenerator.generateMessage(scannedMap.get(key));
                notifyChanges(msg);
            }
        }

        //result from removed beacons are exited zones.
        Set<String> auxKeySet = auxMap.keySet();
        Iterator<String> auxIterator = auxKeySet.iterator();
        while (auxIterator.hasNext()) {
            String key = auxIterator.next();
            String msg = "You have left " + MessageGenerator.generateMessage(auxMap.get(key));
            Log.d(TAG, "LEFT ZONE beacon: " + auxMap.get(key).getMinor());
            notifyChanges(msg);
        }

    }

    private void notifyChanges(String msg) {

        message = msg;

        if (isOnBackground) {
            //Send notification
            Log.d(TAG, "app is on BACKGROUND, send notification: " + msg);
            generateNotification(msg);

        } else {
            //Send broadcast to refresh activity
            Log.d(TAG, "app is on FOREGROUND, send broadcast: " + msg);
            Intent intent = new Intent(EVENT_NAME);
            //No need to send global broadcast besides LocalBroadcastManager is more efficient.
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    /**
     * Callback for ble scans. Discriminates devices by uuid.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi,
                             final byte[] scanRecord) {

            Log.d(TAG, "device found: " + device);

            String uuid = IBeacon.getUUIDFromScan(scanRecord);

            if (uuid.equals(BusinessUUID)) {

                // https://code.google.com/p/android/issues/detail?id=58607
                IBeacon b = new IBeacon();
                b.setProximityUuid(uuid);
                b.setMajor(IBeacon.getMajorFromScan(scanRecord));
                b.setMinor(IBeacon.getMinorFromScan(scanRecord));

                Log.d(TAG, "scannedIBeacons ibeacon: " + b.getProximityUuid());
                Log.d(TAG, "with major: " + b.getMajor());
                Log.d(TAG, "and minor: " + b.getMinor());

                // discriminate already scanned ibeacons
                if (isNewBeacon(scannedIBeacons, b)) {
                    Log.d(TAG, "found NEW IBEACON");
                    Log.d(TAG, "scannedIBeacons NEW ibeacon: " + b.getProximityUuid());
                    Log.d(TAG, "with major: " + b.getMajor());
                    Log.d(TAG, "and minor: " + b.getMinor());
                    Log.d(TAG, "isOnBackground: " + isOnBackground);
                    scannedIBeacons.add(b);
                }
            }

        }
    };

    /**
     * @param ArrayList<IBeacon> list
     * @param IBeacon            b
     * @return true if the given IBeacon does not exist in the given list
     */
    private boolean isNewBeacon(ArrayList<IBeacon> list, IBeacon b) {
        boolean result = false;

        if (list.isEmpty()) {
            result = true;
        } else {
            int l = 0;
            for (IBeacon beacon : list) {
                if (!isSameBeacon(b, beacon)) {
                    l++;
                }
            }
            if (l == list.size()) {
                result = true;
            }
        }
        return result;
    }

    /**
     * @param IBeacon a
     * @param IBeacon b
     * @return true if both ibeacons have same attributes.
     */
    private boolean isSameBeacon(IBeacon a, IBeacon b) {
        boolean result = false;

        String uuidA = a.getProximityUuid();
        String uuidB = b.getProximityUuid();
        int majorA = a.getMajor();
        int majorB = a.getMajor();
        int minorA = a.getMinor();
        int minorB = b.getMinor();

        if (uuidA.equals(uuidB) && majorA == majorB && minorA == minorB) {
            result = true;
        }
        return result;
    }

    private void generateNotification(String message) {

        try {

            String title = "iBeaconTest";

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("msg", message);

            stackBuilder.addNextIntent(intent);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // create the notification
            CharSequence contentTitle, contentMessage = null;
            contentTitle = (CharSequence) title;

            contentMessage = (CharSequence) message;

            Notification.Builder mBuilder = new Notification.Builder(this)
                    .setTicker(contentTitle)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(contentTitle)
                    .setContentText(contentMessage)
                    .setSound(
                            RingtoneManager
                                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVibrate(new long[]{0, 100, 200, 300})
                    .setAutoCancel(true);

            mBuilder.setContentIntent(contentIntent);

            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void setIsBackground(boolean background) {
        isOnBackground = background;
    }

    public ArrayList<IBeacon> getScannedIBeacons() {
        return previousScannedIBeacons;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Removes all scanned ibeacons from the list.
     */
    public void resetScannedIBeacons() {
        scannedIBeacons.clear();
    }
}
