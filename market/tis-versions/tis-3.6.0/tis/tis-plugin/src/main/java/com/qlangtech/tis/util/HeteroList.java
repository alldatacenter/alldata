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
package com.qlangtech.tis.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.util.GroovyShellEvaluate;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.lang.StringUtils;

import java.net.URLEncoder;
import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class HeteroList<T extends Describable<T>> {

    private List<Descriptor<T>> descriptors;

    private List<T> items = new ArrayList<>();

    private String caption;
    private String identityId;

    private Class<?> extensionPoint;

    // 标志Item可以选几个
    private Selectable selectable;
    private final UploadPluginMeta pluginMeta;


    public HeteroList(UploadPluginMeta pluginMeta) {
        this.pluginMeta = pluginMeta;
    }

    public Selectable getSelectable() {
        return selectable;
    }

    public void setSelectable(Selectable selectable) {
        this.selectable = selectable;
    }

    public List<Descriptor<T>> getDescriptors() {
        return descriptors;
    }

    public Class<?> getExtensionPoint() {
        return this.extensionPoint;
    }

    public void setExtensionPoint(Class<?> extensionPoint) {
        this.extensionPoint = extensionPoint;
    }

    public void setIdentityId(String identity) {
        this.identityId = identity;
    }

    public void setDescriptors(List<Descriptor<T>> descriptors) {
        this.descriptors = descriptors;
    }

    public List<T> getItems() {
        return this.items == null ? Collections.emptyList() : this.items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public void addItem(T item) {
        this.items.add(item);
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public JSONObject toJSON() throws Exception {
        JSONObject o = new JSONObject();
        o.put("caption", this.getCaption());
        o.put("identityId", this.identityId);
        o.put("cardinality", this.getSelectable().identity);
        o.put("extensionPoint", this.extensionPoint.getName());
        //http://tis.pub/docs/guide/plugin/plugins/#%E6%89%A9%E5%B1%95%E7%82%B9comqlangtechtisasyncmessageclientconsumerimplabstractasyncmsgdeserialize
        o.put("extensionPointUrl", Config.TIS_PUB_PLUGINS_DOC_URL
                + URLEncoder.encode(  StringUtils.lowerCase(StringUtils.remove(this.extensionPoint.getName(), ".")), TisUTF8.getName()));


        Optional<IPropertyType.SubFormFilter> subFormFilter = pluginMeta.getSubFormFilter();
        DescriptorsJSON desc2Json = new DescriptorsJSON(this.descriptors);


        if (this.getItems().size() == 1) {
            for (T plugin : this.getItems()) {
                Map<Class<? extends Descriptor>, Describable> pluginThreadLocal
                        = GroovyShellEvaluate.pluginThreadLocal.get();
                pluginThreadLocal.put(plugin.getDescriptor().getClass(), plugin);
                break;
            }
            // GroovyShellEvaluate.pluginThreadLocal.set(this.getItems().get(0));
        }
        o.put("descriptors", desc2Json.getDescriptorsJSON(subFormFilter));
        o.put("items", createItemsJSONArray(this.getItems(), subFormFilter));

        return o;
    }

    public static <TT extends Describable<TT>> JSONArray createItemsJSONArray(
            List<TT> items, Optional<IPropertyType.SubFormFilter> subFormFilter) throws Exception {
        JSONArray result = new JSONArray();
        JSONObject item = null;
        for (TT i : items) {
            item = (new DescribableJSON(i)).getItemJson(subFormFilter);
            result.add(item);
        }
        return result;
    }


}
