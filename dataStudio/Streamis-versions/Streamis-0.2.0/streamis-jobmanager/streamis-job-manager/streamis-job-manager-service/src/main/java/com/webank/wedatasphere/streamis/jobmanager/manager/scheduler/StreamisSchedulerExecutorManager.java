package com.webank.wedatasphere.streamis.jobmanager.manager.scheduler;

import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.events.AbstractStreamisSchedulerEvent;
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.exception.StreamisScheduleException;
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.exception.StreamisScheduleRetryException;
import org.apache.linkis.protocol.engine.EngineState;
import org.apache.linkis.scheduler.exception.LinkisJobRetryException;
import org.apache.linkis.scheduler.executer.*;
import org.apache.linkis.scheduler.listener.ExecutorListener;
import org.apache.linkis.scheduler.queue.SchedulerEvent;
import scala.Option;
import scala.Some;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.Objects;

/**
 * Executor manager
 */
public class StreamisSchedulerExecutorManager extends ExecutorManager {

    /**
     * Just hold a singleton executor
     */
    private Executor singletonExecutor;

    @Override
    public void setExecutorListener(ExecutorListener engineListener) {
        // Empty
    }

    @Override
    public Executor createExecutor(SchedulerEvent event) {
        return getOrCreateExecutor();
    }

    @Override
    public Option<Executor> askExecutor(SchedulerEvent event) {
        return Some.apply(getOrCreateExecutor());
    }

    @Override
    public Option<Executor> askExecutor(SchedulerEvent event, Duration wait) {
        return askExecutor(event);
    }

    @Override
    public Option<Executor> getById(long id) {
        return null;
    }

    @Override
    public Executor[] getByGroup(String groupName) {
        return new Executor[0];
    }

    @Override
    public void delete(Executor executor) {

    }

    @Override
    public void shutdown() {

    }

    private Executor getOrCreateExecutor(){
        if (Objects.isNull(this.singletonExecutor)){
            synchronized (this){
                if (Objects.isNull(this.singletonExecutor)){
                    this.singletonExecutor = new LocalExecutor();
                }
            }
        }
        return this.singletonExecutor;
    }
    public static class LocalExecutor implements Executor{

        @Override
        public long getId() {
            return 0;
        }

        @Override
        public ExecuteResponse execute(ExecuteRequest executeRequest) {
            if (executeRequest instanceof AbstractStreamisSchedulerEvent.LocalExecuteRequest){
                try {
                    ((AbstractStreamisSchedulerEvent.LocalExecuteRequest) executeRequest).localExecute();
                    return new SuccessExecuteResponse();
                } catch (StreamisScheduleException e) {
                    if (e instanceof StreamisScheduleRetryException){
                        e.setErrCode(LinkisJobRetryException.JOB_RETRY_ERROR_CODE());
                    }
                    return new ErrorExecuteResponse("Scheduling exception, scheduled job will fail or retry on the next time, message: ["
                            + e.getMessage() + "]", e);
                } catch (Exception e){
                    return new ErrorExecuteResponse("Scheduling with unknown exception, message: [" + e.getMessage() + "]", e);
                }
            }else{
                return new ErrorExecuteResponse("Unsupported execute request: code: [" + executeRequest.code() + "]", null);
            }
        }

        @Override
        public EngineState state() {
            return null;
        }

        @Override
        public ExecutorInfo getExecutorInfo() {
            return null;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
