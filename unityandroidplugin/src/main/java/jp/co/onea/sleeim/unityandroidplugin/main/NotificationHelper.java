package jp.co.onea.sleeim.unityandroidplugin.main;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import jp.co.onea.sleeim.unityandroidplugin.R;
import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;

//通知内容
public class NotificationHelper extends ContextWrapper {

    private NotificationManager manager;

    //チャンネルID
    private static final String CHANNEL_GENERAL_ID = "correspond";
    public static final String CHANNEL_GENERAL_ID2 = "alarm";

    //アクション
    public static final String CLICK_NOTIFICATION = "click_notification";
    public static final String DELETE_NOTIFICATION = "delete_notification";

    private static Notification.Builder cr_builder; //通信用通知
    private static Notification.Builder al_builder2; //アラーム用通知
    private static NotificationReceiver receiver; //Oreo以降用レシーバー

    private final static String TAG = NotificationHelper.class.getSimpleName();

    public NotificationHelper(Context base) {
        super(base);

        DebugLog.d(TAG, "NotificationHelper:");

        //Oreo以降は設定
        if (isOreoOrLater()) {
            // 通知チャンネルのIDにする任意の文字列
            // 通知チャンネル名
            // デフォルトの重要度
            //https://feel-log.net/android/o_notification_no_sound_vibration/

            NotificationChannel channel = new NotificationChannel(CHANNEL_GENERAL_ID, "サービス", NotificationManager.IMPORTANCE_MIN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // 通知時にライトを有効にする
            //channel.enableLights(true);
            // 通知時のライトの色
            //channel.setLightColor(Color.WHITE);
            //channel.setDescription("通知チャンネルの説明"); // 必須ではない
            getManager().createNotificationChannel(channel);


            NotificationChannel channel2 = new NotificationChannel(CHANNEL_GENERAL_ID2, "アラーム通知", NotificationManager.IMPORTANCE_HIGH);
            channel2.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // 通知時にライトを有効にする
            //channel2.enableLights(true);
            // 通知時のライトの色
            //channel2.setLightColor(Color.WHITE);
            //channel2.setDescription("通知チャンネルの説明"); // 必須ではない
            channel2.enableVibration(false); //デフォルトをバイブレーションなしに設定
            channel2.setSound(null, null); //デフォルトを音声なしに設定
            getManager().createNotificationChannel(channel2);
        }

        if (isOreoOrLater()) { //Oreo以降用レシーバー設定
            receiver = new NotificationReceiver();
        }

        cr_builder = isOreoOrLater()
                ? new Notification.Builder(this, CHANNEL_GENERAL_ID)
                : new Notification.Builder(this);

        al_builder2 = isOreoOrLater()
                ? new Notification.Builder(this, CHANNEL_GENERAL_ID2)
                : new Notification.Builder(this);
    }

    public Notification.Builder getNotification() {

        DebugLog.d(TAG, "getNotification:");

        // プロセスがあればそのまま復帰、なければ起動画面から開始する
        Intent ni = new Intent(Intent.ACTION_MAIN);
        ni.addCategory(Intent.CATEGORY_LAUNCHER);
        ni.setClassName(getApplicationContext().getPackageName(), MainActivity.class.getName());
        ni.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(MyApplication.getInstance(), 456, ni, PendingIntent.FLAG_CANCEL_CURRENT);

        return cr_builder.setContentTitle(getString(R.string.app_name)) //タイトル：アプリ名取得

                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_push_notice)

                //.setAutoCancel(true) //サービス起動で通知する場合は設定しても無効
                .setShowWhen(false) //時間表示
                //.setWhen(System.currentTimeMillis())

                .setFullScreenIntent(null, true)
                .setContentIntent( //通知タップ時のPendingIntent
                        pi
                        //getPendingIntentWithBroadcast(NotificationHelper.CLICK_NOTIFICATION)
                );
        //.setDeleteIntent(  //通知の削除時のPendingIntent
        //       getPendingIntentWithBroadcast(NotificationHelper.DELETE_NOTIFICATION)
        //);
    }

    //アラーム用の通知を設定
    public void alarmNotificationSet(String title, String text) {

        DebugLog.d(TAG, "alarmNotificationSet:");

        // プロセスがあればそのまま復帰、なければ起動画面から開始する
        Intent ni = new Intent(Intent.ACTION_MAIN);
        ni.addCategory(Intent.CATEGORY_LAUNCHER);
        ni.setClassName(getApplicationContext().getPackageName(), MainActivity.class.getName());
        ni.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(MyApplication.getInstance(), 789, ni, PendingIntent.FLAG_CANCEL_CURRENT);

        al_builder2.setContentTitle(title) //タイトル

                .setContentText(text)
                .setSmallIcon(R.drawable.ic_push_alerm)

                .setAutoCancel(false) //サービス起動で通知する場合は設定しても無効
                .setShowWhen(true) //時間表示
                .setWhen(System.currentTimeMillis())
                .setOngoing(false) //通知を通知バーから削除できるようにする

                .setFullScreenIntent(
                        PendingIntent.getActivity(MyApplication.getInstance(), 0, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT),
                        true)
                .setContentIntent( //通知タップ時のPendingIntent
                        pi
                        //getPendingIntentWithBroadcast(NotificationHelper.CLICK_NOTIFICATION)
                )
                .setDeleteIntent(  //通知の削除時のPendingIntent
                        getPendingIntentWithBroadcast(NotificationHelper.DELETE_NOTIFICATION)
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            al_builder2.setChannelId(CHANNEL_GENERAL_ID2);
        }
        getManager().notify(BleService.SERVICE_RUNNING_NOTIFICATION_ID2, al_builder2.build());
    }

    //特定の通知を削除
    public void cancelNotification() {
        getManager().cancel(BleService.SERVICE_RUNNING_NOTIFICATION_ID2); // Notification ID to cancel
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    private boolean isOreoOrLater() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O;
    }

    private PendingIntent getPendingIntentWithBroadcast(String action) {

        if (isOreoOrLater()) { //Oreo以降はAndroidmanifestの設定と関連して必要
            IntentFilter filter0 = new IntentFilter();
            filter0.addAction(action);
            registerReceiver(receiver, filter0);
        }

        return PendingIntent.getBroadcast(getApplicationContext(), 111, new Intent(action), 0);
    }
}