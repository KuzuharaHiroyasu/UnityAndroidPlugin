package jp.co.onea.sleeim.unityandroidplugin.main;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import jp.co.onea.sleeim.unityandroidplugin.data.CsvWriter;
import jp.co.onea.sleeim.unityandroidplugin.utils.DataUtil;
import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;
import jp.co.onea.sleeim.unityandroidplugin.utils.ExtractionKey;

import static com.unity3d.player.UnityPlayer.UnitySendMessage;

//BLE:Serivceクラス
//https://akira-watson.com/android/service.html
//https://qiita.com/naoi/items/03e76d10948fe0d45597
//http://www.gigas-jp.com/appnews/archives/6228
public class BleService extends Service {

    private final static String TAG = BleService.class.getSimpleName();

    //Bluetooth
    private static BluetoothManager _BluetoothManager;
    private static BluetoothAdapter _BluetoothAdapter;
    private static BluetoothGattCharacteristic _BluetoothGattReadCharacteristic;
    private static BluetoothGattCharacteristic _BluetoothGattWriteCharacteristic;

    /**
     * FW更新データ通信用キャラクタリスティック
     */
    private static BluetoothGattCharacteristic bleGattCharWriteFWUPData;

    /**
     * FW更新制御コマンド通信用キャラクタリスティック
     */
    private static BluetoothGattCharacteristic bleGattCharWriteFWUPControl;

    /**
     * ファーム更新通信種別
     */
    private static FirmwareUpdateCharacteristicType fwUpType = FirmwareUpdateCharacteristicType.Control;

    private static BluetoothGatt _BluetoothGatt;
    private static BluetoothDevice _BluetoothDevice;
    private static String _BluetoothDeviceAddress;

    //ステータス
    enum States {
        None,
        Scan,
        ScanStop,
        ConnectWait,
        Connectting,
        Connected,
        Didonnected
    }

    /**
     * ファーム更新通信種別
     */
    enum FirmwareUpdateCharacteristicType {
        Control,
        Data,
    }

    private static States _state = States.None; //初期ステータス

    //Command List
    private static final int COMMAND1 = 1; //状態変更
    private static final int COMMAND2 = 2; //待機状態
    private static final int COMMAND3 = 3; //GET状態
    private static final int COMMAND_G1D_UPDATE = 5; //プログラム更新状態(G1D)
    private static final int COMMAND6 = 6; //日時設定
    private static final int COMMAND7 = 7; //情報取得(電池残量取得)
    private static final int COMMAND8 = 8; //バージョン取得
    private static final int COMMAND9 = 9;

    private static final int COMMAND10 = 10;
    private static final int COMMAND11 = 11;
    private static final int COMMAND12 = 12;
    private static final int COMMAND_PROGRAM_TRANSFER = 13; //プログラムデータ転送
    private static final int COMMAND14 = 14; //プログラム転送結果
    private static final int COMMAND_COMPLETE_UPDATE = 15; //プログラム更新完了確認
    private static final int COMMAND16 = 16; //アラーム設定
    private static final int COMMAND17 = 17;
    private static final int COMMAND18 = 18; //デバイス状況取得
    private static final int COMMAND19 = 19; //データ取得完了通知
    private static final int DEVICE_SETTING_CHANGE_COMMAND = 0xC6;

    /**
     * G1Dファーム更新データコマンドコード
     *
     * この値は、BLE通信コマンド仕様として定められたものではない
     */
    private static final int COMMAND_G1D_UPDATE_DATA = 0xD0;

    /**
     * G1Dファーム更新制御コマンドコード
     *
     * この値は、BLE通信コマンド仕様として定められたものではない
     */
    private static final int COMMAND_G1D_UPDATE_CONTROL = 0xD1;

    //ErrcCode List
    private static final int CODE0 = 0; //なし
    private static final int CODE1 = -1; //
    private static final int CODE2 = -2; //
    private static final int CODE3 = -3; //機器と接続できない
    private static final int CODE4 = -4; //機器と接続がきれた
    private static final int CODE5 = -5; //タイムアウトエラー
    private static final int CODE6 = -6; //
    private static final int CODE7 = -7; //データ解析エラー
    private static final int CODE8 = -8; //
    private static final int CODE9 = -9; //ディスク容量不足でCSV書き込みエラー
    private static final int CODE10 = -10; //CSV書き込みエラー

    //管理
    private static int _writeType = 0; //送信中データタイプ
    private static int _recvTimerCount = Integer.MAX_VALUE; //受信タイムカウンタ：カウント用変数
    private static int _recvCount[] = new int[10]; //受信回数（データ長）
    private static String _filename[] = new String[10]; //ファイルパス
    private static String _aftername[] = new String[10]; //ファイルパス
    private static int _getCounter = 0;                  //データ取得件数カウント用
    private static boolean _endReciveFlag = false;    //END受信済みフラグ

    //フォーマット
    private static final DateFormat dateStdFormat = new SimpleDateFormat("yyyyMMddHHmmss"); //CSVファイル名用
    private static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMM"); //CSVフォルダ名用

    //Device
   private static final String TARGET_DEVICE_NAME = "Sleeim";

    //BLE
    private static final int SCAN_TIMEOUT = 30000;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int TIMER_TIME = 200; //ms
    private static final int RCV_TIMEOUT = 5; //TIMER_TIME * RCV_TIMEOUT ms

    private static final int MESSAGE_TIMEOUT_STS = 1;
    private static final int GET_DATA_MAX = 10; //最大受信件数
    private static final int H1D_PROGRAM_CODE = 20; //H1D転送単位

    //UUID：Declaration
    //characteristic: Indicate
    private static final String UUID_SERVICE_DECLARATION = "d68c0001-a21b-11e5-8cb8-0002a5d5c51b";
    //characteristic: Indicate（スマホ←機器）
    private static final String UUID_READ_DECLARATION = "d68c0002-a21b-11e5-8cb8-0002a5d5c51b";
    //characteristic: Write（スマホ→機器）
    private static final String UUID_WRITE_DECLARATION = "d68c0003-a21b-11e5-8cb8-0002a5d5c51b";
    //Androidの固定値
    private static final String ANDROID_CENTRAL_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * FWアップデート用サービスUUID
     */
    private static final String UUID_SERVICE_FWUP = "01010000-0000-0000-0000-000000000080";

    /**
     * FWアップデートWrite Characteristic Declaration
     *
     * 制御コマンド通信用キャラクタリスティックUUID
     */
    private static final String UUID_CHAR_FWUP_WRITE_CONTROL = "02010000-0000-0000-0000-000000000080";

    /**
     * FWアップデートWrite Characteristic Declaration
     *
     * FW更新データ通信用キャラクタリスティックUUID
     */
    private static final String UUID_CHAR_FWUP_WRITE_DATA = "03010000-0000-0000-0000-000000000080";

    /**
     * サービスUUID
     */
    private String serviceUUID = UUID_SERVICE_DECLARATION;

    // This is any integer value unique to the application.
    public static final int SERVICE_RUNNING_NOTIFICATION_ID = 10000;
    public static final int SERVICE_RUNNING_NOTIFICATION_ID2 = 20000;

    //Handler
    final static Handler uhandler = new Handler(); //UnitySendMessage用
    private static Handler sHandler = new Handler(); //スキャン用
    private static Handler cHandler = new Handler(); //コネクション用
    final static Handler mServiceHndlr = new ServiceHandler(); //タイムアウトメッセージ用

    private static int _sendCommandId;
    private static byte[] notification_data;

    //Timer
    private static Timer timer;
    private static Timer vTimer;
    private static Timer mTimer;

    //日付
    private static Date gDate;
    private static GregorianCalendar gCal;

    //ファイル
    private static final CsvWriter mWriter = new CsvWriter();
    private static final int HEADER_LINE = 2; //CSVファイル内データ長のヘッダ位置

    //戻り値返却用
    private static Gson gson = new Gson();

    //Notification
    private static NotificationHelper notificationHelper;

    //////////////////////////////////////////////////////////////////////////////////////////

    //呼び出し元の情報
    private static String mGameObject = "";
    private static String callbackCommandError = "";
    private static String callbackCommandResponse = "";
    private static String mMethod3 = "";
    private static String mMethod4 = "";
    private static String mMethod5 = "";
    private static String mMethod6 = "";
    private static String mMethod7 = "";
    private static String mMethod8 = "";
    private static String mMethod9 = "";
    private static String callbackH1dTransferDataDone = "";
    private static String mMethod11 = "";
    private static String callbackCommandWrite = "";

    //JSON：key
    private static final String jKey1 = "KEY1";
    private static final String jKey2 = "KEY2";
    private static final String jKey3 = "KEY3";
    private static final String jKey4 = "KEY4";
    private static final String jKey5 = "KEY5";
    private static final String jKey6 = "KEY6";
    private static final String jKey7 = "KEY7";
    private static final String jKey8 = "KEY8";
    private static final String jKey9 = "KEY9";
    private static final String jKey10 = "KEY10";
    private static final String jKey11 = "KEY11";
    private static final String jKey12 = "KEY12";
    private static final String jKey13 = "KEY13";

    //////////////////////////////////////////////////////////////////////////////////////////

    private static int _selectAlarm; //選択中のアラーム
    private static boolean _isVibration; //バイブレーションのON/OFF
    private static boolean _isFeedin; //フェードインのON/OFF
    private static int _alarmCalltime; //鳴動時間

    private static Vibrator mVibrator;
    private static BroadcastReceiver receiver;
    private static AudioManager mAudioManager;
    private static MediaPlayer player;
    private static PLAY_STAT mStat = PLAY_STAT.STOP;
    private static int mNowVolume = 0;
    private static float mChgCntVolume = 0.0f;
    private static int mPlayCoumt = 0;

    enum PLAY_STAT {
        STOP, FADEIN, PLAYING, FADEOUT;
    }

    private static final String ARMTYPE1 = "低呼吸";
    private static final String ARMTYPE2 = "いびき";
    private static final String ARMTITLE = "アラーム";
    private static final String ARMTEXT = "を検知しました。";

    private static final int[] MEIDOU_TIME = {Integer.MAX_VALUE, 5000, 10000, 15000, 30000}; //オフ,5秒,10秒,15秒,30秒

    //////////////////////////////////////////////////////////////////////////////////////////

