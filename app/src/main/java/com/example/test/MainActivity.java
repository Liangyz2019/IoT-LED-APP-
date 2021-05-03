package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;


import com.example.test.AliyunIoTSignUtil;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MainActivity extends AppCompatActivity {
    private static final String TAG =MainActivity.class.getSimpleName();

    private TextView msgTextView;

    private String productKey="a16OKk6dTya";
    private String deviceName="KAMI";
    private String deviceSecret="8790bd0545dd874d77fcac85729fc4bf";

    private final String payloadJson1="{\"ParkingState\":1}";
    private final String payloadJson2="{\"ParkingState\":0}";
    private MqttClient mqttClient=null;

    final int POST_DEVICE_PROPERTIES_SUCCESS = 1002;
    final int POST_DEVICE_PROPERTIES_ERROR = 1003;

    private String responseBody = "";


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case POST_DEVICE_PROPERTIES_SUCCESS:
                    showToast("发送数据成功");
                    break;
                case POST_DEVICE_PROPERTIES_ERROR:
                    showToast("post数据失败");
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        msgTextView = findViewById(R.id.msgTextView);

        findViewById(R.id.activate_button).setOnClickListener((l) -> {
            new Thread(() -> initAliyunIoTClient()).start();
        });

        findViewById(R.id.post_button1).setOnClickListener((l) -> {
            mHandler.postDelayed(() -> postDeviceProperties1(), 1000);
        });

        findViewById(R.id.post_button2).setOnClickListener((l) -> {
            mHandler.postDelayed(() -> postDeviceProperties2(), 1000);
        });

    }



    private void initAliyunIoTClient() {

        try {
            String clientId = "12345"+ System.currentTimeMillis();

            Map<String, String> params = new HashMap<String, String>(16);
            params.put("productKey", productKey);
            params.put("deviceName", deviceName);
            params.put("clientId", clientId);
            String timestamp = String.valueOf(System.currentTimeMillis());
            params.put("timestamp", timestamp);

            // cn-shanghai
            String targetServer ="tcp://"+ productKey + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";

            String mqttclientId = clientId + "|securemode=3,signmethod=hmacsha1,timestamp=" + timestamp + "|";
            String mqttUsername = deviceName + "&" + productKey;
            String mqttPassword = AliyunIoTSignUtil.sign(params, deviceSecret, "hmacsha1");

            connectMqtt(targetServer, mqttclientId, mqttUsername, mqttPassword);


        } catch (Exception e) {
            e.printStackTrace();
            responseBody = e.getMessage();
            mHandler.sendEmptyMessage(POST_DEVICE_PROPERTIES_ERROR);
        }
    }

    public void connectMqtt(String url, String clientId, String mqttUsername, String mqttPassword) throws Exception {

        MemoryPersistence persistence = new MemoryPersistence();
        mqttClient = new MqttClient(url, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        // MQTT 3.1.1
        connOpts.setMqttVersion(4);
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);

        connOpts.setUserName(mqttUsername);
        connOpts.setPassword(mqttPassword.toCharArray());
        connOpts.setKeepAliveInterval(60);

        mqttClient.connect(connOpts);
        Log.d(TAG, "connected " + url);

    }
    private void postDeviceProperties1() {

        try {

            Random random = new Random();

            //上报数据
            String payload = String.format(payloadJson1, String.valueOf(System.currentTimeMillis()), 10 + random.nextInt(20), 50 + random.nextInt(50));
            responseBody = payload;
            MqttMessage message = new MqttMessage(payload.getBytes("utf-8"));
            message.setQos(1);


            String pubTopic = "/" + productKey + "/" + deviceName + "/user/update";
            mqttClient.publish(pubTopic, message);
            Log.d(TAG, "publish topic=" + pubTopic + ",payload=" + payload);
            mHandler.sendEmptyMessage(POST_DEVICE_PROPERTIES_SUCCESS);

            mHandler.postDelayed(() -> postDeviceProperties1(), 5 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
            responseBody = e.getMessage();
            mHandler.sendEmptyMessage(POST_DEVICE_PROPERTIES_ERROR);
            Log.e(TAG, "postDeviceProperties error " + e.getMessage(), e);
        }
    }

    private void postDeviceProperties2() {

        try {

            Random random = new Random();

            //上报数据
            String payload = String.format(payloadJson2, String.valueOf(System.currentTimeMillis()), 10 + random.nextInt(20), 50 + random.nextInt(50));
            responseBody = payload;
            MqttMessage message = new MqttMessage(payload.getBytes("utf-8"));
            message.setQos(1);


            String pubTopic = "/" + productKey + "/" + deviceName + "/user/update";
            mqttClient.publish(pubTopic, message);
            Log.d(TAG, "publish topic=" + pubTopic + ",payload=" + payload);
            mHandler.sendEmptyMessage(POST_DEVICE_PROPERTIES_SUCCESS);

            mHandler.postDelayed(() -> postDeviceProperties2(), 5 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
            responseBody = e.getMessage();
            mHandler.sendEmptyMessage(POST_DEVICE_PROPERTIES_ERROR);
            Log.e(TAG, "postDeviceProperties error " + e.getMessage(), e);
        }
    }
    private void showToast(String msg) {
        msgTextView.setText(msg + "\n" + responseBody);
    }

}
