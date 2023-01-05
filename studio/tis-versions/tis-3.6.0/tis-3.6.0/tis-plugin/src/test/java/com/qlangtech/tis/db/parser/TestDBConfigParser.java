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
package com.qlangtech.tis.db.parser;

import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.plugin.ds.DBConfig;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年9月7日
 */
public class TestDBConfigParser extends TestCase {

    public void testJustParseDBHost() throws Exception {
        DBConfigParser p = getDBParser("order_db_config.txt");
        DBConfig db = p.startParser();
        String dbName = "order";
        Map<String, List<String>> dbEnum = DBConfigParser.parseDBEnum(dbName, db.getHostDesc().toString());
        assertTrue(dbEnum.size() > 1);
        validateDbEnum(dbName, dbEnum);
    }

    public void testLocalhost() {
        String host = "localhost";
        Map<String, List<String>> localhost = DBConfigParser.parseDBEnum("flink-test", host);
        assertTrue(localhost.get(host).size() > 0);
        localhost.get(host);
    }


    public void testMulti2() throws Exception {
        DBConfigParser parser = getDBParser("order_db_config.txt");
        DBConfig db = parser.startParser();
        assertEquals("10.1.6.99[1-32],10.1.6.101[01-32],10.1.6.102[33-64],10.1.6.103[65-96],10.1.6.104[97-128],127.0.0.3", db.getHostDesc().toString());
        final String orderName = "order";
        Assert.assertEquals("mysql", db.getDbType());
        Assert.assertEquals(orderName, db.getName());
        Map<String, List<String>> dbEnum = db.getDbEnum();
        validateDbEnum(orderName, dbEnum);
    }

    private void validateDbEnum(String orderName, Map<String, List<String>> dbEnum) {
        DecimalFormat format = new DecimalFormat("00");
        List<String> dbs = dbEnum.get("10.1.6.101");
        Assert.assertNotNull(dbs);
        Assert.assertEquals(32, dbs.size());
        for (int i = 1; i <= 32; i++) {
            assertTrue("dbIndex:" + orderName + format.format(i), dbs.contains(orderName + (format.format(i))));
        }
        dbs = dbEnum.get("127.0.0.3");
        Assert.assertNotNull(dbs);
        Assert.assertEquals(1, dbs.size());
        for (String dbName : dbs) {
            Assert.assertEquals(orderName, dbName);
        }
        dbs = dbEnum.get("10.1.6.99");
        for (int i = 1; i <= 32; i++) {
            assertTrue("dbIndex:" + orderName + i, dbs.contains(orderName + (i)));
        }
    }

    public void testSingle() throws Exception {
        DBConfigParser parser = getDBParser("host_desc_single.txt");
        Assert.assertTrue(parser.parseHostDesc());
        assertEquals("127.0.0.3", parser.hostDesc.toString());
        DBConfig db = parser.dbConfigResult;
        assertEquals(1, db.getDbEnum().entrySet().size());
        // StringBuffer dbdesc = new StringBuffer();
        for (Map.Entry<String, List<String>> e : db.getDbEnum().entrySet()) {
            // dbdesc.append(e.getKey()).append(":");
            //
            // dbdesc.append("\n");
            assertEquals("127.0.0.3", e.getKey());
            assertEquals(1, e.getValue().size());
            for (String dbName : e.getValue()) {
                Assert.assertNull(dbName);
            }
        }
        // System.out.println(dbdesc.toString());
    }

    // public void testMulti() throws Exception {
    // File f = new File("./host_desc.txt");
    // String content = FileUtils.readFileToString(f, "utf8");
    // // System.out.println(content);
    //
    // DBTokenizer tokenizer = new DBTokenizer(content);
    // tokenizer.parse();
    // // for (Token t : ) {
    // // System.out.println(t.getContent() + " "
    // // + t.getToken());
    // // }
    //
    // TokenBuffer buffer = tokenizer.getTokenBuffer();
    // Token t = null;
    // while ((t = buffer.nextToken()) != null) {
    // System.out.println(t.getToken() + "-" + t.getContent());
    // buffer.popToken();
    // }
    //
    // DBConfigParser parser = new DBConfigParser(tokenizer.getTokenBuffer());
    //
    // parser.dbConfigResult.setName("order");
    //
    // parser.parseHostDesc();
    //
    // DBConfig db = parser.dbConfigResult;
    //
    // // System.out.println("type:" + db.getDbType());
    // // System.out.println("name:" + db.getName());
    // // System.out.println("getPassword:" + db.getPassword());
    // // System.out.println("getPort:" + db.getPort());
    // // System.out.println("UserName:" + db.getUserName());
    //
    // StringBuffer dbdesc = new StringBuffer();
    // for (Map.Entry<String, List<String>> e : db.getDbEnum().entrySet()) {
    // dbdesc.append(e.getKey()).append(":");
    // for (String dbName : e.getValue()) {
    // dbdesc.append(dbName).append(",");
    // }
    // dbdesc.append("\n");
    // }
    //
    // System.out.println(dbdesc.toString());
    // }
    private DBConfigParser getDBParser(String resourceName) throws IOException {
        try {
            DBTokenizer tokenizer = null;
            try (InputStream reader = this.getClass().getResourceAsStream(resourceName)) {
                tokenizer = new DBTokenizer(IOUtils.toString(reader, StandardCharsets.UTF_8));
            }
            tokenizer.parse();
            return new DBConfigParser(tokenizer.getTokenBuffer());
        } catch (Exception e) {
            throw new RuntimeException(resourceName, e);
        }
    }
}
