package com.alibaba.sreworks.job.worker.taskhandlers;

import com.alibaba.sreworks.job.utils.BeansUtil;
import com.alibaba.sreworks.job.worker.services.ElasticTaskInstanceService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class AbstractScriptTaskHandler extends AbstractTaskHandler {

    public abstract String getCmd();

    public abstract String getSuffix();

    private String scriptPath;

    private String varConfPath;

    private Process process = null;

    public void initFile() throws Exception {
        // init root dir
        String rootDir = System.getProperty("user.home") + "/sreworks-job-worker/task/" + getTaskInstance().getId();
        FileUtils.deleteDirectory(new File(rootDir));
        FileUtils.forceMkdir(new File(rootDir));
        // init script file
        setScriptPath(String.format("%s/script%s", rootDir, getSuffix()));
        FileUtils.write(new File(getScriptPath()), getTaskInstance().getExecContent());
        // init varConf file
        setVarConfPath(String.format("%s/varConf", rootDir));
        FileUtils.write(
            new File(getVarConfPath()),
            getTaskInstance().getVarConf()
        );
    }

    @Override
    public void execute() throws Exception {

        initFile();
        execCmd();
        getTaskInstance().setOutVarConf(
            FileUtils.readFileToString(new File(getVarConfPath()))
        );

    }

    @Override
    public void destroy() {
        if (process != null) {
            process.destroyForcibly();
        }
    }

    private void execCmd() throws Exception {

        String id = getTaskInstance().getId();
        process = Runtime.getRuntime().exec(getCmd(), new String[] {"varConfPath=" + getVarConfPath()});
        Thread stdoutThread = new Thread(() -> {
            try {
                append(id, process.getInputStream(), "stdout", new byte[1024]);
            } catch (IOException e) {
                log.error("", e);
            }
        });
        Thread stderrThread = new Thread(() -> {
            try {
                append(id, process.getErrorStream(), "stderr", new byte[1024]);
            } catch (IOException e) {
                log.error("", e);
            }
        });
        stdoutThread.start();
        stderrThread.start();
        int exitValue = process.waitFor();
        stdoutThread.join();
        stderrThread.join();
        if (exitValue != 0) {
            throw new Exception("EXECUTE EXCEPTION");
        }

    }

    private void append(String id, InputStream inputStream, String key, byte[] buffer) throws IOException {

        try (inputStream) {
            while (true) {
                int res = inputStream.read(buffer);
                if (res == -1) {
                    break;
                }
                if (res > 0) {
                    log.info("task instance id: " + id);
                    BeansUtil.context.getBean(ElasticTaskInstanceService.class)
                        .appendString(id, key, new String(buffer, 0, res, StandardCharsets.UTF_8));
                }
            }
        }

    }

}
