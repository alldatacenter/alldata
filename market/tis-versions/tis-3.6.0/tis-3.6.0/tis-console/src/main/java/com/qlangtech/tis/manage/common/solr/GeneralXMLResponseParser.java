///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.manage.common.solr;
//
//import java.net.URLEncoder;
//import javax.xml.stream.XMLStreamConstants;
//import javax.xml.stream.XMLStreamException;
//import javax.xml.stream.XMLStreamReader;
//import org.apache.solr.client.solrj.impl.XMLResponseParser;
//import org.apache.solr.common.SolrDocument;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2012-4-23
// */
//public class GeneralXMLResponseParser extends XMLResponseParser {
//
//    protected SolrDocument readDocument(XMLStreamReader parser) throws XMLStreamException {
//        if (XMLStreamConstants.START_ELEMENT != parser.getEventType()) {
//            throw new RuntimeException("must be start element, not: " + parser.getEventType());
//        }
//        if (!"doc".equals(parser.getLocalName().toLowerCase())) {
//            throw new RuntimeException("must be 'lst', not: " + parser.getLocalName());
//        }
//        SolrDocument doc = new SolrDocument();
//        StringBuilder builder = new StringBuilder();
//        KnownType type = null;
//        String name = null;
//        // just eat up the events...
//        int depth = 0;
//        while (true) {
//            switch(parser.next()) {
//                case XMLStreamConstants.START_ELEMENT:
//                    depth++;
//                    // reset the text
//                    builder.setLength(0);
//                    type = KnownType.get(parser.getLocalName());
//                    if (type == null) {
//                        type = KnownType.STR;
//                    }
//                    // if (type == null) {
//                    // throw new RuntimeException("this must be known type! not: "
//                    // + parser.getLocalName());
//                    // }
//                    // 百岁修改 2012/04/23 为了solr不同版本之间兼容 end
//                    name = null;
//                    int cnt = parser.getAttributeCount();
//                    for (int i = 0; i < cnt; i++) {
//                        if ("name".equals(parser.getAttributeLocalName(i))) {
//                            name = parser.getAttributeValue(i);
//                            break;
//                        }
//                    }
//                    if (name == null) {
//                        throw new XMLStreamException("requires 'name' attribute: " + parser.getLocalName(), parser.getLocation());
//                    }
//                    // Handle multi-valued fields
//                    if (type == KnownType.ARR) {
//                        for (Object val : readArray(parser)) {
//                            doc.addField(name, val);
//                        }
//                        // the array reading clears out the 'endElement'
//                        depth--;
//                    }
//                    // }
//                    break;
//                case XMLStreamConstants.END_ELEMENT:
//                    if (--depth < 0) {
//                        return doc;
//                    }
//                    // System.out.println( "FIELD:"+type+"::"+name+"::"+builder );
//                    Object val = type.read(builder.toString().trim());
//                    if (val == null) {
//                        throw new XMLStreamException("error reading value:" + type, parser.getLocation());
//                    }
//                    doc.addField(name, val);
//                    break;
//                // TODO? should this be trimmed? make
//                case XMLStreamConstants.SPACE:
//                // sure it only gets one/two space?
//                case XMLStreamConstants.CDATA:
//                case XMLStreamConstants.CHARACTERS:
//                    builder.append(parser.getText());
//                    break;
//            }
//        }
//    }
//
//    public static void main(String[] arg) throws Exception {
//        System.out.println(URLEncoder.encode("美图", "GBk"));
//    }
//}
