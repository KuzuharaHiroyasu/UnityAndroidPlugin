package jp.co.onea.sleeim.unityandroidplugin.data;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import jp.co.onea.sleeim.unityandroidplugin.main.MyApplication;
import jp.co.onea.sleeim.unityandroidplugin.utils.DataUtil;
import jp.co.onea.sleeim.unityandroidplugin.utils.DebugLog;

//睡眠データ保存クラス
public class CsvWriter {

    private final static String TAG = CsvWriter.class.getSimpleName();

    //フォーマット
    private final DateFormat dateStdFormat = new SimpleDateFormat("yyyy/M/d"); //CSV内日付用
    private final DateFormat dateTimeFormat = new SimpleDateFormat("HH:mm:ss"); //CSV内時刻用

    //文字コード
    private static final String ENCODING = "UTF-8";
    //挿入文字タイプ
    private static final String NEWLINE = "\r\n";   //改行
    private static final String COMMMA = ",";       //CSVカンマ

    private File _file; //操作ファイル

    private static DataSet dataSet = new DataSet();

    /**
     * CSVヘッダ用データセット
     */
    public static void InitialDataset(String str1, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9) {
        dataSet._deviceID = str1; //デバイスID
        dataSet._nickName = str2; //ニックネーム
        dataSet._sex = str3; //性別
        dataSet._birthday = str4; //生年月日
        dataSet._height = str5; //身長
        dataSet._weight = str6; //体重
        dataSet._startTime = str7; //睡眠時間：開始
        dataSet._endTime = str8; //睡眠時間：終了
        dataSet._fmg1dver = str9; //G1Dファームウェアバージョン
        DebugLog.d(TAG, dataSet._deviceID + "," + dataSet._nickName + "," + dataSet._sex + "," + dataSet._birthday + "," + dataSet._height + "," + dataSet._weight + "," + dataSet._startTime + "," + dataSet._endTime + "," + dataSet._fmg1dver);
    }

    /**
     * デバイスID取得
     */
    public static String GetDeviceID() {
        return dataSet._deviceID;
    }

    /**
     * CSVファイル作成
     */
    public File CsvFileCreate(String fileName) {

        DebugLog.d(TAG, "ファイル名" + fileName);

        //アプリ内部のデータ領域ディレクトリを取得
        Context context = MyApplication.getInstance().getApplicationContext();
        String externalDirectory = context.getExternalFilesDir(null).getPath(); ///storage/emulated/0/Android/data/<アプリのID>/files
        _file = new File(externalDirectory, fileName);
        //_file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/sleeim_debug/", fileName); //デバッグ用ディレクトリ

        File _parentDir = _file.getParentFile(); //親ディレクトリを取得

        //ディレクトリがなければ作成しておく
        if (!_parentDir.exists()) {
            if (!(_parentDir.mkdirs())) {
            } else {
                DebugLog.d(TAG, "ディレクトリを作成しました。");
            }
        }

        //既にファイルが存在していれば削除しておく
        if (_file.isFile()) {
            _file.delete();
            DebugLog.d(TAG, "削除ファイル：" + _file.getName());
        }

        DebugLog.d(TAG, "ファイルパス：" + _file.getPath());


        return _file;
    }

