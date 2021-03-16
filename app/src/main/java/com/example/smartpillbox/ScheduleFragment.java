package com.example.smartpillbox;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;



import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.UUID;


public class ScheduleFragment extends Fragment {

    private Button syncButton;
    private static final int REQUEST_ENABLE_BT = 1;
    private ListView list;
    private ArrayList<String> listItems;
    private ArrayAdapter<String> adapter;

    private ArrayList<HashMap> scheduleList;
    private HashMap scheduleMap = new HashMap();
    private ArrayList<HashMap> tempList = new ArrayList<HashMap>();

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //private BluetoothAdapter.LeScanCallback mLeScanCallback =

    private BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    private boolean scanning;
    private Handler handler = new Handler();
    private DeviceScanActivity.LeDeviceListAdapter mLeDeviceListAdapter;


    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    /**
     onCreateView creates the page layout when the page is opened
     @param inflater
     @param container
     @param savedInstanceState
     @return view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_schedule, container, false);
        loadFromDatabase();//Load the users medication from firebase
        bluetoothSetUp();
        syncButton = (Button) view.findViewById(R.id.bluetooth);
        listItems = new ArrayList<>();
        list = (ListView) view.findViewById(R.id.medSchedule);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, listItems);
        list.setAdapter(adapter);
        syncButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final String TAG = "Bluetooth Button";
                Log.d(TAG, "Pressed!");
                scanLeDevice();

            }
        });
        return view;
    }

    private void scanLeDevice() {
        if(bluetoothLeScanner != null) {
            if (!scanning) {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);
                    }
                }, SCAN_PERIOD);

                scanning = true;
                bluetoothLeScanner.startScan(leScanCallback);
            } else {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mLeDeviceListAdapter.addDevice(result.getDevice());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    };

    
    /**
    bluetoothSetUp performs 2 tasks relating to Bluetooth
        1. Ensure that the device the app is running on supports Bluetooth.
        2. Check that bluetooth is enabled, if not request permission to turn it on.
     */
    private void bluetoothSetUp() {
        final String TAG = "Bluetooth";
        //Check that device supports Bluetooth
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Bluetooth not supported!");
        } else {
            Log.d(TAG, "Bluetooth supported!");
        }

