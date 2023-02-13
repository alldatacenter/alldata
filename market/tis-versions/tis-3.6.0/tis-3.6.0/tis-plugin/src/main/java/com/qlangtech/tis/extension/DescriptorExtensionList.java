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
package com.qlangtech.tis.extension;

import com.qlangtech.tis.TIS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019-12-31 10:26
 */
public class DescriptorExtensionList<T extends Describable<T>, D extends Descriptor<T>> extends ExtensionList<D> {

    private final Class<T> describableType;

    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorExtensionList.class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Describable<T>, D extends Descriptor<T>> DescriptorExtensionList<T, D> createDescriptorList(TIS tis, Class<T> describableType) {
        return new DescriptorExtensionList<T, D>(tis, describableType);
    }

    protected DescriptorExtensionList(TIS tis, Class<T> describableType) {
        super(tis, (Class) Descriptor.class);
        this.describableType = describableType;
    }

    public Class<T> getDescribableType() {
        return this.describableType;
    }

    @Override
    protected List<ExtensionComponent<D>> load() {
        return _load(this.tis.getExtensionList(Descriptor.class).getComponents());
    }

    @Override
    protected Collection<ExtensionComponent<D>> load(ExtensionComponentSet delta) {
        return _load(delta.find(Descriptor.class));
    }

    private List<ExtensionComponent<D>> _load(Iterable<ExtensionComponent<Descriptor>> set) {
        List<ExtensionComponent<D>> r = new ArrayList<ExtensionComponent<D>>();
        for (ExtensionComponent<Descriptor> c : set) {
            Descriptor d = c.getInstance();
            try {
               // if (d.getT() == describableType) {
               if (describableType.isAssignableFrom(d.getT())  ) {
                    r.add((ExtensionComponent) c);
                }
            } catch (IllegalStateException e) {
                // skip this one
                LOGGER.error(d.getClass() + " doesn't extend Descriptor with a type parameter", e);
            }
        }
        return r;
    }
}
