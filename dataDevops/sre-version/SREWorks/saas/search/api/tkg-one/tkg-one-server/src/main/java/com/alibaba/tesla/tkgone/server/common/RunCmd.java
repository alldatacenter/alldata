package com.alibaba.tesla.tkgone.server.common;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjinghua
 */

@Slf4j
public class RunCmd {

    private Process process;
    private BufferedReader stdoutBufferedReader;
    private BufferedReader stderrBufferedReader;
    private long startTime;
    private int timeout;
    private int status = 0;
    private StringBuilder stderr = new StringBuilder();
    private Boolean end = false;
    private int getStdoutTimes = 0;

    public void start(String command, String workDir, String[] envp, int timeout) throws Exception {

        this.timeout = timeout;
        startTime = System.currentTimeMillis() / 1000;
        Runtime runtime = Runtime.getRuntime();
        process = runtime.exec(command, envp, new File(workDir));
        stdoutBufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        stderrBufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

    }

    public Boolean checkEnd() {
        if (timeout > 0 && System.currentTimeMillis() / 1000 - startTime > timeout) {
            status = 2;
            stderr.append("命令执行超时退出");
            process.destroy();
            end = true;
        }
        if (!process.isAlive()) {
            status = process.exitValue();
            end = true;
        }
        return end;
    }

    public void waitForEnd() {
        while (!checkEnd()) {
            Tools.sleepMilli(100);
        }
    }

    public String getStdout() throws IOException {
        while (true) {
            if (stdoutBufferedReader.ready()) {
                String line = stdoutBufferedReader.readLine();
                getStdoutTimes++;
                return line == null ? "" : line;
            }
            if (checkEnd()) {
                log.info(String.format("[GET_STDOUT] already get all stdout[%s]", getStdoutTimes));
                return null;
            }
            Tools.sleepMilli(10);
        }
    }

    public String getStderr() throws IOException {
        while (stderrBufferedReader.ready()) {
            stderr.append(stderrBufferedReader.readLine()).append("\n");
        }
        return stderr.toString();
    }

    public List<String> getStdouts(long size) throws Exception {
        List<String> lines = new ArrayList<>();
        while (lines.size() < size) {
            String line = getStdout();
            if (line == null) {
                return lines;
            } else {
                lines.add(line);
            }
        }
        return lines;
    }

    public int getStatus() {
        return status;
    }
}