        //Check that bluetooth is enabled, if not request permission to turn it on.
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    /**
    loadFromDatabase loads all of the users previously saved data. This function is called in
    onCreateView so that the users information is displayed when they open the page.
     */
    public void loadFromDatabase(){
        GoogleSignInAccount acct = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getActivity());   //Get last signed in account
        if (acct != null) {
            //Retrieve all documents in path users->userId->medications
            db.collection("users").document(acct.getId()).collection("medications")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        private static final String TAG = "Load Data";

                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    Map temp = document.getData();
                                    String tempName = (String) temp.get("medName");
                                    String tempTime = (String) temp.get("time");
                                    HashMap tempMap = new HashMap();
                                    tempMap.put("time", tempTime);
                                    tempMap.put("medName", tempName);
                                    tempList.add(tempMap);
                                    Log.d(TAG, "Successfully loaded data!");
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                            sortByTime(tempList);
                        }
                    });
        }
    }


    /**
    sortByTime sorts the users pill schedule by time from 00:00 hrs to 23:59 hrs.
    @param schedule : the schedule to be sorted
     */
    public void sortByTime(ArrayList schedule){
        HashMap tempMap;
        int [] intArray = new int[schedule.size()];
        //Get all of the times that medications are taken
        for (int i = 0; i < schedule.size(); i++) {
            tempMap = (HashMap) schedule.get(i);
            intArray[i] = Integer.parseInt((String) tempMap.get("time"));
        }
        //Sort the times
        Arrays.sort(intArray);
        //Add the items to the schedule
        for(int i = 0; i < intArray.length; i++) {
            for (int j = 0; j < schedule.size(); j++){
                tempMap = (HashMap) schedule.get(j);
                if (((String) tempMap.get("time")).equals(String.valueOf(intArray[i]))){
                    if((String) tempMap.get("medName") != null) {
                        listItems.add((String) tempMap.get("time") + ":00 - " + (String) tempMap.get("medName"));
                        tempMap.remove("medName");
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }


    /**
     * Activity for scanning and displaying available Bluetooth LE devices.
     */
    public static class DeviceScanActivity extends ListActivity {
        private LeDeviceListAdapter mLeDeviceListAdapter;
        private BluetoothAdapter mBluetoothAdapter;
        private boolean mScanning;
        private Handler mHandler;
        private static final int REQUEST_ENABLE_BT = 1;
        // Stops scanning after 10 seconds.
        private static final long SCAN_PERIOD = 10000;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //getActionBar().setTitle(R.string.title_devices);
            mHandler = new Handler();
            // Use this check to determine whether BLE is supported on the device.  Then you can
            // selectively disable BLE-related features.
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                finish();
            }
            // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
            // BluetoothAdapter through BluetoothManager.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            // Checks if Bluetooth is supported on the device.
            if (mBluetoothAdapter == null) {
                //Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            //getMenuInflater().inflate(R.menu.main, menu);
            if (!mScanning) {
               // menu.findItem(R.id.menu_stop).setVisible(false);
               // menu.findItem(R.id.menu_scan).setVisible(true);
               // menu.findItem(R.id.menu_refresh).setActionView(null);
            } else {
               // menu.findItem(R.id.menu_stop).setVisible(true);
               // menu.findItem(R.id.menu_scan).setVisible(false);
               // menu.findItem(R.id.menu_refresh).setActionView(
                     //   R.layout.actionbar_indeterminate_progress);
            }
            return true;
        }
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
               /* case R.id.menu_scan:
                    mLeDeviceListAdapter.clear();
                    scanLeDevice(true);
                    break;
                case R.id.menu_stop:
                    scanLeDevice(false);
                    break;*/
            }
            return true;
        }
        @Override
        protected void onResume() {
            super.onResume();
            // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user to grant permission to enable it.
            if (!mBluetoothAdapter.isEnabled()) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
            // Initializes list view adapter.
            mLeDeviceListAdapter = new LeDeviceListAdapter();
            setListAdapter(mLeDeviceListAdapter);
            scanLeDevice(true);
        }
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            // User chose not to enable Bluetooth.
            if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
        @Override
        protected void onPause() {
            super.onPause();
            scanLeDevice(false);
            mLeDeviceListAdapter.clear();
        }
        @Override
        protected void onListItemClick(ListView l, View v, int position, long id) {
            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
            if (device == null) return;
            //final Intent intent = new Intent(this, DeviceControlActivity.class);
            //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
            //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            }
            //startActivity(intent);
        }
        private void scanLeDevice(final boolean enable) {
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        invalidateOptionsMenu();
                    }
                }, SCAN_PERIOD);
                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            invalidateOptionsMenu();
        }
        // Adapter for holding devices found through scanning.
        private class LeDeviceListAdapter extends BaseAdapter {
            private ArrayList<BluetoothDevice> mLeDevices;
            private LayoutInflater mInflator;
            public LeDeviceListAdapter() {
                super();
                mLeDevices = new ArrayList<BluetoothDevice>();
                mInflator = DeviceScanActivity.this.getLayoutInflater();
            }
            public void addDevice(BluetoothDevice device) {
                if(!mLeDevices.contains(device)) {
                    mLeDevices.add(device);
                }
            }
            public BluetoothDevice getDevice(int position) {
                return mLeDevices.get(position);
            }
            public void clear() {
                mLeDevices.clear();
            }
            @Override
            public int getCount() {
                return mLeDevices.size();
            }
            @Override
            public Object getItem(int i) {
                return mLeDevices.get(i);
            }
            @Override
            public long getItemId(int i) {
                return i;
            }
            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                ViewHolder viewHolder;
                // General ListView optimization code.
                if (view == null) {
                    //view = mInflator.inflate(R.layout.listitem_device, null);
                    viewHolder = new ViewHolder();
                    //viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                    //viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                    view.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) view.getTag();
                }
                BluetoothDevice device = mLeDevices.get(i);
                final String deviceName = device.getName();
                if (deviceName != null && deviceName.length() > 0)
                    viewHolder.deviceName.setText(deviceName);
                else
                    //viewHolder.deviceName.setText(R.string.unknown_device);
                viewHolder.deviceAddress.setText(device.getAddress());
                return view;
            }
        }
        // Device scan callback.
        private BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                };
        static class ViewHolder {
            TextView deviceName;
            TextView deviceAddress;
        }
    }


    public static class BluetoothLeUart extends BluetoothGattCallback implements BluetoothAdapter.LeScanCallback {

        // UUIDs for UART service and associated characteristics.
        public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
        public static UUID TX_UUID   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
        public static UUID RX_UUID   = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

        // UUID for the UART BTLE client characteristic which is necessary for notifications.
        public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        // UUIDs for the Device Information service and associated characeristics.
        public static UUID DIS_UUID       = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
        public static UUID DIS_MANUF_UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
        public static UUID DIS_MODEL_UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
        public static UUID DIS_HWREV_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
        public static UUID DIS_SWREV_UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");

        // Internal UART state.
        private Context context;
        private WeakHashMap<Callback, Object> callbacks;
        private BluetoothAdapter adapter;
        private BluetoothGatt gatt;
        private BluetoothGattCharacteristic tx;
        private BluetoothGattCharacteristic rx;
        private boolean connectFirst;
        private boolean writeInProgress; // Flag to indicate a write is currently in progress

        // Device Information state.
        private BluetoothGattCharacteristic disManuf;
        private BluetoothGattCharacteristic disModel;
        private BluetoothGattCharacteristic disHWRev;
        private BluetoothGattCharacteristic disSWRev;
        private boolean disAvailable;

        // Queues for characteristic read (synchronous)
        private Queue<BluetoothGattCharacteristic> readQueue;

        // Interface for a BluetoothLeUart client to be notified of UART actions.
        public interface Callback {
            public void onConnected(BluetoothLeUart uart);
            public void onConnectFailed(BluetoothLeUart uart);
            public void onDisconnected(BluetoothLeUart uart);
            public void onReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx);
            public void onDeviceFound(BluetoothDevice device);
            public void onDeviceInfoAvailable();
        }

        public BluetoothLeUart(Context context) {
            super();
            this.context = context;
            this.callbacks = new WeakHashMap<Callback, Object>();
            this.adapter = BluetoothAdapter.getDefaultAdapter();
            this.gatt = null;
            this.tx = null;
            this.rx = null;
            this.disManuf = null;
            this.disModel = null;
            this.disHWRev = null;
            this.disSWRev = null;
            this.disAvailable = false;
            this.connectFirst = false;
            this.writeInProgress = false;
            this.readQueue = new ConcurrentLinkedQueue<BluetoothGattCharacteristic>();
        }

        // Return instance of BluetoothGatt.
        public BluetoothGatt getGatt() {
            return gatt;
        }

        // Return true if connected to UART device, false otherwise.
        public boolean isConnected() {
            return (tx != null && rx != null);
        }

        public String getDeviceInfo() {
            if (tx == null || !disAvailable ) {
                // Do nothing if there is no connection.
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Manufacturer : " + disManuf.getStringValue(0) + "\n");
            sb.append("Model        : " + disModel.getStringValue(0) + "\n");
            sb.append("Firmware     : " + disSWRev.getStringValue(0) + "\n");
            return sb.toString();
        };

        public boolean deviceInfoAvailable() { return disAvailable; }

        // Send data to connected UART device.
        public void send(byte[] data) {
            if (tx == null || data == null || data.length == 0) {
                // Do nothing if there is no connection or message to send.
                return;
            }
            // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
            tx.setValue(data);
            writeInProgress = true; // Set the write in progress flag
            gatt.writeCharacteristic(tx);
            // ToDo: Update to include a timeout in case this goes into the weeds
            while (writeInProgress); // Wait for the flag to clear in onCharacteristicWrite
        }

        // Send data to connected UART device.
        public void send(String data) {
            if (data != null && !data.isEmpty()) {
                send(data.getBytes(Charset.forName("UTF-8")));
            }
        }

        // Register the specified callback to receive UART callbacks.
        public void registerCallback(Callback callback) {
            callbacks.put(callback, null);
        }

        // Unregister the specified callback.
        public void unregisterCallback(Callback callback) {
            callbacks.remove(callback);
        }

        // Disconnect to a device if currently connected.
        public void disconnect() {
            if (gatt != null) {
                gatt.disconnect();
            }
            gatt = null;
            tx = null;
            rx = null;
        }

        // Stop any in progress UART device scan.
        public void stopScan() {
            if (adapter != null) {
                adapter.stopLeScan(this);
            }
        }

        // Start scanning for BLE UART devices.  Registered callback's onDeviceFound method will be called
        // when devices are found during scanning.
        public void startScan() {
            if (adapter != null) {
                adapter.startLeScan(this);
            }
        }

        // Connect to the first available UART device.
        public void connectFirstAvailable() {
            // Disconnect to any connected device.
            disconnect();
            // Stop any in progress device scan.
            stopScan();
            // Start scan and connect to first available device.
            connectFirst = true;
            startScan();
        }

        // Handlers for BluetoothGatt and LeScan events.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Connected to device, start discovering services.
                    if (!gatt.discoverServices()) {
                        // Error starting service discovery.
                        connectFailure();
                    }
                }
                else {
                    // Error connecting to device.
                    connectFailure();
                }
            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // Disconnected, notify callbacks of disconnection.
                rx = null;
                tx = null;
                notifyOnDisconnected(this);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // Notify connection failure if service discovery failed.
            if (status == BluetoothGatt.GATT_FAILURE) {
                connectFailure();
                return;
            }

            // Save reference to each UART characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);

            // Save reference to each DIS characteristic.
            disManuf = gatt.getService(DIS_UUID).getCharacteristic(DIS_MANUF_UUID);
            disModel = gatt.getService(DIS_UUID).getCharacteristic(DIS_MODEL_UUID);
            disHWRev = gatt.getService(DIS_UUID).getCharacteristic(DIS_HWREV_UUID);
            disSWRev = gatt.getService(DIS_UUID).getCharacteristic(DIS_SWREV_UUID);

            // Add device information characteristics to the read queue
            // These need to be queued because we have to wait for the response to the first
            // read request before a second one can be processed (which makes you wonder why they
            // implemented this with async logic to begin with???)
            readQueue.offer(disManuf);
            readQueue.offer(disModel);
            readQueue.offer(disHWRev);
            readQueue.offer(disSWRev);

            // Request a dummy read to get the device information queue going
            gatt.readCharacteristic(disManuf);

            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true)) {
                // Stop if the characteristic notification setup failed.
                connectFailure();
                return;
            }
            // Next update the RX characteristic's client descriptor to enable notifications.
            BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
            if (desc == null) {
                // Stop if the RX characteristic has no client descriptor.
                connectFailure();
                return;
            }
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!gatt.writeDescriptor(desc)) {
                // Stop if the client descriptor could not be written.
                connectFailure();
                return;
            }
            // Notify of connection completion.
            notifyOnConnected(this);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            notifyOnReceive(this, characteristic);
        }

        @Override
        public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Log.w("DIS", characteristic.getStringValue(0));
                // Check if there is anything left in the queue
                BluetoothGattCharacteristic nextRequest = readQueue.poll();
                if(nextRequest != null){
                    // Send a read request for the next item in the queue
                    gatt.readCharacteristic(nextRequest);
                }
                else {
                    // We've reached the end of the queue
                    disAvailable = true;
                    notifyOnDeviceInfoAvailable();
                }
            }
            else {
                //Log.w("DIS", "Failed reading characteristic " + characteristic.getUuid().toString());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Log.d(TAG,"Characteristic write successful");
            }
            writeInProgress = false;
        }

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            // Stop if the device doesn't have the UART service.
            if (!parseUUIDs(scanRecord).contains(UART_UUID)) {
                return;
            }
            // Notify registered callbacks of found device.
            notifyOnDeviceFound(device);
            // Connect to first found device if required.
            if (connectFirst) {
                // Stop scanning for devices.
                stopScan();
                // Prevent connections to future found devices.
                connectFirst = false;
                // Connect to device.
                gatt = device.connectGatt(context, true, this);
            }
        }

        // Private functions to simplify the notification of all callbacks of a certain event.
        private void notifyOnConnected(BluetoothLeUart uart) {
            for (Callback cb : callbacks.keySet()) {
                if (cb != null) {
                    cb.onConnected(uart);
                }
            }
        }

        private void notifyOnConnectFailed(BluetoothLeUart uart) {
            for (Callback cb : callbacks.keySet()) {
                if (cb != null) {
                    cb.onConnectFailed(uart);
                }
            }
        }

        private void notifyOnDisconnected(BluetoothLeUart uart) {
            for (Callback cb : callbacks.keySet()) {
                if (cb != null) {
                    cb.onDisconnected(uart);
                }
            }
        }

        private void notifyOnReceive(BluetoothLeUart uart, BluetoothGattCharacteristic rx) {
            for (Callback cb : callbacks.keySet()) {
                if (cb != null ) {
                    cb.onReceive(uart, rx);
                }
            }
        }

        private void notifyOnDeviceFound(BluetoothDevice device) {
            for (Callback cb : callbacks.keySet()) {
                if (cb != null) {
                    cb.onDeviceFound(device);
                }
            }
        }

        private void notifyOnDeviceInfoAvailable() {
            for (Callback cb : callbacks.keySet()) {
                if (cb != null) {
                    cb.onDeviceInfoAvailable();
                }
            }
        }

        // Notify callbacks of connection failure, and reset connection state.
        private void connectFailure() {
            rx = null;
            tx = null;
            notifyOnConnectFailed(this);
        }

        // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
        //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
        // This is a workaround function from the SO thread to manually parse advertisement data.
        private List<UUID> parseUUIDs(final byte[] advertisedData) {
            List<UUID> uuids = new ArrayList<UUID>();

            int offset = 0;
            while (offset < (advertisedData.length - 2)) {
                int len = advertisedData[offset++];
                if (len == 0)
                    break;

                int type = advertisedData[offset++];
                switch (type) {
                    case 0x02: // Partial list of 16-bit UUIDs
                    case 0x03: // Complete list of 16-bit UUIDs
                        while (len > 1) {
                            int uuid16 = advertisedData[offset++];
                            uuid16 += (advertisedData[offset++] << 8);
                            len -= 2;
                            uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                        }
                        break;
                    case 0x06:// Partial list of 128-bit UUIDs
                    case 0x07:// Complete list of 128-bit UUIDs
                        // Loop through the advertised 128-bit UUID's.
                        while (len >= 16) {
                            try {
                                // Wrap the advertised bits and order them.
                                ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                                long mostSignificantBit = buffer.getLong();
                                long leastSignificantBit = buffer.getLong();
                                uuids.add(new UUID(leastSignificantBit,
                                        mostSignificantBit));
                            } catch (IndexOutOfBoundsException e) {
                                // Defensive programming.
                                //Log.e(LOG_TAG, e.toString());
                                continue;
                            } finally {
                                // Move the offset to read the next uuid.
                                offset += 15;
                                len -= 16;
                            }
                        }
                        break;
                    default:
                        offset += (len - 1);
                        break;
                }
            }
            return uuids;
        }
    }

}
