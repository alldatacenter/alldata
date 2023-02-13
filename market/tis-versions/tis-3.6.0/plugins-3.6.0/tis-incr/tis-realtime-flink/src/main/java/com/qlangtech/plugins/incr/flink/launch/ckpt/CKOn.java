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

package com.qlangtech.plugins.incr.flink.launch.ckpt;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.plugins.incr.flink.launch.CheckpointFactory;
import com.qlangtech.plugins.incr.flink.launch.FlinkDescriptor;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.util.OverwriteProps;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.ExecutionCheckpointingOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-01 16:22
 **/
@Public
public class CKOn extends CheckpointFactory {

    //ExecutionCheckpointingOptions.CHECKPOINTING_INTERVAL
    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public Integer ckpointInterval;

    // ExecutionCheckpointingOptions.CHECKPOINTING_MODE
    @FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {Validator.require})
    public String checkpointMode;

    // ExecutionCheckpointingOptions.CHECKPOINTING_TIMEOUT
    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.require, Validator.integer})
    public Integer checkpointTimeout;
    //    ExecutionCheckpointingOptions.MAX_CONCURRENT_CHECKPOINTS;
    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.require, Validator.integer})
    public Integer maxConcurrentNum;

    // ExecutionCheckpointingOptions.MIN_PAUSE_BETWEEN_CHECKPOINTS;
    @FormField(ordinal = 5, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.require, Validator.integer})
    public Integer minPause;

    // ExecutionCheckpointingOptions.TOLERABLE_FAILURE_NUMBER)
    @FormField(ordinal = 6, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.require, Validator.integer})
    public Integer maxFaildNum;

    //  ExecutionCheckpointingOptions.EXTERNALIZED_CHECKPOINT)
    @FormField(ordinal = 7, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public String enableExternal;

    //  ExecutionCheckpointingOptions.ENABLE_UNALIGNED
    @FormField(ordinal = 8, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public Boolean enableUnaligned;
//    ExecutionCheckpointingOptions.CHECKPOINT_ID_OF_IGNORED_IN_FLIGHT_DATA
//            checkpointIdOfIgnoredInFlightData

    //    ExecutionCheckpointingOptions.ALIGNED_CHECKPOINT_TIMEOUT
//    alignedCheckpointTimeout
    // ExecutionCheckpointingOptions.FORCE_UNALIGNED
    @FormField(ordinal = 9, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public Boolean forceUnaligned;

    @Override
    public void setProps(StreamExecutionEnvironment env) {
        env.enableCheckpointing(Duration.ofSeconds(this.ckpointInterval).toMillis());
        CheckpointConfig cpCfg = env.getCheckpointConfig();
        cpCfg.setCheckpointingMode(CheckpointingMode.valueOf(this.checkpointMode));
        cpCfg.setCheckpointTimeout(Duration.ofSeconds(this.checkpointTimeout).toMillis());
        cpCfg.setMaxConcurrentCheckpoints(maxConcurrentNum);
        cpCfg.setMinPauseBetweenCheckpoints(Duration.ofSeconds(minPause).toMillis());
        cpCfg.setTolerableCheckpointFailureNumber(maxFaildNum);
        cpCfg.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.valueOf(this.enableExternal));
        cpCfg.enableUnalignedCheckpoints(this.enableUnaligned);
        cpCfg.setForceUnalignedCheckpoints(forceUnaligned);
//        // 每 ** ms 开始一次 checkpoint
//        env.enableCheckpointing(10*1000);
//        // 设置模式为精确一次 (这是默认值)
//        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.AT_LEAST_ONCE);
//        // 确认 checkpoints 之间的时间会进行 ** ms
//        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
//        // Checkpoint 必须在一分钟内完成，否则就会被抛弃
        //  env.getCheckpointConfig().setCheckpointTimeout(60000);
//        // 同一时间只允许一个 checkpoint 进行
//        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
//        // 开启在 job 中止后仍然保留的 externalized checkpoints
//        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
//        // 允许在有更近 savepoint 时回退到 checkpoint
//        env.getCheckpointConfig().setPreferCheckpointForRecovery(true);

    }

    @TISExtension()
    public static class DefaultDescriptor extends FlinkDescriptor<CheckpointFactory> {

        public DefaultDescriptor() {
            super();


            this.addFieldDescriptor("ckpointInterval"
                    , ExecutionCheckpointingOptions.CHECKPOINTING_INTERVAL, new OverwriteProps().setDftVal(Duration.ofSeconds(200)));
            this.addFieldDescriptor("checkpointMode", ExecutionCheckpointingOptions.CHECKPOINTING_MODE);
            this.addFieldDescriptor("checkpointTimeout", ExecutionCheckpointingOptions.CHECKPOINTING_TIMEOUT);
            this.addFieldDescriptor("maxConcurrentNum", ExecutionCheckpointingOptions.MAX_CONCURRENT_CHECKPOINTS);
            this.addFieldDescriptor("minPause", ExecutionCheckpointingOptions.MIN_PAUSE_BETWEEN_CHECKPOINTS);
            this.addFieldDescriptor("maxFaildNum", ExecutionCheckpointingOptions.TOLERABLE_FAILURE_NUMBER, new OverwriteProps().setDftVal(0));
            this.addFieldDescriptor("enableExternal", ExecutionCheckpointingOptions.EXTERNALIZED_CHECKPOINT
                    , new OverwriteProps().setDftVal(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION));
            this.addFieldDescriptor("enableUnaligned", ExecutionCheckpointingOptions.ENABLE_UNALIGNED);
            this.addFieldDescriptor("forceUnaligned", ExecutionCheckpointingOptions.FORCE_UNALIGNED);
        }

        @Override
        public String getDisplayName() {
            return SWITCH_ON;
        }

        private static final int MIN_INTERVAL = 5;

        public boolean validateCkpointInterval(
                IFieldErrorHandler msgHandler, Context context, String fieldName, String val) {
            int interval = 0;
            try {
                interval = Integer.parseInt(val);
            } catch (Throwable e) {

            }
            if (interval < MIN_INTERVAL) {
                msgHandler.addFieldError(context, fieldName, "不能小于最小值：" + MIN_INTERVAL);
                return false;
            }
            return true;

        }
    }


}
