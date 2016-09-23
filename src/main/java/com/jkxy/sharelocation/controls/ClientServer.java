package com.jkxy.sharelocation.controls;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.jkxy.sharelocation.R;
import com.jkxy.sharelocation.model.ConstantString;
import com.jkxy.sharelocation.model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Created by X on 2016/5/12.
 */
public class ClientServer extends Service {
    private int PORT = 8000;
    private Socket socket = null;
    private BufferedWriter writer;
    private BufferedReader reader;
    private InterfaceReceiverListener receiverListenerListener;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG", "Service-onCreate()");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d("TAG", "Service-onStart()");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG", "Service-onStartCommand");
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TAG", "Service-onDestroy()");
        try {
            if (writer != null) {
                writer.close();//关闭writer
            }
            if (reader != null) {
                reader.close();//关闭reader
            }
            if (socket != null) {
                socket.close();////关闭socker
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * 连接服务器函数
     */
    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ConstantString.IP, PORT);//根据IP地址和端口号获取socket
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));//定义BufferedWriter
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));//定义BufferedReader
                    User user = new User();
                    user.setUserName(ConstantString.name);
                    user.setIsOnLine(true);
                    String info = GsonUtils.userMessageToJson(user);//定义一个登录服务器消息
                    sendMessage(info);
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast(getString(R.string.tip_connectserverfail));
                }
                try {
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        if (receiverListenerListener!=null){
                            receiverListenerListener.onReceiver(line);
                        }


                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }


    /**
     * 刷新数据接口
     */
    public interface InterfaceReceiverListener {
        public void onReceiver(String receiver);
    }

    /**
     * 设置接口
     * @param receiverListenerListener
     */
    public void setInterface(InterfaceReceiverListener receiverListenerListener) {
        this.receiverListenerListener = receiverListenerListener;
    }

    /**
     * 向服务器发送信息
     * @param message
     */
   public void sendMessage(String message){
       String info = message + "\n";
       try {
           if (writer != null) {
               writer.write(info);//向服务器发送消息
               writer.flush();
           }

       } catch (IOException e) {
           e.printStackTrace();
       }
   }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ClientServerBinder();
    }
    public class ClientServerBinder extends Binder
    {
        public ClientServer getService(){
            return ClientServer.this;//返回服务实体
        }
    }

    /**
     * Toast提示函数
     * @param string
     */
    private void showToast(String string) {
        Looper.prepare();
        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
        Looper.loop();
    }
}
