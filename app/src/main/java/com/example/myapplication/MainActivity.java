package com.example.myapplication;

import static java.lang.Integer.parseInt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter; // 本地蓝牙适配器
    private BluetoothDevice targetDevice;
    private BluetoothSocket bluetoothSocket;  // 蓝牙通信Socket
    private OutputStream outputStream;  // 数据输出流
    private ListView lvDevices; // 设备列表视图
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();// 发现的设备集合

    private ArrayAdapter<String> deviceAdapter;// 列表适配器
    private BroadcastReceiver discoveryReceiver;// 广播接收器
    private volatile boolean isRunning = false;
    private Thread controlThread;
    private Future<?> movementTask; // 线程池？用于控制任务执行

    private static final byte[] CMD_FORWARD1 = {(byte)0xA5, 0x01, 0x01, (byte)0x5A}; //前进
    private static final byte[] CMD_FORWARD2 = {(byte)0xA5, 0x02, 0x02, (byte)0x5A};
    private static final byte[] CMD_FORWARD3 = {(byte)0xA5, 0x03, 0x03, (byte)0x5A};
    private static final byte[] CMD_FORWARD4 = {(byte)0xA5, 0x04, 0x04, (byte)0x5A};
    private static final byte[] CMD_FORWARD5 = {(byte)0xA5, 0x00, 0x00, (byte)0x5A}; //停止
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // 蓝牙配置常量
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBluetoothSupport();// 初始化蓝牙支持检查
        requestNecessaryPermissions(); // 请求必要权限
        setupScanComponents();// 初始化扫描相关组件
        setupDeviceSelection(); // 配置设备选择事件
        setupButtons();// 设置控制按钮事件
    }

    private void requestNecessaryPermissions() { // 请求必要的运行时权限
        List<String> neededPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    neededPermissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void connectToDevice(BluetoothDevice device) { //连接到指定蓝牙设备
        if (!checkBluetoothPermissions()) {
            requestNecessaryPermissions();
            return;
        }

        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                } else {
                    try {
                        Method m = device.getClass().getMethod("createRfcommSocket", int.class);
                        bluetoothSocket = (BluetoothSocket) m.invoke(device, 1);
                    } catch (Exception e) {
                        throw new IOException("创建Socket失败", e);
                    }
                }

                runOnUiThread(() -> {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    bluetoothAdapter.cancelDiscovery();
                });

                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();

                runOnUiThread(() -> {
                    Toast.makeText(this, "已连接：" + device.getName(), Toast.LENGTH_SHORT).show();
                    lvDevices.setVisibility(View.GONE);
                });

            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "连接失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
                closeSocket();
            }
        }).start();
    }

    private void closeSocket() { //关闭蓝牙Socket连接
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendCommand(byte[] data) { //发送控制指令到已连接设备 发送数据见上面
        try {
            if (outputStream != null) {
                outputStream.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void setupButtons() { // 设置控制，初始化方向控制按钮事件,暂时先这么写着，有需要再改
        Switch switch1 = findViewById(R.id.switch1);
        EditText moveTime = findViewById(R.id.editText1);
        EditText stopTime = findViewById(R.id.editText2);

        // Switch状态监听
        switch1.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                try {
                    int t1 = Integer.parseInt(moveTime.getText().toString());
                    int t2 = Integer.parseInt(stopTime.getText().toString());
                    Toast.makeText(this, "开始前进", Toast.LENGTH_SHORT).show();
                    startIntermittentMovement(t1, t2);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(false); // 回滚 Switch 状态
                }
            } else {
                findViewById(R.id.btnStop).setOnClickListener(v -> sendCommand(CMD_FORWARD5));
                Toast.makeText(this, "终止前进", Toast.LENGTH_SHORT).show();
                stopIntermittentMovement();
                isRunning = false;
            }
        });
        findViewById(R.id.btnForward).setOnClickListener(v -> sendCommand(CMD_FORWARD1)); // 前进
        findViewById(R.id.btnStop).setOnClickListener(v -> sendCommand(CMD_FORWARD5)); // 停止
        findViewById(R.id.btnBackward).setOnClickListener(v -> sendCommand(CMD_FORWARD2));
        findViewById(R.id.btnLeft).setOnClickListener(v -> sendCommand(CMD_FORWARD3));
        findViewById(R.id.btnRight).setOnClickListener(v -> sendCommand(CMD_FORWARD4));
    }
    private void startIntermittentMovement(int t1, int t2) { //循环指令
        if (movementTask != null && !movementTask.isDone()) {
            movementTask.cancel(true);
        }


        isRunning = true;
        executor.execute(() -> {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    sendCommand(CMD_FORWARD1);
                    Thread.sleep(t1);
                    sendCommand(CMD_FORWARD5);
                    Thread.sleep(t2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void stopIntermittentMovement() { // 终止循环
        isRunning = false;
//        executor.shutdownNow();
        if (movementTask != null) {
            movementTask.cancel(true); // 发送中断信号
        }
    }

    private void checkBluetoothSupport() { //检查设备是否支持蓝牙
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show();
//            finish(); // 终止程序，方便调试就注释掉了
        }
    }

    private boolean checkBluetoothPermissions() { //
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, //处理权限请求结果
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            "需要授予" + getPermissionName(permissions[i]) + "权限",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    private String getPermissionName(String permission) { //将android权限转换为可读名称
        switch (permission) {
            case Manifest.permission.BLUETOOTH_CONNECT:
                return "蓝牙连接";
            case Manifest.permission.ACCESS_FINE_LOCATION:
                return "位置信息";
            case Manifest.permission.BLUETOOTH:
                return "蓝牙";
            default:
                return "必要";
        }
    }

    private void setupDeviceSelection() {
        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            if (checkBluetoothPermissions()) {
                bluetoothAdapter.cancelDiscovery();
                BluetoothDevice selectedDevice = deviceList.get(position);
                connectToDevice(selectedDevice);
            }
        });
    }

    private void startDiscovery() {
        if (!checkBluetoothPermissions()) {
            requestNecessaryPermissions();
            return;
        }

        deviceAdapter.clear();
        deviceList.clear();
        // 添加已配对设备
        if (bluetoothAdapter.getBondedDevices() != null) {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                deviceList.add(device);
                deviceAdapter.add("[已配对] " + device.getName() + "\n" + device.getAddress());
            }
        }
        lvDevices.setVisibility(View.VISIBLE);

        discoveryReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        // 检查是否已存在列表中
                        boolean exists = false;
                        for (BluetoothDevice existing : deviceList) {
                            if (existing.getAddress().equals(device.getAddress())) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            deviceList.add(device);
                            String status = (device.getBondState() == BluetoothDevice.BOND_BONDED) ?
                                    "[已配对] " : "[未配对] ";
                            deviceAdapter.add(status + device.getName() + "\n" + device.getAddress());
                        }
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    private void setupScanComponents() {
        lvDevices = findViewById(R.id.lvDevices);
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvDevices.setAdapter(deviceAdapter);

        // 添加下拉刷新功能
        findViewById(R.id.btnScan).setOnClickListener(v -> {
            // 每次点击扫描按钮时重新加载已配对设备
            if (bluetoothAdapter.getBondedDevices() != null) {
                deviceAdapter.clear();
                deviceList.clear();
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                    deviceList.add(device);
                    deviceAdapter.add("[已配对] " + device.getName() + "\n" + device.getAddress());
                }
            }
            startDiscovery();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (discoveryReceiver != null) {
            unregisterReceiver(discoveryReceiver);
        }
        closeSocket();
    }
}