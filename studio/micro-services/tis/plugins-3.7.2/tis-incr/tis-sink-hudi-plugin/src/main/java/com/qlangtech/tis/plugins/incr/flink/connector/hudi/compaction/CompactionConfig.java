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

package com.qlangtech.tis.plugins.incr.flink.connector.hudi.compaction;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.plugins.incr.flink.launch.FlinkDescriptor;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.hudi.configuration.FlinkOptions;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-09 17:22
 * @see org.apache.hudi.util.StreamerUtil
 **/
public class CompactionConfig implements Describable<CompactionConfig> {

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String payloadClass;

    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.require, Validator.integer})
    public Integer targetIOPerInMB;

    @FormField(ordinal = 3, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public String triggerStrategy;

    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.integer, Validator.require})
    public Integer maxNumDeltaCommitsBefore;

    @FormField(ordinal = 5, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.integer, Validator.require})
    public Integer maxDeltaSecondsBefore;

    @FormField(ordinal = 6, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public Boolean asyncClean;

    @FormField(ordinal = 7, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.integer, Validator.require})
    public Integer retainCommits;

    @FormField(ordinal = 8, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.integer, Validator.require})
    public Integer archiveMinCommits;

    @FormField(ordinal = 9, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.integer, Validator.require})
    public Integer archiveMaxCommits;

//            HoodieCompactionConfig.newBuilder()
//            .withPayloadClass(conf.getString(FlinkOptions.PAYLOAD_CLASS_NAME))
//            .withTargetIOPerCompactionInMB(conf.getLong(FlinkOptions.COMPACTION_TARGET_IO))
//            .withInlineCompactionTriggerStrategy(
//            CompactionTriggerStrategy.valueOf(conf.getString(FlinkOptions.COMPACTION_TRIGGER_STRATEGY).toUpperCase(Locale.ROOT)))
//            .withMaxNumDeltaCommitsBeforeCompaction(conf.getInteger(FlinkOptions.COMPACTION_DELTA_COMMITS))
//            .withMaxDeltaSecondsBeforeCompaction(conf.getInteger(FlinkOptions.COMPACTION_DELTA_SECONDS))
//            .withAsyncClean(conf.getBoolean(FlinkOptions.CLEAN_ASYNC_ENABLED))
//            .retainCommits(conf.getInteger(FlinkOptions.CLEAN_RETAIN_COMMITS))
//            // override and hardcode to 20,
//            // actually Flink cleaning is always with parallelism 1 now
//            .withCleanerParallelism(20)
//                    .archiveCommitsWith(conf.getInteger(FlinkOptions.ARCHIVE_MIN_COMMITS), conf.getInteger(FlinkOptions.ARCHIVE_MAX_COMMITS))
//            .withCleanerPolicy(HoodieCleaningPolicy.KEEP_LATEST_COMMITS)
//                    .build()


    private static final String KEY_archiveMinCommits = "archiveMinCommits";
    private static final String KEY_archiveMaxCommits = "archiveMaxCommits";

    @TISExtension
    public static class DefaultDescriptor extends FlinkDescriptor<CompactionConfig> {
        public DefaultDescriptor() {
            addFieldDescriptor("payloadClass", FlinkOptions.PAYLOAD_CLASS_NAME);
            addFieldDescriptor("targetIOPerInMB", FlinkOptions.COMPACTION_TARGET_IO);
            addFieldDescriptor("triggerStrategy", FlinkOptions.COMPACTION_TRIGGER_STRATEGY);
            addFieldDescriptor("maxNumDeltaCommitsBefore", FlinkOptions.COMPACTION_DELTA_COMMITS);
            addFieldDescriptor("maxDeltaSecondsBefore", FlinkOptions.COMPACTION_DELTA_SECONDS);
            addFieldDescriptor("asyncClean", FlinkOptions.CLEAN_ASYNC_ENABLED);
            addFieldDescriptor("retainCommits", FlinkOptions.CLEAN_RETAIN_COMMITS);
            addFieldDescriptor(KEY_archiveMinCommits, FlinkOptions.ARCHIVE_MIN_COMMITS);
            addFieldDescriptor(KEY_archiveMaxCommits, FlinkOptions.ARCHIVE_MAX_COMMITS);
        }

        @Override
        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            //return super.validateAll(msgHandler, context, postFormVals);

            int minCommit = Integer.parseInt(postFormVals.getField(KEY_archiveMinCommits));
            int maxCommit = Integer.parseInt(postFormVals.getField(KEY_archiveMaxCommits));
            if (maxCommit < minCommit) {
                msgHandler.addFieldError(context, KEY_archiveMaxCommits, "不能小于'" + minCommit + "'");
                msgHandler.addFieldError(context, KEY_archiveMinCommits, "不能大于'" + maxCommit + "'");
                return false;
            }
            return true;
        }

        @Override
        public String getDisplayName() {
            return "default";
        }
    }


}
