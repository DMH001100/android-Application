package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ArmControlActivity extends AppCompatActivity {

    // 初始位置值 (500-2500)
    private int p0 = 1500;  // 通道0
    private int p3 = 1500;  // 通道3
    private int p16 = 1500; // 通道16
    private int p18 = 1500; // 通道18
    private int p24 = 1500; // 通道24
    private int p26 = 1500; // 通道26

    // 步进值
    private static final int STEP = 100;
    private static final int MIN_PULSE = 500;
    private static final int MAX_PULSE = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arm_control);

        // 返回主页面按钮
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 初始化所有按钮
        setupButton(R.id.btn1u, 0, STEP);
        setupButton(R.id.btn1d, 0, -STEP);
        setupButton(R.id.btn3u, 3, STEP);
        setupButton(R.id.btn3d, 3, -STEP);
        setupButton(R.id.btn16u, 16, STEP);
        setupButton(R.id.btn16d, 16, -STEP);
        setupButton(R.id.btn18u, 18, STEP);
        setupButton(R.id.btn18d, 18, -STEP);
        setupButton(R.id.btn24l, 24, STEP);
        setupButton(R.id.btn24r, 24, -STEP);
        setupButton(R.id.btn26u, 26, STEP);
        setupButton(R.id.btn26d, 26, -STEP);

        // 添加复位按钮
//        Button btnReset = findViewById(R.id.btnReset);
//        btnReset.setOnClickListener(v -> resetAllPositions());
    }

    private void setupButton(int buttonId, final int channel, final int delta) {
        Button button = findViewById(buttonId);
        button.setOnClickListener(v -> updatePosition(channel, delta));
    }

    private void updatePosition(int channel, int delta) {
        // 根据通道更新对应的位置值
        switch (channel) {
            case 0:
                p0 = clamp(p0 + delta);
                sendCommand(channel, p0);
                break;
            case 3:
                p3 = clamp(p3 + delta);
                sendCommand(channel, p3);
                break;
            case 16:
                p16 = clamp(p16 + delta);
                sendCommand(channel, p16);
                break;
            case 18:
                p18 = clamp(p18 + delta);
                sendCommand(channel, p18);
                break;
            case 24:
                p24 = clamp(p24 + delta);
                sendCommand(channel, p24);
                break;
            case 26:
                p26 = clamp(p26 + delta);
                sendCommand(channel, p26);
                break;
        }
    }

    private void sendCommand(int channel, int pulse) {
        String command = "#" + channel + " P" + pulse + " T500\r\n";
        MainActivity.sendCommandToDevice(command.getBytes());
    }

    private int clamp(int value) {
        // 确保脉宽值在安全范围内
        return Math.max(MIN_PULSE, Math.min(MAX_PULSE, value));
    }

    private void resetAllPositions() {
        // 重置所有位置到中间值
        p0 = 1500; sendCommand(0, p0);
        p3 = 1500; sendCommand(3, p3);
        p16 = 1500; sendCommand(16, p16);
        p18 = 1500; sendCommand(18, p18);
        p24 = 1500; sendCommand(24, p24);
        p26 = 1500; sendCommand(26, p26);
    }
}