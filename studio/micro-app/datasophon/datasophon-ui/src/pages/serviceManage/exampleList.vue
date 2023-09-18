<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-07-07 20:55:16
 * @FilePath: \ddh-ui\src\pages\serviceManage\exampleList.vue
-->
<template>
  <div class="example-page">
     <a-card class="mgb16 card-shadow">
      <a-row type="flex" align="middle">
        <a-col :span="16">
          <a-input placeholder="请输入主机名" class="w180 mgr12" @change="(value) => getVal(value, 'hostname')" :allowClear='true' />
            <a-select placeholder="请选择角色类型"  class="w180 mgr12" allowClear @change="(value) => getVal(value, 'serviceRoleName')">
            <a-select-option :value="item.serviceRoleName" v-for="(item,index) in cateList" :key="index">{{item.serviceRoleName}}</a-select-option>
          </a-select>
          <a-select placeholder="请选择角色组" class="w180 mgr12" :allowClear='true' @change="(value) => getVal(value, 'roleGroupId')">
            <a-select-option :value="item.id" v-for="(item,index) in groupList" :key="index">{{item.roleGroupName}}</a-select-option>
          </a-select>
          <a-select placeholder="请选择状态" class="w180 mgr12" :allowClear='true' @change="(value) => getVal(value, 'serviceRoleState')">
            <a-select-option :value="item.id" v-for="(item,index) in serviceRoleState" :key="index">{{item.key}}</a-select-option>
          </a-select>
          <a-button class type="primary" icon="search" @click="onSearch"></a-button>
        </a-col>
        <a-col :span="8" style="text-align: right">
         <a-dropdown>
        <a-menu slot="overlay" @click="handleMenuClick">
          <a-menu-item key="start">启动</a-menu-item>
          <a-menu-item key="stop">停止</a-menu-item>
          <a-menu-item key="reStart">重启</a-menu-item>
          <a-menu-item key="decommission" v-show="serviceId==36||serviceId==37">退役</a-menu-item>
          <a-menu-item key="roleGroup">分配角色组</a-menu-item>
          <a-menu-item key="del">删除</a-menu-item>
        </a-menu>
        <a-button class="mgr12" type="primary">
          选择操作
          <a-icon type="down" />
        </a-button>
      </a-dropdown>
      <a-button type="primary" @click="addExample" class="mgr12">添加新实例</a-button>
      <a-button type="primary" @click="addCharacter({})">添加角色组</a-button>
        </a-col>
      </a-row>
    </a-card>
    <a-table :columns="loadTable()" @change="tableChange" :loading="loading" class="release-table-custom" :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" :dataSource="dataSources" rowKey="id" :pagination="pagination"></a-table>
    <!-- 配置集群的modal -->
    <a-modal v-if="visible" title :visible="visible" :maskClosable="false" :closable="false" :width="1576" :confirm-loading="confirmLoading" @cancel="handleCancel" :footer="null">
      <Steps :clusterId="clusterId" stepsType="service-example" :steps4Data="steps4Data" />
    </a-modal>
    <!-- 查看日志的modal -->
    <a-modal v-if="logsVisible" title="查看日志" :visible="logsVisible" :maskClosable="false" :closable="false" :width="1576" @cancel="handleLogCancel" :footer="null" >
      <a-button @click="handleLogCancel" class="mgb16" style="height: 28px;position: absolute;right: 20px;top:15px;z-index:2" icon="close" />
      <LOGS :logData="logData" @getLog="getLog" :logsVisible="logsVisible" />
    </a-modal>
  </div>
</template>

