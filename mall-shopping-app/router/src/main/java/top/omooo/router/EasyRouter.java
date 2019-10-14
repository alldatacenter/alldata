package top.omooo.router;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import top.omooo.logger.Logger;
import top.omooo.logger.StackTraceUtil;
import top.omooo.router_annotations.Router;

/**
 * Created by Omooo
 * Date:2019/4/1
 */
public class EasyRouter {

    private static final String TAG = "EasyRouter";

    private static final String PAGE_NAME = "pageName";
    //路由表 key:pageName value: Activity
    private HashMap<String, String> mRouterMap;
    private Context mContext;
    private Bundle mBundle;

    private boolean isUseAnno = false;  //使用注解

    private static final String META_DATE_EMPTY_DESC = "This Activity doesn't have metaData!";
    private static final String PAGE_NAME_EMPTY_DESC = "This MetaData doesn't have pageName!";
    private static final String PAGE_NAME_NOT_AVAIRABLE = "This pageName is not available!";

    public static EasyRouter getInstance() {
        return EasyRouterHolder.sRouter;
    }

    private static class EasyRouterHolder {
        private static top.omooo.router.EasyRouter sRouter = new top.omooo.router.EasyRouter();
    }

    /**
     * 1. 通过 Application 获得所有 ActivityInfo
     * 2. 通过 ActivityInfo 获得每一个 Activity 的 MetaData 元数据
     * 注意: 获取元数据也要构造一个新的 ActivityInfo
     */
    public void inject(Application application) {
        mRouterMap = new HashMap<>();
        if (isUseAnno) {
            getRouterMapFromAnno(application);
            return;
        }
        //使用 Meta-Data
        try {
            ActivityInfo[] activityInfos = application.getPackageManager()
                    .getPackageInfo(application.getPackageName(), PackageManager.GET_ACTIVITIES)
                    .activities;
            Bundle bundle;
            ActivityInfo metaDataInfo;
            for (ActivityInfo activityInfo : activityInfos) {
                metaDataInfo = application.
                        getPackageManager().
                        getActivityInfo(new ComponentName(application.getPackageName(), activityInfo.name), PackageManager.GET_META_DATA);
                if (metaDataInfo.metaData == null) {
                    Logger.e(TAG, META_DATE_EMPTY_DESC, StackTraceUtil.getStackTrace(), activityInfo.name);
                } else {
                    bundle = metaDataInfo.metaData;
                    if (TextUtils.isEmpty(bundle.getString(PAGE_NAME))) {
                        Logger.e(TAG, PAGE_NAME_EMPTY_DESC, StackTraceUtil.getStackTrace(), activityInfo.name);
                    } else {
                        mRouterMap.put(bundle.getString(PAGE_NAME), activityInfo.name);
                        Logger.d(TAG, null, "ClassName: " + activityInfo.name,
                                "PageName: " + bundle.getString(PAGE_NAME));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            //ignore
            e.printStackTrace();
        }
    }

    public EasyRouter with(@NonNull Context context) {
        this.mContext = context;
        return this;
    }

    public void navigate(@NonNull String pageName) {
        Log.i(TAG, "navigate: ");
        if (!TextUtils.isEmpty(pageName) && !TextUtils.isEmpty(mRouterMap.get(pageName))) {
            Intent intent = new Intent();
            intent.setClassName(mContext.getPackageName(), mRouterMap.get(pageName));
            if (mBundle != null) {
                intent.putExtras(mBundle);
            }
            mContext.startActivity(intent);
        } else {
            Logger.e(TAG, PAGE_NAME_NOT_AVAIRABLE, StackTraceUtil.getStackTrace(), "pageName: " + pageName);
        }
    }

    /**
     * 编译时注解扫描所有 pageName，然后返回路由表
     * {@link Router}
     */
    public void getRouterMapFromAnno(Application application) {
        List<String> moduleList = getModuleList(application);
        if (moduleList == null || moduleList.size() == 0) {
            return;
        }
        try {
            for (String moduleName : moduleList) {
                Class clazz = Class.forName(moduleName + ".factory.RouterFactory");
                Method method = clazz.getMethod("init");
                method.invoke(null);
                mRouterMap.putAll((HashMap<String, String>) clazz.getField("sHashMap").get(clazz));
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取所有包含 {@link Router} 注解的 module
     */
    private List<String> getModuleList(Application application) {
        List<String> moduleList = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        try {
            //获取assets资源管理器
            AssetManager assetManager = application.getAssets();
            //通过管理器打开文件并读取
            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open("module_info")));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
            JSONObject jsonObject = new JSONObject(stringBuilder.toString());
            JSONArray jsonArray = jsonObject.getJSONArray("modules");
            for (int i = 0; i < jsonArray.length(); i++) {
                moduleList.add(((JSONObject) jsonArray.get(i)).getString("packageName"));
            }
            return moduleList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 添加 Bundle 数据
     */

    public EasyRouter putAll(Bundle bundle) {
        mBundle = bundle;
        return this;
    }

    public EasyRouter putString(String key, String value) {
        Log.i(TAG, "putString: ");
        if (mBundle == null) {
            mBundle = new Bundle();
            mBundle.putString(key, value);
        } else {
            mBundle.putString(key, value);
        }
        return this;
    }

    public EasyRouter putInt(String key, int value) {
        if (mBundle == null) {
            mBundle = new Bundle();
        } else {
            mBundle.putInt(key, value);
        }
        return this;
    }

    public EasyRouter putParcelable(String key, Parcelable value) {
        if (mBundle == null) {
            mBundle = new Bundle();
        } else {
            mBundle.putParcelable(key, value);
        }
        return this;
    }

    public EasyRouter putSerializable(String key, Serializable value) {
        if (mBundle == null) {
            mBundle = new Bundle();
        } else {
            mBundle.putSerializable(key, value);
        }
        return this;
    }
}
