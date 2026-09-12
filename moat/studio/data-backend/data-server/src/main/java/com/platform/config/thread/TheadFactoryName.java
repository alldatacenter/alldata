
package com.platform.config.thread;

import com.platform.utils.StringUtils;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义线程名称
 * @author AllDataDC
 * @date 2023-01-27 17:49:55
 */
@Component
public class TheadFactoryName implements ThreadFactory {

    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    private final static String DEF_NAME = "el-pool-";

    public TheadFactoryName() {
        this(DEF_NAME);
    }

    public TheadFactoryName(String name){
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        //此时namePrefix就是 name + 第几个用这个工厂创建线程池的
        this.namePrefix = (StringUtils.isNotBlank(name) ? name : DEF_NAME) + "-" + POOL_NUMBER.getAndIncrement();
    }

    @Override
    public Thread newThread(Runnable r) {
        //此时线程的名字 就是 namePrefix + -exec- + 这个线程池中第几个执行的线程
        Thread t = new Thread(group, r,
                namePrefix + "-exec-"+threadNumber.getAndIncrement(),
                0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }
}
