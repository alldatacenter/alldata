package org.dromara.cloudeon.utils;

import cn.hutool.core.thread.NamedThreadFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 功能描述: Shell命令运行工具类封装
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShellCommandExecUtil {

    private Logger log;


    public int runShellCommandSync(String baseShellDir, String[] cmd,
                                   Charset outputCharset) throws IOException {
        return runShellCommandSync(baseShellDir, cmd, outputCharset, null);
    }

    /**
     * 真正运行shell命令
     *
     * @param baseShellDir  运行命令所在目录（先切换到该目录后再运行命令）
     * @param cmd           命令数组
     * @param outputCharset 日志输出字符集，一般windows为GBK, linux为utf8
     * @param logFilePath   日志输出文件路径, 为空则直接输出到当前应用日志中，否则写入该文件
     * @return 进程退出码, 0: 成功, 其他:失败
     * @throws IOException 执行异常时抛出
     */
    public int runShellCommandSync(String baseShellDir, String[] cmd,
                                   Charset outputCharset, String logFilePath)
            throws IOException {
        long startTime = System.currentTimeMillis();
        boolean needReadProcessOutLogStreamByHand = true;
        log.info("【cli】receive new Command. baseDir: {}, cmd: {}, logFile:{}",
                baseShellDir, String.join(" ", cmd), logFilePath);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(baseShellDir));
        initErrorLogHolder(logFilePath, outputCharset);
        int exitCode = 0;
        try {
            if (logFilePath != null) {
                ensureFilePathExists(logFilePath);
//            String redirectLogInfoAndErrCmd = " > " + logFilePath + " 2>&1 ";
//            cmd = mergeTwoArr(cmd, redirectLogInfoAndErrCmd.split("\\s+"));
                pb.redirectErrorStream(true);
                pb.redirectOutput(new File(logFilePath));
                needReadProcessOutLogStreamByHand = false;
            }
            Process p = pb.start();
            if (needReadProcessOutLogStreamByHand) {
                readProcessOutLogStream(p, outputCharset);
            }
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                log.error("进程被中断", e);
                setProcessLastError("中断异常:" + e.getMessage());
            } finally {
                exitCode = p.exitValue();
                log.info("【cli】process costTime:{}ms, exitCode:{}",
                        System.currentTimeMillis() - startTime, exitCode);
            }
            if (exitCode != 0) {
                throw new RuntimeException(
                        "进程返回异常信息, returnCode:" + exitCode
                                + ", lastError:" + getProcessLastError());
            }
            return exitCode;
        } finally {
            removeErrorLogHolder();
        }
    }

    /**
     * 使用 Runtime.exec() 运行shell
     */
    public int runShellWithRuntime(String baseShellDir,
                                   String[] cmd,
                                   Charset outputCharset) throws IOException {
        long startTime = System.currentTimeMillis();
        initErrorLogHolder(null, outputCharset);
        Process p = Runtime.getRuntime().exec(cmd, null, new File(baseShellDir));
        readProcessOutLogStream(p, outputCharset);
        int exitCode;
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            log.error("进程被中断", e);
            setProcessLastError("中断异常:" + e.getMessage());
        } catch (Throwable e) {
            log.error("其他异常", e);
            setProcessLastError(e.getMessage());
        } finally {
            exitCode = p.exitValue();
            log.info("【cli】process costTime:{}ms, exitCode:{}",
                    System.currentTimeMillis() - startTime, exitCode);
        }
        if (exitCode != 0) {
            throw new RuntimeException(
                    "进程返回异常信息, returnCode:" + exitCode
                            + ", lastError:" + getProcessLastError());
        }
        return exitCode;
    }

    /**
     * 确保文件夹存在
     *
     * @param filePath 文件路径
     * @throws IOException 创建文件夹异常抛出
     */
    public void ensureFilePathExists(String filePath) throws IOException {
        File path = new File(filePath);
        if (path.exists()) {
            return;
        }
        File p = path.getParentFile();
        if (p.mkdirs()) {
            log.info("为文件创建目录: {} 成功", p.getPath());
            return;
        }
        log.warn("创建目录:{} 失败", p.getPath());
    }

    /**
     * 合并两个数组数据
     *
     * @param arrFirst  左边数组
     * @param arrAppend 要添加的数组
     * @return 合并后的数组
     */
    public String[] mergeTwoArr(String[] arrFirst, String[] arrAppend) {
        String[] merged = new String[arrFirst.length + arrAppend.length];
        System.arraycopy(arrFirst, 0,
                merged, 0, arrFirst.length);
        System.arraycopy(arrAppend, 0,
                merged, arrFirst.length, arrAppend.length);
        return merged;
    }

    /**
     * 删除以某字符结尾的字符
     *
     * @param originalStr 原始字符
     * @param toTrimChar  要检测的字
     * @return 裁剪后的字符串
     */
    public String trimEndsWith(String originalStr, char toTrimChar) {
        char[] value = originalStr.toCharArray();
        int i = value.length - 1;
        while (i > 0 && value[i] == toTrimChar) {
            i--;
        }
        return new String(value, 0, i + 1);
    }

    /**
     * 错误日志读取线程池（不设上限）
     */
    private final ExecutorService errReadThreadPool = Executors.newCachedThreadPool(
            new NamedThreadFactory("ReadProcessErrOut", true));

    /**
     * 最后一次异常信息
     */
    private final Map<Thread, ProcessErrorLogDescriptor>
            lastErrorHolder = new ConcurrentHashMap<>();

    /**
     * 主动读取进程的标准输出信息日志
     *
     * @param process       进程实体
     * @param outputCharset 日志字符集
     * @throws IOException 读取异常时抛出
     */
    private void readProcessOutLogStream(Process process,
                                         Charset outputCharset) throws IOException {
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                process.getInputStream(), outputCharset))) {
            Thread parentThread = Thread.currentThread();
            // 另起一个线程读取错误消息，必须先启该线程
            errReadThreadPool.submit(() -> {
                try {
                    try (BufferedReader stdError = new BufferedReader(
                            new InputStreamReader(process.getErrorStream(), outputCharset))) {
                        String err;
                        while ((err = stdError.readLine()) != null) {
                            log.info("【cli】{}", err);
                            setProcessLastError(parentThread, err);
                        }
                    }
                } catch (IOException e) {
                    log.error("读取进程错误日志输出时发生了异常", e);
                    setProcessLastError(parentThread, e.getMessage());
                }
            });
            // 外部线程读取标准输出消息
            String stdOut;
            while ((stdOut = stdInput.readLine()) != null) {
                log.info("【cli】{}", stdOut);
            }
        }
    }

    /**
     * 新建一个进程错误信息容器
     *
     * @param logFilePath 日志文件路径,如无则为 null
     */
    private void initErrorLogHolder(String logFilePath, Charset outputCharset) {
        lastErrorHolder.put(Thread.currentThread(),
                new ProcessErrorLogDescriptor(logFilePath, outputCharset));
    }

    /**
     * 移除错误日志监听
     */
    private void removeErrorLogHolder() {
        lastErrorHolder.remove(Thread.currentThread());
    }

    /**
     * 获取进程的最后错误信息
     * <p>
     * 注意: 该方法只会在父线程中调用
     */
    private String getProcessLastError() {
        Thread thread = Thread.currentThread();
        return lastErrorHolder.get(thread).getLastError();
    }

    /**
     * 设置最后一个错误信息描述
     * <p>
     * 使用当前线程或自定义
     */
    private void setProcessLastError(String lastError) {
        lastErrorHolder.get(Thread.currentThread()).setLastError(lastError);
    }

    private void setProcessLastError(Thread thread, String lastError) {
        lastErrorHolder.get(thread).setLastError(lastError);
    }

    /**
     * 判断当前系统是否是 windows
     */
    public boolean isWindowsSystemOs() {
        return System.getProperty("os.name").toLowerCase()
                .startsWith("win");
    }

    /**
     * 进程错误信息描述封装类
     */
    private class ProcessErrorLogDescriptor {

        /**
         * 错误信息记录文件
         */
        private String logFile;

        /**
         * 最后一行错误信息
         */
        private String lastError;
        private Charset charset;

        ProcessErrorLogDescriptor(String logFile, Charset outputCharset) {
            this.logFile = logFile;
            charset = outputCharset;
        }

        String getLastError() {
            if (lastError != null) {
                return lastError;
            }
            try {
                if (logFile == null) {
                    return null;
                }
                List<String> lines = FileUtils.readLines(
                        new File(logFile), charset);
                StringBuilder sb = new StringBuilder();
                for (int i = lines.size() - 1; i >= 0; i--) {
                    sb.insert(0, lines.get(i) + "\n");
                    if (sb.length() > 200) {
                        break;
                    }
                }
                return sb.toString();
            } catch (Exception e) {
                log.error("【cli】读取最后一次错误信息失败", e);
            }
            return null;
        }

        void setLastError(String err) {
            if (lastError == null) {
                lastError = err;
                return;
            }
            lastError = lastError + "\n" + err;
            if (lastError.length() > 200) {
                lastError = lastError.substring(lastError.length() - 200);
            }
        }
    }
}  


