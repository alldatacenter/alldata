package com.alibaba.tesla.appmanager.workflow.service.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Workflow 任务运行对象 (用于等待)
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class ExecuteWorkflowTaskWaitingObject {

    /**
     * 线程执行结果对象
     */
    private ExecuteWorkflowTaskResult obj;

    /**
     * Lock & Condition
     */
    private final Lock lock = new ReentrantLock();
    private final Condition done = lock.newCondition();

    /**
     * 全局 Workflow 任务运行对象 Map
     */
    private static final Map<Long, ExecuteWorkflowTaskWaitingObject> TASK_MAP = new ConcurrentHashMap<>();

    /**
     * 根据 Workflow 任务 ID 创建一个等待中的运行对象
     *
     * @param workflowTaskId Workflow 任务 ID
     * @return 返回等待中的运行对象
     */
    public static ExecuteWorkflowTaskWaitingObject create(Long workflowTaskId) {
        ExecuteWorkflowTaskWaitingObject task = new ExecuteWorkflowTaskWaitingObject();
        TASK_MAP.put(workflowTaskId, task);
        return task;
    }

    /**
     * 触发指定 Workflow 任务运行对象的完成事件，通知外界可获取当前结果
     *
     * @param workflowTaskId Workflow 任务 ID
     * @param obj            完成的 Future 结果对象
     */
    public static void triggerFinished(Long workflowTaskId, ExecuteWorkflowTaskResult obj) {
        ExecuteWorkflowTaskWaitingObject task = TASK_MAP.remove(workflowTaskId);
        if (task != null) {
            task.onFinished(obj);
        }
    }

    /**
     * 触发指定 Workflow 任务运行对象的终止事件，通知外界可获取当前结果
     *
     * @param workflowTaskId Workflow 任务 ID
     * @return boolean 是否存在该记录以发送终止信号
     */
    public static boolean triggerTerminated(Long workflowTaskId, String extMessage) {
        ExecuteWorkflowTaskWaitingObject task = TASK_MAP.remove(workflowTaskId);
        if (task != null) {
            task.onTerminated(extMessage);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 触发指定 Workflow 任务运行对象的暂停事件，通知外界可获取当前结果
     *
     * @param workflowTaskId Workflow 任务 ID
     */
    public static void triggerSuspend(Long workflowTaskId, String extMessage) {
        ExecuteWorkflowTaskWaitingObject task = TASK_MAP.remove(workflowTaskId);
        if (task != null) {
            task.onSuspend(extMessage);
        }
    }

    /**
     * 等待 Workflow 任务对象结果 (内部通过 await 等待)，允许 InterruptedException 中断
     *
     * @param runnable 每个等待周期结束后需要运行的函数
     * @param time     等待时间
     * @param unit     等待单位
     * @return Future of Workflow 任务对象，是否完成需要自行继续调用 future isDone() 判断
     */
    public ExecuteWorkflowTaskResult wait(Runnable runnable, long time, TimeUnit unit) throws InterruptedException {
        lock.lock();
        try {
            while (this.obj == null) {
                if (!done.await(time, unit)) {
                    runnable.run();
                }
            }
        } finally {
            lock.unlock();
        }
        return obj;
    }

    /**
     * 对指定 Workflow 运行对象进行状态完成的标识
     *
     * @param obj 运行对象
     */
    private void onFinished(ExecuteWorkflowTaskResult obj) {
        lock.lock();
        try {
            this.obj = obj;
            done.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 对指定 Workflow 运行对象进行终止
     *
     * @param extMessage 扩展存储信息
     */
    private void onTerminated(String extMessage) {
        lock.lock();
        try {
            ExecuteWorkflowTaskResult res = new ExecuteWorkflowTaskResult();
            res.setTerminated(true);
            res.setExtMessage(extMessage);
            this.obj = res;
            done.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 对指定 Workflow 运行对象进行休眠
     *
     * @param extMessage 扩展存储信息
     */
    private void onSuspend(String extMessage) {
        lock.lock();
        try {
            ExecuteWorkflowTaskResult res = new ExecuteWorkflowTaskResult();
            res.setPaused(true);
            res.setExtMessage(extMessage);
            this.obj = res;
            done.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
