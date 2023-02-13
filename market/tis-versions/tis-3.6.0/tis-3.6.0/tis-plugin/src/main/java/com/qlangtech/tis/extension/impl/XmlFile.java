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
package com.qlangtech.tis.extension.impl;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.util.AtomicFileWriter;
import com.qlangtech.tis.util.PluginMeta;
import com.qlangtech.tis.util.XStream2;
import com.qlangtech.tis.utils.MD5Utils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.core.MapBackedDataHolder;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public final class XmlFile {

    private final XStream2 xs;

    private final File file;
    public final String relativePath;

    public XmlFile(File file) {
        this(DEFAULT_XSTREAM, file);
    }

    public XmlFile(File file, String relativePath) {
        this(DEFAULT_XSTREAM, file, relativePath);
    }

    public XmlFile(XStream2 xs, File file) {
        this(xs, file, TIS.pluginCfgRoot.toPath().relativize(file.toPath()).toString());
    }

    public XmlFile(XStream2 xs, File file, String relativePath) {
        this.xs = xs;
        this.file = file;
        this.relativePath = relativePath;
    }

    public File getFile() {
        return file;
    }

    public XStream getXStream() {
        return xs;
    }

    /**
     * Loads the contents of this file into a new object.
     */
    public Object read() throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Reading " + file);
        }
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            return xs.fromXML(in);
        } catch (XStreamException e) {
            throw new IOException("Unable to read " + file.getAbsolutePath(), e);
        } catch (Error e) {
            // mostly reflection errors
            throw new IOException("Unable to read " + file, e);
        } finally {
            in.close();
        }
    }

    public Object unmarshal(Object o) throws IOException {
        return unmarshal(o, null);
    }

    /**
     * Loads the contents of this file into an existing object.
     *
     * @return The unmarshalled object. Usually the same as <tt>o</tt>, but would be different
     * if the XML representation is completely new.
     */
    public Object unmarshal(Object o, DataHolder dataHolder) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            // TODO: expose XStream the driver from XStream
            return xs.unmarshal(DEFAULT_DRIVER.createReader(in), o, dataHolder);
        } catch (Throwable e) {
            // mostly reflection errors
            throw new IOException("Unable to read " + file, e);
        } finally {
            in.close();
        }
    }

    /**
     * 文件更新之前和更新之后是否有变化
     *
     * @param o
     * @param pluginsMeta
     * @return
     * @throws IOException
     */
    public boolean write(Object o, Set<PluginMeta> pluginsMeta) throws IOException {
        mkdirs();
        String preMd5 = null;
        boolean preFileExist = file.exists();
        if (preFileExist) {
            preMd5 = MD5Utils.md5file(file);
        }
        AtomicFileWriter w = new AtomicFileWriter(file);
        try {
            w.write("<?xml version='1.0' encoding='UTF-8'?>\n");
            DefaultDataHolder dataHolder = new DefaultDataHolder(pluginsMeta, this);
            HierarchicalStreamWriter writer = xs.createHierarchicalStreamWriter(w);
            try {
                xs.marshal(o, writer, dataHolder);
            } finally {
                writer.flush();
            }
            // xs.toXML(o, w);
            w.commit();
            return !preFileExist || !preMd5.equals(MD5Utils.md5file(file));
        } catch (StreamException e) {
            throw new IOException(e);
        } finally {
            w.abort();
        }
    }

    public static class DefaultDataHolder extends MapBackedDataHolder {

        // Map<Object, Object> map = new HashMap<>();
        private final Set<PluginMeta> pluginsMeta;
        private final XmlFile xmlFile;

        public DefaultDataHolder(Set<PluginMeta> pluginsMeta, XmlFile xmlFile) {
            super();
            this.pluginsMeta = pluginsMeta;
            this.xmlFile = xmlFile;
        }

        @Override
        public Object get(Object key) {
            if (key == PluginMeta.class) {
                return pluginsMeta;
            }
            if (key == XmlFile.class) {
                return xmlFile;
            }
            return super.get(key);
        }

//        @Override
//        public void put(Object key, Object value) {
//            // map.put(key, value);
//        }
//
//        @Override
//        public Iterator keys() {
//            // return map.keySet().iterator();
//            return null;
//        }
    }

    public boolean exists() {
        return file.exists();
    }

    public void delete() {
        file.delete();
    }

    public void mkdirs() {
        try {
            FileUtils.forceMkdirParent(file);
        } catch (IOException e) {
            throw new RuntimeException("file:" + file.getAbsolutePath(), e);
        }
//        file.getParentFile().mkdirs();
    }

    @Override
    public String toString() {
        return file.toString();
    }

    /**
     * Opens a {@link Reader} that loads XML.
     * This method uses {@link #sniffEncoding() the right encoding},
     * not just the system default encoding.
     */
    public Reader readRaw() throws IOException {
        return new InputStreamReader(new FileInputStream(file), sniffEncoding());
    }

    /**
     * Returns the XML file read as a string.
     */
    public String asString() throws IOException {
        StringWriter w = new StringWriter();
        writeRawTo(w);
        return w.toString();
    }

    /**
     * Writes the raw XML to the given {@link Writer}.
     * Writer will not be closed by the implementation.
     */
    public void writeRawTo(Writer w) throws IOException {
        Reader r = readRaw();
        try {
            // Util.copyStream(r,w);
            IOUtils.copy(r, w);
        } finally {
            r.close();
        }
    }

    /**
     * Parses the beginning of the file and determines the encoding.
     *
     * @return always non-null.
     * @throws IOException if failed to detect encoding.
     */
    public String sniffEncoding() throws IOException {
        class Eureka extends SAXException {

            final String encoding;

            public Eureka(String encoding) {
                this.encoding = encoding;
            }
        }
        InputSource input = new InputSource(file.toURI().toASCIIString());
        input.setByteStream(new FileInputStream(file));
        try {
            JAXP.newSAXParser().parse(input, new DefaultHandler() {

                private Locator loc;

                @Override
                public void setDocumentLocator(Locator locator) {
                    this.loc = locator;
                }

                @Override
                public void startDocument() throws SAXException {
                    attempt();
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    attempt();
                    // if we still haven't found it at the first start element, then we are not going to find it.
                    throw new Eureka(null);
                }

                private void attempt() throws Eureka {
                    if (loc == null)
                        return;
                    if (loc instanceof Locator2) {
                        Locator2 loc2 = (Locator2) loc;
                        String e = loc2.getEncoding();
                        if (e != null)
                            throw new Eureka(e);
                    }
                }
            });
            // can't reach here
            throw new AssertionError();
        } catch (Eureka e) {
            if (e.encoding != null)
                return e.encoding;
            // in such a case, assume UTF-8 rather than fail, since Jenkins internally always write XML in UTF-8
            return "UTF-8";
        } catch (SAXException e) {
            throw new IOException("Failed to detect encoding of " + file, e);
        } catch (ParserConfigurationException e) {
            // impossible
            throw new AssertionError(e);
        } finally {
            // some JAXP implementations appear to leak the file handle if we just call parse(File,DefaultHandler)
            input.getByteStream().close();
        }
    }

    /**
     * {@link XStream} instance is supposed to be thread-safe.
     */
    // new XStream2();
    private static final Logger LOGGER = Logger.getLogger(XmlFile.class.getName());

    private static final SAXParserFactory JAXP = SAXParserFactory.newInstance();

    public static final XppDriver DEFAULT_DRIVER = new XppDriver();

    public static final XStream2 DEFAULT_XSTREAM = new XStream2(DEFAULT_DRIVER);

    static {
        JAXP.setNamespaceAware(true);
    }
}
