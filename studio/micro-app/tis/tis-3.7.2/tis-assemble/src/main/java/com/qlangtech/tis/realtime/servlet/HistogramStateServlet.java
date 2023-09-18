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
package com.qlangtech.tis.realtime.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.qlangtech.tis.order.center.IndexSwapTaskflowLauncher;
import com.qlangtech.tis.realtime.servlet.RealtimeStatePageServlet.RowPair;
import com.qlangtech.tis.realtime.transfer.IOnsListenerStatus;
import com.qlangtech.tis.manage.common.TISCollectionUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年12月10日
 */
public class HistogramStateServlet extends javax.servlet.http.HttpServlet {

    private static final long serialVersionUID = 1L;

    private Collection<IOnsListenerStatus> incrChannels;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        IndexSwapTaskflowLauncher launcherContext = IndexSwapTaskflowLauncher.getIndexSwapTaskflowLauncher(config.getServletContext());
        this.incrChannels = launcherContext.getIncrChannels();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<RowPair> stats = new ArrayList<>();
        RowPair p = null;
        for (IOnsListenerStatus stat : this.incrChannels) {
            if (p == null) {
                p = new RowPair();
            }
            if (!p.add(stat)) {
                // 放满了
                stats.add(p);
                p = null;
            }
        }
        req.setAttribute("stats", stats);
        req.getRequestDispatcher("/vm/realtime_state.vm").forward(req, resp);
    }
}
