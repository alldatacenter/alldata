package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stop.DestroyProcessStopper;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 命令工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class CommandUtil {

    /**
     * 在本机运行命令，并返回运行结果 (异步)
     *
     * @param command 命令
     * @param envMap  环境变量字典
     * @return 命令执行结果
     */
    public static Future<ProcessResult> asyncRunLocalCommand(String command, Map<String, String> envMap) {
        try {
            return new ProcessExecutor()
                .command(getBashCommand(command))
                .environment(envMap)
                .redirectOutput(Slf4jStream.ofCaller().asInfo())
                .redirectErrorStream(true)
                .timeout(120, TimeUnit.MINUTES)
                .stopper(DestroyProcessStopper.INSTANCE)
                .readOutput(true)
                .start()
                .getFuture();
        } catch (IOException e) {
            throw new AppException(AppErrorCode.COMMAND_ERROR,
                String.format("action=asyncRunLocalCommand|command=%s", String.join(" ", command)), e);
        }
    }

    /**
     * 在本机运行命令，并返回运行结果 (异步)
     *
     * @param command 命令
     * @return 命令执行结果
     */
    public static Future<ProcessResult> asyncRunLocalCommand(String command) {
        return asyncRunLocalCommand(command, new HashMap<>());
    }

    /**
     * 在本机运行命令，并返回运行结果
     *
     * @param command 命令
     * @param envMap  环境变量字典
     * @return 命令执行结果
     */
    public static String runLocalCommand(String command, Map<String, String> envMap) {
        try {
            ProcessResult result = new ProcessExecutor()
                .command(getBashCommand(command))
                .environment(envMap)
                .redirectOutput(Slf4jStream.ofCaller().asInfo())
                .redirectErrorStream(true)
                .timeout(120, TimeUnit.MINUTES)
                .stopper(DestroyProcessStopper.INSTANCE)
                .readOutput(true)
                .execute();
            int retCode = result.getExitValue();
            String output = result.outputUTF8();
            if (retCode != 0) {
                throw new AppException(AppErrorCode.COMMAND_ERROR,
                    String.format("action=runLocalCommand|command=%s|retCode=%d|output=%s",
                        command, retCode, output));
            }
            log.info("action=runLocalCommand|command={}|retCode={}", command, retCode);
            return output;
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new AppException(AppErrorCode.COMMAND_ERROR,
                String.format("action=runLocalCommand|command=%s", String.join(" ", command)), e);
        }
    }

    /**
     * 在本机运行命令，并返回运行结果
     *
     * @param command 命令
     * @return 命令执行结果
     */
    public static String runLocalCommand(String command) {
        return runLocalCommand(command, new HashMap<>());
    }

    /**
     * 判断当前是否为 Windows 系统
     *
     * @return true or false
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 将传入的普通命令调整为 ProcessExecutor 需要的命令格式（bash）
     *
     * @param command 原始命令
     * @return 调整后的命令数组
     */
    public static String[] getBashCommand(String command) {
        String[] actualCommand;
        command = command.replaceAll("\\(", "\\\\(");
        command = command.replaceAll("\\)", "\\\\)");
        if (isWindows()) {
            command = command.replace("C:\\", "/mnt/c/").replaceAll("\\\\", "/");
        }
        actualCommand = new String[]{"bash", "-c", command};
        return actualCommand;
    }
}
