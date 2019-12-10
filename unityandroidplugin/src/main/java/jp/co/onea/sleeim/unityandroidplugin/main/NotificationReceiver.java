package jp.co.onea.sleeim.unityandroidplugin.main;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * 通知操作
 */
public class NotificationReceiver extends BroadcastReceiver {

    private final static String TAG = NotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        DebugLog.d(TAG, "onReceive:");

        String action = intent.getAction();

        switch (action) {
            case NotificationHelper.CLICK_NOTIFICATION:
                //通知タップ時のイベントを書く
                DebugLog.d(TAG, NotificationHelper.CLICK_NOTIFICATION);
                break;

            case NotificationHelper.DELETE_NOTIFICATION:
                //通知削除時のイベントを書く
                DebugLog.d(TAG, NotificationHelper.DELETE_NOTIFICATION);

                MyApplication.AlarmStop(); //アラーム処理停止
                break;

            default:
                break;
        }
    }
}