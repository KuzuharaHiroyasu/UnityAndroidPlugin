package jp.co.onea.sleeim.unityandroidplugin.main;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

//import com.squareup.leakcanary.LeakCanary;
import com.unity3d.player.UnityPlayer;

import jp.co.onea.sleeim.unityandroidplugin.data.CsvWriter;
import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;

/**
 * プラグイン全体でApplicationContextをどこからでも呼び出せるようにする。
 * 参考 http://www.bokukoko.info/entry/2014/05/08/Android_%E3%81%A7_Context_%E3%82%92%E6%B8%A1%E3%81%95%E3%81%AA%E3%81%8F%E3%81%A6%E3%82%82_getString_%E3%82%84Shared_Preference_%E3%82%92%E4%BD%BF%E3%81%88%E3%82%8B%E3%82%88%E3%81%86%E3%81%AB%E3%81%99
 *      http://tondol.hatenablog.jp/entry/20120616/1339786005
 *      https://qiita.com/MuuKojima/items/04081fcd7f82c063fee1
 * 実装方法
 *      Context context = MyApplication.getInstance().getApplicationContext();
 */
// メモ：
// ApplicationクラスではonDestroyはコールされない
//
public class MyApplication extends Application {

    private final static String TAG = MyApplication.class.getSimpleName();

    private static MyApplication singleton  = null;
    private static BleService _BleService;

    public static boolean  mBound = false;

    @Override
    public void onCreate() {
        super.onCreate();

        DebugLog.d(TAG, TAG+": onCreate:");

        singleton  = this;

        // Start to the service
//        BleService.start(this); //接続開始：ServiceをDestoryしたくないので起動時にやっておく

        // Bind to the service
        //Intent gattServiceIntent = new Intent(this, BleService.class);
        //bindService(gattServiceIntent, _ServiceConnection, BIND_AUTO_CREATE); //BIND_AUTO_CREATE

        //TODO：以下デバッグ
        //Thread wreiteLogThread = new WriteLogThread(getApplicationContext()); //TODO：デバッグ
        //wreiteLogThread.start(); //TODO：デバッグ

        //TODO：以下デバッグ
        //if (LeakCanary.isInAnalyzerProcess(this)) {
        //    // This process is dedicated to LeakCanary for heap analysis.
        //    // You should not init your app in this process.
        //    return;
        //}
        //LeakCanary.install(this);
    }

    public static Context getInstance() {
        return singleton;
    }

    // Code to manage Service lifecycle.
    public static final ServiceConnection _ServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            _BleService = ((BleService.LocalBinder) service).getService();

            DebugLog.d(TAG, "ServiceConnected:"+String.valueOf(componentName)+" / " +((BleService.LocalBinder) service).getService());

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // サービスとの切断(異常系処理)
            // プロセスのクラッシュなど意図しないサービスの切断が発生した場合に呼ばれる。
            //　Serviceを動かしてるProcessがcrashするかkillされない限り呼ばれない

            DebugLog.d(TAG, "ServiceDisConnected:"+String.valueOf(componentName));

