//package jp.co.onea.sleeim.unityandroidplugin.reserve.dialogs;
//
//import android.app.Dialog;
//import android.os.Bundle;
//import android.support.v4.app.DialogFragment;
//import android.support.v4.app.FragmentManager;
//import android.util.Log;
//
//
//public class BaseDialogFragment extends DialogFragment {
//	private static final String TAG = BaseDialogFragment.class.getSimpleName();
//
//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//
//        //ダイアログ表示時にナビゲーションバーが表示される問題の対策
//        //Utility.setFullScreen(getDialog().getWindow());
//    }
//
//
//    @Override
//	public void show(FragmentManager manager, String tag) {
//        if (checkShow(manager, tag)) {
//        	super.show(manager, tag);
//        }else{
//            DebugLog.d(TAG, "ダイアログ表示済み");
//        }
//	}
//
//	/**
//	 * ダイアログを表示するかどうかのチェック
//	 * @param manager
//	 * @param tag
//	 * @return
//	 */
//	private boolean checkShow(FragmentManager manager, String tag) {
//		DialogFragment dialogFragment = (DialogFragment) manager.findFragmentByTag(tag);
//		DebugLog.d(TAG, "checkShow start:" + dialogFragment);
//
//        // フラグメントが表示されていなければOK
//        if (dialogFragment == null) {
//            return true;
//        }
//        final DiaDebugLog.dialog = dialogFragment.getDialog();
//        // ダイアログがなければOK
//        if (dialog == null) {
//            return true;
//        }
//        // ダイアログが表示されていなければOK
//        if (!dialog.isShowing()) {
//            return true;
//        }
//		return false;
//	}
//}
