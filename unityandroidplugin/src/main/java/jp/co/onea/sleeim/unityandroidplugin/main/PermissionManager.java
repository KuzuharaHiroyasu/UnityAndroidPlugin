package jp.co.onea.sleeim.unityandroidplugin.main;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;


//↓Java実装方法
//参考：UnityでRuntimePermissionを実装する
//      http://smartgames.hatenablog.com/entry/2016/03/22/232819
//↓Unityからの呼び出し方法
//参考：UnityでRuntimePermissionを利用するプラグイン
//      http://smartgames.hatenablog.com/entry/2016/03/25/220312
//git-hub     https://github.com/sanukin39/UniAndroidPermission
public class PermissionManager {
    //PermissionManager.requestPermissionをコールすることで任意のタイミングで
    // 権限許可ダイアログを立ち上げてユーザーの操作結果を受け取ることが可能。
//    public static void requestPermission(String permissionName) {
//        Activity activity = UnityPlayer.currentActivity;
//        if (!hasPermission(permissionName)) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                activity.requestPermissions(new String[]{permissionName}, 0);
//            }
//        }
//    }

    public static boolean hasPermission(String permissionName){
        Activity activity = UnityPlayer.currentActivity;
        DebugLog.d("permissionCheck", "hasPermission起動");
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            DebugLog.d("permissionCheck", "APIレベル23未満");
            return true;
        }
        DebugLog.d("permissionCheck", "permissionName:"+permissionName);
        Context context = activity.getApplicationContext();
        return  context.checkCallingOrSelfPermission(permissionName) == PackageManager.PERMISSION_GRANTED;
    }
}