<script>
import Steps from "@/components/steps";
import LOGS from "@/components/logs";
import { mapMutations ,mapState} from "vuex";
import AddCharacter from "./addCharacter.vue";
import AllotCharacter from "./allotCharacter.vue";
export default {
  components: { LOGS, Steps },
  name: "exampleList",
  props: {
    serviceId: String,
  },
  provide() {
    return {
      handleCancel: this.handleCancel,
      onSearch: this.onSearch,
    };
  },
  data() {
    return {
      params: {},
      logsVisible: false,
      visible: false,
      confirmLoading: false,
      loading: false,
      dataSources: [],
      groupList:[],
      cateList:[],
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      steps4Data: {
        serviceIds: [],
        serviceNames: [],
      },
      serviceRoleState:[
        {id: "1", key: "正在运行" },
        {id: "2", key: "停止" },
        {id: "3", key: "告警" },
        {id: "4", key: "退役中" },
        {id: "5", key: "已退役" },
      ],
      tableColumns: [
        { title: "序号", key: "index" },
        { title: "角色类型", key: "serviceRoleName" },
        { title: "主机", key: "hostname" },
        { title: "角色组", key: "roleGroupName" },
        { title: "状态", key: "serviceRoleState" },
        {
          title: "操作",
          key: "action",
        },
      ],
      timer: null,
      refreshData:null,
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      exampleId: "",
      changeRoleState:false ,
      changeRoleName:false ,
      changeGroupName:false
    };
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    ...mapState("setting",["menuData"]),
    handleLogCancel() {
      this.logsVisible = false;
    },
    getVal(val, filed) {
      if (filed === "serviceRoleState") this.changeRoleState = true;
      if (filed === "roleGroupId") this.changeGroupName = true;
      if (filed === "serviceRoleName") this.changeRoleName = true;
      this.params[`${filed}`] =
         filed === "serviceRoleState" ? val : filed === "roleGroupId"? val: filed === "serviceRoleName"? val :val.target.value
    },
    getServiceRoleType() {
      const params={
        serviceInstanceId :this.$route.params.serviceId || "",
      }
      //角色组类型
      this.$axiosPost(global.API.getServiceRoleType, params).then((res) => {
        if (res.code !== 200) return
        this.cateList = res.data
      }
      ) 
      //角色组列表
      this.$axiosPost(global.API.getRoleGroupList, params).then((res) => {
        if (res.code !== 200) return  //this.$message.error('获取角色组列表失败')
        this.groupList = res.data 
      })
    },
    handleMenuClick(key) {
      if (key.key === "del") {
        this.delExample();
        return false;
      }
      if (key.key === "roleGroup") {
        this.allotCharacter();
        return false;
      }
      this.optServices(key);
    },
    delExample() {
      if (this.selectedRowKeys.length < 1) {
        this.$message.warning("请至少选择一个实例");
        return false;
      }
      this.$confirm({
        width: 450,
        title: () => {
          return (
            <div style="font-size: 22px;">
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content: (
          <div style="margin-top:20px">
            <div style="padding:0 65px;font-size: 16px;color: #555555;">
              确认删除吗？
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.confirmDelExample()}
              >
                确定
              </a-button>
              <a-button
                style="margin-right:10px;"
                onClick={() => this.$destroyAll()}
              >
                取消
              </a-button>
            </div>
          </div>
        ),
        icon: () => {
          return <div />;
        },
        closable: true,
      });
    },
    confirmDelExample() {
      let params = {
        serviceRoleInstancesIds: this.selectedRowKeys.join(","),
      };
      this.$axiosPost(global.API.deleteExample, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.$destroyAll();
          this.pollingSearch();
          this.selectedRowKeys = [];
        }
      });
    },
    optServices(item) {
      if (this.selectedRowKeys.length < 1) {
        this.$message.warning("请至少选择一个实例");
        return false;
      }
      this.$confirm({
        width: 450,
        title: () => {
          return (
            <div style="font-size: 22px;">
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content: (
          <div style="margin-top:20px">
            <div style="padding:0 65px;font-size: 16px;color: #555555;">
              {'确认' + (item.key=='start'?'启动':item.key=='stop'?'停止':item.key=='reStart'?'重启':item.key=='decommission'?'退役':"") +'吗？'}
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.ensureServices(item)}
              >
                确定
              </a-button>
              <a-button
                style="margin-right:10px;"
                onClick={() => this.$destroyAll()}
              >
                取消
              </a-button>
            </div>
          </div>
        ),
        icon: () => {
          return <div />;
        },
        closable: true,
      });
    
    },
    ensureServices(item){
      if(item.key==="decommission"){
        let params = {
          serviceRoleInstanceIds: this.selectedRowKeys.join(","),
        };
        this.$axiosPost(global.API.decommissionNode, params).then(
          (res) => {
            if (res.code === 200) {
              this.$message.success("操作成功");
              this.pollingSearch();
              this.selectedRowKeys = [];
            }
          }
        );
      }else{
        let params = {
          clusterId: this.clusterId,
          commandType:
          item.key === "stop"
            ? "STOP_SERVICE"
            : item.key === "start"
              ? "START_SERVICE"
              : "RESTART_SERVICE",
          serviceInstanceId: this.$route.params.serviceId || "",
          serviceRoleInstancesIds: this.selectedRowKeys.join(","),
        };
        this.$axiosPost(global.API.generateServiceRoleCommand, params).then(
          (res) => {
            if (res.code === 200) {
              this.$message.success("操作成功");
              this.selectedRowKeys = [];
              this.pollingSearch();
              this.showClusterSetting(true);
            }
          }
        );
      }
      this.$destroyAll()
    },
    allotCharacter(){
      if (this.selectedRowKeys.length < 1) {
        this.$message.warning("请至少选择一个实例");
        return false;
      }
      const self = this;
      let width = 520;
      let title =  "分配角色组";
      let serviceId = {id:this.$route.params.serviceId || ""}
      let roleInstanceIds  =this.selectedRowKeys
      let content = (
        <AllotCharacter serviceId={serviceId} roleInstanceIds={roleInstanceIds} callBack={() => self.pollingSearch(),()=>{this.selectedRowKeys = []}} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    addExample() {
      let serviceName = [];
      let frameServiceId = null;
      const serviceId = this.$route.params.serviceId || "";
      const menuData = JSON.parse(localStorage.getItem("menuData")) || [];
      const arr = menuData.filter((item) => item.path === "service-manage");
      if (arr.length > 0) {
        arr[0].children.map((item) => {
          if (item.meta.params.serviceId == serviceId) {
            serviceName = [
              {
                serviceName: item.name,
                serviceId: item.meta.obj.frameServiceId,
              },
            ];
            frameServiceId = item.meta.obj.frameServiceId;
          }
        });
      }
      this.steps4Data = {
        serviceIds: [frameServiceId],
        serviceNames: serviceName,
      };
      this.visible = true;
    },
    addCharacter() {
      const self = this;
      let width = 520;
      let title =  "新建角色组";
      let serviceId = {id:this.$route.params.serviceId || ""}
      let content = (
        <AddCharacter serviceId={serviceId}  callBack={() => self.pollingSearch()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    handleCancel(e) {
      this.visible = false;
    },
    loadTable() {
      let that = this;
      let columns = that.tableColumns;
      return columns.map((item, index) => {
        return {
          title: item.title,
          key: item.key,
          fixed: item.fixed ? item.fixed : "",
          width: item.width ? item.width : "",
          ellipsis: item.ellipsis ? item.ellipsis : "",
          customRender: (text, record, index) => {
            if (item.key == "index") {
              return `${index + 1}`;
            } else if (item.key === "serviceRoleName") {
              return (
                <span class="flex-container">
                  <span
                    class={[
                      "circle-point",
                      record.serviceRoleStateCode === 1
                        ? "success-point"
                        : record.serviceRoleStateCode === 2
                          ? "error-point"
                          : "configured-point",
                    ]}
                  />
                  {record[item.key]}
                </span>
              );
            } else if (item.key === "serviceRoleState") {
              return (
                <span class="flex-container">
                  <svg-icon
                    icon-class={
                      record.serviceRoleStateCode === 1
                        ? "success-status"
                        : record.serviceRoleStateCode === 2
                          ? "stop-status"
                          : "gaojing"
                    }
                    class={[
                      "mgr6",
                      record.serviceRoleStateCode === 1
                        ? "success-status-color"
                        : record.serviceRoleStateCode === 2
                          ? "error-status-color"
                          : "configured-status-color", // 告警的颜色
                    ]}
                  />
                  {record[item.key]}
                </span>
              );
            } else if (item.key === "action") {
              return (
                <span class="flex-container">
                  <a class="btn-opt" onClick={() => this.getLog(record)}>
                    查看日志
                  </a>
                </span>
              );
            } else {
              return <span title={record[item.key]}> {record[item.key]} </span>;
            }
          },
        };
      });
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      this.selectedRowKeys = selectedRowKeys;
    },
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.pollingSearch();
    },
    getLog(row) {
      if (row && row.id) this.exampleId = row.id;
      let exampleId = this.exampleId ||row.id;
      this.$axiosPost(global.API.getLog, {
        serviceRoleInstanceId: exampleId,
      }).then((res) => {
        if(!row) this.$message.success("刷新日志成功")
        this.logsVisible = true;
        this.logData = res.data;
      });
    },
    bindTime(){
      let exampleId = this.exampleId
      this.$axiosPost(global.API.getLog, {
        serviceRoleInstanceId: exampleId,
      }).then((res) => {
        this.logData = res.data;
      });
    },
    //   查询
    onSearch() {
      this.pagination.current = 1;
      this.getExampleList();
    },
    getExampleList(flag) {
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        serviceInstanceId: this.$route.params.serviceId,
        hostname:this.params.hostname ||"",
        serviceRoleState:this.changeRoleState ? this.params.serviceRoleState||"":"",
        roleGroupId:this.changeGroupName ? this.params.roleGroupId||"":"",
        serviceRoleName:this.changeRoleName ? this.params.serviceRoleName||"":""
      };
      this.$axiosPost(global.API.instanceList, params).then((res) => {
        this.loading = false;
        if (res.code === 200) {
          this.dataSources = res.data;
          this.pagination.total = res.total;
        }
      });
    },
    pollingSearch() {
      this.getExampleList(); // 先立马刷一次
      let self = this;
      if (self.timer) clearInterval(self.timer);
      self.timer = setInterval(() => {
        self.getExampleList(true);
      }, global.intervalTime);
    },
  },
  watch:{
    logsVisible: {
      handler(val) {
        if(val){
          this.refreshData=setInterval(()=>{
            this.bindTime()
          },10000);
        }else{
          clearInterval(this.refreshData);
          this.refreshData=null;
        }

      },
      immediate: true,
      deep:true 
    },
  },
  mounted() {
    this.pollingSearch();
    this.getServiceRoleType()
  },
  activated() {
    clearInterval(this.timer);
    this.pollingSearch();
  },
  deactivated() {
    clearInterval(this.timer);
  },
  beforeDestroy() {
    clearInterval(this.timer);
    clearInterval(this.refreshData);
    this.refreshData=null;
  },
};
</script>

<style lang="less" scoped>
.example-page {
  position: relative;
  background: #fff;
  // padding: 20px;
  .circle-point {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: block;
    z-index: 1000;
    margin-right: 6px;
  }
  .hide-point {
    visibility: hidden;
  }
  .success-point {
    background: @success-status-color;
  }
  .error-point {
    background: @error-status-color;
  }
  .configured-point {
    background: @configured-status-color;
  }
  .addExample {
    position: absolute;
    top: -55px;
    right: 0px;
  }
}
/deep/ .ant-modal-body {
  padding: 0;
}
/deep/ .ant-modal {
  top: 61px;
  .ant-modal-content {
    border-radius: 4px;
  }
}
</style>
