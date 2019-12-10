//package jp.co.onea.sleeim.unityandroidplugin.main;
//
//import android.os.Bundle;
//import android.support.v4.app.FragmentActivity;
//import android.util.Log;
//
//import jp.co.onea.sleeim.unityandroidplugin.reserve.dialogs.CommonDialogFragment;
//import jp.co.onea.sleeim.unityandroidplugin.utils.ExtractionKey;
//
//public class AlertDialogActivity extends FragmentActivity implements CommonDialogFragment.CommonDialogCallback{
//    private final static String TAG = AlertDialogActivity.class.getSimpleName();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//
//        DebugLog.d(TAG, "onCreate");
//
//        super.onCreate(savedInstanceState);
//
//        showCommonDialog("Error", "データを1つ選択してください。", true, false, true, ExtractionKey.SET_COMMON_DIALOG_TAG);
//
//        //AlertDialogFragment fragment = new AlertDialogFragment();
//        //fragment.show(getSupportFragmentManager(), "alert_dialog");
//    }
//
//    @Override
//    protected void onDestroy() {
//        DebugLog.d(TAG ,"onDestroy");
//
//        super.onDestroy();
//    }
//    @Override
//    public void onCommonDiaDebugLog.eftButtonClicked(CommonDialogFragment dialog) {
//
//        DebugLog.d(TAG ,"onCommonDiaDebugLog.eftButtonClicked");
//
//        AlarmPlayer.AlarmStop();
//
//        //diaDebugLog.dismiss();
//    }
//
//    @Override
//    public void onCommonDialogRightButtonClicked(CommonDialogFragment dialog) {
//        //diaDebugLog.dismiss();
//    }
//
//    @Override
//    public void onCommonDialogDismiss() {
//        //ダイアログ非表示時にナビゲーションバーが表示される問題の対策
//        //Utility.setFullScreen(getWindow());
//    }
//    /**
//     * 共通ダイアログ表示
//     * @param title         タイトル
//     * @param message       本文
//     * @param leftButton    左ボタン表示フラグ
//     * @param rightButton   右ボタン表示フラグ
//     * @param cancelable    ダイアログ外タッチや戻るボタンでダイアログを閉じれるかどうか
//     * @param tag            ダイアログタグ名
//     */
//    public void showCommonDialog(String title, String message, boolean leftButton, boolean rightButton, boolean cancelable, String tag) {
//        //ダイアログフラグメント生成
//        CommonDialogFragment commonDialog = new CommonDialogFragment();
//        //ダイアログキャンセル設定
//        commonDialog.setCancelable(cancelable);
//
//        //ダイアログ情報をセット
//        Bundle bundle = new Bundle();
//        bundle.putString(ExtractionKey.SET_COMMON_DIALOG_TITLE, title);
//        bundle.putString(ExtractionKey.SET_COMMON_DIALOG_MESSAGE, message);
//        bundle.putBoolean(ExtractionKey.SET_COMMON_DIALOG_LEFT_BUTTON, leftButton);
//        bundle.putBoolean(ExtractionKey.SET_COMMON_DIALOG_RIGHT_BUTTON, rightButton);
//        commonDialog.setArguments(bundle);
//
//        //ダイアログ表示
//        commonDialog.show(getSupportFragmentManager(), tag);
//    }
//
//    @Override
//    public void onResume(){
//        super.onResume();
//        DebugLog.v(TAG, "onResume");
//    }
//
//    @Override
//    public void onPause(){
//        super.onPause();
//        DebugLog.v(TAG, "onPause");
//    }
//
//    @Override
//    public void onRestart(){
//        super.onRestart();
//        DebugLog.v(TAG, "onRestart");
//    }
//
//    @Override
//    public void onStop(){
//        super.onStop();
//        DebugLog.v(TAG, "onStop");
//    }
//}