    public static void start(@NonNull Context context) {
        DebugLog.d(TAG, "start");

        Intent intent = new Intent(context, BleService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(@NonNull Context context) {
        DebugLog.d(TAG, "stop");

        Intent intent = new Intent(context, BleService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.stopService(intent);
        } else {
            context.stopService(intent);
        }
    }

    //フォアグラウンドサービス状態を停止
    public void stopForeground() {
        stopForeground(true);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        DebugLog.d(TAG, "onTaskRemoved");

        //stopSelf();

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DebugLog.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DebugLog.d(TAG, "onStartCommand Received start id " + startId + ": " + intent);
        //Toast.makeText(this, "MyService#onStartCommand", Toast.LENGTH_SHORT).show();

        notificationHelper = new NotificationHelper(this);
        Notification.Builder builder = notificationHelper.getNotification();
        startForeground(SERVICE_RUNNING_NOTIFICATION_ID, builder.build());

        // 強制終了時、システムによる再起動を求める場合はSTART_STICKYを利用
        // 再起動が不要な場合はSTART_NOT_STICKYを利用する
        // サービスを起動するペンディングインテントが存在しない限りサービスは再起動されません 。
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        DebugLog.d(TAG, "onDestroy");

        // Service終了

        super.onDestroy();
    }

    //サービス
    //
    private final IBinder _Binder = new LocalBinder();

    public class LocalBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        DebugLog.d(TAG, "onBind");

        return _Binder;
    }

    @Override
    public void onRebind(Intent intent) {
        DebugLog.d(TAG, "onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        DebugLog.d(TAG, "onUnbind");
        return true;
    }

    /**
     * BLE対応チェック
     *
     * @return boolearn      返却値(TRUE:有効 FALSE:無効)
     */
    //  *  @return code          返却値(TRUE:対応 FALSE:非対応)
    public boolean BlesupportCheck() {

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false; //ble_not_supported
        }
        return true;
    }

    /**
     * Bluetooth有効/無効チェック
     *
     * @return boolearn      返却値(TRUE:有効 FALSE:無効)
     */
    //「API18 : Android 4.3」以前は、BLE非対応です。本メソッドは使用しないこと。
    public boolean BluetoothValidCheck() {

        if (_BluetoothManager == null) {
            _BluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            //Android端末がBluetoothをサポートしているか
            if (_BluetoothManager == null) {
                DebugLog.e(TAG, "bluetoothValidCheck: BluetoothManagerを初期化できませんでした。");
                return false;
            }
        }

        _BluetoothAdapter = _BluetoothManager.getAdapter();
        if (_BluetoothAdapter == null) {
            DebugLog.e(TAG, "bluetoothValidCheck: BluetoothAdapterを取得できませんでした。");
            return false;
        }

        return _BluetoothAdapter.isEnabled();
    }

    /**
     * Bluetooth初期化処理
     *
     * @return boolearn      成功:1  失敗(Device does not support Bluetooth)：0
     */
    //「API18 : Android 4.3」以前は、BLE非対応です。本メソッドは使用しないこと。
    public boolean Initialize(
        final String gameObject,
        final String callbackCommandErrorString,
        final String callbackCommandResponseString,
        final String method3,
        final String method4,
        final String method5,
        final String method6,
        final String method7,
        final String method8,
        final String method9,
        final String callbackH1dTransferDataDoneString,
        final String method11,
        final String callbackCommandWriteString) {

        //呼び出し元情報を取得。
        mGameObject = gameObject;
        callbackCommandError = callbackCommandErrorString;
        callbackCommandResponse = callbackCommandResponseString;
        mMethod3 = method3;
        mMethod4 = method4;
        mMethod5 = method5;
        mMethod6 = method6;
        mMethod7 = method7;
        mMethod8 = method8;
        mMethod9 = method9;
        callbackH1dTransferDataDone = callbackH1dTransferDataDoneString;
        mMethod11 = method11;
        callbackCommandWrite = callbackCommandWriteString;

        _state = States.None;

        if (_BluetoothManager == null) {
            _BluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            //Android端末がBluetoothをサポートしているか
            if (_BluetoothManager == null) {
                DebugLog.e(TAG, "Initialize: BluetoothManagerを初期化できませんでした。");
                return false;
            }
        }

        _BluetoothAdapter = _BluetoothManager.getAdapter();
        if (_BluetoothAdapter == null) {
            DebugLog.e(TAG, "Initialize: BluetoothAdapterを取得できませんでした。");
            return false;
        }

        mVibrator = (Vibrator) MyApplication.getInstance().getSystemService(Context.VIBRATOR_SERVICE);
        mAudioManager = (AudioManager) MyApplication.getInstance().getSystemService(Context.AUDIO_SERVICE);
        schedule(); //タイムアウト監視用Timer開始

        DebugLog.d(TAG, "Initialize");

        return true;
    }

    /**
     * Bluetooth終了処理
     */
    public void Deinitialize() {

        DebugLog.d(TAG, "Deinitialize:start");

        Disconnect(); //コネクション切断
        Close(); //GATTサーバークローズ

        _state = States.None;
        StopScanning(); //スキャン停止

        _BluetoothManager = null;
        _BluetoothAdapter = null;
        _BluetoothDeviceAddress = null;

        DebugLog.d(TAG, "Deinitialize:end");
    }

    /**
     * BLEデバイスをスキャンします
     */
    public void ScanBleDevice() {

        if (_state == States.Scan) { //スキャン中なら再度実行しない
            DebugLog.d(TAG, "ScanBleDevice:既にスキャン中です。");
            if(sHandler!=null){
                DebugLog.d(TAG, "ScanBleDevice: sHandler:not null");

                sHandler.removeCallbacksAndMessages(null); //自動スキャン停止を停止させる
                //一定時間経過後にスキャンをストップする
                //スキャン停止忘れると電池が減り続ける
                sHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DebugLog.d(TAG, "ScanBleDevice: postDelayed:timeout");
                        Context context = MyApplication.getInstance().getApplicationContext();

                        _state = States.Scan;
                        StopScanning(); //スキャン停止

                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";

                        hMap.put(jKey1, (int) 0);
                        hMap.put(jKey2, (int) CODE5); //
                        rJson = gson.toJson(hMap);
                        UnitySendMessage(mGameObject, callbackCommandError, rJson);
                    }
                }, SCAN_TIMEOUT);
            }

