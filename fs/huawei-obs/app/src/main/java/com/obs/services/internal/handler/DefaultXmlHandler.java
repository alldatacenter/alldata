/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.handler;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public abstract class DefaultXmlHandler extends DefaultHandler {

    private StringBuilder currText = null;

    @Override
    public void startElement(String uri, String name, String qualifiedName, Attributes attrs) {
        this.currText = new StringBuilder();
        this.startElement(name, attrs);
    }

    public void startElement(String name, Attributes attrs) {
        this.startElement(name);
    }

    public void startElement(String name) {
        return;
    }

    @Override
    public void endElement(String uri, String name, String qualifiedName) {
        String elementText = this.currText.toString();
        this.endElement(name, elementText);
    }

    public abstract void endElement(String name, String content);

    public void characters(char[] ch, int start, int length) {
        this.currText.append(ch, start, length);
    }
}
