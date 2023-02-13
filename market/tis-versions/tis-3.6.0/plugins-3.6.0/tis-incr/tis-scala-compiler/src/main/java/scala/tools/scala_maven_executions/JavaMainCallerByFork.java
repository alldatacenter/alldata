/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package scala.tools.scala_maven_executions;

import org.apache.commons.exec.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * forked java commands.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class JavaMainCallerByFork extends JavaMainCallerSupport {

    private final boolean _forceUseArgFile = false;

    private static final Logger logger = LoggerFactory.getLogger(JavaMainCallerByFork.class);

    /**
     * Location of java executable.
     */
    private final String _javaExec = "java";

    private final boolean _redirectToLog = true;

    private final LogProcessorUtils.LoggerListener loggerListener;

    public JavaMainCallerByFork(String mainClassName1, String classpath, String[] jvmArgs1, String[] args1, LogProcessorUtils.LoggerListener loggerListener) throws Exception {
        super(mainClassName1, classpath, jvmArgs1, args1);
        for (String key : System.getenv().keySet()) {
            env.add(key + "=" + System.getenv(key));
        }
        if (loggerListener == null) {
            throw new IllegalArgumentException("argument loggerListener can not be null");
        }
        this.loggerListener = loggerListener;
    // _forceUseArgFile = forceUseArgFile;
    }

    @Override
    public boolean run(boolean displayCmd, boolean throwFailure) throws Exception {
        List<String> cmd = buildCommand();
        displayCmd(displayCmd, cmd);
        Executor exec = new DefaultExecutor();
        // err and out are redirected to out
        if (!_redirectToLog) {
            exec.setStreamHandler(new PumpStreamHandler(System.out, System.err, System.in));
        } else {
            exec.setStreamHandler(new PumpStreamHandler(new LogOutputStream() {

                private LogProcessorUtils.LevelState _previous = new LogProcessorUtils.LevelState();

                @Override
                protected void processLine(String line, int level) {
                    try {
                        _previous = LogProcessorUtils.levelStateOf(line, _previous);
                        switch(_previous.level) {
                            case ERROR:
                                logger.error(line);
                                loggerListener.receiveLog(LogProcessorUtils.Level.ERROR, line);
                                break;
                            case WARNING:
                                logger.warn(line);
                                loggerListener.receiveLog(LogProcessorUtils.Level.WARNING, line);
                                break;
                            default:
                                logger.info(line);
                                loggerListener.receiveLog(LogProcessorUtils.Level.INFO, line);
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }));
        }
        CommandLine cl = new CommandLine(cmd.get(0));
        for (int i = 1; i < cmd.size(); i++) {
            cl.addArgument(cmd.get(i), false);
        }
        try {
            int exitValue = exec.execute(cl);
            if (exitValue != 0) {
                if (throwFailure) {
                    throw new RuntimeException("command line returned non-zero value:" + exitValue);
                }
                return false;
            }
            if (!displayCmd)
                tryDeleteArgFile(cmd);
            return true;
        } catch (ExecuteException exc) {
            if (throwFailure) {
                throw exc;
            }
            return false;
        }
    }

    public SpawnMonitor spawn(boolean displayCmd) throws Exception {
        List<String> cmd = buildCommand();
        File out = new File(System.getProperty("java.io.tmpdir"), mainClassName + ".out");
        out.delete();
        cmd.add(">" + out.getCanonicalPath());
        File err = new File(System.getProperty("java.io.tmpdir"), mainClassName + ".err");
        err.delete();
        cmd.add("2>" + err.getCanonicalPath());
        List<String> cmd2 = new ArrayList<>();
        if (OS.isFamilyDOS()) {
            cmd2.add("cmd.exe");
            cmd2.add("/C");
            cmd2.addAll(cmd);
        } else {
            cmd2.add("/bin/sh");
            cmd2.add("-c");
            cmd2.addAll(cmd);
        }
        displayCmd(displayCmd, cmd2);
        ProcessBuilder pb = new ProcessBuilder(cmd2);
        // pb.redirectErrorStream(true);
        final Process p = pb.start();
        return () -> {
            try {
                p.exitValue();
                return false;
            } catch (IllegalThreadStateException e) {
                return true;
            }
        };
    }

    private void displayCmd(boolean displayCmd, List<String> cmd) {
        if (displayCmd) {
            logger.info("cmd: " + " " + StringUtils.join(cmd.iterator(), " "));
        } else if (logger.isDebugEnabled()) {
            logger.debug("cmd: " + " " + StringUtils.join(cmd.iterator(), " "));
        }
    }

    private List<String> buildCommand() throws Exception {
        ArrayList<String> back = new ArrayList<>(2 + jvmArgs.size() + args.size());
        back.add(_javaExec);
        if (// && (lengthOf(args, 1) + lengthOf(jvmArgs, 1) < 400)
        !_forceUseArgFile) {
            back.addAll(jvmArgs);
            back.add(mainClassName);
            back.addAll(args);
        } else {
            File jarPath = new File(MainHelper.locateJar(MainHelper.class));
            logger.debug("plugin jar to add :" + jarPath);
            addToClasspath(jarPath);
            back.addAll(jvmArgs);
            back.add(MainWithArgsInFile.class.getName());
            back.add(mainClassName);
            back.add(MainHelper.createArgFile(args).getCanonicalPath());
        }
        return back;
    }

    private void tryDeleteArgFile(List<String> cmd) {
        String last = cmd.get(cmd.size() - 1);
        if (last.endsWith(MainHelper.argFileSuffix)) {
            File f = new File(last);
            if (f.exists() && f.getName().startsWith(MainHelper.argFilePrefix)) {
                f.delete();
            }
        }
    }

    private long lengthOf(List<String> l, long sepLength) {
        long back = 0;
        for (String str : l) {
            back += str.length() + sepLength;
        }
        return back;
    }

    @Override
    public void redirectToLog() {
    // _redirectToLog = true;
    }
}
