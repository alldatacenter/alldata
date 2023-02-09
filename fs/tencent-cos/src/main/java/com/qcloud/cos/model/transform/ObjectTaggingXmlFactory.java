/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */
package com.qcloud.cos.model.transform;

import com.qcloud.cos.internal.XmlWriter;
import com.qcloud.cos.model.ObjectTagging;
import com.qcloud.cos.model.Tag.Tag;

public class ObjectTaggingXmlFactory {
    public byte[] convertToXmlByteArray(ObjectTagging tagging) {
        XmlWriter writer = new XmlWriter();
        writer.start("Tagging").start("TagSet");
        for (Tag tag : tagging.getTagSet()) {
            writer.start("Tag");
            writer.start("Key").value(tag.getKey()).end();
            writer.start("Value").value(tag.getValue()).end();
            writer.end(); // </Tag>
        }
        writer.end(); // </TagSet>
        writer.end(); // </Tagging>

        return writer.getBytes();
    }
}