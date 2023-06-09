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

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.PluginWrapper;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.plugin.annotation.ITmpFileStore;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class XStream2 extends XStream {

    private static final Logger logger = LoggerFactory.getLogger(XStream2.class);
    private final XppDriver xppDruver;

    public XStream2(XppDriver xppDruver) {
        super(null, null, xppDruver);
        this.xppDruver = xppDruver;
        this.addPermission(AnyTypePermission.ANY);
    }

    private RobustReflectionConverter reflectionConverter;

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, Object root, DataHolder dataHolder) {
        TIS h = TIS.get();
        if (h != null && h.pluginManager != null && h.pluginManager.uberClassLoader != null) {
            setClassLoader(h.pluginManager.uberClassLoader);
        }
        Object o = super.unmarshal(reader, root, dataHolder);
        return o;
    }

    @Override
    protected void setupConverters() {
        reflectionConverter = new RobustReflectionConverter(getMapper(), createReflectionProvider(), new PluginClassOwnership());
        this.registerConverter(reflectionConverter, PRIORITY_VERY_LOW);
        this.registerConverter(new TempFileConvert(this.getMapper(), this.getReflectionProvider()), PRIORITY_VERY_HIGH);
        super.setupConverters();
    }

    public ReflectionProvider createReflectionProvider() {
        return JVM.newReflectionProvider();
    }

    @Override
    public void registerConverter(Converter converter, int priority) {
        if (converter instanceof ReflectionConverter) {
            return;
        }
        super.registerConverter(converter, priority);
    }

    public HierarchicalStreamWriter createHierarchicalStreamWriter(AtomicFileWriter w) {
        return this.xppDruver.createWriter(w);
    }

    /**
     * Marks serialized classes as being owned by particular components.
     */
    interface ClassOwnership {

        /**
         * Looks up the owner of a class, if any.
         *
         * @param clazz a class which might be from a plugin
         * @return an identifier such as plugin name, or null
         */
        String ownerOf(Class<?> clazz);
    }

    class PluginClassOwnership implements ClassOwnership {

        private PluginManager pm;

        @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        // classOwnership checked for null so why does FB complain?
        @Override
        public String ownerOf(Class<?> clazz) {
            if (pm == null) {
                TIS j = TIS.get();
                if (j != null) {
                    pm = j.getPluginManager();
                }
            }
            if (pm == null) {
                return null;
            }
            // TODO: possibly recursively scan super class to discover dependencies
            PluginWrapper p = pm.whichPlugin(clazz);
            return p != null ? p.getDesc().toString() : null;
        }
    }

    /**
     * TIS 前端提交的临时文件转化，将临时文件保存到plugin的save目录中去
     */
    private static class TempFileConvert extends AbstractReflectionConverter {

        public TempFileConvert(Mapper mapper, ReflectionProvider reflectionProvider) {
            super(mapper, reflectionProvider);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            ITmpFileStore tmpFileStore = (ITmpFileStore) source;
            XmlFile xmlFile = (XmlFile) context.get(XmlFile.class);
            Objects.requireNonNull(xmlFile, "xmlFile can not be null");
            tmpFileStore.save(xmlFile.getFile().getParentFile());

            doMarshal(source, writer, context);
        }

//        @Override
//        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
//
//            this.doUnmarshal(reader,context);
//            return null;
//        }


        @Override
        public Object doUnmarshal(Object result, HierarchicalStreamReader reader, UnmarshallingContext context) {
            ITmpFileStore tmpFileStore = (ITmpFileStore) super.doUnmarshal(result, reader, context);
            XmlFile xmlFile = (XmlFile) context.get(XmlFile.class);
            Objects.requireNonNull(xmlFile, "xmlFile can not be null");
            tmpFileStore.setTmpeFile((new ITmpFileStore.TmpFile(new File(xmlFile.getFile().getParentFile(), tmpFileStore.getStoreFileName()))));
            return tmpFileStore;
        }

        @Override
        public boolean canConvert(Class type) {
            return ITmpFileStore.class.isAssignableFrom(type);
        }
    }
}
