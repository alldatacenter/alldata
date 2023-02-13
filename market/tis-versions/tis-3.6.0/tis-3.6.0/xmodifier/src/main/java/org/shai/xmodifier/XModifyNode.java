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
package org.shai.xmodifier;

import org.shai.xmodifier.util.Cons;
import org.shai.xmodifier.util.StringUtils;
import java.util.*;

/**
 * Created by Shenghai on 14-11-24.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class XModifyNode {

    private final Map<String, String> nsMap;

    private final String xPath;

    private final String value;

    private final String[] elements;

    private final String[] elementXPaths;

    // below fields are for current node status
    private int index = 0;

    private String nsPrefix;

    private String namespaceURI;

    private String localName;

    private String[] conditions;

    private String mark;

    public XModifyNode(Map<String, String> nsMap, String xPath, String value) {
        this.nsMap = nsMap;
        if (xPath.contains("(:")) {
            this.xPath = StringUtils.removeQuotingString(xPath, new Cons<String, String>("(:", ")"), Arrays.asList(new Cons<String, String>("(", ")")));
        } else {
            this.xPath = xPath;
        }
        this.value = value;
        index = 0;
        elements = StringUtils.splitBySeparator(xPath, new String[] { "/", "//" }, new char[][] { { '\'', '\'' }, { '[', ']' }, { '(', ')' } }, false);
        elementXPaths = StringUtils.removeMarks(StringUtils.splitBySeparator(xPath, new String[] { "/", "//" }, new char[][] { { '\'', '\'' }, { '[', ']' }, { '(', ')' } }, true));
        for (int i = 0; i < elementXPaths.length; i++) {
            if (i == 0) {
                continue;
            }
            elementXPaths[i] = "." + elementXPaths[i];
        }
        analyzeCurrentNode();
    }

    private XModifyNode(Map<String, String> nsMap, String xPath, String value, String[] elements, String[] elementXPaths, int index) {
        this.nsMap = nsMap;
        this.xPath = xPath;
        this.value = value;
        this.elements = elements;
        this.elementXPaths = elementXPaths;
        this.index = index;
        analyzeCurrentNode();
    }

    public String getCurNode() {
        return elements[index];
    }

    public String getPreNode() {
        if (index - 1 >= 0) {
            return elements[index - 1];
        }
        return null;
    }

    public boolean moveNext() {
        if (index >= elements.length - 1) {
            return false;
        }
        index++;
        analyzeCurrentNode();
        return true;
    }

    private void analyzeCurrentNode() {
        String nodeExpression = getCurNode();
        // nsPrefix:local[condition][condition] for example
        // ns:person[@name='john'][@age='16'][job]
        String temp = nodeExpression.trim();
        if (nodeExpression.contains("(:")) {
            Cons<String, String> findingResult = StringUtils.findFirstQuotingString(nodeExpression, new Cons<String, String>("(:", ")"), Arrays.asList(new Cons<String, String>("(", ")")));
            mark = findingResult.getLeft();
            temp = findingResult.getRight();
        }
        // 1. deal with namespace prefix temp =
        // nsPrefix:local[condition][condition]
        String[] split = StringUtils.splitBySeparator(temp, ':', new char[][] { { '\'', '\'' }, { '[', ']' }, { '(', ')' } }, false);
        if (split[1] != null) {
            nsPrefix = StringUtils.trimToNull(split[0]);
            temp = split[1];
        }
        namespaceURI = nsMap.get(nsPrefix);
        // 2. deal with local name temp = local[condition][condition]
        split = StringUtils.splitBySeparator(temp, '[', true);
        if (split[1] == null) {
            localName = StringUtils.trimToNull(split[0]);
            return;
        } else {
            localName = StringUtils.trimToNull(split[0]);
            temp = split[1];
        }
        // 3. deal with conditions temp = [condition][condition]
        split = temp.substring(1).split("\\[");
        conditions = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            conditions[i] = StringUtils.stripEnd(s, "]");
        }
    }

    public String getValue() {
        return value;
    }

    public boolean isAttributeModifier() {
        return getCurNode().startsWith("@");
    }

    public boolean isRootNode() {
        return index == 0 && StringUtils.isEmpty(getCurNode());
    }

    public String getXPath() {
        return xPath;
    }

    public String getCurNodeXPath() {
        return elementXPaths[index];
    }

    public String getNsPrefix() {
        return nsPrefix;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public String getLocalName() {
        return localName;
    }

    public String[] getConditions() {
        return conditions;
    }

    public String getMark() {
        return mark;
    }

    @Override
    public String toString() {
        return "XModifyNode{" + "xPath='" + xPath + '\'' + ", value='" + value + '\'' + ", index=" + index + '}';
    }

    public XModifyNode duplicate() {
        return new XModifyNode(nsMap, xPath, value, elements, elementXPaths, index);
    }

    public boolean isAdding() {
        return mark != null && mark.equals("add");
    }

    public boolean isDeleting() {
        return mark != null && mark.equals("delete");
    }

    public boolean isInsertBefore() {
        return mark != null && mark.startsWith("insertBefore");
    }

    public String getInsertBeforeXPath() {
        Cons<String, String> result = StringUtils.findFirstQuotingString(mark, new Cons<String, String>("insertBefore(", ")"), Arrays.asList(new Cons<String, String>("(", ")")));
        if (result != null) {
            return "./" + result.getLeft();
        } else {
            return null;
        }
    }
}
