package com.alibaba.tesla.appmanager.workflow.service.pubsub;

import com.alibaba.tesla.appmanager.domain.res.workflow.WorkflowInstanceOperationRes;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Workflow 命令执行等待对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class WorkflowInstanceOperationCommandWaitingObject {

    /**
     * 执行结果对象
     */
    private WorkflowInstanceOperationRes obj;

    /**
     * Lock & Condition
     */
    private final Lock lock = new ReentrantLock();
    private final Condition done = lock.newCondition();

    /**
     * 全局 Workflow 命令执行等待对象 Map
     */
    private static final Map<String, WorkflowInstanceOperationCommandWaitingObject> TASK_MAP = new ConcurrentHashMap<>();

    /**
     * 根据 Workflow 任务 ID 创建一个等待中的命令执行对象
     *
     * @param workflowTaskId Workflow 任务 ID
     * @return 返回等待中的运行对象
     */
    public static WorkflowInstanceOperationCommandWaitingObject create(Long workflowTaskId, String command) {
        WorkflowInstanceOperationCommandWaitingObject task = new WorkflowInstanceOperationCommandWaitingObject();
        TASK_MAP.put(key(workflowTaskId, command), task);
        return task;
    }

    /**
     * 触发指定 Workflow 任务命令执行对象的完成事件，通知外界可获取当前结果
     *
     * @param workflowTaskId Workflow 任务 ID
     * @param command        命令
     * @param obj            完成的 Future 结果对象
     */
    public static void triggerFinished(Long workflowTaskId, String command, WorkflowInstanceOperationRes obj) {
        WorkflowInstanceOperationCommandWaitingObject task = TASK_MAP.remove(key(workflowTaskId, command));
        if (task != null) {
            task.onFinished(obj);
        }
    }

    /**
     * 等待 Workflow 任务命令执行结果 (内部通过 await 等待)，允许 InterruptedException 中断
     *
     * @param time 等待时间
     * @param unit 等待单位
     * @return 命令执行结果 (null 表示超时后仍无人触发结果返回)
     */
    public WorkflowInstanceOperationRes wait(long time, TimeUnit unit) throws InterruptedException {
        lock.lock();
        try {
            while (this.obj == null) {
                if (!done.await(time, unit)) {
                    return null;
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
    private void onFinished(WorkflowInstanceOperationRes obj) {
        lock.lock();
        try {
            this.obj = obj;
            done.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 生成 Key
     *
     * @param workflowInstanceId Workflow Instance ID
     * @param command            命令
     * @return Key
     */
    private static String key(Long workflowInstanceId, String command) {
        return String.format("%d_%s", workflowInstanceId, command);
    }
}
