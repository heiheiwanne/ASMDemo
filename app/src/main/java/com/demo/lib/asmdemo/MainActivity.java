package com.demo.lib.asmdemo;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.demo.lib.annotations.Cost;

public class MainActivity extends AppCompatActivity {

    private static int MESSAGE_KEY = 0x2019;
    @SuppressLint("HandlerLeak")
    private static Handler sHandler =new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what ==MESSAGE_KEY) {
                if (msg.obj!=null) {
                    Log.i("xmq", String.valueOf(msg.obj));
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendMessage(getClass().getSimpleName());
    }

    @Cost
    private String sendMessage(String string) {
        Message message = new Message();
        message.what =MESSAGE_KEY;
        sHandler.sendMessage(message);
        return string;
    }
}