            return;
        }else{
            if(sHandler!=null){
                DebugLog.d(TAG, "ScanBleDevice: sHandler:not null");

                sHandler.removeCallbacksAndMessages(null); //自動スキャン停止を停止させる
                //一定時間経過後にスキャンをストップする
                //スキャン停止忘れると電池が減り続ける
                sHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DebugLog.d(TAG, "ScanBleDevice: postDelayed:timeout");
                        Context context = MyApplication.getInstance().getApplicationContext();

                        _state = States.Scan;
                        StopScanning(); //スキャン停止

                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";

                        hMap.put(jKey1, (int) 0);
                        hMap.put(jKey2, (int) CODE5); //
                        rJson = gson.toJson(hMap);
                        UnitySendMessage(mGameObject, callbackCommandError, rJson);
                    }
                }, SCAN_TIMEOUT);
            }
        }


        ScanFilter scanFilter =
                new ScanFilter.Builder()
                        .setDeviceName(TARGET_DEVICE_NAME) //デバイス名でフィルタを指定
                        .build();

        ArrayList<ScanFilter> scanFilterList = new ArrayList<ScanFilter>();
        scanFilterList.add(scanFilter);

        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(); //消費電力激しいのでフォアグランド時にするもの


        //一定時間経過後にスキャンをストップする
        //スキャン停止忘れると電池が減り続ける
        if(sHandler == null) {
            DebugLog.d(TAG, "ScanBleDevice: sHandler:null");

            sHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    DebugLog.d(TAG, "ScanBleDevice: postDelayed:timeout");
                    Context context = MyApplication.getInstance().getApplicationContext();

                    _state = States.Scan;
                    StopScanning(); //スキャン停止

                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    hMap.put(jKey1, (int) 0);
                    hMap.put(jKey2, (int) CODE5); //
                    rJson = gson.toJson(hMap);
                    UnitySendMessage(mGameObject, callbackCommandError, rJson);
                }
            }, SCAN_TIMEOUT);
        }

        BluetoothLeScanner scanner = _BluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) {
            DebugLog.d(TAG, "ScanBleDevice: スキャンを開始します。");
             scanner.startScan(scanFilterList, scanSettings, _ScanCallback);
        } else {
            // 基本実行前にBluetooth有効/無効確認をしているため、タイムアウトエラーまで待つ
            // Device Bluetooth is disabled;
        }

        _state = States.Scan;
    }

    /**
     * BLEデバイスをスキャンを停止させます
     */
    public void StopScanning() {

        if ((_state == States.Scan)||(_state == States.None)) { //スキャン中又は、アプリ終了時のみ受け付ける

            sHandler.removeCallbacksAndMessages(null); //自動スキャン停止を停止させる

            if (_BluetoothAdapter != null) {
                BluetoothLeScanner scanner = _BluetoothAdapter.getBluetoothLeScanner();

                try {
                    if (scanner != null) {
                        scanner.stopScan(_ScanCallback); //探索を停止
                    } else {
                        // Device Bluetooth is disabled;
                    }
                    DebugLog.d(TAG, "StopScanning:Stop");
                } catch (NullPointerException exception) {
                    DebugLog.e(TAG, "BluetoothAdapter: StopScanning: Null " + exception.getMessage());
                }
            }

            _state = States.ScanStop;
        }else{
            DebugLog.d(TAG, "StopScanning:スキャン中以外は停止させない");
        }
    }

    /**
     * スキャン結果のコールバック
     */
    private final ScanCallback _ScanCallback = new ScanCallback() {

        Map<String, Object> hMap = new HashMap<>();

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            super.onScanResult(callbackType, result);

            _BluetoothDevice = result.getDevice();

            DebugLog.d(TAG, "onScanResult()");
            if (null != _BluetoothDevice.getName()) {
                DebugLog.d(TAG, "DeviceName:" + _BluetoothDevice.getName());
            } else {
                DebugLog.d(TAG, "DeviceName:" + "null");
            }
            DebugLog.d(TAG, "DeviceAddr:" + _BluetoothDevice.getAddress());
            DebugLog.d(TAG, "RSSI:" + result.getRssi());
            DebugLog.d(TAG, "UUID:" + result.getScanRecord().getServiceUuids());

            //接続するデバイスが見つかった場合
            if (null != _BluetoothDevice.getName()) {
                if (TARGET_DEVICE_NAME.equalsIgnoreCase(_BluetoothDevice.getName())) {

                    DebugLog.d(TAG, "resultDevice:" + _BluetoothDevice.getName());

                    uhandler.post(new Runnable() {
                        public void run() {

                            String tmp1 = "", tmp2 = "";
                            if (null != _BluetoothDevice.getName())
                                tmp1 = _BluetoothDevice.getName();
                            if (null != _BluetoothDevice.getAddress())
                                tmp2 = _BluetoothDevice.getAddress();

                            Map<String, Object> hMap = new HashMap<>();
                            String rJson = "";

                            hMap.put(jKey1, tmp1); //デバイス名
                            hMap.put(jKey2, tmp2);  //デバイスアドレス
                            rJson = gson.toJson(hMap);
                            UnitySendMessage(mGameObject, mMethod4, rJson);
                        }
                    });
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {

            super.onScanFailed(errorCode);

            // エラーが発生するとこちらが呼び出されます
            String errorMessage = "";

            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    errorMessage = "既にBLEスキャンを実行中です";
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    errorMessage = "BLEスキャンを開始できませんでした";
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    errorMessage = "BLEの検索をサポートしていません。";
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    errorMessage = "内部エラーが発生しました";
                    break;
            }
            DebugLog.d(TAG, "onScanFailed:errocode=" + String.valueOf(errorCode) + " ,errorMessage:" + errorMessage);

            if (errorCode != SCAN_FAILED_ALREADY_STARTED) {

                //スキャン停止時とはエラーが違うので、StopScanning()をコールしない
                BluetoothLeScanner scanner = _BluetoothAdapter.getBluetoothLeScanner();
                if (scanner != null) {
                    scanner.stopScan(_ScanCallback); //探索を停止
                } else {
                    // Device Bluetooth is disabled;
                }

                _state = States.ScanStop;
                sHandler.removeCallbacksAndMessages(null); //自動スキャン停止を停止させる

                uhandler.post(new Runnable() {
                    public void run() {
                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";

                        hMap.put(jKey1, (int) 0);
                        hMap.put(jKey2, (int) CODE5); //
                        rJson = gson.toJson(hMap);
                        UnitySendMessage(mGameObject, callbackCommandError, rJson);
                    }
                });
            }
        }
    };

    /**
     * デバイス接続
     */
    public void Connect(String address) {

        if (_state == States.Connectting) {
            DebugLog.d(TAG, "ScanBleDevice:接続待機中です。");
            return;
        }
        if (_state == States.Connected) {
            DebugLog.d(TAG, "ScanBleDevice:既に接続中です。");

            uhandler.post(new Runnable() {
                public void run() {

                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    hMap.put(jKey1, (String) TARGET_DEVICE_NAME); // NULLで何故か取得してしまうので固定値を返す
                    hMap.put(jKey2, _BluetoothDeviceAddress); //接続済のアドレスを返す
                    rJson = gson.toJson(hMap);
                    DebugLog.d(TAG, rJson);

                    UnitySendMessage(mGameObject, mMethod3, rJson);
                }
            });
            return;
        }

        if(_state==States.Scan) {
            StopScanning();
        }
        _state = States.ScanStop;

        if (_BluetoothAdapter == null || address == null) {
            DebugLog.e(TAG, "Connect: _BluetoothAdapter null / address null");

            uhandler.post(new Runnable() {
                public void run() {
                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    hMap.put(jKey1, (int) 0);
                    hMap.put(jKey2, (int) CODE3); //
                    rJson = gson.toJson(hMap);
                    UnitySendMessage(mGameObject, callbackCommandError, rJson);
                }
            });
            return;
        }

        //過去に接続済みのアドレスであれば再接続をする
        if (_BluetoothDeviceAddress != null && address.equals(_BluetoothDeviceAddress)
                && _BluetoothGatt != null) {

            DebugLog.d(TAG, "Connect: Retry Gatt Connecting");

            //コネクション中断
            cHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (_state != States.Connected) {

                        DebugLog.d(TAG, "Connect: postDelayed:timeout");

                        Context context = MyApplication.getInstance().getApplicationContext();

                        if (_BluetoothGatt != null) {
                            if (serviceUUID.equals(UUID_SERVICE_DECLARATION)) {
                                // 汎用通信用
                                if(_BluetoothGattReadCharacteristic != null)
                                {
                                    _BluetoothGatt.setCharacteristicNotification(_BluetoothGattReadCharacteristic, false);
                                }
                            } else {
                                if (fwUpType == FirmwareUpdateCharacteristicType.Control && bleGattCharWriteFWUPControl != null) {
                                   _BluetoothGatt.setCharacteristicNotification(bleGattCharWriteFWUPControl, false);
                                }
                            }
                            _BluetoothGatt.disconnect();
                            DebugLog.d(TAG, "Connect: cHandler.postDelayed-disconnect");
                        }
                        _BluetoothGatt = null;
                        _state = States.ConnectWait;

                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";

                        hMap.put(jKey1, (int) 0);
                        hMap.put(jKey2, (int) CODE3); //
                        rJson = gson.toJson(hMap);
                        UnitySendMessage(mGameObject, callbackCommandError, rJson);
                    }
                }
            }, CONNECTION_TIMEOUT);

            if (_BluetoothGatt.connect()) {
                DebugLog.d(TAG, "Connect/Retry: connecting");

                _state = States.Connectting;
                return;

            } else {
                DebugLog.d(TAG, "Connect/Retry: disconnected");

                cHandler.removeCallbacksAndMessages(null); //接続要求を停止させる

                _state = States.ConnectWait;

                if (_BluetoothGatt != null) {
                    if (serviceUUID.equals(UUID_SERVICE_DECLARATION)) {
                        // 汎用通信用
                        if(_BluetoothGattReadCharacteristic != null)
                        {
                            _BluetoothGatt.setCharacteristicNotification(_BluetoothGattReadCharacteristic, false);
                        }
                    } else {
                        if (fwUpType == FirmwareUpdateCharacteristicType.Control && bleGattCharWriteFWUPControl != null) {
                            _BluetoothGatt.setCharacteristicNotification(bleGattCharWriteFWUPControl, false);
                        }
                    }
                    _BluetoothGatt.disconnect();
                    DebugLog.d(TAG, "Connect: _BluetoothGatt.connect()_else-disconnect");
                }
                _BluetoothGatt = null;
                _state = States.Didonnected;

                uhandler.post(new Runnable() {
                    public void run() {
                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";

                        hMap.put(jKey1, (int) 0);
                        hMap.put(jKey2, (int) CODE3); //
                        rJson = gson.toJson(hMap);
                        UnitySendMessage(mGameObject, callbackCommandError, rJson);
                    }
                });
                return;

            }
        }

        //メモ：BLEデバイスはペアリング不要ですが，端末の設定からペアリングしておくことで，ペアリング済みデバイスの一覧を取得できます。
        //https://qiita.com/binzume/items/1044ffd21cbb38b72bdb
        //for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
        //    DebugLog.d("BLE_TEST", "Bonded " + device.getName() + "addr:" + device.getAddress());
        //}

        //128ビットのアドレスが分かっている場合は，いきなり接続できる
        //すでに接続したことがあるデバイスであればこの方法も取れます
        // BluetoothデバイスをMACアドレスから取得
        _BluetoothDevice = _BluetoothAdapter.getRemoteDevice(address);
        if (_BluetoothDevice == null) {
            uhandler.post(new Runnable() {
                public void run() {
                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    hMap.put(jKey1, (int) 0);
                    hMap.put(jKey2, (int) CODE3); //
                    rJson = gson.toJson(hMap);
                    UnitySendMessage(mGameObject, callbackCommandError, rJson);
                }
            });
            DebugLog.d(TAG, "デバイスを取得できませんでした。：アドレス=" + address);
            return;
        }

        //コネクション中断
        cHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (_state != States.Connected) {
                    DebugLog.d(TAG, "Connect: postDelayed:timeout");
                    Context context = MyApplication.getInstance().getApplicationContext();

                    if (_BluetoothGatt != null) {
                        if (serviceUUID.equals(UUID_SERVICE_DECLARATION)) {
                            if(_BluetoothGattReadCharacteristic != null)
                            {
                                _BluetoothGatt.setCharacteristicNotification(_BluetoothGattReadCharacteristic, false);
                            }
                        } else {
                            if (fwUpType == FirmwareUpdateCharacteristicType.Control && bleGattCharWriteFWUPControl != null) {
                                _BluetoothGatt.setCharacteristicNotification(bleGattCharWriteFWUPControl, false);
                            }
                        }
                        _BluetoothGatt.disconnect();
                        DebugLog.d(TAG, "Connect: cHandler.postDelayed2-disconnect");
                    }
                    _BluetoothGatt = null;
                    _state = States.ConnectWait;

                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    hMap.put(jKey1, (int) 0);
                    hMap.put(jKey2, (int) CODE3); //
                    rJson = gson.toJson(hMap);
                    UnitySendMessage(mGameObject, callbackCommandError, rJson);
                }
            }
        }, CONNECTION_TIMEOUT);

        Context context = MyApplication.getInstance().getApplicationContext();
        _BluetoothGatt = _BluetoothDevice.connectGatt(context, false, _mGattCallback); //auctoConnectはtureにしても効かないものがあるらしい。スキャンしてるのでfalseで行う
        _BluetoothDeviceAddress = address;
        _state = States.Connectting;

        DebugLog.d(TAG, "Connect: 接続中..");

        return;
    }

    /**
     * デバイス切断（通常は切断しない）
     */
    public void Disconnect() {

        cHandler.removeCallbacksAndMessages(null); //接続要求を停止させる

        _state = States.Didonnected;

        if (_BluetoothAdapter == null) {
            DebugLog.e(TAG, "Disconnect: BluetoothAdapter not initialized");
            return;
        }

        if (_BluetoothGatt == null) {
            DebugLog.e(TAG, "Disconnect: BluetoothGatt not initialized");
            return;
        }

        //notoficationを無効にする
        //成功した場合、onConnectionStateChangeのコールバックにBluetoothGatt.STATE_DISCONNECTEDが返ってきます。
        if (serviceUUID.equals(UUID_SERVICE_DECLARATION)) {
            if (_BluetoothGattReadCharacteristic != null) {
                _BluetoothGatt.setCharacteristicNotification(_BluetoothGattReadCharacteristic, false);
            } else {
                if (fwUpType == FirmwareUpdateCharacteristicType.Control && bleGattCharWriteFWUPControl != null) {
                    _BluetoothGatt.setCharacteristicNotification(bleGattCharWriteFWUPControl, false);
                }
            }
        }
        _BluetoothGatt.disconnect();

        DebugLog.d(TAG, "Disconnect: disconnected");
    }

    /**
     * BLEのリソースをクローズ
     */
    private void Close() {

        if (_BluetoothGatt == null) {
            DebugLog.e(TAG, "Close");
            return;
        }
        _BluetoothGatt.close();
        _BluetoothGatt = null;
    }

    // GATT イベントハンドラ
    private final BluetoothGattCallback _mGattCallback = new BluetoothGattCallback() {

        // サービスの検索結果を返す
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            List<BluetoothGattService> serviceList = gatt.getServices();

            DebugLog.d(TAG, "onServicesDiscovered: serviceList.size=" + serviceList.size());

            for (BluetoothGattService s : serviceList) {
                DebugLog.d(TAG, "onServicesDiscovered: svc uuid=" + s.getUuid().toString());
                List<BluetoothGattCharacteristic> chlist = s.getCharacteristics();
                DebugLog.d(TAG, "onServicesDiscovered: chrlist.size=" + chlist.size());

                for (BluetoothGattCharacteristic c : chlist) {
                    UUID uuid = c.getUuid();
                    DebugLog.d(TAG, "onServicesDiscovered:  chr uuid=" + uuid.toString());
                    List<BluetoothGattDescriptor> dlist = c.getDescriptors();

                    DebugLog.d(TAG, "onServicesDiscovered:  desclist.size=" + dlist.size());
                    for (BluetoothGattDescriptor d : dlist) {
                        DebugLog.d(TAG, "onServicesDiscovered:   desc uuid=" + d.getUuid());
                    }
                }
            }

            //接続時
            if ((status == BluetoothGatt.GATT_SUCCESS) && (_state == States.Connectting)) {
                DebugLog.d(TAG, "onServicesDiscovered:GATT_SUCCESS");

                BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));

                if (service != null) {
                    DebugLog.d(TAG, "onServicesDiscovered: " + serviceUUID);

                    //Notifyの接続を試みてる
                    if (serviceUUID.equals(UUID_SERVICE_DECLARATION)) {
                        _BluetoothGattReadCharacteristic = service.getCharacteristic(UUID.fromString(UUID_READ_DECLARATION));
                        _BluetoothGattWriteCharacteristic = service.getCharacteristic(UUID.fromString(UUID_WRITE_DECLARATION));

                        if (_BluetoothGattReadCharacteristic != null)
                            DebugLog.d(TAG, "_BluetoothGattReadCharacteristic: " + UUID_READ_DECLARATION);
                        if (_BluetoothGattWriteCharacteristic != null)
                            DebugLog.d(TAG, "_BluetoothGattWriteCharacteristic: " + UUID_WRITE_DECLARATION);
                    } else {
                        bleGattCharWriteFWUPControl = service.getCharacteristic(UUID.fromString(UUID_CHAR_FWUP_WRITE_CONTROL));
                        bleGattCharWriteFWUPData = service.getCharacteristic(UUID.fromString(UUID_CHAR_FWUP_WRITE_DATA));

                        if (bleGattCharWriteFWUPControl != null)
                            DebugLog.d(TAG, "bleGattCharWriteFWUPControl: " + UUID_CHAR_FWUP_WRITE_CONTROL);
                        if (bleGattCharWriteFWUPData != null)
                            DebugLog.d(TAG, "bleGattCharWriteFWUPData: " + UUID_CHAR_FWUP_WRITE_DATA);
                    }

                    if (serviceUUID.equals(UUID_SERVICE_DECLARATION)
                            && _BluetoothGattReadCharacteristic != null
                            && _BluetoothGattWriteCharacteristic != null) {
                        registerGatt(gatt, _BluetoothGattReadCharacteristic);
                    } else if (serviceUUID.equals(UUID_SERVICE_FWUP)
                            && bleGattCharWriteFWUPControl != null
                            && bleGattCharWriteFWUPData != null) {
                        connectedFwUp();
                    } else {
                        DebugLog.e(TAG, "notification:failed");
                        uhandler.post(new Runnable() {
                            public void run() {
                                Map<String, Object> hMap = new HashMap<>();
                                String rJson = "";

                                hMap.put(jKey1, (int) 0);
                                hMap.put(jKey2, (int) CODE3); //
                                rJson = gson.toJson(hMap);
                                UnitySendMessage(mGameObject, callbackCommandError, rJson);
                            }
                        });
                    }
                }
            } else {
                DebugLog.d(TAG, "onServicesDiscovered:status=" + String.valueOf(status));

                if (status == BluetoothGatt.GATT_SUCCESS)
                    DebugLog.d(TAG, "onServicesDiscovered:GATT_SUCCESS");
                if (_state != States.Connectting)
                    DebugLog.d(TAG, "onServicesDiscovered:_state=" + _state.toString());
            }
        }

        /**
         * FW更新状態のデバイスとの接続を完了する
         */
        private void connectedFwUp() {

            _state = States.Connected;
            uhandler.post(new Runnable() {
                public void run() {

                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    if (null != _BluetoothDevice.getName()) {
                        hMap.put(jKey1, _BluetoothDevice.getName()); //
                    } else {
                        hMap.put(jKey1, TARGET_DEVICE_NAME); // NULLで何故か取得してしまうので固定値を返す
                    }
                    hMap.put(jKey2, _BluetoothDevice.getAddress()); //
                    rJson = gson.toJson(hMap);
                    DebugLog.d(TAG, rJson);

                    UnitySendMessage(mGameObject, mMethod3, rJson);
                }
            });
        }

        /**
         * GATT設定
         * @param gatt
         * @param bleGattChar
         */
        private void registerGatt(BluetoothGatt gatt, BluetoothGattCharacteristic bleGattChar) {

            boolean registered = gatt.setCharacteristicNotification(bleGattChar, true);

            if (registered) {
                cHandler.removeCallbacksAndMessages(null); //接続要求を停止させる

                _BluetoothGatt = gatt;

                // ペリフェラルのnotificationを有効化する。下のUUIDはCharacteristic Configuration Descriptor UUIDというもの
                BluetoothGattDescriptor descriptor = bleGattChar.getDescriptor(UUID.fromString(ANDROID_CENTRAL_UUID));

                // characteristic のnotification 有効化する
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                _BluetoothGatt.writeDescriptor(descriptor);

                _BluetoothDevice = _BluetoothGatt.getDevice();

                DebugLog.d(TAG, "notification:successed");
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            DebugLog.d(TAG, "onConnectionStateChange");

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                DebugLog.d(TAG, "onConnectionStateChange:STATE_CONNECTED");

                _BluetoothGatt = gatt;

                // ペリフェラルとの接続に成功した時点でサービスを検索する
                // 接続できたらサービスの検索
                boolean result = gatt.discoverServices();
                DebugLog.d(TAG, "Attempting to start service discovery:" + result);
            } else if (status == 133 && newState == BluetoothProfile.STATE_DISCONNECTED) {
                // エラー133で接続が切れたらリトライする
                Context context = MyApplication.getInstance().getApplicationContext();
                _BluetoothGatt = _BluetoothDevice.connectGatt(context, false, _mGattCallback);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) { //マイコンの応答がなくなった時の処理
                //ペリフェラルのサービスまで接続せずにGATTサーバーに接続して、
                //切れた場合でも本処理が行われる。（とりあえず接続タイムアウトエラーでなく、接続エラーで返している）

                DebugLog.d(TAG, "onConnectionStateChange:STATE_DISCONNECTED");

                cHandler.removeCallbacksAndMessages(null); //接続要求を停止させる

                // ペリフェラルとの接続が切れた時点でオブジェクトを空にする
                if (_BluetoothGatt != null) {
                    _BluetoothGatt.close();
                    DebugLog.d(TAG, "STATE_DISCONNECTED: disconnect");
                }
                _BluetoothGatt = null;
                _state = States.Didonnected;

                uhandler.post(new Runnable() {
                    public void run() {
                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";

                        hMap.put(jKey1, (int) 0); //コマンド
                        hMap.put(jKey2, (int) CODE4); //
                        rJson = gson.toJson(hMap);
                        UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す

                        Context context = MyApplication.getInstance().getApplicationContext();
                    }
                });
            }else{
                DebugLog.d(TAG, "onConnectionStateChange:error="+ String.valueOf(newState));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            DebugLog.d(TAG, "onCharacteristicWrite: " + status);

            switch (status) {
                case BluetoothGatt.GATT_SUCCESS: //書き込み完了

                    DebugLog.d(TAG, "onCharacteristicWrite: GATT_SUCCESS");

                    // WRITE成功
                    byte[] read_data = characteristic.getValue();
                    StringBuilder sb = new StringBuilder();
                    for (byte d : read_data) {
                        sb.append(String.format("%02X", d));
                    }
                    String str = sb.toString();

                    DebugLog.d(TAG, "Write-successed:" + str);
                    DebugLog.d(TAG, "Write-successed:timer=" + String.valueOf(_recvTimerCount));

                    switch (_writeType) {

                        case COMMAND3: //GET状態
                        case COMMAND_G1D_UPDATE: //プログラム更新状態(G1D)
                        case COMMAND6: //日時設定
                        case COMMAND7: //情報取得(電池残量取得)
                        case COMMAND8: //バージョン取得
                        case COMMAND14: //プログラム転送結果
                        case COMMAND16: //アラーム設定
                        case DEVICE_SETTING_CHANGE_COMMAND:
                            if (_recvTimerCount < RCV_TIMEOUT) { //既にタイムアウトになっていなければクリア
                                _recvTimerCount = 0; //書込成功時に受信タイマをクリア
                            }
                            break;
                        case COMMAND_G1D_UPDATE_DATA:
                        case COMMAND_G1D_UPDATE_CONTROL:
                            _recvTimerCount = Integer.MAX_VALUE; // タイムアウト判定不要
                            break;
                        default:
                            break;
                    }

                    uhandler.post(new Runnable() {
                        public void run() {
                            Map<String, Object> hMap = new HashMap<>();
                            String rJson = "";

                            hMap.put(jKey1, _writeType); //コマンド
                            hMap.put(jKey2, (boolean) true);
                            rJson = gson.toJson(hMap);
                            UnitySendMessage(mGameObject, callbackCommandWrite, rJson);
                        }
                    });
                    break;

                default:
                    DebugLog.d(TAG, "onCharacteristicWrite: default");
                    uhandler.post(new Runnable() {
                        public void run() {
                            Map<String, Object> hMap = new HashMap<>();
                            String rJson = "";

                            hMap.put(jKey1, _writeType); //コマンド
                            hMap.put(jKey2, (boolean) false); //
                            rJson = gson.toJson(hMap);
                            UnitySendMessage(mGameObject, callbackCommandWrite, rJson);
                        }
                    });
                    break;
            }
        }

        // Notification/Indicateの受信コールバック
        // 受信
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            DebugLog.d(TAG, "onCharacteristicChanged");

            String uuid;
            if (serviceUUID.equals(UUID_SERVICE_DECLARATION)) {
                uuid = UUID_READ_DECLARATION;
            } else {
                uuid = UUID_CHAR_FWUP_WRITE_CONTROL;
                // FW更新データは応答を返さないので不要
            }

            if (uuid.equals(characteristic.getUuid().toString())) {

                notification_data = characteristic.getValue();

                StringBuilder sb = new StringBuilder();
                for (byte d : notification_data) {
                    sb.append(String.format("%02X", d));
                }
                String str = sb.toString();
                DebugLog.d(TAG, "onCharacteristicChanged:" + str);

                switch (notification_data[0]) {

                    case (byte) 0xB0://状態変更(G1D)
                    case (byte) 0xC1://日時設定
                    case (byte) 0xC0://アラーム設定変更
                        switch (notification_data[1]) {
                            case (byte) 0x00: //OK(成功)
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要

                                if (_writeType == 3) { //GET状態
                                    _endReciveFlag = false;    //END受信済みフラグクリア

                                    _recvTimerCount = 0; //受信タイマクリア

                                    //データ取得用ロジック
                                    gDate = null; //初期化
                                    _getCounter = 0; //データ取得件数
                                    for (int i = 0; i < GET_DATA_MAX; i++) {
                                        _recvCount[i] = 0; //データ長(受信回数)
                                        _filename[i] = ""; //ファイルパス
                                        _aftername[i] = ""; //ファイルパス
                                    }
                                }
                                if (_writeType == 4)
                                    _recvTimerCount = Integer.MAX_VALUE; //プログラム更新状態(H1D):タイムアウト判定不要
                                if (_writeType == COMMAND_G1D_UPDATE)
                                    _recvTimerCount = Integer.MAX_VALUE;

                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        int tmp = 0;

                                        if ((byte) 0xB0 == notification_data[0])
                                            tmp = (int) COMMAND1;
                                        if ((byte) 0xC1 == notification_data[0])
                                            tmp = (int) COMMAND6;
                                        if ((byte) 0xC0 == notification_data[0])
                                            tmp = (int) COMMAND16;
                                        hMap.put(jKey1, tmp); //コマンド
                                        hMap.put(jKey2, (boolean) true); //
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, callbackCommandResponse, rJson); //応答結果を返す
                                    }
                                });
                                break;

                            case (byte) 0x01: //1：NG(失敗)
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        int tmp = 0;
                                        if ((byte) 0xB0 == notification_data[0])
                                            tmp = (int) COMMAND1;
                                        if ((byte) 0xC1 == notification_data[0])
                                            tmp = (int) COMMAND6;
                                        if ((byte) 0xC0 == notification_data[0])
                                            tmp = (int) COMMAND16;
                                        hMap.put(jKey1, tmp); //コマンド
                                        hMap.put(jKey2, (boolean) false); //
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, callbackCommandResponse, rJson); //応答結果を返す
                                    }
                                });
                                break;
                        }
                        break;
                    case (byte) 0xC2://情報取得
                        switch (notification_data[1]) {
                            case (byte) 0x00: //OK(成功)
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        hMap.put(jKey1, (int) notification_data[2]); //電池残量
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, mMethod5, rJson); //情報取得値を返す
                                    }
                                });
                                break;

                            case (byte) 0x01: //1：NG(失敗)
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        hMap.put(jKey1, COMMAND7); //コマンド
                                        hMap.put(jKey2, (boolean) false); //
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, callbackCommandResponse, rJson); //応答結果を返す
                                    }
                                });
                                break;
                        }
                        break;

                    case (byte)DEVICE_SETTING_CHANGE_COMMAND:
                        DebugLog.d(TAG, "onCharacteristicChanged: " + DEVICE_SETTING_CHANGE_COMMAND);
                        final boolean response = notification_data[1] == 0;
                        _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                        uhandler.post(new Runnable() {
                            public void run() {
                                Map<String, Object> hMap = new HashMap<>();
                                String rJson = "";

                                hMap.put(jKey1, DEVICE_SETTING_CHANGE_COMMAND);
                                hMap.put(jKey2, response);
                                rJson = gson.toJson(hMap);
                                UnitySendMessage(mGameObject, callbackCommandResponse, rJson);
                            }
                        });
                        break;

                    case (byte) 0xC3://バージョン取得
                        switch (notification_data[1]) {
                            case (byte) 0x00: //OK(成功)
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        hMap.put(jKey1, (int) notification_data[2]); //G1DアプリVer情報(メジャー)
                                        hMap.put(jKey2, (int) notification_data[3]); //G1DアプリVer情報(マイナー)
                                        hMap.put(jKey3, (int) notification_data[4]); //G1DアプリVer情報(リビジョン)
                                        hMap.put(jKey4, (int) notification_data[5]); //G1DアプリVer情報(ビルド)

                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, mMethod6, rJson); //情報取得値を返す
                                    }
                                });
                                break;

                            case (byte) 0x01: //1：NG(失敗)
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        hMap.put(jKey1, COMMAND8); //コマンド
                                        hMap.put(jKey2, (boolean) false); //
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, callbackCommandResponse, rJson); //応答結果を返す
                                    }
                                });
                                break;
                        }
                        break;

                    case (byte) 0xC5://デバイス状況取得
                        switch (notification_data[1]) {
                            case (byte) 0x00: //OK(成功)
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";

                                        byte[] devAdr = DataUtil.TakeOut(notification_data, 2, 6);
                                        StringBuilder sb = new StringBuilder();

                                        sb.append(String.format("%02X", devAdr[5]));
                                        sb.append(String.format("%02X", devAdr[4]));
                                        sb.append(String.format("%02X", devAdr[3]));
                                        sb.append(String.format("%02X", devAdr[2]));
                                        sb.append(String.format("%02X", devAdr[1]));
                                        sb.append(String.format("%02X", devAdr[0]));

                                        String str = sb.toString();
                                        hMap.put(jKey1, str); //デバイスアドレス xx:yy:zz:xx:yy:zz
                                        hMap.put(jKey2, (int) notification_data[8]); //測定データ保持数(0～10)
                                        hMap.put(jKey3, (int) notification_data[9]); //年
                                        hMap.put(jKey4, (int) notification_data[10]); //月
                                        hMap.put(jKey5, (int) notification_data[11]); //曜日
                                        hMap.put(jKey6, (int) notification_data[12]); //日
                                        hMap.put(jKey7, (int) notification_data[13]); //時
                                        hMap.put(jKey8, (int) notification_data[14]); //分
                                        hMap.put(jKey9, (int) notification_data[15]); //秒
                                        DebugLog.d(TAG, "onCharacteristicChanged:0xC500, DeviceAddress=" + str + "データ保存件数：" + String.valueOf((int) notification_data[8]));
                                        DebugLog.d(TAG, "onCharacteristicChanged:0xC500," + String.valueOf((int) notification_data[8]) + " " + String.valueOf((int) notification_data[9]) + " " + String.valueOf((int) notification_data[10]) + " " + String.valueOf((int) notification_data[11]) + " " + String.valueOf((int) notification_data[12]) + " " + String.valueOf((int) notification_data[13]) + " " + String.valueOf((int) notification_data[14]) + " " + String.valueOf((int) notification_data[15]));
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, mMethod7, rJson); //デバイス状況取得を返す
                                    }
                                });
                                break;

                            case (byte) 0x01: //1：NG(失敗)
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        hMap.put(jKey1, COMMAND18); //コマンド
                                        hMap.put(jKey2, (boolean) false); //
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, callbackCommandResponse, rJson); //応答結果を返す
                                    }
                                });
                                break;
                        }
                        break;

                    //データ取得///////////////////////////////////////////開始
                    case (byte) 0xE0: //NEXT

                        try {
                            DebugLog.d(TAG, "onCharacteristicChanged:" + "0xE0");

                            _recvTimerCount = 0;//受信タイマクリア

                            //データ長を既存のファイルに書込
                            mWriter.CsvModifyDate(HEADER_LINE, String.valueOf(_recvCount[_getCounter]), _filename[_getCounter]);

                            _getCounter = _getCounter + 1; //データ取得件数

                            uhandler.post(new Runnable() {
                                public void run() { //非同期でこの処理の際中に次の受信されることある
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    //データ取得の進捗状況を返す
                                    hMap.put(jKey1, (int) _getCounter); //現在の取得カウント
                                    //hMap.put(jKey2, 0); //トータルのカウント
                                    hMap.put(jKey2, (boolean) true); //NEXTならtrue NEXTじゃないならfalse
                                    hMap.put(jKey3, (boolean) false); //ENDならtrue ENDじゃないならfalse
                                    hMap.put(jKey4, _filename[_getCounter-1]); //CSVのパスの添付パス
                                    hMap.put(jKey5, _aftername[_getCounter-1]); //CSVのファイル名
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, mMethod8, rJson); //応答結果を返す

                                    DebugLog.d(TAG, "受信回数：" + String.valueOf(_recvCount[_getCounter-1]));
                                    DebugLog.d(TAG, "ファイル名(before)：" + _filename[_getCounter-1]);
                                    DebugLog.d(TAG, "ファイル名(after)：" + _aftername[_getCounter-1]);

                                }
                            });
                        } catch (FileNotFoundException e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE10); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                        } catch (IOException e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE10); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                        } catch (Exception e) { //不明（パース？）は該当データをスルー
                            DebugLog.e(TAG, e.getMessage());
                        }

                        break;

                    case (byte) 0xE1: //END
                        if(_endReciveFlag){ //異常受信時
                            DebugLog.d(TAG, "onCharacteristicChanged:END受信異常（複数回受信）" + "0xE1");
                            return;
                        }

                        _endReciveFlag = true;    //END受信済みフラグセット　//通常この処理はいらないが、なぜか複数回連続でENDが送られてきていたことがあった

                        try {
                            DebugLog.d(TAG, "onCharacteristicChanged:" + "0xE1");

                            _recvTimerCount = Integer.MAX_VALUE; //受信タイマクリア(データ取得完了通知をアプリから送るため）

                            if (gDate != null) { //1件でも取得していれば

                                //データ長を既存のファイルに書込
                                mWriter.CsvModifyDate(HEADER_LINE, String.valueOf(_recvCount[_getCounter]), _filename[_getCounter]);

                                _getCounter = _getCounter + 1; //データ取得件数

                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        Context context = MyApplication.getInstance().getApplicationContext();
                                        //Toast.makeText(context, "データ取得件数:" + String.valueOf(_getCounter), Toast.LENGTH_SHORT).show(); //TODO：削除する

                                        //データ取得の進捗状況を返す
                                        hMap.put(jKey1, (int) _getCounter); //現在の取得カウント
                                        //hMap.put(jKey2, 0); //トータルのカウント
                                        hMap.put(jKey2, (boolean) false); //NEXTならtrue NEXTじゃないならfalse
                                        hMap.put(jKey3, (boolean) true); //ENDならtrue ENDじゃないならfalse
                                        hMap.put(jKey4, _filename[_getCounter-1]); //CSVのパスの添付パス
                                        hMap.put(jKey5, _aftername[_getCounter-1]); //CSVのファイル名
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, mMethod8, rJson); //応答結果を返す


                                        for (int i = 0; i < _getCounter; i++) {
                                            DebugLog.d(TAG, String.valueOf(i + 1) + "/" + String.valueOf(_getCounter) + "件目のデータを処理中");
                                            DebugLog.d(TAG, "受信回数：" + String.valueOf(_recvCount[i]));
                                            DebugLog.d(TAG, "ファイル名(before)：" + _filename[i]);
                                            DebugLog.d(TAG, "ファイル名(after)：" + _aftername[i]);
                                        }

                                    }
                                });
                            } else {
                                DebugLog.d(TAG, "取得データなし");

                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        Context context = MyApplication.getInstance().getApplicationContext();

                                        //データ取得の進捗状況を返す
                                        hMap.put(jKey1, (int) 0); //現在の取得カウント
                                        //hMap.put(jKey2, 0); //トータルのカウント
                                        hMap.put(jKey2, (boolean) false); //NEXTならtrue NEXTじゃないならfalse
                                        hMap.put(jKey3, (boolean) true); //ENDならtrue ENDじゃないならfalse
                                        hMap.put(jKey4, _filename[0]); //CSVのパスの添付パス
                                        hMap.put(jKey5, _aftername[0]); //CSVのファイル名
                                        rJson = gson.toJson(hMap);
                                        UnitySendMessage(mGameObject, mMethod8, rJson); //応答結果を返す
                                    }
                                });
                            }

                        } catch (FileNotFoundException e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE10); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                        } catch (IOException e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";

                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE10); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                        } catch (Exception e) { //不明（パース？）は該当データをスルー
                            DebugLog.e(TAG, e.getMessage());
                        }

                        break;

                    case (byte) 0xE2: //枠情報(日時等)

                        try {
                            DebugLog.d(TAG, "onCharacteristicChanged:" + "0xE2");

                            _recvTimerCount = 0;///受信タイマクリア
                            _recvCount[_getCounter] = 0;//データ長(受信回数)

                            //年を20xxに変換
                            int _year = Integer.parseInt("20" + String.valueOf((int) (notification_data[1] & 0xff)));
                            DebugLog.d(TAG, String.valueOf(_year));

                            int _month;
                            //月が1-12の範囲で取得できるので、Calender用に-1をする
                            if (notification_data[2] != 0) {
                                _month = ((notification_data[2] & 0xff) - 1);
                            } else {
                                _month = 0;
                            }

                            int _date = notification_data[4] & 0xff;
                            int _hour = notification_data[5] & 0xff;
                            int _min = notification_data[6] & 0xff;
                            int _sec = notification_data[7] & 0xff;

                            DebugLog.d(TAG, String.format("%d/%d/%d %d:%d:%d", _year, _month, _date, _hour, _min, _sec));

                            gCal = new GregorianCalendar(_year, _month, _date, _hour, _min, _sec);
                            gDate = gCal.getTime();

                            String aftername = dateStdFormat.format(gDate); //ファイル名(Unityに返却用)
                            String directory = dateTimeFormat.format(gDate); //パス名
                            DebugLog.d(TAG, "aftername: " + aftername);
                            DebugLog.d(TAG, "directory: " + directory);

                            //tmp01～tmp10.csvをnative側で作成し
                            //Unity側でリネームしてもらう
                            String deviceID = mWriter.GetDeviceID();
                            DebugLog.d(TAG, "deviceID: " + deviceID);
                            deviceID = deviceID.replace(":", ""); //コロンを削除
                            String filename = "/" + deviceID + "/" + directory + "/tmp" + String.format("%02d", (_getCounter + 1)) + ".csv";
                            DebugLog.d(TAG, "filename: " + filename);
                            DebugLog.d(TAG, "_getCounter: " + _getCounter);
                            _filename[_getCounter] = filename; //dataフォルダ以下のファイルパス
                            _aftername[_getCounter] = aftername + ".csv"; //例：20180624182431.csv

                            File _file = mWriter.CsvFileCreate(filename); //CSVファイル作成
                            DebugLog.d(TAG, "CSVファイルパス：" + _file.getPath());

                            mWriter.CsvWriteHeader(); //ヘッダ情報作成
                            mWriter.CsvWriteDate(gCal, notification_data); //睡眠記録開始時間セット

                            //DebugLog.d(TAG, "CSVファイルパス：" + _file.getPath());
                            DebugLog.d(TAG, String.valueOf(_getCounter + 1) + "件目の受信中");
                            DebugLog.d(TAG, "受信回数：" + String.valueOf(_recvCount[_getCounter]));
                            DebugLog.d(TAG, "ファイル名：" + _filename[_getCounter]);

                        } catch (FileNotFoundException e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE10); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                        } catch (IOException e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE10); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                        } catch (Exception e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE7); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                            DebugLog.e(TAG, e.getMessage());
                        }
                        break;

                    case (byte) 0xE3: //機器データ
                        try {
                            DebugLog.d(TAG, "onCharacteristicChanged:" + "0xE3");

                            _recvTimerCount = 0;///受信タイマクリア

                            gCal.add(Calendar.SECOND, 30); //受信毎に30秒加算
                            gDate = gCal.getTime();
                            mWriter.CsvWriteData(gCal, notification_data); //睡眠データセット

                            _recvCount[_getCounter] = _recvCount[_getCounter] + 1;//データ長(受信回数)

                            DebugLog.d(TAG, "受信回数：" + String.valueOf(_recvCount[_getCounter]));

                        } catch (FileNotFoundException e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE10); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                        } catch (IOException e) {
                            uhandler.post(new Runnable() {
                                public void run() {
                                    Map<String, Object> hMap = new HashMap<>();
                                    String rJson = "";
                                    hMap.put(jKey1, COMMAND3); //コマンド
                                    hMap.put(jKey2, (int) CODE10); //
                                    rJson = gson.toJson(hMap);
                                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                                }
                            });
                        } catch (Exception e) { //機器データ取得時の不明（パース？）は該当データをスルー
                            DebugLog.e(TAG, e.getMessage());
                        }
                        break;
                    //データ取得///////////////////////////////////////////終了

                    case (byte) 0xC4://アラーム通知
                        switch (notification_data[1]) {
                            case (byte) 0x00: //0：いびき
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        boolean _jdg;
                                        hMap.put(jKey1, (int) 0); //コマンド

                                        if (notification_data[2] == 0) { //発生
                                            hMap.put(jKey2, (boolean) false); //
                                            _jdg = false;

                                        } else { //解除
                                            hMap.put(jKey2, (boolean) true); //
                                            _jdg = true;
                                        }
                                        rJson = gson.toJson(hMap);

                                        if (_jdg == false) { //発生
                                            if (mStat == PLAY_STAT.STOP) { //再生中はアラーム受信を無視（スルー）する
                                                ReadPreferences(); //アラーム機能のアプリ内設定値を読み込み
                                                VibrationStart(); //バイブレーション再生
                                                StartAlarmPlay(); //アラーム再生
                                                notificationHelper.alarmNotificationSet(ARMTYPE2 + ARMTITLE, ARMTYPE2 + ARMTEXT); //アラーム用の通知を設定
                                                UnitySendMessage(mGameObject, mMethod11, rJson);
                                            }
                                        } else { //解除
                                            ReadPreferences(); //アラーム機能のアプリ内設定値を読み込み
                                            VibrationStop(); //バイブレーション停止
                                            StopAlarm(); //アラーム停止
                                            notificationHelper.cancelNotification(); //通知削除
                                            UnitySendMessage(mGameObject, mMethod11, rJson);
                                        }
                                    }
                                });
                                break;
                            case (byte) 0x01: //1：低呼吸
                                _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                                uhandler.post(new Runnable() {
                                    public void run() {
                                        Map<String, Object> hMap = new HashMap<>();
                                        String rJson = "";
                                        boolean _jdg;
                                        hMap.put(jKey1, (int) 1); //コマンド
                                        if (notification_data[2] == 0) { //発生
                                            hMap.put(jKey2, (boolean) false); //
                                            _jdg = false;

                                        } else { //解除
                                            hMap.put(jKey2, (boolean) true); //
                                            _jdg = true;
                                        }
                                        rJson = gson.toJson(hMap);

                                        if (_jdg == false) { //発生
                                            if (mStat == PLAY_STAT.STOP) { //再生中はアラーム受信を無視（スルー）する
                                                ReadPreferences(); //アラーム機能のアプリ内設定値を読み込み
                                                VibrationStart(); //バイブレーション再生
                                                StartAlarmPlay(); //アラーム再生
                                                notificationHelper.alarmNotificationSet(ARMTYPE1 + ARMTITLE, ARMTYPE1 + ARMTEXT); //アラーム用の通知を設定
                                                UnitySendMessage(mGameObject, mMethod11, rJson);
                                            }
                                        } else { //解除
                                            ReadPreferences(); //アラーム機能のアプリ内設定値を読み込み
                                            VibrationStop(); //バイブレーション停止
                                            StopAlarm(); //アラーム停止
                                            notificationHelper.cancelNotification(); //通知削除
                                            UnitySendMessage(mGameObject, mMethod11, rJson);
                                        }
                                    }
                                });
                                break;
                        }

                    case (byte) 0xD1://プログラム転送結果
                        _recvTimerCount = Integer.MAX_VALUE; //タイムアウト判定不要
                        uhandler.post(new Runnable() {
                            public void run() {
                                Map<String, Object> hMap = new HashMap<>();
                                String rJson = "";
                                hMap.put(jKey1, (int) notification_data[1]);

                                rJson = gson.toJson(hMap);
                                UnitySendMessage(mGameObject, mMethod9, rJson); //情報取得値を返す
                            }
                        });
                        break;

                    case (byte) 0xD3://プログラム更新完了確認
                        uhandler.post(new Runnable() {
                            public void run() {
                                Map<String, Object> hMap = new HashMap<>();
                                String rJson = "";
                                _recvTimerCount = Integer.MAX_VALUE; //受信タイマクリア(Unity側でタイムアウト判定）

                                hMap.put(jKey1, (int) notification_data[1]);
                                hMap.put(jKey2, (int) notification_data[2]);
                                hMap.put(jKey3, (int) notification_data[3]);
                                hMap.put(jKey4, (int) notification_data[4]);
                                hMap.put(jKey5, (int) notification_data[5]);

                                rJson = gson.toJson(hMap);
                                UnitySendMessage(mGameObject, callbackH1dTransferDataDone, rJson); //情報取得値を返す
                            }
                        });
                        break;

                    default:
                        // TODO: ファーム更新コマンドの応答の可能性があるので、判定する
                        DebugLog.d(TAG, "受信データを表示します。");
                        sb = new StringBuilder();
                        for (byte d : notification_data) {
                            sb.append(String.format("%02X ", d));
                        }
                        DebugLog.d(TAG, "wVal = " + sb.toString());
                        break;
                }
            }
        }

        //未使用
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {

            DebugLog.d(TAG, "onCharacteristicRead: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // READ成功
                byte[] read_data = characteristic.getValue();

                StringBuilder sb = new StringBuilder();

                for (byte d : read_data) {
                    sb.append(String.format("%02X", d));
                }
                String str = sb.toString();
                DebugLog.d(TAG, "onCharacteristicRead:" + str);
            }
        }

        //Notify登録の結果
        //Callback indicating the result of a descriptor write operation.
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            DebugLog.d(TAG, "onDescriptorWrite:" + "uuid=" + descriptor.getUuid().toString() + ", status=" + status);
            DebugLog.d(TAG, "onDescriptorWrite:" + "uuid=" + UUID.fromString(ANDROID_CENTRAL_UUID).toString());

            if (descriptor.getUuid().toString().equals(UUID.fromString(ANDROID_CENTRAL_UUID).toString())) {
                DebugLog.d(TAG, "onDescriptorWrite:" + "uuid=same");

                _state = States.Connected;
                uhandler.post(new Runnable() {
                    public void run() {

                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";


                        if (null != _BluetoothDevice.getName()) {
                            hMap.put(jKey1, (String) _BluetoothDevice.getName()); //
                        } else {
                            hMap.put(jKey1, (String) TARGET_DEVICE_NAME); // NULLで何故か取得してしまうので固定値を返す
                        }
                        hMap.put(jKey2, (String) _BluetoothDevice.getAddress()); //
                        rJson = gson.toJson(hMap);
                        DebugLog.d(TAG, rJson);

                        UnitySendMessage(mGameObject, mMethod3, rJson);
                    }
                });
            }else{
                DebugLog.d(TAG, "onDescriptorWrite:" + "uuid=not same");
            }
        }

        //Callback reporting the result of a descriptor read operation.
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            DebugLog.d(TAG, "onDescriptorRead:" + "uuid=" + descriptor.getUuid().toString() + ", status=" + status);

            if (descriptor.getUuid().toString().equals(UUID.fromString(ANDROID_CENTRAL_UUID).toString())) {
            }
        }
    };


    /**
     * 時刻設定
     */
    public void SendCommandDate(String date) {

        try {
            DebugLog.d(TAG, "SendCommandDate:" + date);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(date));

            byte[] wVal = new byte[8];
            wVal[0] = (byte) 0xC1;

            int _year = cal.get(Calendar.YEAR) % 100;
            int _month = cal.get(Calendar.MONTH) + 1;
            int _date = cal.get(Calendar.DATE);
            int _hour = cal.get(Calendar.HOUR_OF_DAY);
            int _min = cal.get(Calendar.MINUTE);
            int _sec = cal.get(Calendar.SECOND);

            wVal[1] = (byte) (_year & 0xff);
            wVal[2] = (byte) (_month & 0xff);
            wVal[3] = (byte) (cal.get(Calendar.DAY_OF_WEEK) - 1);
            wVal[4] = (byte) (_date & 0xff);
            wVal[5] = (byte) (_hour & 0xff);
            wVal[6] = (byte) (_min & 0xff);
            wVal[7] = (byte) (_sec & 0xff);

            WriteGatt(wVal, COMMAND6); //送信

        } catch (ParseException e) {
            DebugLog.d(TAG, "SendCommandDate-ParseException:" + e.getMessage());
            uhandler.post(new Runnable() {
                public void run() {
                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    hMap.put(jKey1, COMMAND6); //コマンド
                    hMap.put(jKey2, (int) CODE7); //
                    rJson = gson.toJson(hMap);
                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                }
            });
        }
    }

    /**
     * アラーム設定
     */
    public void SendCommandAlarm(int data1, int data2, int data3, int data4, int data5, int data6, int data7) {

        DebugLog.d(TAG, String.valueOf(data1) + "," + String.valueOf(data2) + "," + String.valueOf(data3) + "," + String.valueOf(data4) + "," + String.valueOf(data5) + "," + String.valueOf(data6) + "," + String.valueOf(data7));

        byte[] wVal = new byte[8];
        wVal[0] = (byte) 0xC0;
        wVal[1] = (byte) data1;
        wVal[2] = (byte) data2;
        wVal[3] = (byte) data3;
        wVal[4] = (byte) data4;
        wVal[5] = (byte) data5;
        wVal[6] = (byte) data6;
        wVal[7] = (byte) data7;

        WriteGatt(wVal, COMMAND16); //送信
    }

    /**
     * BLEコマンドを送信する.
     *
     * @param commandType コマンド種別
     * @param command コマンド
     */
    public void sendBleCommand(int commandType, byte[] command) {
        WriteGatt(command, commandType);
    }

    /**
     * ファーム更新用のサービスUUIDに変更する
     */
    public void changeServiceUUIDToFirmwareUpdate() {
        serviceUUID = UUID_SERVICE_FWUP;
    }

    /**
     * ファーム更新制御コマンド用のキャラクタリスティックUUIDに変更する
     */
    public void changeCharacteristicUUIDToFirmwareUpdateControl() {
        fwUpType = FirmwareUpdateCharacteristicType.Control;
    }

    /**
     * ファーム更新データ用のキャラクタリスティックUUIDに変更する
     */
    public void changeCharacteristicUUIDToFirmwareUpdateData() {
        fwUpType = FirmwareUpdateCharacteristicType.Data;
    }

    /**
     * 汎用通信用のサービスUUIDに変更する
     */
    public void changeServiceUUIDToNormal() {
        serviceUUID = UUID_SERVICE_DECLARATION;
    }

    /**
     * データ取得完了通知
     */
    public void SendCommandGetFinish(boolean result) {

        byte[] wVal = new byte[2];
        wVal[0] = (byte) 0xE4;
        wVal[1] = (byte) (int) (result ? 0 : 1); //TRUE:OK(0), FALSE:NG(1)

        StringBuilder sb = new StringBuilder();

        for (byte d : wVal) {
            sb.append(String.format("%02X", d));
        }
        String str = sb.toString();
        DebugLog.d(TAG, "SendCommandGetFinish:" + str);

        WriteGatt(wVal, COMMAND19); //送信
    }

    /**
     * プログラム転送結果(H1D)
     */
    public void SendCommandH1dSum(byte[] sum) {

        byte[] wVal;

        wVal = new byte[5];
        wVal[0] = (byte) 0xD1;
        wVal[1] = sum[0];
        wVal[2] = sum[1];
        wVal[3] = sum[2];
        wVal[4] = sum[3];

        StringBuilder sb = new StringBuilder();

        for (byte d : wVal) {
            sb.append(String.format("%02X", d));
        }
        String str = sb.toString();
        DebugLog.d(TAG, "SendCommandH1dSum:" + str);

        WriteGatt(wVal, COMMAND14); //送信
    }

    /**
     * プログラム転送(H1D)
     */
    public void SendCommandH1dCode(byte[] code, int len) {

        if (len != H1D_PROGRAM_CODE) {
            DebugLog.d(TAG, "SendCommandH1dCode:failed,length=" + String.valueOf(len));
            uhandler.post(new Runnable() {
                public void run() {
                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    hMap.put(jKey1, _writeType); //コマンド
                    hMap.put(jKey2, (boolean) false); //
                    rJson = gson.toJson(hMap);
                    UnitySendMessage(mGameObject, callbackCommandWrite, rJson); //応答結果を返す
                }
            });
            return;
        }
        WriteGatt(code, COMMAND_PROGRAM_TRANSFER); //送信
    }

    /**
     * コマンド送信を行う
     */
    public void SendCommandId(int id) {

        Map<String, Object> hMap = new HashMap<>();
        String rJson = "";
        byte[] wVal;

        _sendCommandId = id;

        switch (id) {

            case COMMAND2: //待機状態
                wVal = new byte[2];
                wVal[0] = (byte) 0xB0;
                wVal[1] = (byte) 0x00;
                break;

            case COMMAND3: //GET状態
                wVal = new byte[2];
                wVal[0] = (byte) 0xB0;
                wVal[1] = (byte) 0x03;
                break;

            case COMMAND_G1D_UPDATE: //プログラム更新状態(G1D)
                wVal = new byte[2];
                wVal[0] = (byte) 0xB0;
                wVal[1] = (byte) 0x05;
                break;

            case COMMAND7: //情報取得(電池残量取得)
                wVal = new byte[1];
                wVal[0] = (byte) 0xC2;
                break;


            case COMMAND8: //バージョン取得
                wVal = new byte[1];
                wVal[0] = (byte) 0xC3;
                break;

            case COMMAND_COMPLETE_UPDATE: //プログラム更新完了確認
                wVal = new byte[1];
                wVal[0] = (byte) 0xD3;
                break;

            case COMMAND18: //デバイス状況取得
                wVal = new byte[1];
                wVal[0] = (byte) 0xC5;
                break;

            default:
                DebugLog.d(TAG, "SendCommandId:failed,id=" + String.valueOf(id));
                uhandler.post(new Runnable() {
                    public void run() {
                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";
                        hMap.put(jKey1, _sendCommandId); //コマンド
                        hMap.put(jKey2, (boolean) false); //
                        rJson = gson.toJson(hMap);
                        UnitySendMessage(mGameObject, callbackCommandWrite, rJson); //応答結果を返す
                    }
                });
                return;
        }

        WriteGatt(wVal, id); //送信
    }

    /**
     * 書込(キャラクタリスティック)
     */
    public void WriteGatt(byte[] wVal, int wType) {

        _writeType = wType; //書込成功時にタイプを設定

        BluetoothGattCharacteristic gattCharWrite = null;

        if (serviceUUID.equals(UUID_SERVICE_DECLARATION)) {
            gattCharWrite = _BluetoothGattWriteCharacteristic;
        } else {
            if (fwUpType == FirmwareUpdateCharacteristicType.Control && bleGattCharWriteFWUPControl != null) {
                bleGattCharWriteFWUPControl.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                if(_BluetoothGatt != null) {
                    _BluetoothGatt.setCharacteristicNotification(bleGattCharWriteFWUPControl, false);
                }
                gattCharWrite = bleGattCharWriteFWUPControl;
            } else if (bleGattCharWriteFWUPData != null) {
                bleGattCharWriteFWUPData.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                gattCharWrite = bleGattCharWriteFWUPData;
            }
        }

        if (gattCharWrite != null) {
            gattCharWrite.setValue(wVal);
            if (_BluetoothGatt != null) {
                boolean status = _BluetoothGatt.writeCharacteristic(gattCharWrite);

                if (!status) {
                    DebugLog.d(TAG, "WriteGatt:failed,type=" + String.valueOf(wType));
                    uhandler.post(new Runnable() {
                        public void run() {
                            Map<String, Object> hMap = new HashMap<>();
                            String rJson = "";

                            hMap.put(jKey1, _writeType); //コマンド
                            hMap.put(jKey2, (boolean) false); //
                            rJson = gson.toJson(hMap);
                            UnitySendMessage(mGameObject, callbackCommandWrite, rJson); //応答結果を返す
                        }
                    });

                } else {
                    _recvTimerCount = Integer.MAX_VALUE;

                    DebugLog.d(TAG, "書き込みデータを表示します。");
                    StringBuilder sb = new StringBuilder();
                    for (byte d : wVal) {
                        sb.append(String.format("%02X ", d));
                    }
                    DebugLog.d(TAG, "wVal = " + sb.toString());

                    switch (_writeType) {

                        case COMMAND2: //待機状態
                        case COMMAND3: //GET状態
                        case COMMAND_G1D_UPDATE: //プログラム更新状態(G1D)
                        case COMMAND6: //日時設定
                        case COMMAND7: //情報取得(電池残量取得)
                        case COMMAND8: //バージョン取得
                        case COMMAND14: //プログラム転送結果
                        case COMMAND16: //アラーム設定
                        case COMMAND18: //デバイス状況取得
                        case DEVICE_SETTING_CHANGE_COMMAND:
                            _recvTimerCount = 0; //書込成功時に受信タイマをクリア
                            break;
                        case COMMAND_G1D_UPDATE_DATA:
                        case COMMAND_G1D_UPDATE_CONTROL:
                            _recvTimerCount = Integer.MAX_VALUE; // タイムアウト判定不要
                            break;
                        default:
                            break;
                    }

                    DebugLog.d(TAG, "WriteGatt:successed,type=" + String.valueOf(wType));
                    DebugLog.d(TAG, "WriteGatt:successed,timer=" + String.valueOf(_recvTimerCount));
                }
            } else {
                DebugLog.e(TAG, "WriteGatt:BluetoothGatt=null"); //Bluetooth無効状態で送信しようとするとここ
                uhandler.post(new Runnable() {
                    public void run() {
                        Map<String, Object> hMap = new HashMap<>();
                        String rJson = "";

                        hMap.put(jKey1, _writeType); //コマンド
                        hMap.put(jKey2, (boolean) false); //
                        rJson = gson.toJson(hMap);
                        UnitySendMessage(mGameObject, callbackCommandWrite, rJson); //応答結果を返す
                    }
                });
            }
        } else {
            DebugLog.e(TAG, "WriteGatt:BluetoothGattWriteCharacteristic=null");
            uhandler.post(new Runnable() {
                public void run() {
                    Map<String, Object> hMap = new HashMap<>();
                    String rJson = "";

                    hMap.put(jKey1, _writeType); //コマンド
                    hMap.put(jKey2, (boolean) false); //
                    rJson = gson.toJson(hMap);
                    UnitySendMessage(mGameObject, callbackCommandWrite, rJson); //応答結果を返す
                }
            });
        }
    }

    /**
     * 読込(キャラクタリスティック)　※未使用のためコールバック処理なし
     */
    public void ReadCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (_BluetoothAdapter == null || _BluetoothGatt == null) {
            DebugLog.e(TAG, "ReadCharacteristic: BluetoothAdapter not initialized");
            return;
        }

        _BluetoothGatt.readCharacteristic(characteristic);
    }

    //受信タイムアウト判定用
