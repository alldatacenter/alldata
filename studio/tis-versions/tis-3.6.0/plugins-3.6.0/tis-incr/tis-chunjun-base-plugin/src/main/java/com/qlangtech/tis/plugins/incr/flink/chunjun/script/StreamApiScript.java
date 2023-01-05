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

package com.qlangtech.tis.plugins.incr.flink.chunjun.script;

import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugins.incr.flink.connector.streamscript.BasicFlinkStreamScriptCreator;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-11-14 12:40
 **/
public class StreamApiScript extends ChunjunStreamScriptType {
    @Override
    public BasicFlinkStreamScriptCreator createStreamTableCreator(
            IStreamTableMeataCreator.ISinkStreamMetaCreator sinkStreamMetaCreator) {
        return new StreamAPIStreamScriptCreator(sinkStreamMetaCreator);
    }

    static class StreamAPIStreamScriptCreator extends BasicFlinkStreamScriptCreator {

        public StreamAPIStreamScriptCreator(IStreamTableMeataCreator.ISinkStreamMetaCreator sinkStreamMetaGetter) {
            super(sinkStreamMetaGetter);
        }
        @Override
        public IStreamTemplateResource getFlinkStreamGenerateTplResource() {
            return IStreamTemplateResource.createClasspathResource("flink_source_handle_rowdata_scala.vm", true);
        }
        @Override
        public IStreamTemplateData decorateMergeData(IStreamTemplateData mergeData) {
            return mergeData;
        }
    }

    @TISExtension
    public static class DefaultDescriptor extends Descriptor<ChunjunStreamScriptType> {
        @Override
        public String getDisplayName() {
            return "StreamAPI";
        }
    }
}
