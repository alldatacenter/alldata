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
package com.qlangtech.tis.manage.module.screen;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.runtime.module.action.BasicModule;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class BuildNavData extends BasicModule {

    /**
     */
    private static final long serialVersionUID = 1L;

    // @Autowired
    // private HttpServletResponse response;
    private static String navtreehead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><tree id=\"0\" radio=\"1\">";

    private static String TAIL = "</tree>";

    protected abstract String createItemStart(String id, String text, String icon, Item item, boolean isLast);

    protected abstract String createBooksItemStart(String id, String text);

    protected abstract String createBooksItemEnd(boolean islast);

    public String loadMenuFromDB() {
        return getNavtreehead() + getQueryAdminLink() + getConfigAdmin() + getTriggerConfiguration() + getBasicConfiguration() + getRoleSubMenu() + getTail();
    }

    public String rowStart() {
        return "<item text=\"";
    }

    public String rowEnd() {
        return "\"/>";
    }

    public String itemEnd() {
        return "</item>";
    }

    public String rowId() {
        return "\" id=\"";
    }

    public String quotes() {
        return "\"";
    }

    public String square() {
        return ">";
    }

    // private String getAppsDefault() {
    //
    // String sns = "<item text=\"淘江湖SNS\" id=\"sns\">"
    // + "<item text=\"search4realwidget\" id=\"sns1\"/>"
    // + "<item text=\"search4friend\" id=\"sns2\"/>"
    // + "<item text=\"search4company\" id=\"sns3\"/>"
    // + "<item text=\"search4addressFriend\" id=\"sns4\"/>"
    // + "<item text=\"search4newIpFriend\" id=\"sns5\"/>"
    // + "<item text=\"search4phone\" id=\"sns6\"/>"
    // + "<item text=\"search4ipfriend\" id=\"sns7\"/>"
    // + "<item text=\"search4group\" id=\"sns8\"/>"
    // + "<item text=\"search4bbs\" id=\"sns9\"/>"
    // + "<item text=\"search4grouplus\" id=\"sns10\"/>"
    // + "<item text=\"search4album\" id=\"sns11\"/>"
    // + "<item text=\"search4diary\" id=\"sns12\"/>"
    // + "<item text=\"search4blo\" id=\"sns13\"/>"
    // + "<item text=\"search4galaxy\" id=\"sns14\"/>"
    // + "<item text=\"search4tag\" id=\"sns15\"/>"
    // + "<item text=\"search4comment\" id=\"sns16\"/>"
    // + "<item text=\"search4cms\" id=\"sns17\"/>"
    // + "</item>";
    //
    // String ju = "<item text=\"聚划算\" id=\"ju\">"
    // + "<item text=\"search4snsjuDepositItem\" id=\"ju01\"/>"
    // + "<item text=\"search4snsjuSellerauth\" id=\"ju02\"/>"
    // + "<item text=\"search4snsjuCheckItem\" id=\"ju03\"/>"
    // + "<item text=\"search4snsjuItemOnline\" id=\"ju04\"/>"
    // + "<item text=\"search4snsjuke\" id=\"ju05\"/>"
    // + "<item text=\"search4snsjukeseller\" id=\"ju06\"/>"
    // + "</item>";
    //
    // String zixun = "<item text=\"资讯社区\" id=\"zixun\">"
    // + "<item text=\"search4matrixtry\" id=\"zixun01\"/>"
    // + "<item text=\"search4magicroom\" id=\"zixun02\"/>"
    // + "<item text=\"search4magicroom2\" id=\"zixun03\"/>"
    // + "<item text=\"search4tstart\" id=\"zixun04\"/>"
    // + "<item text=\"search4cms\" id=\"zixun05\"/>"
    // + "</item>";
    //
    // String jishudaxue = "<item text=\"技术大学\" id=\"tec\">"
    // + "<item text=\"search4techcollege\" id=\"tec01\"></item>"
    // + "</item>";
    //
    // String xinyewu = "<item text=\"新业务\" id=\"job\">"
    // + "<item text=\"search4jobOnline\" id=\"job01\"/>"
    // + "<item text=\"search4resumeOnline\" id=\"job02\"/>"
    // + "</item>";
    //
    // String fuwupingtai = "<item text=\"服务平台\" id=\"top\">"
    // + "<item text=\"search4ark\" id=\"top01\"/>"
    // + "<item text=\"search4arkisv\" id=\"top02\"/>" + "</item>";
    //
    // String daogou = "<item text=\"导购\" id=\"tms\">"
    // + "<item text=\"search4tmspage\" id=\"tms01\"/>"
    // + "<item text=\"search4tmshistory\" id=\"tms02\"/>"
    // + "</item>";
    //
    // String shanghupingtai = "<item text=\"商户平台\" id=\"shanghu\">"
    // + "<item text=\"search4bbcproduct\" id=\"shanghu01\"/>"
    // + "<item text=\"search4bbcsupplier\" id=\"shanghu02\"/>"
    // + "<item text=\"search4ecrmonline\" id=\"shanghu03\"/>"
    // + "</item>";
    // return sns + ju + zixun + jishudaxue + xinyewu + fuwupingtai + daogou
    // + shanghupingtai;
    // }
    // public String loadMenuFromFile() throws FileNotFoundException {
    // SAXBuilder builder = new SAXBuilder();
    // String outPut = null;
    // FileInputStream in = new FileInputStream("lib/navview.xml");
    // try {
    // doc = builder.build(in);
    //
    // Format format = Format.getCompactFormat();
    // format.setEncoding("UTF-8");
    //
    // format.setIndent(" ");
    //
    // XMLOutputter xmlOutputter = new XMLOutputter(format);
    //
    // outPut = new String(xmlOutputter.outputString(doc)
    // .getBytes("UTF-8"), "UTF-8");
    // // System.out.println(outPut);
    // } catch (Exception e) {
    // System.out.println("Load xml exception" + e);
    // }
    // return outPut;
    // }
    /**
     * @return the queryAdmin
     */
    private String getQueryAdminLink() {
        return addGroups("部署信息查询", "deploy", items_base, false);
    // StringBuffer l = new StringBuffer(createBooksItemStart("deploy",
    // "部署信息查询"));
    //
    // // for (Item item : items_base) {
    // // addItemLink(l, item);
    // // }
    // for (int i = 0; i < items_base.length; i++) {
    // addItemLink(l, items_base[i], (i + 1) == items_base.length);
    // }
    //
    // return l.append(createBooksItemEnd()).toString();
    }

    private String addGroups(String groupName, String groupKey, Item[] items, boolean isLast) {
        StringBuffer l = new StringBuffer(createBooksItemStart(groupKey, groupName));
        // }
        for (int i = 0; i < items.length; i++) {
            addItemLink(l, items[i], (i + 1) == items.length);
        }
        return l.append(createBooksItemEnd(isLast)).toString();
    }

    private void addItemLink(StringBuffer l, Item item, boolean isLast) {
        if (!PermissionConstant.UN_CHECK.equals(item.permissionCode) && !this.getAppsFetcher().hasGrantAuthority(item.permissionCode)) {
            return;
        }
        // return createItem(id, text, "iconText.gif", item)
        l.append(createItemStart(item.linkid, item.name, StringUtils.EMPTY, item, isLast) + itemEnd());
    // l.append(createItem(item.linkid, item.name, item));//
    // "<item text=\"应用视图\" id=\"deploy_app\" im0=\"iconText.gif\"
    // im1=\"iconText.gif\" im2=\"iconText.gif\"/>");
    // }
    }

    // /**
    // * @param configAdmin the configAdmin to set
    // */
    // private static void setConfigAdmin(String configAdmin) {
    // BuildNavData.configAdmin = configAdmin;
    // }
    /**
     * @return the configAdmin
     */
    private String getConfigAdmin() {
        return addGroups("配置发布", "configAdmin", items_config, false);
    // StringBuffer result = new StringBuffer(this.createBooksItemStart(
    // "configAdmin", "配置发布"));
    //
    // for (Item item : items_config) {
    // addItemLink(result, item);
    // }
    // return result.append(this.createBooksItemEnd()).toString();
    }

    private String getTriggerConfiguration() {
        return addGroups("任务设置", "triggerconfig", trigger_config, false);
    // StringBuffer result = new StringBuffer(this.createBooksItemStart(
    // "triggerconfig", "任务设置"));
    //
    // for (Item item : trigger_config) {
    // addItemLink(result, item);
    // }
    // return result.append(this.createBooksItemEnd()).toString();
    }

    private String getBasicConfiguration() {
        return addGroups("基础配置", "basicconfig", basic_config, false);
    // StringBuffer result = new StringBuffer(this.createBooksItemStart(
    // "basicconfig", "基础配置"));
    //
    // for (Item item : basic_config) {
    // addItemLink(result, item);
    // }
    // return result.append(this.createBooksItemEnd()).toString();
    }

    private String getRoleSubMenu() {
        return addGroups("权限管理", "rolemanage", role_menu, true);
    // StringBuffer result = new StringBuffer(this.createBooksItemStart(
    // "rolemanage", ""));
    //
    // for (Item item : role_menu) {
    // // addItemLink(result, item);
    // }
    // return result.append(this.createBooksItemEnd()).toString();
    }

    /**
     * @return the navtreehead
     */
    protected String getNavtreehead() {
        return navtreehead;
    }

    /**
     * @return the tAIL
     */
    protected String getTail() {
        return TAIL;
    }

    // private static final Item[] items_base = new Item[] {
    // new Item(PermissionConstant.INDEX_QUERY, "index_query", "索引查询",
    // "/runtime/index_query.htm"),
    // new Item(PermissionConstant.PERMISSION_QUERY_REACT_TIME_VIEW,
    // "query_response", "查询响应时间", "/runtime/queryresponse.htm"),
    // new Item(PermissionConstant.PERMISSION_REALTIME_LOG_VIEW,
    // "query_log", "服务器日志", "/runtime/realtimelog.htm") };
    private static final Item[] items_base = new Item[] { // "/runtime/realtimelog.htm"),
    new Item(PermissionConstant.UN_CHECK, "index_query", "索引查询", "/runtime/index_query.htm"), // "/runtime/hsf_monitor.htm"),
    new Item(PermissionConstant.UN_CHECK, "zklockview", "ZK锁查询", "/runtime/zklockview.htm"), // "/runtime/hsf_monitor.htm"),
    new Item(PermissionConstant.UN_CHECK, "hdfs_view", "HDFS VIEW", "/runtime/hdfs_view.htm"), // "/runtime/hsf_monitor.htm"),
    new Item(PermissionConstant.UN_CHECK, "authority_func", "查看授权", "/runtime/query_authority_func.htm"), new Item(PermissionConstant.UN_CHECK, "dump_trigger_monitor", "DUMP触发监控", "/runtime/app_trigger_view.htm"), new Item(PermissionConstant.UN_CHECK, "server_config_view", "远端配置一览", "/runtime/server_config_view.htm"), new Item(PermissionConstant.UN_CHECK, "server_config_view", "应用申请", "/runtime/my_apply.htm"), new Item(PermissionConstant.UN_CHECK, "server_cluster_stat", "趋势监控", "/runtime/cluster_state.htm") };

    public static final Item[] items_config = new Item[] { // "查看组", "/runtime/jarcontent/grouplist.htm"),
    new Item(PermissionConstant.CONFIG_SNAPSHOT_LIST, "conf_project", "配置资源一览", "/runtime/jarcontent/snapshotset.htm"), new Item(PermissionConstant.APP_SCHEMA_UPDATE, "conf_project", "高级schema", "/runtime/schema_manage.htm?aid=1") };

    public static final Item[] trigger_config = new Item[] { // "/coredefine/coredefine_step1.htm"),
    new Item(PermissionConstant.TRIGGER_JOB_LIST, "trigger", "定时任务", "/trigger/app_list.htm"), // "/coredefine/corenodemanage_fluid_control.htm"),
    new Item(PermissionConstant.APP_CORE_MANAGE_VIEW, "trigger", "索引实例", "/coredefine/corenodemanage.htm"), // new
    new Item(PermissionConstant.APP_BUILD_RESULT_VIEW, "trigger", "任务中心", "/trigger/buildindexmonitor.htm") // Item(PermissionConstant.PERMISSION_CORE_CONFIG_RESOURCE_MANAGE,
    // "trigger", "触发器监控", "/trigger/triggermonitor.htm"),
    // coredefine/cluster_servers_view
    // new Item(PermissionConstant.CENTER_NODE_SERVERS_LIST, "trigger",
    // "服务器一览", "/coredefine/cluster_servers_view.htm")
    };

    public static final Item[] basic_config = new Item[] { // "dpt_manage", "部门管理", "/runtime/dpt_manage.htm"),
    new Item(PermissionConstant.APP_DEPARTMENT_LIST, "departmentlist", "部门管理", "/runtime/bizdomainlist.htm"), // "dpt_manage", "部门管理", "/runtime/dpt_manage.htm"),
    new Item(PermissionConstant.APP_LIST, "newApp", "应用管理", "/runtime/applist.htm"), // "dpt_manage", "部门管理", "/runtime/dpt_manage.htm"),
   // new Item(PermissionConstant.GLOBAL_PARAMETER_SET, "global_resource", "全局参数配置", "/runtime/config_file_parameters.htm"), // "dpt_manage", "部门管理", "/runtime/dpt_manage.htm"),
    new Item(PermissionConstant.UN_CHECK, "noble_app_list", "NobleApps", "/engineplugins/noble_app_list.htm"), //
      new Item(PermissionConstant.GLOBAL_OPERATION_LOG_LIST, "operation_log", "操作日志", "/runtime/operation_log.htm") };

    public static final Item[] role_menu = new Item[] { new Item(PermissionConstant.AUTHORITY_ROLE_LIST, "role_manage", "角色管理", "/runtime/role_list.htm"), new Item(PermissionConstant.AUTHORITY_FUNC_LIST, "role_manage", "funclist", "/runtime/func_list.htm"), new Item(PermissionConstant.AUTHORITY_USER_LIST, "usr_manage", "用户一览", "/runtime/usrlist.htm") };

    public static final Map<String, Item> FUNC_MAP;

    static {
        FUNC_MAP = new HashMap<String, Item>();
        Item old = null;
        try {
            for (Item item : items_config) {
                if (old != null && StringUtils.equals(old.getPermissionCode(), item.getPermissionCode())) {
                    old.setName(old.getName() + "," + item.getName());
                    continue;
                }
                Item it = (Item) item.clone();
                FUNC_MAP.put(item.getPermissionCode(), it);
                old = it;
            }
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Item implements Cloneable {

        private final String permissionCode;

        private final String linkid;

        private String name;

        private final String url;

        public String getUrl() {
            return url;
        }

        Item(String permissionCode, String linkid, String name, String url) {
            this.permissionCode = permissionCode;
            this.linkid = linkid;
            this.name = name;
            this.url = url;
        }

        public String getPermissionCode() {
            return permissionCode;
        }

        public String getLinkid() {
            return linkid;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
