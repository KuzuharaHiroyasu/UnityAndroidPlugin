//package jp.co.onea.sleeim.unityandroidplugin.utils;
//
//import android.content.Context;
//import android.os.Environment;
//import android.util.Log;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//
//public class WriteLogThread extends Thread {
//
//    private Context context;
//    private final DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss"); //CSVフォルダ名用 //デバッグ用
//    ArrayList<String> commandLine = new ArrayList<String>();
//    private File _file; //操作ファイル
//    private static final String ENCODING = "UTF-8";
//    private  static String directory;
//
//
//    public WriteLogThread(Context context) {
//        this.context = context;
//        // コマンドの作成
//        commandLine.add( "logcat");
//        commandLine.add( "-v");
//        commandLine.add( "time");
//        commandLine.add( "-s");
//        commandLine.add( "BleService:V");
//
//        Date date = new Date();
//        final String  directory = dateTimeFormat.format(date)+"_log.txt"; //パス名
//
//        _file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/sleeim_debug/", directory); //デバッグ用ディレクトリ
//
//        DebugLog.d("WriteLog", "WriteLogThread");
//
//
//        File _parentDir = _file.getParentFile(); //親ディレクトリを取得
//
//        //ディレクトリがなければ作成しておく
//        if (!_parentDir.exists()) {
//            if (!(_parentDir.mkdirs())) {
//                DebugLog.d("WriteLog", "ディレクトリを作成できませんでした。");
//            } else {
//                DebugLog.d("WriteLog", "ディレクトリを作成しました。");
//            }
//        }else{
//            DebugLog.d("WriteLog", "ディレクトリが存在しています");
//        }
//    }
//
//    @Override
//    public void run() {
//        java.lang.Process proc = null;
//        BufferedReader reader = null;
//        PrintWriter writer = null;
//
//        final String pId =  Integer.toString(android.os.Process.myPid());
//
//        try {
//
//            DebugLog.d("WriteLog", String.valueOf(commandLine.size()));
//
//            proc = Runtime.getRuntime().exec( commandLine.toArray( new String[commandLine.size()]));
//            //proc = Runtime.getRuntime().exec(new String[] { "logcat", "-v", "time", "-s", "tag:BleService", "tag:BluetoothActivity", "tag:permissionCheck", "tag:MyApplication", "tag:CsvWriter"});
//            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()), 1024);
//            String line;
//            while ( true ) {
//                line = reader.readLine();
//                if(line ==null){
//                    try {
//                        Thread.sleep(200);
//                    } catch (InterruptedException e) {
//                    }
//                    continue;
//                }
//
//
//                if (line.length() == 0) {
//                    try {
//                        Thread.sleep(200);
//                    } catch (InterruptedException e) {
//                    }
//                    continue;
//                }
//
//                if (line.indexOf(pId) != -1) {
//                    try (FileOutputStream fos = new FileOutputStream(_file, true); //append：ファイル内容を削除して書込
//                         OutputStreamWriter osw = new OutputStreamWriter(fos, ENCODING);
//                         PrintWriter pw = new PrintWriter(new BufferedWriter(osw))) {
//                            pw.println(line); //行セット
//                            pw.flush(); //フラッシュ書込
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        if (writer != null) {
//                            writer.close();
//                        }
//                    }
//
//                }
//            }
//        } catch (IOException e) {
//            DebugLog.d("WriteLog", e.getMessage());
//
//            e.printStackTrace();
//        } finally {
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//}