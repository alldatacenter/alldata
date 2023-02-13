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
package com.qlangtech.tis.solrdao;

import com.qlangtech.tis.exec.IIndexMetaData;
import com.qlangtech.tis.solrdao.impl.ParseResult;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestSolrFieldParser extends TestCase {

    public void testSchemaFields() throws Exception {
        // SolrFieldsParser solrFieldsParser = new SolrFieldsParser();
        try (InputStream reader = this.getClass().getResourceAsStream("schema-sample.txt")) {
            assertNotNull(reader);
            IIndexMetaData meta = SolrFieldsParser.parse(() -> IOUtils.toByteArray(reader), (typeName) -> {
                throw new UnsupportedOperationException();
            });
            ParseResult parseResult = meta.getSchemaParseResult();
            Assert.assertTrue("isValid shall be true", parseResult.isValid());
            Assert.assertEquals(20, parseResult.dFields.size());
            System.out.println("dFields.size:" + parseResult.dFields.size());
            Assert.assertEquals("parseResult.getFieldNameSet() can not be empty", 20, parseResult.getFieldNameSet().size());
            System.out.println(parseResult.getFieldNameSet().size());
        }
    }
}
