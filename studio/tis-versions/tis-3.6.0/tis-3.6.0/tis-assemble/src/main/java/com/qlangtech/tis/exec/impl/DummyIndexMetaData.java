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
package com.qlangtech.tis.exec.impl;

import com.qlangtech.tis.exec.IIndexMetaData;
import com.qlangtech.tis.exec.lifecycle.hook.IIndexBuildLifeCycleHook;
import com.qlangtech.tis.fullbuild.indexbuild.LuceneVersion;
import com.qlangtech.tis.solrdao.impl.ParseResult;

/**
 * 客户端提交的构建请求，只要求构建宽表，不需要构建索引build
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-06 11:46
 */
public class DummyIndexMetaData implements IIndexMetaData {

    @Override
    public ParseResult getSchemaParseResult() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IIndexBuildLifeCycleHook getIndexBuildLifeCycleHook() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LuceneVersion getLuceneVersion() {
        throw new UnsupportedOperationException();
    }
}
