package com.example.bledemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothLeService mBluetoothLeService;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 5000;
    private static final int REQUEST_ENABLE_BT = 1001;
    private static final int REQUEST_CODE_LOCATION_SETTINGS = 1002;
    private static final int PERMISSION_BLE = 1003;
    private boolean mScanning, enable;
    private Handler mHandler;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private RecyclerView recyclerview;
    private TextView count;
    private String mDeviceAddress;
    private int position;
    private ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        mHandler = new Handler();
        initBleAdapter();
        initView();
        findViewById(R.id.btn_check).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    ToastUtils.with(MainActivity.this).show("已开启蓝牙");
                    enable = true;
                }
            }
        });
        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission();
            }
        });
        findViewById(R.id.btn_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mBluetoothLeService.setCharacteristicNotification();
            }
        });
    }

    @AfterPermissionGranted(PERMISSION_BLE)
    private void checkPermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.BLUETOOTH_ADMIN)) {
            Log.i("scan", "拥有权限");
            scanLeDevice(enable);
        } else {
            Log.i("scan", "申请权限");
            EasyPermissions.requestPermissions(this, "申请蓝牙权限", PERMISSION_BLE, Manifest.permission.BLUETOOTH_ADMIN);
        }
    }

    private void initView() {
        dialog = new ProgressDialog(this);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle("配对中...");
        dialog.setButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBluetoothLeService.disconnect();
                }
            }
        });
        count = (TextView) findViewById(R.id.tv_count);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        recyclerview = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerview.setAdapter(mLeDeviceListAdapter);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void initBleAdapter() {
        bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
    }


    private void scanLeDevice(final boolean enable) {
        if (isLocationEnable(this)) {
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        }
                    }
                }, SCAN_PERIOD);

                mScanning = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mLeDeviceListAdapter.clearDevice();
                    //startLeScan(UUID[], BluetoothAdapter.LeScanCallback)搜索指定uuid的蓝牙设备
                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
            } else {
                mScanning = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }
        } else {
            setLocationService();
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("scan", "扫描回调--->" + device.getAddress());
                            count.setText("可配对蓝牙列表：" + mLeDeviceListAdapter.getItemCount());
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }

            };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ENABLE_BT) {
                enable = true;
                ToastUtils.with(this).show("开启蓝牙");
            } else {
                enable = false;
                ToastUtils.with(this).show("不支持蓝牙");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == PERMISSION_BLE) {
            scanLeDevice(enable);
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == PERMISSION_BLE) {
            ToastUtils.with(this).show("蓝牙权限被拒绝");
        }
    }

    //是否开启定位
    public static final boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }

    //跳转定位界面
    private void setLocationService() {
        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(locationIntent, REQUEST_CODE_LOCATION_SETTINGS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        unregisterReceiver(mGattUpdateReceiver);
        dialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void setInit(MessageType type) {
        if (type.type.equals(MessageType.CONNECT)) {
            mDeviceAddress = type.data.toString();
            position = type.position;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (!mBluetoothLeService.initialize()) {
                    Log.e("scan", "Unable to initialize Bluetooth");
                    finish();
                }
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBluetoothLeService.connect(mDeviceAddress);
                dialog.show();
            }
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //获取蓝牙设备
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                ToastUtils.with(MainActivity.this).show("已连接");
                count.setText("可配对蓝牙列表：" + mLeDeviceListAdapter.getItemCount() + "    已连接" + mLeDeviceListAdapter.getData().get(position-1).getAddress());
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                ToastUtils.with(MainActivity.this).show("已断开");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            }
            dialog.dismiss();
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_PING);
        return intentFilter;
    }
}
