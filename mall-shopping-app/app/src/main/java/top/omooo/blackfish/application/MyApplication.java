package top.omooo.blackfish.application;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.mob.MobSDK;

import top.omooo.auto_track.SensorsDataAPI;
import top.omooo.router.EasyRouter;

/**
 * Created by Omooo on 2018/2/24.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
        MobSDK.init(this);
//        LeakCanary.install(this);
        EasyRouter.getInstance().inject(this);
        SensorsDataAPI.init(this);
    }
}
