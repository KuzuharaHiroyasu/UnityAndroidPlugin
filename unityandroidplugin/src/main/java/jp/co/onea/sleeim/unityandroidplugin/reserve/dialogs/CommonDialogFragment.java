//package jp.co.onea.sleeim.unityandroidplugin.reserve.dialogs;
//
//import android.app.Activity;
//import android.app.Dialog;
//import android.content.DialogInterface;
//import android.os.Bundle;
//import android.support.v4.app.Fragment;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.view.View;
//
//import jp.co.onea.sleeim.unityandroidplugin.utils.ExtractionKey;
//import CommonDialog;
//
//
//public class CommonDialogFragment extends BaseDialogFragment {
//    private String mTag = null;
//    private final static String TAG = CommonDialogFragment.class.getSimpleName();
//
//    /**
//     * 共通ダイアログ呼び出し用インターフェイス（共通ダイアログを利用する各画面で実装）
//     */
//    public interface CommonDialogCallback {
//        //ダイアログを閉じる時の処理
//        void onCommonDialogDismiss();
//        //左ボタンを押した時の処理
//        void onCommonDiaDebugLog.eftButtonClicked(CommonDialogFragment dialog);
//        //右ボタンを押した時の処理
//        void onCommonDialogRightButtonClicked(CommonDialogFragment dialog);
//    }
//
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        //ダイアログ情報を取得
//        final Bundle bundle = getArguments();
//        String title = bundle.getString(ExtractionKey.SET_COMMON_DIALOG_TITLE);
//        String message = bundle.getString(ExtractionKey.SET_COMMON_DIALOG_MESSAGE);
//        boolean showTextField = bundle.getBoolean(ExtractionKey.SET_COMMON_DIALOG_TEXT_FIELD);
//        boolean showLeftButton = bundle.getBoolean(ExtractionKey.SET_COMMON_DIALOG_LEFT_BUTTON);
//        boolean showRightButton = bundle.getBoolean(ExtractionKey.SET_COMMON_DIALOG_RIGHT_BUTTON);
//
//        //ダイアログ作成
//        final CommonDialog commonDialog = new CommonDialog(getActivity(), title, message){
//            @Override
//            public boolean onTouchEvent(MotionEvent event) {
//                try {
//                    getActivity().dispatchTouchEvent(event);
//                    return true;
//                } catch (ClassCastException e) {
//                    throw new ClassCastException("activity が dispatchTouchEvent を実装していません.");
//                }
//            }
//        };
//
//        // textField
//        commonDialog.setTextField(showTextField, message);
//
//        //左ボタン表示
//        commonDialog.setButtonLeft(showLeftButton, new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //共通ダイアログ用インターフェイスを実装している場合
//                Activity activity = getActivity();
//                Fragment target = getTargetFragment();
//                Fragment parent = getParentFragment();
//                if (activity != null && activity instanceof CommonDialogCallback) {
//                    ((CommonDialogCallback)getActivity()).onCommonDiaDebugLog.eftButtonClicked(CommonDialogFragment.this);
//                } else if (target != null && target instanceof CommonDialogCallback) {
//                    ((CommonDialogCallback)target).onCommonDiaDebugLog.eftButtonClicked(CommonDialogFragment.this);
//                } else if (parent != null && parent instanceof CommonDialogCallback) {
//                    ((CommonDialogCallback)parent).onCommonDiaDebugLog.eftButtonClicked(CommonDialogFragment.this);
//                }else {
//                    commonDiaDebugLog.dismiss();
//                }
//            }
//        });
//        //右ボタン設定
//        commonDialog.setButtonRight(showRightButton, new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //共通ダイアログ用インターフェイスを実装している場合
//                Activity activity = getActivity();
//                Fragment target = getTargetFragment();
//                Fragment parent = getParentFragment();
//                if (activity != null && activity instanceof CommonDialogCallback) {
//                    ((CommonDialogCallback)getActivity()).onCommonDialogRightButtonClicked(CommonDialogFragment.this);
//                } else if (target != null && target instanceof CommonDialogCallback) {
//                    ((CommonDialogCallback)target).onCommonDialogRightButtonClicked(CommonDialogFragment.this);
//                } else if (parent != null && parent instanceof CommonDialogCallback) {
//                    ((CommonDialogCallback)parent).onCommonDialogRightButtonClicked(CommonDialogFragment.this);
//                }else {
//                    commonDiaDebugLog.dismiss();
//                }
//            }
//        });
//        return commonDialog;
//    }
//
//    @Override
//    public void onDismiss(DialogInterface dialog) {
//        super.onDismiss(dialog);
//        Activity activity = getActivity();
//        Fragment target = getTargetFragment();
//        Fragment parent = getParentFragment();
//        if (activity != null && activity instanceof CommonDialogCallback) {
//            ((CommonDialogCallback) activity).onCommonDialogDismiss();
//        } else if (target != null && target instanceof CommonDialogCallback) {
//            ((CommonDialogCallback)target).onCommonDialogDismiss();
//        } else if (parent != null && parent instanceof CommonDialogCallback) {
//            ((CommonDialogCallback)parent).onCommonDialogDismiss();
//        }
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        DebugLog.d(TAG, "onStop");
//        //getActivity().finish();
//    }
//
//    /***
//     * Fragmentが破棄される時、最後に呼び出される
//     */
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        DebugLog.d(TAG, "onDestroy");
//        getActivity().finish();
//    }
//}
