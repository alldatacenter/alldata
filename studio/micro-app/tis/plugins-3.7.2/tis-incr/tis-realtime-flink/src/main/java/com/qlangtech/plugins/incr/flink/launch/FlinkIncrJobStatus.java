/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.plugins.incr.flink.launch;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.plugins.incr.flink.launch.statbackend.BasicFinkIncrJobStatus;
import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.JobID;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 保存当前增量任务的执行状态
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-15 12:48
 **/
public class FlinkIncrJobStatus extends BasicFinkIncrJobStatus {
    static final String KEY_SAVEPOINT_DISCARD_PREFIX = "discard";
    //    private final File incrJobFile;
//    private JobID jobID;
    private List<FlinkSavepoint> savepointPaths = Lists.newArrayList();
    // 当前job的状态
    // private State state;

    // 已经废弃的savepoint路径集合
    private final Set<String> discardPaths = Sets.newHashSet();

    public Optional<FlinkSavepoint> containSavepoint(String path) {

        for (FlinkSavepoint sp : savepointPaths) {
            if (StringUtils.equals(path, sp.getPath())) {
                return Optional.of(sp);
            }
        }
        return Optional.empty();
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return this.state;
    }

    public List<FlinkSavepoint> getSavepointPaths() {
        return savepointPaths;
    }

    public FlinkIncrJobStatus(File incrJobFile) {
        super(incrJobFile);

        if (!incrJobFile.exists()) {
            state = State.NONE;
            return;
        }
        DftFlinkSavepoint savepoint = null;
        try {
            List<String> lines = FileUtils.readLines(incrJobFile, TisUTF8.get());
            String line = null;
            for (int i = (lines.size() - 1); i >= 0; i--) {
                line = lines.get(i);

                if (StringUtils.startsWith(line, KEY_SAVEPOINT_DISCARD_PREFIX)) {
                    String[] split = StringUtils.split(line, DftFlinkSavepoint.KEY_TUPLE_SPLIT);
                    if (split.length != 2) {
                        throw new IllegalStateException("split length must be 2:" + line);
                    }
                    this.discardPaths.add(split[1]);
                } else if (StringUtils.indexOf(line, KEY_SAVEPOINT_DIR_PREFIX) > -1) {
                    savepoint = DftFlinkSavepoint.deSerialize(line);
                    if (!this.discardPaths.contains(savepoint.getPath())) {
                        // 只有在没有被废弃的情况下才能被加入
                        savepointPaths.add(savepoint);
                    }
                    if (this.state == null) {
                        this.state = savepoint.state;
                    }

                } else if (jobID == null) {
                    // jobId 一定是最后一个读到的
                    jobID = JobID.fromHexString(line);
                    if (state == null) {
                        state = State.RUNNING;
                    }
                }
            }
            if (state == null) {
                // 说明是空文件
                state = State.NONE;
                // throw new IllegalStateException("job state can not be null");
            }
            // Collections.reverse(savepointPaths);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JobID createNewJob(JobID jobID) {

        try {
            FileUtils.writeLines(incrJobFile
                    , Lists.newArrayList("#" + this.getClass().getName(),
                            jobID.toHexString()), false);
            this.savepointPaths = Lists.newArrayList();
            this.state = State.RUNNING;
            this.jobID = jobID;
            return this.jobID;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JSONField(serialize = false)
    public JobID getLaunchJobID() {
        return this.jobID;
    }

    public void stop(String savepointDirectory) {
//        try {
//            DftFlinkSavepoint savepoint = new DftFlinkSavepoint(savepointDirectory);
//            //StringBuffer spInfo = new StringBuffer();
//            //spInfo.append(savepointDirectory).append(";").append(System.currentTimeMillis());
//            FileUtils.writeLines(incrJobFile, Collections.singletonList(savepoint.serialize()), true);
//            this.savepointPaths.add(savepoint);
//            this.state = State.STOPED;
//        } catch (IOException e) {
//            throw new RuntimeException("savepointDirectory:" + savepointDirectory, e);
//        }
        this.addSavePoint(savepointDirectory, State.STOPED);
        this.state = State.STOPED;
    }

    public void addSavePoint(String savepointDirectory, IFlinkIncrJobStatus.State state) {
        try {
            DftFlinkSavepoint savepoint = new DftFlinkSavepoint(savepointDirectory, state);
            //StringBuffer spInfo = new StringBuffer();
            //spInfo.append(savepointDirectory).append(";").append(System.currentTimeMillis());
            FileUtils.writeLines(incrJobFile, Collections.singletonList(savepoint.serialize()), true);
            this.savepointPaths.add(savepoint);
        } catch (IOException e) {
            throw new RuntimeException("savepointDirectory:" + savepointDirectory, e);
        }
    }

    public void relaunch(JobID jobID) {
        try {
            FileUtils.writeLines(incrJobFile, Collections.singletonList(jobID.toHexString()), true);
            this.state = State.RUNNING;
            this.jobID = jobID;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancel() {
        super.cancel();
        this.savepointPaths = Lists.newArrayList();
    }

    public void discardSavepoint(String savepointDirectory) {
        if (StringUtils.isEmpty(savepointDirectory)) {
            throw new IllegalArgumentException("param savepointDirectory can not be null");
        }
        if (this.state != State.RUNNING) {
            throw new IllegalStateException("current stat must be running ,but now is:" + this.state);
        }
        try {
            FlinkSavepoint path = null;
            FileUtils.writeLines(incrJobFile
                    , Collections.singletonList(KEY_SAVEPOINT_DISCARD_PREFIX + DftFlinkSavepoint.KEY_TUPLE_SPLIT + savepointDirectory), true);
            this.discardPaths.add(savepointDirectory);
            Iterator<FlinkSavepoint> pats = savepointPaths.iterator();

            while (pats.hasNext()) {
                path = pats.next();
                if (StringUtils.equals(path.getPath(), savepointDirectory)) {
                    pats.remove();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static class DftFlinkSavepoint extends FlinkSavepoint {
        private final static String KEY_TUPLE_SPLIT = ";";
        private final IFlinkIncrJobStatus.State state;

        public DftFlinkSavepoint(String path, IFlinkIncrJobStatus.State state) {
            this(path, System.currentTimeMillis(), state);
        }


        public DftFlinkSavepoint(String path, long createTimestamp, IFlinkIncrJobStatus.State state) {
            super(path, createTimestamp);
            this.state = state;
        }

        public String serialize() {
            StringBuffer spInfo = new StringBuffer();
            spInfo.append(this.getPath()).append(KEY_TUPLE_SPLIT).append(this.getCreateTimestamp()).append(KEY_TUPLE_SPLIT).append(this.state);
            return String.valueOf(spInfo);
        }

        public static DftFlinkSavepoint deSerialize(String seri) {
            String[] tuple = StringUtils.split(seri, KEY_TUPLE_SPLIT);
            if (tuple.length != 3) {
                throw new IllegalStateException("param is not illegal:" + seri);
            }
            return new DftFlinkSavepoint(tuple[0], Long.parseLong(tuple[1]), IFlinkIncrJobStatus.State.valueOf(tuple[2]));
        }
    }

}
