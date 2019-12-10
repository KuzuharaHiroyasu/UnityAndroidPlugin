package jp.co.onea.sleeim.unityandroidplugin.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;

import static com.unity3d.player.UnityPlayer.UnitySendMessage;
//https://answers.unity.com/questions/511252/unity-3d-java-plugin-issues.html

public class NotificationActivity extends Activity {
    private final static String TAG = NotificationActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_NF = 512;

    private static String mGameObject = "";
    private static String mMethod = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugLog.d(TAG, "onCreate");

        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //if (channel != null) {
            //    intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            //    intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
            //} else {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, MyApplication.getInstance().getPackageName());
            //}
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", MyApplication.getInstance().getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", MyApplication.getInstance().getPackageName());
            intent.putExtra("app_uid", MyApplication.getInstance().getApplicationInfo().uid);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) { //本アプリでは非対応のバージョンなので確認していない
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + MyApplication.getInstance().getPackageName()));
        } else { //本アプリでは非対応のバージョンなので確認していない
            return;
        }
        startActivityForResult(intent, REQUEST_ENABLE_NF);
    }

    /**
     * Notification設定画面リクエスト
     *
     * @param gameObject 呼び出し元のgameObject名
     * @param method     呼び出し元のメソッド名
     * @return code       返却値(0:許可なし 1:許可)　※Oreo以降は、アラーム通知の許可を返却
     */
    public static void NotificationRequest(String gameObject, String method) {

        //呼び出し元情報を取得。
        mGameObject = gameObject;
        mMethod = method;

        Intent intent = new Intent();
        intent.setAction("androidnativeactions.Notification");
        UnityPlayer.currentActivity.startActivityForResult(intent, 888);

        DebugLog.d(TAG, "NotificationRequest");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        DebugLog.d(TAG, "onActivityResult:requestCode " + requestCode + ", resultCode: " + resultCode);

        super.onActivityResult(requestCode, resultCode, data);

        String str = String.valueOf(MyApplication.isNotificationChannelEnabled(MyApplication.getInstance(), NotificationHelper.CHANNEL_GENERAL_ID2));

        DebugLog.d(TAG, "onActivityResult:result= " + str);

        finish();

        UnitySendMessage(mGameObject, mMethod, str); //Unityに返却
    }

    @Override
    public void onResume() {
        super.onResume();
        DebugLog.v(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        DebugLog.v(TAG, "onPause");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        DebugLog.v(TAG, "onRestart");
    }

    @Override
    public void onStop() {
        super.onStop();
        DebugLog.v(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DebugLog.v(TAG, "onDestroy");
    }
}
