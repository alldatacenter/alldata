package com.platform.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Vibrator;

import com.platform.app.bean.User;
import com.platform.app.data.dao.DaoMaster;
import com.platform.app.data.dao.DaoSession;
import com.platform.app.service.LocationService;
import com.platform.app.utils.UserLocalData;
import com.platform.app.utils.Utils;
import com.mob.MobApplication;
import com.mob.MobSDK;


/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/05
 *     desc   : 整个app的管理
 *     version: 1.0
 * </pre>
 */


public class    MallShoppingApp extends MobApplication {
    private User user;
    public LocationService locationService;
    public  Vibrator mVibrator;

    private        DaoMaster.DevOpenHelper mHelper;
    private        SQLiteDatabase          db;
    private        DaoMaster               mDaoMaster;
    private static DaoSession              mDaoSession;

    //整个app的上下文
    public static Context sContext;

    private static MallShoppingApp mInstance;

    public static MallShoppingApp getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        // 通过代码注册你的AppKey和AppSecret
        MobSDK.init(this, "2cb769a43c200", "f9f3e8e3c8fc889da736c181b866156b");

        sContext = getApplicationContext();
        initUser();
        Utils.init(this);

        locationService = new LocationService(getApplicationContext());
        mVibrator = (Vibrator) getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);

        setDatabase();

    }


    private void initUser() {
        this.user = UserLocalData.getUser(this);
    }


    public User getUser() {
        return user;
    }

    public void putUser(User user, String token) {
        this.user = user;
        UserLocalData.putUser(this, user);
        UserLocalData.putToken(this, token);
    }

    public void clearUser() {
        this.user = null;
        UserLocalData.clearUser(this);
        UserLocalData.clearToken(this);
    }


    public String getToken() {
        return UserLocalData.getToken(this);
    }

    private Intent intent;

    public void putIntent(Intent intent) {
        this.intent = intent;
    }

    public Intent getIntent() {
        return intent;
    }

    public void jumpToTargetActivity(Context context) {
        context.startActivity(intent);
        this.intent = null;
    }


    public static MallShoppingApp getApplication() {
        return mInstance;
    }

    /**
     * 设置greenDao
     */
    private void setDatabase() {
        mHelper = new DaoMaster.DevOpenHelper(this, "shopping", null);
        db = mHelper.getWritableDatabase();
        // 注意：该数据库连接属于 DaoMaster，所以多个 Session 指的是相同的数据库连接。
        mDaoMaster = new DaoMaster(db);
        mDaoSession = mDaoMaster.newSession();
    }

    public static DaoSession getDaoSession() {
        return mDaoSession;
    }

    public SQLiteDatabase getDb() {
        return db;
    }

}
