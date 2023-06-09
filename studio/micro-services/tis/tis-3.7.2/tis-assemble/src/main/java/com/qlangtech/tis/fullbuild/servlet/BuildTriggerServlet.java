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
package com.qlangtech.tis.fullbuild.servlet;

import com.qlangtech.tis.exec.AbstractActionInvocation;
import com.qlangtech.tis.exec.impl.DefaultChainContext;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.servlet.impl.HttpExecContext;
import com.qlangtech.tis.trigger.jst.ImportDataProcessInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据中心準備好數據之後，直接觸發全量構建
 * http://10.1.5.2:8080/hdfs_build?indexname=search4_fat_instance&cols=
 * instance_id,in_order_id,in_kind_menu_id,kindmenu_name,in_menu_id,in_parent_id
 * ,account_num,in_fee,ratio_fee,ratio,entity_id,in_name,kind,unit,account_unit,
 * price,member_price,original_price,pricemode,is_ratio,spec_detail_name,
 * in_op_time,modify_time,load_time,in_last_ver,is_buynumber_changed,seat_id,
 * in_is_valid,in_status,km_kindmenu_id,km_kindmenu_name,km_sort_code,
 * group_kind_id,group_or_kind_id,group_or_kind_name,km_is_valid,od_curr_date,
 * open_time,end_time,people_count,od_is_valid,od_status,od_is_hide,
 * mwh_menuorkind_id,mwh_id,mwh_warehouse_id,mwh_type,mwh_ratio,mwh_create_time,
 * mwh_op_time,wh_name,wh_sort_code,wh_parent_id,wh_is_check&hdfspath=/user/hive
 * /warehouse/solr.db/kuan/pt=20160620&dumpstart=20160622001000&rowcount=
 * 43443159&params_sign=16dff2c2d9bc600d71f074e3dedefe52
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年3月11日
 */
public class BuildTriggerServlet extends TisServlet {

    private static final long serialVersionUID = 1L;

    private static final String KEY_INDEX_NAME = "indexname";

    public static final String KEY_COLS = "cols";

    private static final String KEY_DUMP_START = "dumpstart";

    public static final String KEY_DUMP_ROW_COUNT = "rowcount";

    private static final Logger logger = LoggerFactory.getLogger(BuildTriggerServlet.class);

    // public BuildTriggerServlet(
    // IndexSwapTaskflowLauncher indexSwapTaskflowLauncher) {
    // super(indexSwapTaskflowLauncher);
    // }
    // protected void service(HttpServletRequest req, HttpServletResponse res)
    // throws ServletException, IOException {
    //
    // String indexname = req.getParameter(KEY_INDEX_NAME);
    // MDC.put("app", indexname);
    // try {
    // logger.info("requestURI:" +
    // req.getRequestURL().append("?").append(req.getQueryString()));
    // String clos = req.getParameter(KEY_COLS);
    // String hdfspath = req.getParameter("hdfspath");
    // String dumpstart = req.getParameter(KEY_DUMP_START);
    // String paramsSign = req.getParameter("params_sign");
    // String rowcount = req.getParameter(KEY_DUMP_ROW_COUNT);
    //
    // // 校验参数必须
    // if (StringUtils.isBlank(indexname) || StringUtils.isBlank(clos) ||
    // StringUtils.isBlank(hdfspath)
    // || StringUtils.isBlank(dumpstart) || StringUtils.isBlank(paramsSign)
    // || StringUtils.isBlank(rowcount)) {
    // this.writeResult(false, "one of param is blank", res);
    // return;
    // }
    //
    // final String md5 = DigestUtils.md5Hex(indexname + clos + hdfspath +
    // dumpstart + rowcount);
    // if (!StringUtils.equals(md5, paramsSign)) {
    // this.writeResult(false, "server md5sign:" + md5 + " is not equal client
    // sign:" + paramsSign, res);
    // return;
    // }
    //
    // logger.info("param indexname:" + indexname + "\nclos:" + clos + "\n
    // hdfspath:" + hdfspath + "\n dumpstart:"
    // + dumpstart + "\n paramsSign:" + paramsSign + "\n rowcount:" + rowcount);
    //
    // // 校验rowcount数目
    //
    // super.service(req, res);
    // } finally {
    // MDC.remove("app");
    // }
    // }
    @Override
    protected boolean isValidParams(HttpExecContext execContext, HttpServletRequest req, HttpServletResponse res) throws ServletException {
        logger.info("requestURI:" + req.getRequestURL().append("?").append(req.getQueryString()));
        String clos = req.getParameter(KEY_COLS);
        String hdfspath = req.getParameter("hdfspath");
        String dumpstart = req.getParameter(KEY_DUMP_START);
        String paramsSign = req.getParameter("params_sign");
        String rowcount = req.getParameter(KEY_DUMP_ROW_COUNT);
        String indexname = execContext.getString(IFullBuildContext.KEY_APP_NAME);
        // 校验参数必须
        if (StringUtils.isBlank(indexname)) {
            this.writeResult(false, "one of param indexname is blank", res);
            return false;
        }
        if (StringUtils.isBlank(clos)) {
            this.writeResult(false, "one of param clos is blank", res);
            return false;
        }
        if (StringUtils.isBlank(hdfspath)) {
            this.writeResult(false, "one of param hdfspath is blank", res);
            return false;
        }
        if (StringUtils.isBlank(dumpstart)) {
            this.writeResult(false, "one of param dumpstart is blank", res);
            return false;
        }
        if (StringUtils.isBlank(paramsSign)) {
            this.writeResult(false, "one of param paramsSign is blank", res);
            return false;
        }
        if (StringUtils.isBlank(rowcount)) {
            this.writeResult(false, "one of param rowcount is blank", res);
            return false;
        }
        final String md5 = DigestUtils.md5Hex(indexname + clos + hdfspath + dumpstart + rowcount);
        // 先不校验了
        // if (!StringUtils.equals(md5, paramsSign)) {
        // this.writeResult(false, "server md5sign:" + md5 + " is not equal
        // client sign:" + paramsSign, res);
        // return false;
        // }
        logger.info("param indexname:" + indexname + "\nclos:" + clos + "\n hdfspath:" + hdfspath + "\n dumpstart:" + dumpstart + "\n paramsSign:" + paramsSign + "\n rowcount:" + rowcount);
        return true;
    }

    @Override
    protected HttpExecContext createHttpExecContext(HttpServletRequest req) {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(IFullBuildContext.KEY_APP_NAME, req.getParameter(KEY_INDEX_NAME));
        params.put(AbstractActionInvocation.COMMAND_KEY_DIRECTBUILD, Boolean.TRUE.toString());
        params.put(DefaultChainContext.KEY_PARTITION, req.getParameter(KEY_DUMP_START));
        params.put(KEY_DUMP_ROW_COUNT, req.getParameter(KEY_DUMP_ROW_COUNT));
        String hdfsSplitChar = null;
        if ((hdfsSplitChar = req.getParameter(ImportDataProcessInfo.KEY_DELIMITER)) != null) {
            params.put(ImportDataProcessInfo.KEY_DELIMITER, hdfsSplitChar);
        }
        HttpExecContext execContext = new HttpExecContext(req, params, false);
        return execContext;
    }
}
