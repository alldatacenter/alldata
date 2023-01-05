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

package com.qlangtech.plugins.incr.flink.launch.statbackend;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.plugins.incr.flink.launch.FlinkDescriptor;
import com.qlangtech.plugins.incr.flink.launch.StateBackendFactory;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.util.OverwriteProps;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.IllegalConfigurationException;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackendFactory;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-11 21:52
 **/
@Public
public class FileSystemState extends StateBackendFactory implements StateBackendFactory.ISavePointSupport {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemState.class);

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String checkpointDir;

    @FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {Validator.require})
    public Boolean enableSavePoint;

    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, advance = true,validate = {Validator.integer, Validator.require})
    public Integer smallFileThreshold;

    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, advance = true, validate = {Validator.integer, Validator.require})
    public Integer writeBufferSize;

    @Override
    public void setProps(StreamExecutionEnvironment env) {
        env.setStateBackend(createFsStateBackend((config) -> {
            config.setString(CheckpointingOptions.CHECKPOINTS_DIRECTORY, this.checkpointDir);
            config.set(CheckpointingOptions.FS_SMALL_FILE_THRESHOLD
                    , MemorySize.parse(String.valueOf(smallFileThreshold), MemorySize.MemoryUnit.KILO_BYTES));
            config.setInteger(CheckpointingOptions.FS_WRITE_BUFFER_SIZE, this.writeBufferSize);
        }));
    }

    @Override
    public boolean supportSavePoint() {
        return this.enableSavePoint;
    }

    @Override
    public String getSavePointRootPath() {
        return this.checkpointDir;
    }

    @TISExtension()
    public static class DefaultDescriptor extends FlinkDescriptor<StateBackendFactory> {

        public DefaultDescriptor() {
            super();
            this.addFieldDescriptor("checkpointDir", CheckpointingOptions.CHECKPOINTS_DIRECTORY
                    , (new OverwriteProps())
                            .setAppendHelper("The scheme (hdfs://, file://, etc) is null. Please specify the file system scheme explicitly in the URI.")
                            .setDftVal("file:///opt/data/savepoint"));
            this.addFieldDescriptor("smallFileThreshold", CheckpointingOptions.FS_SMALL_FILE_THRESHOLD);
            this.addFieldDescriptor("writeBufferSize", CheckpointingOptions.FS_WRITE_BUFFER_SIZE);
        }

        public boolean validateCheckpointDir(IFieldErrorHandler msgHandler, Context context, String fieldName, final String val) {
            try {
                createFsStateBackend((cfg) -> {
                    cfg.setString(CheckpointingOptions.CHECKPOINTS_DIRECTORY, val);
                });
            } catch (IllegalConfigurationException e) {
                logger.warn(e.getMessage(), e);
                msgHandler.addFieldError(context, fieldName, e.getCause().getMessage());
                return false;
            }

            return true;
        }

        @Override
        public String getDisplayName() {
            return "FSState";
        }
    }

    private static StateBackend createFsStateBackend(Consumer<Configuration> cfgSetter) throws IllegalConfigurationException {
        FsStateBackendFactory factory = new FsStateBackendFactory();
        Configuration config = new Configuration();
        cfgSetter.accept(config);
        return factory.createFromConfig(config, FileSystemState.class.getClassLoader());
    }

}
