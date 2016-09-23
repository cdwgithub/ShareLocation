package com.jkxy.sharelocation;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.jkxy.sharelocation.controls.ClientServer;
import com.jkxy.sharelocation.controls.GsonUtils;
import com.jkxy.sharelocation.model.ConstantString;
import com.jkxy.sharelocation.model.User;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Button btnShareLocaion;
    private EditText et_IP;//用户IP
    private EditText et_UserName;//用户名
    private TextView tv_Usernum;//在线用户数量

    private LocationClient locationClient = null;
    private BDLocationListener bdLocationListener = null;//定位监听
    private MapView baiduMapView = null;//百度地图控件
    private BaiduMap baiduMap = null;//百度地图
    private BDLocation location = null;
    private boolean isFirstLocation = true;//首次定位标志

    private boolean isConnected = false;//是否成功连接服务器标志
    private ClientServer server;
    private ServiceConnection connection;
    private Intent intent;
    private HashMap<String, Marker> hashMap;//存储在线用户的Mark位置
    private User localUser;//本地用户
    private String receiveMsg;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String KEY_NAME = "USER_NAME";
    private String DEFAULT_NAME = "user1";//默认用户名字
    private String KEY_IP = "USER_IP";
    private String DEFAULT_IP = "192.168.1.105";//默认用户IP
    private  Handler handler;
    private double delta=0.00;//位置偏移，调试用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        //使用SDK各组件之前初始化context信息，传入ApplicationContext
        //该方法要再setContentView方法之前实现
        setContentView(R.layout.activity_main);
        inti();
    }

    @Override
    protected void onDestroy() {
        //stopMyService();
        unbindService(connection);
        super.onDestroy();
    }

    /**
     * 初始化
     */
    private void inti() {

        initMap();//初始化地图
        initLocation();//初始化定位图层

        localUser = new User();
        hashMap = new HashMap<String, Marker>();

        et_IP = (EditText) findViewById(R.id.et_ip);
        et_UserName = (EditText) findViewById(R.id.et_username);
        tv_Usernum = (TextView) findViewById(R.id.tv_usernum);
        sharedPreferences = getPreferences(Activity.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        et_IP.setText(sharedPreferences.getString(KEY_IP, DEFAULT_IP));
        et_UserName.setText(sharedPreferences.getString(KEY_NAME, DEFAULT_NAME));
        initService();

        btnShareLocaion = (Button) findViewById(R.id.btn_send);
        btnShareLocaion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = et_UserName.getText().toString().trim();
                String ip = et_IP.getText().toString().trim();
                if (!name.isEmpty()) {
                    if (isNetWorkConnected(getApplicationContext())) {
                        if (!isConnected) {
                            ConstantString.name = name;
                            ConstantString.IP = ip;
                            editor.putString(KEY_IP, ip);//保存用户设置的IP
                            editor.putString(KEY_NAME, name);//保存用户设置的用户名
                            editor.commit();//提交更改信息
                            localUser.setUserName(name);
                            startMyService();//开启连接服务器的服务
                        } else {
                            stopMyService();
                            tv_Usernum.setVisibility(View.GONE);
                        }
                    } else {
                        showToast(getString(R.string.tip_noconnectnet));

                    }


                } else {
                    showToast(getString(R.string.tip_name_null));
                }


            }
        });


       handler=new MainActivity.myHandler(this);


    }

    /**
     * 初始化服务
     */
    private void initService(){

        connection=new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                server=((ClientServer.ClientServerBinder)iBinder).getService();
                server.setInterface(new ClientServer.InterfaceReceiverListener() {
                    @Override
                    public void onReceiver(String receiver) {
                        receiveMsg=receiver;
                        handler.sendEmptyMessage(0);//发送空信息
                    }
                });

            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };
        intent=new Intent(MainActivity.this,ClientServer.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);//绑定服务
    }
    /**
     * Handler 更新UI
     */
     public  class myHandler extends Handler {
         WeakReference<MainActivity> mainActivity;
        public myHandler(MainActivity activity) {
            mainActivity = new WeakReference<>(activity);
        }
        public void handleMessage(Message msg) {
             final MainActivity activity = mainActivity.get();
            if (activity!=null){
                User user = GsonUtils.jsonToUserMessage(receiveMsg,
                        User.class);
                displayMark(user);
            }

        }

    }
    /**
     * 开启后台服务
     */
    private void startMyService() {
        if (server!=null){
            server.connect();
        }


    }

