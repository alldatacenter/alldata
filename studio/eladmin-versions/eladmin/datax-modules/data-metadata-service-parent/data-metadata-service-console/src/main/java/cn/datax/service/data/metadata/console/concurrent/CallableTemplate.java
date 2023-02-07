package cn.datax.service.data.metadata.console.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 多线程模板类
 */
@Slf4j
public abstract class CallableTemplate<V>  implements Callable<V> {

    /**
     * 前置处理，子类可以Override该方法
     */
    public void beforeProcess() {
        log.info("before process....");
    }

    /**
     * 处理业务逻辑的方法,需要子类去Override
     * @return
     */
    public abstract V process();

    /**
     * 后置处理，子类可以Override该方法
     */
    public void afterProcess() {
        log.info("after process....");
    }

    @Override
    public V call() {
        beforeProcess();
        V result = process();
        afterProcess();
        return result;
    }
}