    /**
     * ヘッダデータセット
     * （あらかじめヘッダ用データをセットしておく）
     */
    public void CsvWriteHeader() throws FileNotFoundException, IOException {

        try (FileOutputStream fos = new FileOutputStream(_file, true); //append：末尾に追加
             OutputStreamWriter osw = new OutputStreamWriter(fos, ENCODING);
             BufferedWriter bw = new BufferedWriter(osw)) {

            //BOM付き設定を最初に行う
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            //ヘッダ：1行目（プロフィール情報）
            bw.write(dataSet._nickName);
            bw.write(COMMMA);
            bw.write(dataSet._sex);
            bw.write(COMMMA);
            bw.write(dataSet._birthday);
            bw.write(COMMMA);
            bw.write(dataSet._height);
            bw.write(COMMMA);
            bw.write(dataSet._weight);
            bw.write(COMMMA);
            bw.write(dataSet._startTime);
            bw.write(COMMMA);
            bw.write(dataSet._endTime);
            bw.newLine();

            //ヘッダ：2行目（ファームウェアVer情報(G1D/H1D)）
            bw.write(dataSet._fmg1dver);
            bw.newLine();

            bw.flush(); //フラッシュ書込
            //DebugLog.d(TAG, "WriteHeader:Successed");

        } catch (FileNotFoundException e) {
            DebugLog.d(TAG, "WriteHeader:FileNotFoundException");
        } catch (IOException e) {
            DebugLog.d(TAG, "WriteHeader:IOException");
        }
    }

    /**
     * 睡眠記録開始時間セット
     */
    public void CsvWriteDate(GregorianCalendar clnder, byte[] rData) throws FileNotFoundException, IOException {

        final int SNORE_DETECTION_COUNT_INDEX_1 = 8;
        final int SNORE_DETECTION_COUNT_INDEX_2 = 9;
        final int APNEA_DETECTION_COUNT_INDEX_1 = 10;
        final int APNEA_DETECTION_COUNT_INDEX_2 = 11;
        final int SNORE_TIME_INDEX_1 = 12;
        final int SNORE_TIME_INDEX_2 = 13;
        final int APNEA_TIME_INDEX_1 = 14;
        final int APNEA_TIME_INDEX_2 = 15;
        final int APNEA_MAX_TIME_INDEX_1 = 16;
        final int APNEA_MAX_TIME_INDEX_2 = 17;

        try (FileOutputStream fos = new FileOutputStream(_file, true); //append：末尾に追加
             OutputStreamWriter osw = new OutputStreamWriter(fos, ENCODING);
             BufferedWriter bw = new BufferedWriter(osw)) {

            //日時用 Date取得
            Date _date = clnder.getTime();

            //年月日
            String _day = dateStdFormat.format(_date);
            bw.write(_day);
            bw.write(COMMMA);

            //曜日
            int _week = clnder.get(Calendar.DAY_OF_WEEK) - 1;
            bw.write(String.valueOf(_week));
            bw.write(COMMMA);

            //時分秒
            String _time = dateTimeFormat.format(_date);
            bw.write(_time);
            bw.write(COMMMA);

            // いびき検知数書き込み
            bw.write(Integer.toString(DataUtil.CovertToInt32(
                rData[SNORE_DETECTION_COUNT_INDEX_1],
                rData[SNORE_DETECTION_COUNT_INDEX_2],
                (byte)0,
                (byte)0)));
            bw.write(COMMMA);
            // 無呼吸検知数書き込み
            bw.write(Integer.toString(DataUtil.CovertToInt32(
                rData[APNEA_DETECTION_COUNT_INDEX_1],
                rData[APNEA_DETECTION_COUNT_INDEX_2],
                (byte)0,
                (byte)0)));
            bw.write(COMMMA);
            // いびき時間書き込み
            bw.write(Integer.toString(DataUtil.CovertToInt32(
                rData[SNORE_TIME_INDEX_1],
                rData[SNORE_TIME_INDEX_2],
                (byte)0,
                (byte)0)));
            bw.write(COMMMA);
            // 無呼吸時間書き込み
            bw.write(Integer.toString(DataUtil.CovertToInt32(
                rData[APNEA_TIME_INDEX_1],
                rData[APNEA_TIME_INDEX_2],
                (byte)0,
                (byte)0)));
            bw.write(COMMMA);
            //最高無呼吸時間(2byte:LE対応)
            bw.write(Integer.toString(DataUtil.CovertToInt32(
                rData[APNEA_MAX_TIME_INDEX_1],
                rData[APNEA_MAX_TIME_INDEX_2],
                (byte)0,
                (byte)0)));
            bw.newLine();

            bw.flush(); //フラッシュ書込
            //DebugLog.d(TAG, "CsvWriteDate:Successed");

        } catch (FileNotFoundException e) {
            DebugLog.d(TAG, "CsvWriteDate:FileNotFoundException");
        } catch (IOException e) {
            DebugLog.d(TAG, "CsvWriteDate:IOException");
        }
    }

