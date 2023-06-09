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
package com.qlangtech.tis.plugin.solr.schema;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IdentityDescribale;
import com.qlangtech.tis.runtime.module.action.IModifierProcess;
import com.qlangtech.tis.solrdao.IFieldTypeFactory;
import com.yushu.tis.xmodifier.XModifier;
import org.jdom2.Document;


/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-01-22 17:44
 */
public abstract class FieldTypeFactory implements IdentityDescribale<FieldTypeFactory, ISolrFieldType>, IModifierProcess, IFieldTypeFactory {


    @Override
    public void process(Document document2, XModifier modifier) {

    }

    /**
     * ∂
     * 是否针对处理String类型的字段，例如json，tags等。是，则在console页面处理的时候会显示到fieldtype的关联下啦菜单
     *
     * @return
     */
    public abstract boolean forStringTokenizer();

    @Override
    public final Descriptor<FieldTypeFactory> getDescriptor() {
        return TIS.get().getDescriptor(this.getClass());
    }
}
