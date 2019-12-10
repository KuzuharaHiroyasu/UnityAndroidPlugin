package jp.co.onea.sleeim.unityandroidplugin.main;

import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;

public class MainActivity extends UnityPlayerActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setVolumeControlStream(AudioManager.STREAM_ALARM);

        super.onCreate(savedInstanceState);
        DebugLog.d(TAG, "onCreate");
        setVolumeControlStream(AudioManager.STREAM_ALARM);
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        //setContentView(R.layout.activity_main);
    }

    /**
     *  パーミッションチェック処理(実装方法はiOSに合わせた。)
     *  @param  gameObject    呼び出し元のgameObject名
     *  @param  method        呼び出し元のメソッド名(戻り値返却)
     *  @return code          返却値(0より大きい値:許可なし 0:許可)
     */
    public static void PermissionCheck(String gameObject, String method) {

        //返却値を宣言
        int retCode = 0;
        try{
            PermissionManager pm = new PermissionManager();

            //Boolean bt = pm.hasPermission(android.Manifest.permission.BLUETOOTH);
            Boolean location = pm.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            Boolean rxsadmin = pm.hasPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);

//            Context context = MyApplication.getInstance();
//            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//                //BLE Not Supported
//                //GooglePlayでの公開以外にアプリ内でチェックするため不要
//            }

            DebugLog.d("permissionCheck", "判断開始");
//            DebugLog.d("permissionCheck", "bt:"+bt);
//            DebugLog.d("permissionCheck", "btadmin:"+btadmin);
            DebugLog.d("permissionCheck", "location:"+location);
//            if (!bt) {
//                retCode += 1;
//                DebugLog.d("permissionCheck", "Bluetooth:許可なし");
//            }
            if (!rxsadmin) {
                retCode += 2;
                DebugLog.d("permissionCheck", "READ_EXTERNAL_STORAGE:許可なし");
            }
            if (!location) {
                retCode += 4;
                DebugLog.d("permissionCheck", "ACCESS_COARSE_LOCATION:許可なし");
            }
        } catch (Exception e) {
            DebugLog.d("permissionCheck", "Exception:"+e.getMessage());
            e.getStackTrace();
        }
        DebugLog.d("permissionCheck", "retCode:"+retCode);
        UnityPlayer.UnitySendMessage(gameObject, method, String.valueOf(retCode));
    }

    @Override
    public void onResume(){
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        super.onResume();
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        DebugLog.v(TAG, "onResume");
    }

    @Override
    public void onPause(){
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        super.onPause();
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        DebugLog.v(TAG, "onPause");
    }

    @Override
    public void onRestart(){
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        super.onRestart();
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        DebugLog.v(TAG, "onRestart");
    }

    @Override
    protected void onStart() {
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        super.onStart();
        //UnityPlayer.currentActivity.setVolumeControlStream(AudioManager.STREAM_ALARM);

        // Bind to LocalService
        MyApplication.doBindService();

        DebugLog.v(TAG, "onStart");
    }

    @Override
    public void onStop(){
        super.onStop();

        // Unbind from the service
        MyApplication.doUnbindService();

        DebugLog.v(TAG, "onStop");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        DebugLog.v(TAG, "onDestroy");
    }
}