    /**
     * 機器データコマンド受信データを解析し、睡眠データを書き込む
     */
    public void CsvWriteData(GregorianCalendar clnder, byte[] rData) throws FileNotFoundException, IOException {

        try (FileOutputStream fos = new FileOutputStream(_file, true); //append：末尾に追加
             OutputStreamWriter osw = new OutputStreamWriter(fos, ENCODING);
             BufferedWriter bw = new BufferedWriter(osw)) {

            // 機器データコマンドのインデックス
            // NOTE:睡眠データ(csvファイル)書き込み順と異なるため注意
            // いびきの大きさ
            final int SNORE_1 = 1;
            final int SNORE_2 = 2;
            final int SNORE_3 = 3;
            // 状態
            final int STATE = 4;
            // 首の向き
            final int NECK_DIRECTION = 5;
            // フォトセンサー
            final int PHOTO_SENSOR_1 = 6;
            final int PHOTO_SENSOR_2 = 7;
            final int PHOTO_SENSOR_3 = 8;

            //日時用 Date取得
            Date _date = clnder.getTime();

            //年月日
            String _day = dateStdFormat.format(_date);
            bw.write(_day);
            bw.write(COMMMA);
            //曜日
            int _week = clnder.get(Calendar.DAY_OF_WEEK) - 1;
            bw.write(String.valueOf(_week));
            bw.write(COMMMA);
            //時分秒
            String _time = dateTimeFormat.format(_date);
            bw.write(_time);
            bw.write(COMMMA);
            // 呼吸状態1
            int msb = 1;
            int lsb = 0;
            bw.write(Integer.toString(
                DataUtil.extractBitInByteToInt(
                    rData[STATE], msb, lsb)));
            bw.write(COMMMA);
            // 呼吸状態2
            msb = 3;
            lsb = 2;
            bw.write(Integer.toString(
                DataUtil.extractBitInByteToInt(
                    rData[STATE], msb, lsb)));
            bw.write(COMMMA);
            // 呼吸状態3
            msb = 5;
            lsb = 4;
            bw.write(Integer.toString(
                DataUtil.extractBitInByteToInt(
                    rData[STATE], msb, lsb)));
            bw.write(COMMMA);
            // 睡眠ステージ
            msb = 7;
            lsb = 6;
            bw.write(Integer.toString(
                DataUtil.extractBitInByteToInt(
                    rData[STATE], msb, lsb)));
            bw.write(COMMMA);
            // いびきの大きさ1 ~ 3
            // Javaのbyte型は符号付きのため、符号なしの値として解釈するために
            // 一旦int型に変換する必要がある
            int snoreVol1 = (int)(rData[SNORE_1]) & 0x00FF;
            int snoreVol2 = (int)(rData[SNORE_2]) & 0x00FF;
            int snoreVol3 = (int)(rData[SNORE_3]) & 0x00FF;
            // 送られてきた値を2ビット左シフトする
            snoreVol1 = snoreVol1 << 2;
            snoreVol2 = snoreVol2 << 2;
            snoreVol3 = snoreVol3 << 2;
            bw.write(Integer.toString(snoreVol1));
            bw.write(COMMMA);
            bw.write(Integer.toString(snoreVol2));
            bw.write(COMMMA);
            bw.write(Integer.toString(snoreVol3));
            bw.write(COMMMA);
            // 首の向き1
            msb = 1;
            lsb = 0;
            bw.write(Integer.toString(
                DataUtil.extractBitInByteToInt(
                    rData[NECK_DIRECTION], msb, lsb)));
            bw.write(COMMMA);
            // 首の向き2
            msb = 3;
            lsb = 2;
            bw.write(Integer.toString(
                DataUtil.extractBitInByteToInt(
                    rData[NECK_DIRECTION], msb, lsb)));
            bw.write(COMMMA);
            // 首の向き3
            msb = 5;
            lsb = 4;
            bw.write(Integer.toString(
                DataUtil.extractBitInByteToInt(
                    rData[NECK_DIRECTION], msb, lsb)));
            bw.write(COMMMA);
            // フォトセンサー1 ~ 3
            // Javaのbyte型は符号付きのため、符号なしの値として解釈するために
            // 一旦int型に変換する必要がある
            int photoSensor1 = (int)(rData[PHOTO_SENSOR_1]) & 0x00FF;
            int photoSensor2 = (int)(rData[PHOTO_SENSOR_2]) & 0x00FF;
            int photoSensor3 = (int)(rData[PHOTO_SENSOR_3]) & 0x00FF;
            // 2bit左シフトする
            photoSensor1 = photoSensor1 << 2;
            photoSensor2 = photoSensor2 << 2;
            photoSensor3 = photoSensor3 << 2;
            bw.write(Integer.toString(photoSensor1));
            bw.write(COMMMA);
            bw.write(Integer.toString(photoSensor2));
            bw.write(COMMMA);
            bw.write(Integer.toString(photoSensor3));
            bw.newLine();

            bw.flush(); //フラッシュ書込
            //DebugLog.d(TAG, "CsvWriteData:Successed");

        } catch (FileNotFoundException e) {
            DebugLog.d(TAG, "CsvWriteData:FileNotFoundException");
        } catch (IOException e) {
            DebugLog.d(TAG, "CsvWriteData:IOException");
        }
    }