            mBound = false;
        }
    };

    // 接続状態
    public static void doBindService() {
        DebugLog.d(TAG, "bindService:");

        Intent gattServiceIntent = new Intent(MyApplication.getInstance(), BleService.class);
        MyApplication.getInstance().bindService(gattServiceIntent, _ServiceConnection, BIND_AUTO_CREATE); //BIND_AUTO_CREATE
    }

    // 接続解除
    // onUnbindでTRUEで返しているため、onRebind時に同じもので再接続する
    public static void doUnbindService() {
        DebugLog.d(TAG, "doUnbindService:");

        // Detach our existing connection.
        if (mBound) {
            MyApplication.getInstance().unbindService(_ServiceConnection);
            mBound = false;
        }
    }

        //////////////////////////////////////////////////////////////////////////////////////////

    /**
     *  Bluetooth初期化処理
     *  各種デリゲートはイニシャル時に渡したメソッドで実行
     *  @param  gameObject   呼び出し元のgameObject名
     *  @param  method1 呼び出し元のメソッド名(戻り値返却)
     *  @param  method2 呼び出し元のメソッド名(戻り値返却)
     *  @param  method3 呼び出し元のメソッド名(戻り値返却)
     *  @param  method4 呼び出し元のメソッド名(戻り値返却)
     *  @param  method5 呼び出し元のメソッド名(戻り値返却)
     *  @param  method6 呼び出し元のメソッド名(戻り値返却)
     *  @param  method7 呼び出し元のメソッド名(戻り値返却)
     *  @param  method8 呼び出し元のメソッド名(戻り値返却)
     *  @param  method9 呼び出し元のメソッド名(戻り値返却)
     *  @param  method10 呼び出し元のメソッド名(戻り値返却)
     *  @param  method11 呼び出し元のメソッド名(戻り値返却)
     *  @param  method12 呼び出し元のメソッド名(戻り値返却)
     *  @return boolearn      成功:1  失敗(Device does not support Bluetooth)：0
     */
    public static boolean Initialize(String gameObject, String method1,String method2,String method3,String method4,String method5,String method6,String method7,String method8,String method9,String method10,String method11,String method12) {

        boolean rtn = _BleService.Initialize(gameObject, method1,method2,method3,method4,method5,method6,method7,method8,method9,method10,method11,method12);

        DebugLog.d(TAG, "Initialize:"+String.valueOf(rtn));
        return rtn;
    }

    /**
     *  Bluetooth終了処理（アプリ正常終了）
     *  @return void
     */
    public static void Deinitialize() {

        DebugLog.d(TAG, "DeInitialize");

        doUnbindService(); //接続解除

        _BleService.Deinitialize(); //Bluetoothリセット

        _BleService.stopForeground(); //フォアグラウンドサービス状態を停止

        _BleService.CancelNotification();   //通知削除 ※削除する通知の種類としては、アラーム通知を削除

        BleService.stop(MyApplication.getInstance()); //接続開始解除
    }

    /**
     *  BLEサポートチェック
     *  @return boolearn        対応:TRUE  非対応：FALSE
     */
    public static boolean BlesupportCheck() {

        boolean rtn = _BleService.BlesupportCheck();
        DebugLog.d(TAG, "BlesupportCheck:"+String.valueOf(rtn));
        return rtn;
    }

    /**
     *  Bluetooth有効/無効チェック
     *  @return boolearn        有効:TRUE  無効：FALSE
     */
    public static boolean BluetoothValidCheck() {

        boolean rtn = _BleService.BluetoothValidCheck();
        DebugLog.d(TAG, "BluetoothValidCheck:"+String.valueOf(rtn));

        return rtn;
    }

    /**
     * BLEデバイスをスキャンします
     *  @return void
     */
    public static void ScanBleDevice(){
        _BleService.ScanBleDevice();
    }

    /**
     * BLEデバイスのスキャンを停止します
     *  @return void
     */
    public static void StopScanning(){
        //Android7対応で30秒以内にスキャン開始と停止を5回行えないので、コールしても無視する
        //_BleService.StopScanning();
    }

    /**
     * BLEデバイスに接続します
     *  @param  address        デバイスアドレス
     */
    public static void Connect(String address){
        _BleService.Connect(address);
    }

    /**
     * BLEデバイスを切断します
     */
    public static void Disconnect(){
        _BleService.Disconnect();
    }

    /**
     * CSVヘッダ情報設定
     */
    public static void CsvHeaderSet(String str1, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9){
        CsvWriter.InitialDataset(str1, str2, str3, str4, str5, str6, str7, str8, str9);
    }

    /**
     * BLEコマンド送信
     */
    public static void sendBleCommand(int commandType, byte[] command) {
        _BleService.sendBleCommand(commandType, command);
    }

    /**
     * ファーム更新用サービスUUIDに変更する
     */
    public static void changeServiceUUIDToFirmwareUpdate() {
        _BleService.changeServiceUUIDToFirmwareUpdate();
    }

    /**
     * ファーム更新制御コマンド用キャラクタリスティックUUIDに変更する
     */
    public static void changeCharacteristicUUIDToFirmwareUpdateControl() {
        _BleService.changeCharacteristicUUIDToFirmwareUpdateControl();
    }

    /**
     * ファーム更新制御コマンド用キャラクタリスティックUUIDに変更する
     */
    public static void changeCharacteristicUUIDToFirmwareUpdateData() {
        _BleService.changeCharacteristicUUIDToFirmwareUpdateData();
    }

    /**
     * 汎用通信用サービスUUIDに変更する
     */
    public static void changeServiceUUIDToNormal() {
        _BleService.changeServiceUUIDToNormal();
    }

    /**
     * コマンド：日時設定を行う
     */
    public static void SendCommandDate(String date) {

        _BleService.SendCommandDate(date);
    }

    /**
     * コマンド：アラーム設定を行う
     */
    public static void SendCommandAlarm(int data1, int data2, int data3, int data4,int data5, int data6,int data7) {

        _BleService.SendCommandAlarm(data1,data2,data3,data4,data5,data6,data7);
    }

    /**
     * コマンド：データ取得完了通知を行う
     */
    public static void SendCommandGetFinish(boolean result) {

        _BleService.SendCommandGetFinish(result);
    }

    /**
     * コマンド：プログラム転送結果(H1D)を行う
     */
    public static void SendCommandH1dSum(byte[] sum) {

        _BleService.SendCommandH1dSum(sum);
    }

    /**
     * コマンド：プログラム転送(H1D)を行う
     */
    public static void SendCommandH1dCode(byte[] code , int len) {

        _BleService.SendCommandH1dCode(code , len);
    }

    /**
     * コマンド送信を行う
     */
    public static void SendCommandId(int id) {

        _BleService.SendCommandId(id);
    }

    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     *  通知許可チェック
     *　 @return code       返却値(0:許可なし 1:許可)　※Oreo以降は、アラーム通知の許可を返却
     */
    public static boolean NotificationCheck () {

        DebugLog.d(TAG,"通知："+String.valueOf(isNotificationChannelEnabled(MyApplication.getInstance(),NotificationHelper.CHANNEL_GENERAL_ID2)));

        return (MyApplication.isNotificationChannelEnabled(MyApplication.getInstance(),NotificationHelper.CHANNEL_GENERAL_ID2));
    }

    public static boolean isNotificationChannelEnabled(Context context, @Nullable String channelId){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //Oreo以降はチャンネルがあるため別判定

            if(!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false; //先に親の通知をチェック

            if(!TextUtils.isEmpty(channelId)) { //チャネル毎の通知をチェック（親が無効でもチャネル設定が有効であればTRUEが返却される
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = manager.getNotificationChannel(channelId);
                return channel.getImportance() != NotificationManager.IMPORTANCE_NONE; //通知無効以外でチェック。デフォルトの設定と変わっている可能性はある。
            }
            return false;
        } else {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }

    /**
     *  再生中のアラームを停止（再生中でなくてもアラーム画面表示時、通知削除時はコールすること）
     */
    public static void AlarmStop() {

        DebugLog.d(TAG, "AlarmStop");

        _BleService.VibrationStop(); //バイブレーション停止
        _BleService.StopAlarm(); //アラーム停止
        _BleService.CancelNotification(); //通知削除 ※削除する通知の種類としては、アラーム通知を削除
    }


    //////////////////////////////////////////////////////////////////////////////////////////
}
