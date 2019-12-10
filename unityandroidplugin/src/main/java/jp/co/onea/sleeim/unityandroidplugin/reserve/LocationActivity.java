/**
 * 位置情報のパーミッション
 * 不使用（作成途中のため未完）
 * Created by kitagawa on 2018/08/09.
 */

//package jp.co.onea.sleeim.unityandroidplugin.main;
//
//import android.Manifest;
//import android.app.Activity;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.util.Log;
//
//import com.unity3d.player.UnityPlayer;
//import com.unity3d.player.UnityPlayerActivity;
//
//import static com.unity3d.player.UnityPlayer.UnitySendMessage;
////https://answers.unity.com/questions/511252/unity-3d-java-plugin-issues.html
//
//public class LocationActivity extends UnityPlayerActivity {
//
//    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
//
//    private static String mGameObject = "";
//    private static String mMethod = "";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        DebugLog.d("LocationActivity", "onCreate開始");
//    }
//
//    /**
//     *  システム設定で Location を有効化するようにリクエストを発行
//     *  @param  gameObject    呼び出し元のgameObject名
//     *  @param  method        呼び出し元のメソッド名
//     *  @return void
//     */
//    public static void LocationRequest(String gameObject, String method) {
//        DebugLog.d("LocationActivity", "LocationRequest");
//
//        Activity activity = UnityPlayer.currentActivity;
//
//        //呼び出し元情報を取得。
//        mGameObject = gameObject;
//        mMethod     = method;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//            if (ContextCompat.checkSelfPermission(activity,
//                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
//                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
//                    //// 一度拒否されないとココの処理は通らないため、初回確認時に説明を表示できない
//                    // 拒否された and 「今後は確認しない」チェック無し
//                    //   or
//                    // 一度は許可したが設定アプリから拒否された
//                    DebugLog.d("LocationActivity", "shouldShowRequestPermissionRationale:true");
//
//                } else {
//                    DebugLog.d("LocationActivity", "shouldShowRequestPermissionRationale:false");
//
//                    // まだ許可されない（初回確認）
//                    //   or
//                    // 拒否された and 「今後は確認しない」チェックあり
//                }
//                // パーミッションの許可要求ダイアログを表示
//                // ただし、「今後は確認しない」にチェックがある場合は、ダイアログが表示されずに onRequestPermissionsResult が呼び出される
//                ActivityCompat.requestPermissions(activity,
//                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
//                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
//            } else {
//                DebugLog.d("LocationActivity", "checkSelfPermission:true");
//
//                //既にパーミッション取得済
//                UnitySendMessage(mGameObject, mMethod, "true");
//            }
//        }else{
//            //Android6.0以前はなし
//            DebugLog.d("LocationActivity", "Android6.0:down");
//
//            UnitySendMessage(mGameObject, mMethod, "true");
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        DebugLog.d("LocationActivity", "onRequestPermissionsResult");
//
//        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // 確認ダイアログの結果、許可された
//                DebugLog.d("LocationActivity", "許可");
//
//                UnitySendMessage(mGameObject, mMethod, "true");
//            } else {
//                DebugLog.d("LocationActivity", "拒否");
//
//                // 確認ダイアログの結果、拒否された
//                //   or
//                // 拒否されて「今後は確認しない」にチェックされている
//
//                UnitySendMessage(mGameObject, mMethod, "false");
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//}
