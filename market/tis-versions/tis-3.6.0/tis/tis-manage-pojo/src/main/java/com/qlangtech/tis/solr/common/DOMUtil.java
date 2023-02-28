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
package com.qlangtech.tis.solr.common;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年10月7日 上午10:36:33
 */
public class DOMUtil {

    public static final String XML_RESERVED_PREFIX = "xml";

    public static String getAttr(NamedNodeMap attrs, String name) {
        return getAttr(attrs, name, null);
    }

    public static String getAttr(Node nd, String name) {
        return getAttr(nd.getAttributes(), name);
    }

    public static String getAttr(NamedNodeMap attrs, String name, String missing_err) {
        Node attr = attrs == null ? null : attrs.getNamedItem(name);
        if (attr == null) {
            if (missing_err == null)
                return null;
            throw new RuntimeException(missing_err + ": missing mandatory attribute '" + name + "'");
        }
        String val = attr.getNodeValue();
        return val;
    }

    public static String getAttr(Node node, String name, String missing_err) {
        return getAttr(node.getAttributes(), name, missing_err);
    }

    public static Map<String, String> toMap(NamedNodeMap attrs) {
        return toMapExcept(attrs);
    }

    public static Map<String, String> toMapExcept(NamedNodeMap attrs, String... exclusions) {
        Map<String, String> args = new HashMap<String, String>();
        outer: for (int j = 0; j < attrs.getLength(); j++) {
            Node attr = attrs.item(j);
            // automatically exclude things in the xml namespace, ie: xml:base
            if (XML_RESERVED_PREFIX.equals(attr.getPrefix()))
                continue outer;
            String attrName = attr.getNodeName();
            for (String ex : exclusions) if (ex.equals(attrName))
                continue outer;
            String val = attr.getNodeValue();
            args.put(attrName, val);
        }
        return args;
    }
}
