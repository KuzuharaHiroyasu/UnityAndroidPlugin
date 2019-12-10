package jp.co.onea.sleeim.unityandroidplugin.main;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;

public class MusicPlayer {

    private final static String TAG = MusicPlayer.class.getSimpleName();
    private static MediaPlayer player;

    /**
     *  指定した音楽リソースを再生
     */
    public static void MusicPlay(String path) {

        DebugLog.d(TAG, "MusicPlay/Path:"+path);

        player = new MediaPlayer();

        player.setLooping(true);
        //player.setVolume(1.0f,1.0f);

        Uri uri = Uri.parse(path);

        DebugLog.d(TAG, "MusicPlay/Uri:"+uri);

        try {
             player.setDataSource(MyApplication.getInstance(), uri); // 音声を設定
             player.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setLegacyStreamType(AudioManager.STREAM_ALARM) // アラームのボリュームで再生
                                .build());
            player.prepare();                                     // 音声を読み込み
            player.start(); // 再生
            DebugLog.d(TAG, "MusicPlay:Play");

         } catch (IllegalStateException e) {
            e.printStackTrace();
            DebugLog.e(TAG, "MusicPlay:"+e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            DebugLog.e(TAG, "MusicPlay:"+e.getMessage());
        }
    }

    /**
     *  再生中の音楽リソースを停止
     */
    public static void MusicStop() {

        if (player!= null) {
            player.stop();
            player.release();
            player= null;
        }else{
            DebugLog.d(TAG, "MusicStop:player=null");
        }
    }
}
