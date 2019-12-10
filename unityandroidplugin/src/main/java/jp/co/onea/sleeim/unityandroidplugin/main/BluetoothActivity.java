package jp.co.onea.sleeim.unityandroidplugin.main;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;

import static com.unity3d.player.UnityPlayer.UnitySendMessage;
//https://answers.unity.com/questions/511252/unity-3d-java-plugin-issues.html

public class BluetoothActivity extends Activity {
    private final static String TAG = BluetoothActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 256;

    private static String mGameObject = "";
    private static String mMethod = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugLog.d(TAG, "onCreate");
        
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    /**
     *  Bluetooth有効化リクエスト発行
     *  @param  gameObject    呼び出し元のgameObject名
     *  @param  method        呼び出し元のメソッド名
     *  @return code          返却値(0:許可なし 1:許可)
     */
    public static void BluetoothRequest(String gameObject, String method) {

        //呼び出し元情報を取得。
        mGameObject = gameObject;
        mMethod     = method;

        //ここで既にBT許可済ならチェック対象外しないと画面が一瞬システムに遷移してしまう
        if(MyApplication.BluetoothValidCheck()){
            DebugLog.d(TAG, "BTリクエスト:既に許可済み。");

            UnitySendMessage(mGameObject, mMethod, String.valueOf( true )); //Unityに返却
            return;
        }

        Intent intent = new Intent();
        intent.setAction("androidnativeactions.Bluetooth");
        UnityPlayer.currentActivity.startActivityForResult(intent, 999);

        DebugLog.d(TAG, "bluetoothRequest");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        DebugLog.d(TAG, "onActivityResult:requestCode " + requestCode + ", resultCode: " + resultCode);

        super.onActivityResult(requestCode, resultCode, data);

        String str= String.valueOf( false );

        if(resultCode != RESULT_OK) { //BluetoothがOFFにされた場合の処理

            DebugLog.d(TAG, "Bluetooth:OFF");
            setResult(resultCode);
            finish();
            str = String.valueOf( false );

        }else{

            switch (requestCode) {
                case REQUEST_ENABLE_BT: //BluetoothがONにされた場合の処理
                    DebugLog.d(TAG, "Bluetooth:ON");
                    str = String.valueOf( true );

                    break;

                    default:
                        DebugLog.d(TAG, "Bluetooth:不明なコード");
                        str = String.valueOf( false );

                        break;
            }

            setResult(resultCode);
            finish();
        }

        UnitySendMessage(mGameObject, mMethod, str); //Unityに返却
    }

    @Override
    public void onResume(){
        super.onResume();
        DebugLog.v(TAG, "onResume");
    }

    @Override
    public void onPause(){
        super.onPause();
        DebugLog.v(TAG, "onPause");
    }

    @Override
    public void onRestart(){
        super.onRestart();
        DebugLog.v(TAG, "onRestart");
    }

    @Override
    public void onStop(){
        super.onStop();
        DebugLog.v(TAG, "onStop");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        DebugLog.v(TAG, "onDestroy");
    }
}
