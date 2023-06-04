package com.datasophon.worker.handler;


import com.datasophon.common.Constants;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceRoleRunner;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.ShellUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER;


public class ServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHandler.class);

    /**
     * log list
     */
    public static List<String> logBuffer;

    protected boolean logOutputIsSuccess = false;

    /**
     * log handler
     */
    protected Consumer<List<String>> logHandler;

    public ServiceHandler() {
        this.logBuffer = Collections.synchronizedList(new ArrayList<>());
        this.logHandler = this::logHandle;
    }

    public ExecResult start(ServiceRoleRunner startRunner, ServiceRoleRunner statusRunner, String decompressPackageName, RunAs runAs) {
        ExecResult statusResult = execRunner(statusRunner, decompressPackageName, null);
        if (statusResult.getExecResult()) {
            logger.info("{} already started", decompressPackageName);
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            return execResult;
        }
        //start service
        ExecResult startResult =execRunner(startRunner, decompressPackageName, runAs);
        //check start result
        if (startResult.getExecResult()) {
            int times = PropertyUtils.getInt("times");
            int count = 0;
            while (count < times) {
                logger.info("check start result at times {}", count + 1);
                ExecResult result = execRunner(statusRunner, decompressPackageName, runAs);
                if (result.getExecResult()) {
                    logger.info("start success in {}", decompressPackageName);
                    break;
                } else {
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                count++;
            }
            if (count == times) {
                logger.info(" start {} timeout", decompressPackageName);
                startResult.setExecResult(false);
            }
        }
        return startResult;
    }


    public ExecResult stop(ServiceRoleRunner runner, ServiceRoleRunner statusRunner, String decompressPackageName, RunAs runAs) {
        ExecResult statusResult = execRunner(statusRunner, decompressPackageName, runAs);
        ExecResult execResult = new ExecResult();
        if (statusResult.getExecResult()) {
            execResult = execRunner(runner, decompressPackageName, runAs);
            //检测是否停止成功
            if (execResult.getExecResult()) {
                int times = PropertyUtils.getInt("times");
                int count = 0;
                while (count < times) {
                    logger.info("check stop result at times {}", count + 1);
                    ExecResult result = execRunner(statusRunner, decompressPackageName, runAs);
                    if (!result.getExecResult()) {
                        logger.info("stop success in {}", decompressPackageName);
                        break;
                    } else {
                        try {
                            Thread.sleep(5 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    count++;
                }
                if (count == times) {//超时，置为失败
                    execResult.setExecResult(false);
                }
            }
        } else {//已经是停止状态，直接返回
            logger.info("{} already stopped", decompressPackageName);
            execResult.setExecResult(true);
        }
        return execResult;
    }

    public ExecResult reStart(ServiceRoleRunner runner, String decompressPackageName) {
        ExecResult result = execRunner(runner, decompressPackageName, null);
        return result;
    }

    public ExecResult status(ServiceRoleRunner runner, String decompressPackageName) {
        ExecResult result = execRunner(runner, decompressPackageName, null);
        return result;
    }

    public ExecResult execRunner(ServiceRoleRunner runner, String decompressPackageName, RunAs runAs) {
        String shell = runner.getProgram();
        List<String> args = runner.getArgs();
        long timeout = Long.parseLong(runner.getTimeout());
        ArrayList<String> command = new ArrayList<>();
        if (Objects.nonNull(runAs) && StringUtils.isNotBlank(runAs.getUser())) {
            command.add("sudo");
            command.add("-u");
            command.add(runAs.getUser());
        }
        if (runner.getProgram().contains(Constants.TASK_MANAGER) || runner.getProgram().contains(Constants.JOB_MANAGER)) {
            logger.info("do not use sh");
        } else {
            command.add("sh");
        }
        command.add(shell);
        command.addAll(args);
        logger.info("execute shell command : {}", command.toString());
        ExecResult execResult = execWithStatus(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName, command, timeout);
        return execResult;
    }


    public ExecResult execWithStatus(String workPath, List<String> command, long timeout) {
        ExecResult result = new ExecResult();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(workPath));
        processBuilder.command(command);
        processBuilder.redirectErrorStream(true);
        Process process = null;
        try {
            process = processBuilder.start();
            getOutput(process);
            boolean execResult = process.waitFor(timeout, TimeUnit.SECONDS);
            if (execResult && process.exitValue() == 0) {
                logger.info("script execute success");
                result.setExecResult(true);
                result.setExecOut("script execute success");
            } else {
                result.setExecOut("script execute failed");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void getOutput(Process process) {
        ExecutorService getOutputLogService = Executors.newSingleThreadExecutor();
        getOutputLogService.submit(() -> {
            BufferedReader inReader = null;
            try {
                inReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = inReader.readLine()) != null) {
                    logBuffer.add(line);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                logOutputIsSuccess = true;
                close(inReader);
            }
        });
        getOutputLogService.shutdown();
        ExecutorService parseProcessOutputExecutorService = Executors.newSingleThreadExecutor();
        parseProcessOutputExecutorService.submit(() -> {
            try {
                long lastFlushTime = System.currentTimeMillis();
                while (logBuffer.size() > 0 || !logOutputIsSuccess) {
                    if (logBuffer.size() > 0) {
                        lastFlushTime = flush(lastFlushTime);
                    } else {
                        Thread.sleep(1000);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                clear();
            }
        });
        parseProcessOutputExecutorService.shutdown();
    }

    /**
     * when log buffer siz or flush time reach condition , then flush
     *
     * @param lastFlushTime last flush time
     * @return last flush time
     */
    private long flush(long lastFlushTime) {
        long now = System.currentTimeMillis();

        /**
         * when log buffer siz or flush time reach condition , then flush
         */
        lastFlushTime = now;
        /** log handle */
        logHandler.accept(logBuffer);

        logBuffer.clear();

        return lastFlushTime;
    }

    /**
     * clear
     */
    private void clear() {

        List<String> markerList = new ArrayList<>();
        markerList.add(ch.qos.logback.classic.ClassicConstants.FINALIZE_SESSION_MARKER.toString());

        if (!logBuffer.isEmpty()) {
            // log handle
            logHandler.accept(logBuffer);
            logBuffer.clear();
        }
        logHandler.accept(markerList);
    }

    /**
     * print command
     *
     * @param commands process builder
     */
    private void printCommand(List<String> commands) {
        String cmdStr = StringUtils.join(commands, " ");
        logger.info("task run command:\n{}", cmdStr);
    }

    /**
     * close buffer reader
     *
     * @param inReader in reader
     */
    private void close(BufferedReader inReader) {
        if (inReader != null) {
            try {
                inReader.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * log handle
     *
     * @param logs log list
     */
    public void logHandle(List<String> logs) {
        // custom logger
        Logger taskLogger = LoggerFactory.getLogger("service");
        // note that the "new line" is added here to facilitate log parsing
        if (logs.contains(FINALIZE_SESSION_MARKER.toString())) {
            taskLogger.info(FINALIZE_SESSION_MARKER, FINALIZE_SESSION_MARKER.toString());
        } else {
            taskLogger.info(" -> {}", String.join("\n\t", logs));
        }
    }
}
