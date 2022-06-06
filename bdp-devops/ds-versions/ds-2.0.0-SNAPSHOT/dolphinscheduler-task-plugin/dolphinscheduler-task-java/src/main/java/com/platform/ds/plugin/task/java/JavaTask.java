package com.platform.ds.plugin.task.java;

import org.apache.dolphinscheduler.plugin.task.api.AbstractTaskExecutor;
import org.apache.dolphinscheduler.spi.task.AbstractParameters;
import org.apache.dolphinscheduler.spi.task.request.TaskRequest;
import org.apache.dolphinscheduler.spi.utils.DateUtils;
import org.apache.dolphinscheduler.spi.utils.JSONUtils;
import static org.apache.dolphinscheduler.spi.task.TaskConstants.TASK_LOG_INFO_FORMAT;

public class JavaTask extends AbstractTaskExecutor {

    /**
     * output
     */
    protected String output;
    /**
     * java parameters
     */
    private JavaParameters javaParameters;
    /**
     * taskExecutionContext
     */
    private TaskRequest taskExecutionContext;

    /**
     * constructor
     *
     * @param taskExecutionContext taskExecutionContext
     */
    public JavaTask(TaskRequest taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void init() {
        logger.info("java task params {}", taskExecutionContext.getTaskParams());
        this.javaParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), JavaParameters.class);
 
    }

    @Override
    public void handle() throws Exception {

        String threadLoggerInfoName = String.format(TASK_LOG_INFO_FORMAT, taskExecutionContext.getTaskAppId());
        Thread.currentThread().setName(threadLoggerInfoName);

        long startTime = System.currentTimeMillis();
        String formatTimeStamp = DateUtils.formatTimeStamp(startTime);
        String statusCode = null;
        String body = null;

        /**
         *  bin/flinkx \
         * 	-mode local \
         * 	-jobType sync \
         * 	-job flinkx-examples/json/stream/stream.json \
         * 	-flinkxDistDir flinkx-dist
         */
        trySubmitFlinkxJob();

    }

    private void trySubmitFlinkxJob() {
        //todo submit FlinkX job
    }

    @Override
    public AbstractParameters getParameters() {
        return this.javaParameters;
    }
}
