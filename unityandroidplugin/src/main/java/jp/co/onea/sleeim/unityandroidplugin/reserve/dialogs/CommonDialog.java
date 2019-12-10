//package jp.co.onea.sleeim.unityandroidplugin.reserve.dialogs;
//
//import android.app.Dialog;
//import android.content.Context;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.TextView;
//
//import jp.co.onea.sleeim.unityandroidplugin.R;
//
//
//public class CommonDiaDebugLog.extends Dialog{
//
//	/**
//	 * コンストラクタ
//	 * @param context
//	 * @param title     タイトル
//	 * @param message   メッセージ
//	 */
//	public CommonDialog(Context context, String title, String message) {
//		super(context, R.style.Theme_AppCompat_Dialog);
//
//		//レイアウト指定
//		setContentView(R.layout.dialog_common);
//
//		//タイトル設定
//        ((TextView) findViewById(R.id.dialog_common_text_title)).setText(title);
//		//メッセージ設定
//        ((TextView) findViewById(R.id.dialog_common_text_message)).setText(message);
//	}
//
//	/**
//	 * テキストフィールド
//	 * @param showFlag	表示フラグ
//	 */
//	public void setTextField(boolean showFlag, String message) {
//		if (showFlag) {
//			findViewById(R.id.dialog_common_text_field).setVisibility(View.VISIBLE);
//			findViewById(R.id.dialog_common_text_field_msg).setVisibility(View.VISIBLE);
//			((TextView) findViewById(R.id.dialog_common_text_message)).setVisibility(View.GONE);
//
//			((TextView) findViewById(R.id.dialog_common_text_field_msg)).setText("備考");
//			EditText textField = (EditText) findViewById(R.id.dialog_common_text_field);
//			textField.setText(message);
//		}
//	}
//
//	/**
//	 * ボタン（左）設定
//	 * @param showFlag	表示フラグ
//	 * @param listener	リスナー
//	 */
//	public void setButtonLeft(boolean showFlag, View.OnClickListener listener) {
//		setButton((Button) findViewById(R.id.dialog_common_layout_btn_left), showFlag, listener);
//	}
//
//	/**
//	 * ボタン（右）設定
//	 * @param showFlag	表示フラグ
//	 * @param listener	リスナー
//	 */
//	public void setButtonRight(boolean showFlag, View.OnClickListener listener) {
//		setButton((Button) findViewById(R.id.dialog_common_layout_btn_right), showFlag, listener);
//	}
//
//	/**
//	 * ボタン設定処理
//	 * @param button	ボタンオブジェクト
//	 * @param showFlag	表示フラグ
//	 * @param listener	リスナー
//	 */
//	private void setButton(Button button, boolean showFlag, View.OnClickListener listener) {
//		if (!showFlag) {
//			button.setVisibility(View.GONE);
//		}
//		button.setOnClickListener(listener);
//	}
//
//}