//スケジューラー
    public static void schedule() {

        if (timer != null) {
            DebugLog.d(TAG, "schedule:timer-cancel");
            timer.cancel();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {

                if (_recvTimerCount < Integer.MAX_VALUE) {
                    _recvTimerCount = _recvTimerCount + 1;
                }
                if (_recvTimerCount == RCV_TIMEOUT) { //タイムアウト判定
                    DebugLog.d(TAG, "schedule-timer:" + String.valueOf(_recvTimerCount) + " writeType:" + _writeType);
                    mServiceHndlr.obtainMessage(MESSAGE_TIMEOUT_STS, _writeType).sendToTarget();
                }
            }
        }, 0, TIMER_TIME);
    }

    //Handler#handleMessage()をoverrideし
//ActivityからのMessageを受け取った際の処理を実装
    static class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            DebugLog.d(TAG, "ServiceHandler:handleMessage:" + String.valueOf(msg.what));

            Map<String, Object> hMap = new HashMap<>();
            String rJson = "";

            switch (msg.what) {

                case MESSAGE_TIMEOUT_STS: //受信タイムアウト

                    int msgType = (int) msg.obj;

                    hMap.put(jKey1, msgType); //コマンド
                    hMap.put(jKey2, (int) CODE5); //
                    rJson = gson.toJson(hMap);
                    UnitySendMessage(mGameObject, callbackCommandError, rJson); //応答結果を返す
                    break;

                default:
                    super.handleMessage(msg);
            }
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////////

    //通知削除 ※削除する通知の種類としては、アラーム通知を削除
    public static void CancelNotification() {

        notificationHelper.cancelNotification();
    }

    /**
     * アプリがバックグラウンド、又はロック画面であればtrueを返却
     */
    static boolean shouldShowNotification(Context context) {
        android.app.ActivityManager.RunningAppProcessInfo myProcess = new RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(myProcess);
        if (myProcess.importance != RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
            return true;

        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        // app is in foreground, but if screen is locked show notification anyway

        return km.isKeyguardLocked();
        //return km.inKeyguardRestrictedInputMode();
    }

    /**
     * アラーム機能のアプリ内設定値を読み込み
     */
    public static void ReadPreferences() {

        Context context = MyApplication.getInstance();
        // SharedPreferences取得。UnityのPlayerPrefsでは、
        // バンドル名のSharedPreferencesを使用しているので合わせる
        SharedPreferences pref = context.getSharedPreferences(context.getPackageName()+".v2.playerprefs", Context.MODE_PRIVATE);

        _selectAlarm = pref.getInt(ExtractionKey.PREFERENCE_KEY_SELECT_ALERM, 0); //選択中のアラーム ※デフォルト：0
        if ((_selectAlarm < 0) || (_selectAlarm > 5)) {
            DebugLog.d(TAG, "異常なインデックス：アラーム");
            _selectAlarm = 0;
        }

        _isVibration = (pref.getInt(ExtractionKey.PREFERENCE_KEY_VIBRATION, 1) != 0); //バイブレーションのON/OFF　※デフォルト：ON
        _isFeedin = (pref.getInt(ExtractionKey.PREFERENCE_KEY_FEEDIN, 1) != 0); //フェードインのON/OFF　※デフォルト：ON
        _alarmCalltime = pref.getInt(ExtractionKey.PREFERENCE_KEY_ALERM_CALLTIME, 0); //鳴動時間　※デフォルト：なし
        if ((_alarmCalltime < 0) || (_alarmCalltime > 4)) {
            DebugLog.d(TAG, "異常なインデックス：鳴動時間");
            _alarmCalltime = 4; //最大値にしておく
        }
        DebugLog.d(TAG, "選択中のアラーム(index)：=" + String.valueOf(_selectAlarm));
        DebugLog.d(TAG, "バイブレーション：=" + String.valueOf(_isVibration));
        DebugLog.d(TAG, "フェードイン：=" + String.valueOf(_isFeedin));
        DebugLog.d(TAG, "鳴動時間(index)：=" + String.valueOf(_alarmCalltime));
    }

    /**
     * バイブレーション再生
     */
    public static void VibrationStart() {
        if (vTimer != null) {
            vTimer.cancel();
        }
        if (_isVibration) {
            vTimer = new Timer();
            vTimer.schedule(new TimerTask() {
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mVibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        mVibrator.vibrate(1000); //リピート
                    }
                }
            }, 0, 2000);
        } else {
            DebugLog.d(TAG, "VibrationStart:バイブレーション無効");
        }
    }

    /**
     * バイブレーション停止
     */
    public static void VibrationStop() {
        if (vTimer != null) {
            vTimer.cancel();
            vTimer = null;
        }
    }

    /**
     * アラーム再生
     */
    private static void StartAlarmPlay() {

        mStat = PLAY_STAT.STOP;

        //選択中のアラーム取得
        String externalDirectory = MyApplication.getInstance().getExternalFilesDir(null).getPath(); ///storage/emulated/0/Android/data/<アプリのID>/files
        String selectNum = String.format("%02d", _selectAlarm + 1); //選択中の番号 0始まりなので+1を加算
        //パスに変換
        String path = externalDirectory + "/Music/Template/alarm" + selectNum + ".ogg"; //音楽パス
        DebugLog.d(TAG, "StartAlarmPlay/path:" + path);

        File file = new File(path);
        if (file.exists()) {
            // 存在する
            DebugLog.d(TAG, "ファイルが存在する");
        } else {
            // 存在しない
            DebugLog.d(TAG, "ファイルが存在しません");
            mStat = PLAY_STAT.PLAYING;
            return;
        }

        Uri uri = Uri.parse(path); //URIに変換
        DebugLog.d(TAG, "StartAlarmPlay/Uri:" + uri);

        if (player == null) {
            player = new MediaPlayer();
        } else {
            DebugLog.d(TAG, "MediaPlayer not null");
        }
        mChgCntVolume = 0.0f;
        mNowVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM); // アラームの現在の音量を取得

        try {
            player.setDataSource(MyApplication.getInstance(), uri); // 音声を設定
            player.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setLegacyStreamType(AudioManager.STREAM_ALARM) // アラームのボリュームで再生
                            .build());
            player.prepare();                                     // 音声を読み込み
            player.start(); // 再生
            mStat = PLAY_STAT.PLAYING;

            WatchAlarmPlay();

        } catch (IllegalStateException e) {
            e.printStackTrace();
            DebugLog.e(TAG, "StartAlarmPlay:" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            DebugLog.e(TAG, "StartAlarmPlay:" + e.getMessage());
        }
    }

    /**
     * アラーム連続再生
     */
    private static void WatchAlarmPlay() {
        if (mTimer != null) {
            StopAlarm();
        }
        mPlayCoumt = 0;
        mChgCntVolume = 0.0f;
        DebugLog.d(TAG, "WatchAlarmPlay");

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            public void run() {

                if(!player.isPlaying()) {
                    player.start(); // 再生
                }

                if (mPlayCoumt < Integer.MAX_VALUE) {
                    mPlayCoumt = mPlayCoumt + 100;
                } else {
                    mPlayCoumt = Integer.MAX_VALUE;
                }
                DebugLog.d(TAG, "WatchAlarmPlay/：MEIDOU_TIME=" + String.valueOf(MEIDOU_TIME[_alarmCalltime]));

                if (mPlayCoumt >= MEIDOU_TIME[_alarmCalltime]) {
                    DebugLog.d(TAG, "WatchAlarmPlay-TimerFinished");
                    StopAlarm();
                    VibrationStop();
                }
                DebugLog.d(TAG, "WatchAlarmPlay/：mPlayCoumt=" + String.valueOf(mPlayCoumt));

                if ((_isFeedin) && (player != null)) {
                    if (mChgCntVolume < mNowVolume) {
                        mStat = PLAY_STAT.FADEIN;

                        float ftmp = ((float) mNowVolume / 50f);

                        if (mNowVolume != 0) {
                            DebugLog.d(TAG, "WatchAlarmPlay/：mNowVolume / 50=" + String.valueOf(ftmp));

                            mChgCntVolume += ftmp; //2.5秒でフェードイン
                        }
                        if ((mChgCntVolume >= mNowVolume) || (ftmp == 0f))
                            mChgCntVolume = mNowVolume;

                        DebugLog.d(TAG, "WatchAlarmPlay/：_chgCntVolume=" + String.valueOf(mChgCntVolume) + " _nowVolume:=" + String.valueOf(mNowVolume));

                        player.setVolume(mChgCntVolume, mChgCntVolume);

                    } else {
                        mStat = PLAY_STAT.PLAYING;

                        DebugLog.d(TAG, "WatchAlarmPlay/：_chgCntVolume=" + String.valueOf(mChgCntVolume) + " _nowVolume:=" + String.valueOf(mNowVolume));
                    }
                } else {
                    DebugLog.d(TAG, "WatchAlarmPlay:フェードイン無効");
                }
            }
        }, 0, 100);
    }

    /**
     * アラーム停止
     */
    public static void StopAlarm() {
        DebugLog.d(TAG, "StopAlarm");

        mStat = PLAY_STAT.STOP;

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        if (player != null) {
            player.stop();
            player.release();
            player = null;
        } else {
            DebugLog.d(TAG, "StopAlarm=null");
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////
}
