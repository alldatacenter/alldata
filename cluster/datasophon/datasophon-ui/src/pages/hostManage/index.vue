<!--
 * @Author: mjzhu
 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2022-11-25 16:56:39
 * @FilePath: \ddh-ui\src\pages\hostManage\index.vue
-->

<template>
  <div class="host-manage steps">
    <a-card class="mgb16 card-shadow">
      <a-row type="flex" align="middle">
        <a-col :span="16">
          <a-input placeholder="请输入主机名" class="w180 mgr12" @change="(value) => getVal(value, 'hostname')" allowClear />
          <a-select placeholder="请选择Cpu架构" class="w180 mgr12" :allowClear="true" @change="(value) => getVal(value, 'cpuArchitecture')">
            <a-select-option :value="item.id" v-for="(item,index) in cpuArchitecture" :key="index">{{item.key}}</a-select-option>
          </a-select>
          <a-select placeholder="请选择状态" class="w180 mgr12" :allowClear="true" @change="(value) => getVal(value, 'hostState')">
            <a-select-option :value="item.id" v-for="(item,index) in hostState" :key="index">{{item.key}}</a-select-option>
          </a-select>
          <a-button class type="primary" icon="search" @click="onSearch"></a-button>
        </a-col>
        <a-col :span="8" style="text-align: right">
          <a-dropdown>
            <a-menu slot="overlay" @click="handleMenuClick">
              <!-- <a-menu-item key="distribution">分配机架</a-menu-item> -->
              <a-menu-item key="handAgent">Agent重新分发</a-menu-item>
              <a-menu-item key="handLabel">分配标签</a-menu-item>
              <a-menu-item key="handRack">分配机架</a-menu-item>
              <a-menu-item key="del">删除</a-menu-item>
            </a-menu>
            <a-button class="mgr12" type="primary">
              选择操作
              <a-icon type="down" />
            </a-button>
          </a-dropdown>
          <a-button type="primary" @click="createUser({})">添加新主机</a-button>
          <a-dropdown>
            <a-menu slot="overlay" @click="handleLabelClick">
              <a-menu-item key="add">添加标签</a-menu-item>
              <a-menu-item key="del">删除标签</a-menu-item>
            </a-menu>
            <a-button class="mgl12" type="primary">
              标签管理
              <a-icon type="down" />
            </a-button>
          </a-dropdown>
          <a-dropdown>
            <a-menu slot="overlay" @click="handleRackClick">
              <a-menu-item key="add">添加机架</a-menu-item>
              <a-menu-item key="del">删除机架</a-menu-item>
            </a-menu>
            <a-button class="mgl12" type="primary">
              机架管理
              <a-icon type="down" />
            </a-button>
          </a-dropdown>
        </a-col>
      </a-row>
    </a-card>
    <a-card class="card-shadow">
      <div class="table-info steps-body">
        <a-table @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" rowKey="id" :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" :pagination="pagination"></a-table>
      </div>
      <!-- 配置集群的modal -->
      <a-modal v-if="visible" title :visible="visible" :maskClosable="false" :closable="false" :width="1576" :confirm-loading="confirmLoading" @cancel="handleCancel" :footer="null">
        <Steps :clusterId="clusterId" stepsType="hostManage" />
      </a-modal>
    </a-card>
  </div>
</template>

<script>
import { mapActions, mapState } from "vuex";

