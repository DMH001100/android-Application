package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ArmManualControlActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        // 返回主页面按钮
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 手动指令
        EditText editText = findViewById(R.id.editText);

        // 执行
        Button btnArmUp = findViewById(R.id.btnAction);
        btnArmUp.setOnClickListener(v -> {
            String command = editText.getText().toString().trim();
            if(!command.isEmpty()){
                sendArmCommand(command);
            }else{
                Toast.makeText(this, "请输入指令", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void sendArmCommand(String command) {
        // 通过MainActivity的静态方法发送指令
        if (!command.endsWith("\r\n")) {
            command += "\r\n";
        }

        MainActivity.sendCommandToDevice(command.getBytes());
    }
}