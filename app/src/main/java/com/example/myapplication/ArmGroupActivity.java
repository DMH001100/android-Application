package com.example.myapplication;

//import static android.os.Build.VERSION_CODES.R;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ArmGroupActivity extends AppCompatActivity {

    String group1 = "PL 0\\r\\nPL 0 SQ 0 SM 100 ONCE\\r\\n";
    String group2 = "PL 0\\r\\nPL 0 SQ 1 SM 100 ONCE\\r\\n";
    String group3 = "PL 0\\r\\nPL 0 SQ 2 SM 100 ONCE\\r\\n";
    String group4 = "PL 0\\r\\nPL 0 SQ 3 SM 100 ONCE\\r\\n";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_control);

        // 返回主页面按钮
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());


        // 执行
        Button btngroup1 = findViewById(R.id.btngroup1);
        btngroup1.setOnClickListener(v -> sendArmCommand(group1));

        Button btngroup2 = findViewById(R.id.btngroup2);
        btngroup2.setOnClickListener(v -> sendArmCommand(group2));

        Button btngroup3 = findViewById(R.id.btngroup3);
        btngroup3.setOnClickListener(v -> sendArmCommand(group3));

        Button btngroup4 = findViewById(R.id.btngroup4);
        btngroup4.setOnClickListener(v -> sendArmCommand(group4));


//        btnArmUp.setOnClickListener(v -> {
//            String command = editText.getText().toString().trim();
//            if(!command.isEmpty()){
//                sendArmCommand(command);
//            }else{
//                Toast.makeText(this, "请输入指令", Toast.LENGTH_SHORT).show();
//            }
//        });

    }

    private void sendArmCommand(String command) {
        // 通过MainActivity的静态方法发送指令
        if (!command.endsWith("\r\n")) {
            command += "\r\n";
        }

        MainActivity.sendCommandToDevice(command.getBytes());
    }
}