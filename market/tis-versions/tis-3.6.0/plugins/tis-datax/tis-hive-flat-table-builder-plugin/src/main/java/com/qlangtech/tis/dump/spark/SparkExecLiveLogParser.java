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

package com.qlangtech.tis.dump.spark;

import com.qlangtech.tis.dump.IExecLiveLogParser;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.fullbuild.phasestatus.JobLog;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: baisui 百岁
 * @create: 2020-06-16 14:23
 **/
public class SparkExecLiveLogParser implements IExecLiveLogParser {

    private static final String STATUS_PREFIX = "\u0001";

    private static final Pattern RUNNING_STATUS = Pattern.compile("\\u0001jobid:(\\d+?),stageid:(\\d+?),alltask:(\\d+?),complete:(\\d+?),percent:(\\d+?)");
    private static final Pattern COMPLETE_STATUS = Pattern.compile("\\u0001complete jobid:(\\d+?),state:(.+?)");
    private static final Pattern START_STATUS = Pattern.compile("\\u0001start jobid:(\\d+?)");


    private static final String EXEC_RESULT_SUCCESS = "success";
    private static final String EXEC_RESULT_FAILD = "faild";

    private final IJoinTaskStatus joinTaskStatus;
    private boolean execOver = false;

    public SparkExecLiveLogParser(IJoinTaskStatus joinTaskStatus) {
        this.joinTaskStatus = joinTaskStatus;
    }

    @Override
    public void process(String log) {
        if (!StringUtils.startsWith(log, STATUS_PREFIX)) {
            return;
        }
        Integer jobId = null;
        Matcher matcher = RUNNING_STATUS.matcher(log);
        Integer percent = null;
        if (matcher.matches()) {
            jobId = Integer.parseInt(matcher.group(1));
            percent = Integer.parseInt(matcher.group(5));
            JobLog job = joinTaskStatus.getJoblog(jobId);
            if (job == null) {
                return;
            }
            job.setWaiting(false);
            job.setReducer(percent);
            job.setMapper(percent);
            return;
        }
        matcher = START_STATUS.matcher(log);
        if (matcher.matches()) {
            jobId = Integer.parseInt(matcher.group(1));
            joinTaskStatus.createJobStatus(jobId);
            joinTaskStatus.setStart();
            return;
        }

        matcher = COMPLETE_STATUS.matcher(log);
        if (matcher.matches()) {
            this.execOver = true;
            jobId = Integer.parseInt(matcher.group(1));
            String result = matcher.group(2);
            joinTaskStatus.setComplete(true);
            joinTaskStatus.setFaild(!EXEC_RESULT_SUCCESS.equals(result));
            return;
        }

    }

    @Override
    public boolean isExecOver() {
        return this.execOver;
    }
}