import Steps from "@/components/steps";
import AistributionRack from "./distributionRack.vue";
import RoleModal from "./roleModal.vue";
import AddLabel from "./addLabel.vue";
import AddRack from './addRack.vue'
export default {
  name: "HOSTMANAGE",
  components: { Steps },
  provide() {
    return {
      handleCancel: this.handleCancel,
      onSearch: this.onSearch,
    };
  },
  data() {
    return {
      params: {},
      selectedRowKeys: [],
      visible: false,
      confirmLoading: false,
      changeCpuArchitecture: false,
      changeUsername: false,
      hostnames: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      sort: {},
      dataSource: [],
      loading: false,
      hostState: [
        { id: "1", key: "正常" },
        { id: "2", key: "掉线" },
        { id: "3", key: "存在告警" },
      ],
      cpuArchitecture: [
        { id: "x86_64", key: "x86_64" },
        { id: "aarch64", key: "aarch64" },
      ],
      columns: [
        {
          title: "序号",
          key: "index",
          width: 70,
          customRender: (text, row, index) => {
            return (
              <span>
                {parseInt(
                  this.pagination.current === 1
                    ? index + 1
                    : index +
                        1 +
                        this.pagination.pageSize * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        {
          title: "状态",
          key: "username",
          dataIndex: "username",
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                <svg-icon
                  icon-class={
                    row.hostState === 1
                      ? "success-status"
                      : row.hostState === 2
                      ? "stop-status"
                      : row.hostState === 3
                      ? "gaojing"
                      : ""
                  }
                  class={[
                    "mgr6",
                    row.hostState === 1
                      ? "success-status-color"
                      : row.hostState === 2
                      ? "error-status-color"
                      : row.hostState === 3
                      ? "configured-status-color"
                      : "", // 告警的颜色
                  ]}
                />
                {row.hostState === 1
                  ? "正常"
                  : row.hostState === 2
                  ? "掉线"
                  : "存在告警"}
              </span>
            );
          },
        },
        {
          title: "主机名",
          key: "hostname",
          dataIndex: "hostname",
          sorter: true,
          sortDirections: ["descend", "ascend"],
        },
        { title: "IP地址", key: "ip", dataIndex: "ip" },
        { title: "核数", key: "coreNum", dataIndex: "coreNum" },
        //
        {
          title: "内存使用",
          key: "usedMem",
          dataIndex: "usedMem",
          width: 132,
          sorter: true,
          sortDirections: ["descend", "ascend"],
          customRender: (text, row) => {
            let usedMem = row.usedMem ? row.usedMem : 0;
            let totalMem = row.totalMem ? row.totalMem : 0;
            const percent = (usedMem / totalMem).toFixed(2) * 100;
            return (
              <div>
                <div class="font-size12">
                  {usedMem}GB/{totalMem}GB
                </div>
                <a-progress
                  class="use-progress"
                  strokeLinecap="square"
                  strokeColor={
                    percent < 70
                      ? "#01AA72"
                      : percent < 90
                      ? "#FF7E01"
                      : "#FF5656"
                  }
                  percent={percent}
                  size="small"
                  status="active"
                  showInfo={false}
                />
              </div>
            );
          },
        },
        {
          title: "磁盘使用",
          key: "usedDisk",
          dataIndex: "usedDisk",
          width: 132,
          sorter: true,
          sortDirections: ["descend", "ascend"],
          customRender: (text, row) => {
            let usedDisk = row.usedDisk ? row.usedDisk : 0;
            let totalDisk = row.totalDisk ? row.totalDisk : 0;
            const percent = (usedDisk / totalDisk).toFixed(2) * 100;
            return (
              <div>
                <div class="font-size12">
                  {usedDisk}GB/{totalDisk}GB
                </div>
                <a-progress
                  class="use-progress"
                  strokeLinecap="square"
                  strokeColor={
                    percent < 70
                      ? "#01AA72"
                      : percent < 90
                      ? "#FF7E01"
                      : "#FF5656"
                  }
                  percent={percent}
                  size="small"
                  status="active"
                  showInfo={false}
                />
              </div>
            );
          },
        },
        {
          title: "平均负载",
          key: "averageLoad",
          dataIndex: "averageLoad",
          sorter: true,
          sortDirections: ["descend", "ascend"],
        },
        { title: "标签", key: "nodeLabel", dataIndex: "nodeLabel" },
        { title: "机架", key: "rack", dataIndex: "rack" },
        {
          title: "Cpu架构",
          key: "cpuArchitecture",
          dataIndex: "cpuArchitecture",
        },
        {
          title: "角色",
          key: "serviceRoleNum",
          dataIndex: "serviceRoleNum",
          customRender: (text, row, index) => {
            return (
              <span
                class="role-name"
                onClick={() => {
                  this.seeRole(row);
                }}
              >
                {text}
              </span>
            );
          },
        },
      ],
      sortDirections: ["descend", "ascend"],
    };
  },
  watch: {
    clusterId: {
      handler(val, oldVal) {
        if (val !== oldVal) {
          this.onSearch();
        }
      },
    },
  },
  computed: {
    ...mapState({
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
    clusterId() {
      return this.setting.clusterId;
    },
  },
  methods: {
    seeRole(row) {
      let width = 520;
      let title = "角色列表";
      let content = <RoleModal detail={row} />;
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
    tableChange(pagination, filters, sort) {
      console.log(filters);
      console.log(sort);
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.sort = {
        orderField: sort.field || "",
        orderType: sort.order || "",
      };
      this.getHostListByPage();
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      this.selectedRowKeys = selectedRowKeys;
      this.hostnames = row.map((item) => item.hostname);
    },
    getVal(val, filed) {
      if (filed === "cpuArchitecture") this.changeCpuArchitecture = true;
      if (filed === "hostState") this.changeUsername = true;
      this.params[`${filed}`] =
        filed === "cpuArchitecture"
          ? val
          : filed === "hostState"
          ? val
          : val.target.value;
    },
    //   查询
    onSearch() {
      this.pagination.current = 1;
      this.getHostListByPage();
    },
    createUser(obj) {
      this.visible = true;
    },
    getHostListByPage() {
      this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId || "",
        hostname: this.params.hostname || "",
        orderField:
          this.sort.orderField == "hostname"
            ? "hostname"
            : this.sort.orderField == "usedMem"
            ? "used_mem"
            : this.sort.orderField == "usedDisk"
            ? "used_disk"
            : this.sort.orderField == "averageLoad"
            ? "average_load"
            : "",
        orderType:
          this.sort.orderType == "descend"
            ? "desc"
            : this.sort.orderType == "ascend"
            ? "asc"
            : "",
        cpuArchitecture: this.changeCpuArchitecture
          ? this.params.cpuArchitecture || ""
          : "",
        hostState: this.changeUsername ? this.params.hostState || "" : "",
      };
      this.$axiosPost(global.API.getHostListByPage, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        this.pagination.total = res.total;
      });
    },
    handleRackClick(e) {
      this.addRack(e.key);
    },
    addRack(key){
      const self = this;
      let width = 520;
      let title = key === 'add' ? "添加机架" : key === 'del' ? "删除机架" : '分配机架';
      let content = (
        <AddRack type={key} hostIds={this.selectedRowKeys} callBack={() => self.refresh()} />
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
    handleLabelClick(e) {
      this.addLabel(e.key);
    },
    addLabel(key){
      const self = this;
      let width = 520;
      let title = key === 'add' ? "添加标签" : key === 'del' ? "删除标签" : '分配标签';
      let content = (
        <AddLabel type={key} hostIds={this.selectedRowKeys} callBack={() => self.refresh()} />
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
    refresh() {
      debugger
      this.selectedRowKeys = []
      this.onSearch()
    },
    handleMenuClick(key) {
      if (this.selectedRowKeys.length < 1) {
        this.$message.warning("请至少选择一台主机");
        return false;
      }
      if (key.key === "del") {
        this.delExample();
        return false;
      }
      if (key.key === 'handLabel') {
        this.addLabel(key.key)
        return false
      }
      if (key.key === 'handRack') {
        this.addRack(key.key)
        return false
      }
      if (key.key === "handAgent") {
        this.handAgent();
        return false;
      }
      this.distributionRack();
    },
    handAgent() {
      let params = {
        hostnames: this.hostnames.join(","),
        clusterId: this.clusterId,
      };
      this.$axiosPost(global.API.reStartDispatcherHostAgent, params).then(
        (res) => {
          if (res.code === 200) {
            this.$message.success("操作成功");
            this.selectedRowKeys = [];
            this.hostnames = [];
            this.$destroyAll();
            this.getHostListByPage();
          }
        }
      );
    },
    delExample() {
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
                onClick={() => this.confirmDelRack()}
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
    confirmDelRack() {
      let params = {
        hostId: this.selectedRowKeys.join(","),
      };
      this.$axiosPost(global.API.deleteRack, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.selectedRowKeys = [];
          this.$destroyAll();
          this.getHostListByPage();
        }
      });
    },
    distributionRack(obj) {
      const self = this;
      let width = 520;
      let title = "分配机架";
      let content = (
        <AistributionRack
          detail={obj}
          callBack={() => self.getHostListByPage()}
        />
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
  },
  mounted() {
    this.getHostListByPage();
  },
};
</script>

<style lang="less" scoped>
.host-manage {
  background: #f5f7f8;
  .use-progress {
    /deep/ .ant-progress-inner {
      border-radius: 0px;
      height: 7px;
      .ant-progress-bg {
        height: 7px !important;
      }
    }
  }
  .btn-opt {
    border-radius: 1px;
    font-size: 12px;
    color: #2872e0;
    letter-spacing: 0;
    font-weight: 400;
    margin: 0 5px;
  }
  .role-name {
    color: #2872e0;
    cursor: pointer;
    &:hover {
      text-decoration: underline;
    }
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
