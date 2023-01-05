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

package com.qlangtech.tis.offline.flattable;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.offline.FlatTableBuilder;
import com.qlangtech.tis.plugin.BaiscPluginTest;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: baisui 百岁
 * @create: 2020-04-21 13:39
 **/
public class TestHiveFlatTableBuilder extends BaiscPluginTest {
    private IPluginStore<FlatTableBuilder> flatTableBuilderStore;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.flatTableBuilderStore = TIS.getPluginStore(FlatTableBuilder.class);
    }


    public void testCreate() {

        // assertNotNull(TSearcherConfigFetcher.get().getLogFlumeAddress());

        // PluginStore<FlatTableBuilder> store = TIS.getPluginStore(FlatTableBuilder.class);
        FlatTableBuilder flatTableBuilder = flatTableBuilderStore.getPlugin();
        assertNotNull(flatTableBuilder);

        AtomicBoolean success = new AtomicBoolean(false);
        flatTableBuilder.startTask((r) -> {
            Connection con = r.getObj();
            assertNotNull(con);
            Statement stmt = null;
            ResultSet result = null;
            try {
                stmt = con.createStatement();
                result = stmt.executeQuery("desc totalpay_summary");
                while (result.next()) {
                    System.out.println("cols:" + result.getString(1));
                }
                success.set(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {

                try {
                    result.close();
                } catch (SQLException e) {

                }
                try {
                    stmt.close();
                } catch (SQLException e) {

                }
            }
        });

        assertTrue("must success", success.get());
    }

}
