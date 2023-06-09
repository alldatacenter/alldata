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
package com.qlangtech.tis.full.dump;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
import com.qlangtech.tis.manage.common.HttpUtils;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年3月11日
 */
public class TestHdfsIndexBuild extends TestCase {

    private static final char splitChar = '\u0001';

    //
    // public void testReadFile() throws Exception {
    // LineIterator it = FileUtils
    // .lineIterator(new File("D:\\Downloads\\000002_0.txt"), "utf8");
    // // String[] cols =
    // //
    // "waitinginstance_id,waitingorder_id,kind,kindmenu_id,kindmenu_name,name,menu_id,make_id,makename,make_price,make_pricemode,spec_detail_name,spec_detail_id,spec_pricemode,spec_detail_price,num,account_num,unit,account_unit,original_price,price,member_price,fee,is_ratio,taste,ratio,ratio_fee,is_backauth,parent_id,price_mode,child_id,service_feemode,service_fee,instance_status,entity_id,is_valid,create_time,op_time,customerregister_id,order_kind,order_status"
    // // .split(",");
    // String line = null;
    // int i = 0;
    // char[] temp = new char[100];
    // int tempCount = 0;
    // int index = 0;
    // while (it.hasNext()) {
    // int count = 0;
    // line = it.next();
    //
    // // count = line.split(String.valueOf(splitChar)).length;
    // // System.out.println(line);
    //
    // for (char c : line.toCharArray()) {
    //
    // if (c == splitChar) {
    // count++;
    //
    // System.out.println("index:" + (index++) + ","
    // + new String(temp, 0, tempCount));
    // tempCount = 0;
    // } else {
    // temp[tempCount++] = c;
    // }
    // }
    //
    // System.out.println("index:" + (index++) + ","
    // + new String(temp, 0, tempCount));
    //
    // // line = StringUtils.split(, "");
    // // System.out.println(count + ","
    // // + StringUtils.substringBefore(line, "" + splitChar)
    // // + ",last:" + StringUtils.substringAfterLast(line,
    // // String.valueOf(splitChar)));
    // // if (i++ > 20) {
    // return;
    // // }
    //
    // }
    // }
    public void testTriggerTotalpayIndexBuild() throws Exception {
        String cols = "totalpay_id,curr_date,outfee,source_amount,discount_amount,result_amount,recieve_amount,ratio,status,entity_id,is_valid,op_time,last_ver,op_user_id,discount_plan_id,operator,operate_date,card_id,card,card_entity_id,is_full_ratio,is_minconsume_ratio,is_servicefee_ratio,invoice_code,invoice_memo,invoice,over_status,is_hide,load_time,modify_time,order_id,seat_id,area_id,is_valido,instance_count,all_menu,people_count,order_from,order_kind,inner_code,kindpay,special_fee_summary,card_code,card_inner_code,card_customer_id,card_customer_name,card_customer_spell,card_customer_moble,card_customer_phone";
        String hdfspath = "/user/hive/warehouse/olap_tis.db/totalpay_summary/pt=20160330";
        String indexName = "search4totalpay";
        String dumpstart = "20160401002000";
        String rowcount = "99999";
        URL url = new URL("http://10.1.5.2:14844/hdfs_build?indexname=" + indexName + "&cols=" + URLEncoder.encode(cols, "utf8") + "&hdfspath=" + URLEncoder.encode(hdfspath, "utf8") + "&dumpstart=" + dumpstart + "&rowcount=" + rowcount + "&params_sign=" + DigestUtils.md5Hex((indexName + cols + hdfspath + dumpstart + rowcount).getBytes(Charset.forName("utf8"))));
        System.out.println("url:" + url);
        HttpUtils.processContent(url, new StreamProcess<Void>() {

            @Override
            public Void p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                try {
                    System.out.println(IOUtils.toString(stream));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return null;
            }
        });
        System.out.println("success");
    }
    // public void testIndexBuild() throws Exception {
    //
    // String cols =
    // "waitinginstance_id,waitingorder_id,kind,kindmenu_id,kindmenu_name,name,menu_id,make_id,makename,make_price,make_pricemode,spec_detail_name,spec_detail_id,spec_pricemode,spec_detail_price,num,account_num,unit,account_unit,original_price,price,member_price,fee,is_ratio,taste,ratio,ratio_fee,is_backauth,parent_id,price_mode,child_id,service_feemode,service_fee,instance_status,entity_id,is_valid,create_time,op_time,customerregister_id,order_kind,order_status";
    // String hdfspath =
    // "/user/hive/warehouse/olap_tis.db/wait_order_instance/pt=20160313";
    // String indexName = "search4waitInstance";
    // String dumpstart = "20160313200041";
    // URL url = new URL("http://10.1.5.2:14844/hdfs_build?indexname="
    // + indexName + "&cols=" + URLEncoder.encode(cols, "utf8")
    // + "&hdfspath=" + URLEncoder.encode(hdfspath, "utf8")
    // + "&dumpstart=" + dumpstart + "&params_sign="
    // + DigestUtils.md5Hex((indexName + cols + hdfspath + dumpstart)
    // .getBytes(Charset.forName("utf8"))));
    //
    // System.out.println("url:" + url);
    //
    // HttpUtils.processContent(url, new StreamProcess<Void>() {
    // @Override
    // public Void p(int status, InputStream stream, String md5) {
    // try {
    // System.out.println(IOUtils.toString(stream));
    // } catch (IOException e) {
    // throw new IllegalStateException(e);
    // }
    // return null;
    // }
    //
    // });
    //
    // System.out.println("success");
    // }
}
