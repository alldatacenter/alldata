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
package com.qlangtech.tis.dump.hive;

import com.qlangtech.tis.dump.IExecLiveLogParser;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.fullbuild.phasestatus.JobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 对应一个Hive SQL的执行
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月26日
 */
public class HiveExecLiveLogParser implements IExecLiveLogParser {

    private static final Pattern START_QUERY = Pattern.compile("Compiling\\s{1}command\\(queryId=(.+)\\)");

    private static final Pattern END_QUERY = Pattern.compile("Completed\\s{1}executing\\s{1}command\\(queryId=(.+)\\)");

    private static final Pattern TOTAL_JOB = Pattern.compile("Total\\sjobs\\s=\\s(\\d+)");

    private static final Pattern LAUNCH_JOB = Pattern.compile("Launching\\sJob\\s(\\d+)\\sout\\sof\\s(\\d+)");

    private static final Pattern START_TASK = Pattern.compile("Starting\\stask\\s\\[Stage-\\d+:MAPRED\\]\\sin\\sserial\\smode");

    private static final Pattern END_TASK = Pattern.compile("MapReduce\\sTotal\\scumulative\\sCPU\\stime");

    private static final Pattern PROCESSING_TASK = Pattern.compile("Stage-\\d+\\smap\\s=\\s(\\d+)%,\\s+reduce\\s+=\\s+(\\d+)%,\\s+Cumulative");

    private static final Logger logger = LoggerFactory.getLogger(HiveExecLiveLogParser.class);

    private int jobCount;

    private String queryId;

    //
    private JobLog currentJobLog;

    private final List<TokenProcess> tokens = new ArrayList<>();

    // private boolean queryStart = false;
    // 日志执行完成了
    private boolean execOver;

    private boolean startprocess = false;

    private final IJoinTaskStatus joinTaskStatus;

    /**
     * Demo:<br>
     * / 看看是否可以用递归下降法来解决这个问题：<br>
     * / http://www.cnblogs.com/Ninputer/archive/2011/06/21/2085527.html<br>
     * / Start:<br>
     * /----06-16 19:45 INFO : Compiling
     * command(queryId=hive_20170616194545_0be2a914-b46f-40ad-b184-f02a8f7f1c98)
     * : <br>
     * //----06-04 22:35 INFO : Total jobs = 3<br>
     * //-------------Launching Job 1 out of 3<br>
     * //-------------Starting task [Stage-1:MAPRED] in serial mode<br>
     * //-------------2017-06-16 19:45:28,369 Stage-1 map = 63%, reduce = 0%,
     * Cumulative CPU 30.0 sec <br>
     * //-------------Launching Job 2 out of 3<br>
     * //----06-16 19:54 INFO : Completed executing
     * command(queryId=hive_20170616195252_e60225fe-ce09-47a9-91cb-a256fc255fe4)
     * ; Time taken: 104.997 seconds<br>
     */
    public HiveExecLiveLogParser(IJoinTaskStatus joinTaskStatus) {
        this.joinTaskStatus = joinTaskStatus;
        this.tokens.add(new TokenProcess(START_QUERY) {
            @Override
            public void process(Matcher m) {
                queryId = m.group(1);
            }
        });
        this.tokens.add(new TokenProcess(END_QUERY) {

            @Override
            public void process(Matcher m) {
                execOver = true;
                joinTaskStatus.setStart();
                joinTaskStatus.setComplete(true);
                joinTaskStatus.setFaild(false);
                joinTaskStatus.jobsLog().forEach((log) -> {
                    log.setMapper(100);
                    log.setReducer(100);
                    log.setWaiting(false);
                });
            }
        });
        this.tokens.add(new TokenProcess(TOTAL_JOB) {
            @Override
            public void process(Matcher m) {
                jobCount = Integer.parseInt(m.group(1));
                for (int i = 0; i < jobCount; i++) {
                    joinTaskStatus.createJobStatus(i);
                }
                currentJobLog = null;
                startprocess = false;
            }
        });
        this.tokens.add(new TokenProcess(LAUNCH_JOB) {
            @Override
            public void process(Matcher m) {
                // if (!queryStart) {
                // throw new IllegalStateException("has not pre match query
                // start");
                // }
                // 從0開始
                int jobIndex = Integer.parseInt(m.group(1)) - 1;
                JobLog jobLog = joinTaskStatus.getJoblog(jobIndex);
                jobLog.setWaiting(false);
                if (currentJobLog != null) {
                    currentJobLog.setMapper(100);
                    currentJobLog.setReducer(100);
                }
                currentJobLog = jobLog;
            }
        });
        this.tokens.add(new TokenProcess(START_TASK) {
            @Override
            public void process(Matcher m) {
                joinTaskStatus.setStart();
                startprocess = true;
            }
        });
        this.tokens.add(new TokenProcess(END_TASK) {
            @Override
            public void process(Matcher m) {
                if (currentJobLog == null) {
                    return;
                }
                currentJobLog.setMapper(100);
                currentJobLog.setReducer(100);
            }
        });
        this.tokens.add(new TokenProcess(PROCESSING_TASK) {

            @Override
            public void process(Matcher m) {
                if (!startprocess && currentJobLog == null) {
                    return;
                }
                currentJobLog.setMapper(Integer.parseInt(m.group(1)));
                currentJobLog.setReducer(Integer.parseInt(m.group(2)));
            }
        });
    }

    /**
     * 处理日志
     *
     * @param log
     * @return 是否处理完成 ，如果处理完成就没有必要再处理了
     */
    @Override
    public void process(String log) {
        // System.out.println(log);
        Matcher matcher = null;
        for (TokenProcess t : tokens) {
            matcher = t.pattern.matcher(log);
            if (matcher.find()) {
                logger.info("'" + log + "' match pattern:" + t.pattern);
                t.process(matcher);
                return;
            }
        }
    // return execOver;
    }

    /**
     * 是否执行完成
     *
     * @return
     */
    @Override
    public boolean isExecOver() {
        return this.execOver;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Matcher m = END_QUERY.matcher("06-16 19:54 INFO  : Completed executing command(queryId=hive_20170616195252_e60225fe-ce09-47a9-91cb-a256fc255fe4); Time taken: 104.997 seconds");
        if (m.find()) {
            System.out.println(m.group(1));
        }
        m = TOTAL_JOB.matcher("Total jobs = 3");
        if (m.find()) {
            System.out.println(m.group(1));
        }
        m = LAUNCH_JOB.matcher("Launching Job 1 out of 3");
        if (m.find()) {
            System.out.println(m.group(1) + " " + m.group(2));
        }
        m = START_TASK.matcher("Starting task [Stage-1:MAPRED] in serial mode");
        if (m.find()) {
            System.out.println(m.group());
        }
        m = PROCESSING_TASK.matcher("2017-06-16 19:45:28,369 Stage-1 map = 63%,  reduce = 0%, Cumulative CPU 30.0 sec");
        if (m.find()) {
            System.out.println(m.group(1) + " " + m.group(2));
        } else {
            System.out.println("not match");
        }
    }

    private class TokenProcess {

        private final Pattern pattern;

        // private boolean processed;
        public TokenProcess(Pattern pattern) {
            super();
            this.pattern = pattern;
        }

        public void process(Matcher m) {
        }
    }
}
