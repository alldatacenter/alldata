package cn.datax.service.data.market.utils;

import cn.datax.service.data.market.api.dto.ApiLogDto;

public class ThreadUtil {

    private ThreadUtil() {}

    private static volatile ThreadUtil instance;

    public static ThreadUtil getInstance() {
        if(instance == null) {
            synchronized (ThreadUtil.class) {
                if(instance == null) {
                    instance = new ThreadUtil();
                }
            }
        }
        return instance;
    }

    private final static ThreadLocal<ApiLogDto> logHolder = new ThreadLocal<>();

    public void set(ApiLogDto log){
        logHolder.set(log);
    }

    public void remove(){
        logHolder.remove();
    }

    public ApiLogDto get(){
        return logHolder.get();
    }
}
