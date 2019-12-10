package jp.co.onea.sleeim.unityandroidplugin.utils;

/**
 *
 * データ抽出等に必要なキーをまとめたクラス(Intentのエクストラ情報)
 *
 * @since
 * @version
 */
public class ExtractionKey {

    /** プライベートコンストラクタ */
    private ExtractionKey() {}

    /** アプリ固有のデフォルトフォルダ用キー */
	//public static final String DEFAULT_APPLICATION_FOLDER = "sleeim";

	/** SharedPrefereneces用キー */
	public static final String PREFERENCE_KEY_SELECT_ALERM = "KAIMIN_SETTING_SELECT_ALERM"; //選択中のアラーム
	public static final String PREFERENCE_KEY_VIBRATION = "SETTING_VIBRATION_ISENABLE"; //バイブレーションのON/OFF
	public static final String PREFERENCE_KEY_FEEDIN = "SETTING_FEEDIN_ISENABLE"; //フェードインのON/OFF
	public static final String PREFERENCE_KEY_ALERM_CALLTIME = "KAIMIN_SETTING_ALERM_CALLTIME"; //鳴動時間

	/** 持ち回り情報用キー */
//	public static final String SET_COMMON_DIALOG_TITLE = "SET_COMMON_DIALOG_TITLE";
//	public static final String SET_COMMON_DIALOG_MESSAGE = "SET_COMMON_DIALOG_MESSAGE";
//	public static final String SET_COMMON_DIALOG_TEXT_FIELD = "SET_COMMON_DIALOG_TEXT_FIELD";
//    public static final String SET_COMMON_DIALOG_LEFT_BUTTON = "SET_COMMON_DIALOG_LEFT_BUTTON";
//    public static final String SET_COMMON_DIALOG_RIGHT_BUTTON = "SET_COMMON_DIALOG_RIGHT_BUTTON";
//	public static final String SET_COMMON_DIALOG_TAG = "SET_COMMON_DIALOG_TAG";
}
