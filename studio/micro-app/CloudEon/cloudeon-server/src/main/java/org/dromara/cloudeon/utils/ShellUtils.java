package org.dromara.cloudeon.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShellUtils {


    private Logger logger;


    public Process exec(List<String> command) {
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String[] commands = new String[command.size()];
            command.toArray(commands);
            processBuilder.command(commands);
            process = processBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return process;
    }

    /**
     * @param pathOrCommand 脚本路径或者命令
     * @return
     */
    public ExecResult exceShell(String pathOrCommand) {
        ExecResult result = new ExecResult();
        StringBuffer stringBuffer = new StringBuffer();
        try {
            // 执行脚本
            Process ps = Runtime.getRuntime().exec(new String[]{"sh", "-c", pathOrCommand});
            int exitValue = ps.waitFor();
            if (0 == exitValue) {
                // 只能接收脚本echo打印的数据，并且是echo打印的最后一次数据
                BufferedInputStream in = new BufferedInputStream(ps.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null) {
                    logger.info("脚本返回的数据如下：{}", line);
                    stringBuffer.append(line);
                }
                in.close();
                br.close();
                result.setExecResult(true);
                result.setExecOut(stringBuffer.toString());
            } else {
                result.setExecOut("call shell failed. error code is :" + exitValue);
            }

        } catch (Exception e) {
            result.setExecOut(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    // 获取cpu架构 arm或x86
    public String getCpuArchitecture() {
        try {
            Process ps = Runtime.getRuntime().exec("arch");
            StringBuffer stringBuffer = new StringBuffer();
            int exitValue = ps.waitFor();
            if (0 == exitValue) {
                // 只能接收脚本echo打印的数据，并且是echo打印的最后一次数据
                BufferedInputStream in = new BufferedInputStream(ps.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null) {
                    logger.info("脚本返回的数据如下： " + line);
                    stringBuffer.append(line);
                }
                in.close();
                br.close();
                return stringBuffer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 获取cpu架构 arm或x86
    public String getPackageMd5(String md5Cmd) {
        try {
            Process ps = Runtime.getRuntime().exec(new String[]{"sh", "-c", md5Cmd});
            StringBuffer stringBuffer = new StringBuffer();
            int exitValue = ps.waitFor();
            if (0 == exitValue) {
                // 只能接收脚本echo打印的数据，并且是echo打印的最后一次数据
                BufferedInputStream in = new BufferedInputStream(ps.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null) {
                    logger.info("脚本返回的数据如下： " + line);
                    stringBuffer.append(line);
                }
                in.close();
                br.close();
                return stringBuffer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public ExecResult execWithStatus(String workPath, List<String> command, long timeout) {
        Process process = null;
        ExecResult result = new ExecResult();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(new File(workPath));
            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            // 处理输出
            dealOutput(process);
            getError(process);
            boolean execResult = process.waitFor(timeout, TimeUnit.SECONDS);
            if (execResult && process.exitValue() == 0) {
                logger.info("script execute success");
                result.setExecResult(true);
                result.setExecOut("script execute success");
            } else {
                result.setExecOut("script execute failed");
            }
            return result;
        } catch (Exception e) {
            result.setExecErrOut(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }


    public void dealOutput(Process process) {


        ExecutorService getOutputLogService = Executors.newSingleThreadExecutor();

        getOutputLogService.submit(() -> {
            BufferedReader inReader = null;
            try {
                inReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = inReader.readLine()) != null) {
                    logger.info(line);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                closeQuietly(inReader);
            }
        });
        getOutputLogService.shutdown();
    }


    public void getError(Process process) {


        ExecutorService getOutputLogService = Executors.newSingleThreadExecutor();
        getOutputLogService.submit(() -> {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuffer stringBuffer = new StringBuffer();
                String line;
                while ((line = reader.readLine())!=null) {
                    logger.error(line);
                    stringBuffer.append("\n" + line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeQuietly(reader);
            }
        });

    }


    public void closeQuietly(Reader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    public void destroy(Process process) {
        if (process != null) {
            process.destroyForcibly();
        }
    }

//    public  void addChmod(String path, String chmod) {
//        ArrayList<String> command = new ArrayList<>();
//        command.add("chmod");
//        command.add("-R");
//        command.add(chmod);
//        command.add(path);
//        execWithStatus(Constants.INSTALL_PATH,command,60);
//    }
}