/***********************************服务相关***********************************************************/
    /**
     * 关闭服务
     */
    private void stopMyService() {
        btnShareLocaion.setText(R.string.btn_shareloction);
        isConnected = false;
        baiduMap.clear();
        sendOffLineMessage(false);
        hashMap.clear();



    }
/****************************************地图相关**********************************************************/

    /**
     * 初始化地图
     */
    private void initMap() {
        baiduMapView = (MapView) findViewById(R.id.mapBaiduMap);
        baiduMap = baiduMapView.getMap();//获取百度地图对象
        baiduMap.setMyLocationEnabled(true);//开启定位图层

    }

    /**
     * 设置定位方式
     */
    private void initLocation() {
        location = new BDLocation();
        locationClient = new LocationClient(getApplicationContext());//声明LocationClient类
        bdLocationListener = new MyLocationListener();
        locationClient.registerLocationListener(bdLocationListener);//注册监听函数
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(2000);//定位请求间隔时间
        option.setOpenGps(true);//开启GPS
        locationClient.setLocOption(option);
        locationClient.start();//开启定位
    }




    /**
     * 实现定位回调监听
     */
    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null || baiduMap == null) {
                return;
            }
            //获取用户位置信息
            MyLocationData myLocationData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();//获取经纬度
            baiduMap.setMyLocationData(myLocationData);

            if (isFirstLocation) {
                isFirstLocation = false;//首次定位标志置为false
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude()); //获取经纬度
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);//设置缩放级别
                baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
            upLoadLocation(location);//向服务器上传位置信息
        }

    }
    /**
     * 显示各个在线客户端共享的位置
     *
     * @param user
     */
    private void displayMark(User user) {
      if (!isConnected && user.getUserName().equals(ConstantString.name) && user.isOnLine()) {//本用户上线信息判断
            isConnected = true;
            tv_Usernum.setVisibility(View.VISIBLE);
            refeshOnlineNum();
            btnShareLocaion.setText(R.string.tip_cancelsharelocation);
        } else if (isConnected && user.isOnLine() && !user.getUserName().equals(ConstantString.name)) {//非本用户的位置信息
            LatLng point = new LatLng(user.getLatitude(), user.getLongitude());
            if (!hashMap.containsKey(user.getUserName())) {
                BitmapDescriptor bitmap = BitmapDescriptorFactory
                        .fromResource(R.drawable.tab_icon_me_selected);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions overlayOptions = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);
                Marker marker = (Marker) baiduMap.addOverlay(overlayOptions);//新添加一个在线用户，则添加一个Mark
                hashMap.put(user.getUserName(), marker);//保存在线用户对应的Mark
                refeshOnlineNum();
                Log.d("TAG","hash"+user.getUserName());
            } else {
                hashMap.get(user.getUserName()).setPosition(point);

            }

        } else if (!user.isOnLine()) {
            if (hashMap.containsKey(user.getUserName())) {
                hashMap.get(user.getUserName()).remove();
                hashMap.remove(user.getUserName());//移除离线用户对应的Mark
                refeshOnlineNum();
            }

        }
        Log.d("TAG", user.getUserName());
    }
    /**
     * 向服务器上传位置信息
     */
    private void upLoadLocation(BDLocation location) {
        //开启了位置共享功能
        if (isConnected) {
            delta+=0.0001;
            localUser.setLatitude(location.getLatitude()+delta);
            localUser.setLongitude(location.getLongitude());
            localUser.setIsOnLine(true);
            String info = GsonUtils.userMessageToJson(localUser);
            server.sendMessage(info);

        }
    }
/********************************其他******************************************************************/
    /**
     * 判断网络是否连接
     *
     * @param context
     * @return
     */
    private static boolean isNetWorkConnected(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }

    /**
     * 发送上线或者下线的消息
     *
     * @param flag
     */
    private void sendOffLineMessage(boolean flag) {
        localUser.setIsOnLine(flag);
        String info = GsonUtils.userMessageToJson(localUser);
        server.sendMessage(info);

    }


    /**
     * 显示提示信息
     *
     * @param string
     */
    private void showToast(String string) {
        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();

    }

    /**
     * 更新在线人数
     */
    private void refeshOnlineNum() {
        tv_Usernum.setText(hashMap.size() + 1 + getString(R.string.tip_onlinenum));
    }




}