    /**
     * 睡眠記録開始時間にデータ長(受信回数)を書込
     * 全てのファイル/データを書き込み後、修正内容を反映し再度全内容の書き込みをし直す
     */
    public void CsvModifyDate(int lineNumber, String rcvCount, String fileName) throws FileNotFoundException, IOException {

        Context context = MyApplication.getInstance().getApplicationContext();
        String externalDirectory = context.getExternalFilesDir(null).getPath(); ///storage/emulated/0/Android/data/<アプリのID>/files
        _file = new File(externalDirectory, fileName);
        //File _file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/sleeim_debug/", fileName); //デバッグ用ディレクトリ

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(_file), ENCODING))) {

            String _str; //行読込
            List<String> _lines = new ArrayList<>(); //退避用

            while ((_str = br.readLine()) != null) { //全ライン読込
                _lines.add(_str);
            }

            String _tmp = _lines.get(lineNumber); //編集前のラインを読込
            String _mline = _tmp + "," + rcvCount;
            _lines.set(lineNumber, _mline); //データ長を追加してセット

            try (FileOutputStream fos = new FileOutputStream(_file, false); //append：ファイル内容を削除して書込
                 OutputStreamWriter osw = new OutputStreamWriter(fos, ENCODING);
                 PrintWriter pw = new PrintWriter(new BufferedWriter(osw))) {

                for (String line : _lines) { //全ライン読込
                    pw.println(line); //行セット

                    pw.flush(); //フラッシュ書込
                }
                pw.close(); // フラッシュ終了

            } catch (FileNotFoundException e) {
                DebugLog.d(TAG, "CsvModifyDate:FileNotFoundException");
            } catch (IOException e) {
                DebugLog.d(TAG, "CsvModifyDate:IOException");
            }
        } catch (FileNotFoundException e) {
            DebugLog.d(TAG, "CsvModifyDate(BR):FileNotFoundException");
        } catch (IOException e) {
            DebugLog.d(TAG, "CsvModifyDate(BR):IOException");
        }

        DebugLog.d(TAG, "CsvModifyDate:Successed");
    }
}
