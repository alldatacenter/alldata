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
package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestDBNode extends TestCase {

    public void testLoadAndDump() throws Exception {
        List<DBNode> nodes = Lists.newArrayList();
        String dbname = "baisuidbName";
        int dbid = 9527;
        long timestamp = 12378845l;
        DBNode node = new DBNode(dbname, dbid);
        node.setTimestampVer(timestamp);
        nodes.add(node);
        File f = new File("dataflow/dbnodes.yaml");
        DBNode.dump(nodes, f);
        try (InputStream input = FileUtils.openInputStream(f)) {
            nodes = DBNode.load(input);
        }
        assertEquals(1, nodes.size());
        node = nodes.get(0);
        assertEquals(dbname, node.getDbName());
        assertEquals(dbid, node.getDbId());
        assertEquals(timestamp, node.getTimestampVer());
    }
}
