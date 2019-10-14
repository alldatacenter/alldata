package top.omooo.blackfish.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by SSC on 2018/5/10.
 */

public class SharePerUtil {
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    public SharePerUtil(Context context) {
        init(context);
    }

    private void init(Context context) {
        mPreferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        mEditor = mPreferences.edit();
    }

    public void saveString(String key, String value) {
        mEditor.putString(key, value).commit();
    }

    public String getString(String key) {
        return mPreferences.getString(key, null);
    }

    public void saveBoolean(String key,boolean value) {
        mEditor.putBoolean(key, value).commit();
    }

    public boolean getBoolean(String key) {
        return mPreferences.getBoolean(key,false);
    }

    public void saveInt(String key, int value) {
        mEditor.putInt(key, value).commit();
    }

    public int getInt(String key) {
        return mPreferences.getInt(key, 0);
    }

    public void deleteValue(String key) {
        mEditor.remove(key).commit();
    }
}
