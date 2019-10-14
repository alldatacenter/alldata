package top.omooo.blackfish.utils;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Created by SSC on 2018/3/27.
 */

public class KeyBoardUtil {

    public static void showKeyBoard(final EditText editText) {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
                editText.requestFocus();
                InputMethodManager methodManager = (InputMethodManager) editText.getContext().getSystemService(INPUT_METHOD_SERVICE);
                methodManager.showSoftInput(editText, 0);
            }
        },500);
    }

    public static void closeKeyBoard(EditText editText) {
        editText.clearFocus();
        InputMethodManager imm = (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}
