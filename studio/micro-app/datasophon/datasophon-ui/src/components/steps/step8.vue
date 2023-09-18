<!--
 * @Author: mjzhu
 * @describe: step8-安装并启动服务 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-10-31 16:01:51
 * @FilePath: \ddh-ui\src\components\steps\step8.vue
-->
<template>
  <div class="steps8 steps">
    <div class="steps-title flex-bewteen-container pdr30">
      <div>
        <a-icon v-if="currentPage !== 1" type="left" @click="goBack" />
         {{title}}
        </div>
      <!-- <div class="close-x" @click="handleCancel">X</div> -->
      <a-button @click="handleCancel" class="mgb16" style="height: 28px;position: absolute;right: 20px;top:15px;z-index:2" icon="close" />
      <!-- <div v-if="currentPage === 1" class="flex-bewteen-container"> -->
        <!-- <div class="status-num mgr20">
          <span :class="[hostType === 'all' ? 'host-selected' : '']" @click="changeType('all')">
            全部
            <span>10</span>
          </span>
          <a-divider type="vertical" />
          <span :class="[hostType === '1' ? 'host-selected' : '']" @click="changeType('1')">
            安装中
            <span>10</span>
          </span>
          <a-divider type="vertical" />
          <span :class="[hostType === '2' ? 'host-selected' : '']" @click="changeType('2')">
            成功
            <span>10</span>
          </span>
          <a-divider type="vertical" />
          <span :class="[hostType === '3' ? 'host-selected' : '']" @click="changeType('3')">
            失败
            <span>10</span>
          </span>
        </div>-->
       <!-- <a-button type="primary" @click="retryHost('all')">全部重试</a-button>-->
      <!-- </div> -->
    </div>
    <div class="table-info mgt16 steps-body" style="overflow-y: visible;max-height: 700px;">
      <a-table v-if="currentPage === 1" @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" rowKey="commandId" :pagination="pagination"></a-table>
      <a-table v-if="[2,3].includes(currentPage)" @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" rowKey="hostCommandId" :pagination="pagination"></a-table>
      <LOGS v-if="currentPage === 4" :logData="logData" :hideCancel="true" />
    </div>
    <!-- <div class="cluster-setting-footer pdr30" v-if="stepsType === 'cluster-setting'">
      <a-button type="primary" @click="handleCancel">关闭</a-button>
    </div> -->
  </div>
</template>
<script>
import { mapActions, mapState } from "vuex";
import LOGS from "@/components/logs";

export default {
  inject: ["clusterId", "handleCancel"],
  props: {
    stepsType: {
      type: String,
      default: "cluster",
    },
  },
  components: { LOGS },
  data() {
    return {
      hostType: "all",
      title: "安装并启动服务",
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      dataSource: [],
      timer1: null,
      timer2: null,
      timer3: null,
      loading: false,
      currentPage: 1,
      commandId: "", // 第二个列表请求页面需要的参数
      hostname: "", // 第三个列表请求页面需要的参数
      commandHostId: "", // 第三个列表请求页面需要的参数
      commandName: "",
      logData: "",
    };
  },
  watch: {
    stepsType: {
      handler(val) {
        if (this.stepsType === "cluster-setting") {
          this.title = "后台操作";
        }
        if (this.stepsType === "service") {
          // this.currentSteps = 4
        }
      },
      immediate: true,
    },
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
      setting: (state) => state.setting
    }),
    columns() {
      let arr = [
        {
          title: "序号",
          key: "index",
          width: 120,
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
          title:
            this.currentPage === 1
              ? "命令"
              : this.currentPage === 2
                ? "主机"
                : "指令名称",
          key: this.currentPage === 2 ? "hostname" : "commandName",
          dataIndex: this.currentPage === 2 ? "hostname" : "commandName",
          width: 300,
          customRender: (text, row, index) => {
            return this.currentPage !== 3 ? (
              <span class={"command-name"} onClick={() => this.seeDetail(row)}>
                {text}
              </span>
            ) : (
              <span>{text}</span>
            );
          },
        },
        {
          title: "状态",
          key: "commandProgress",
          dataIndex: "commandProgress",
          customRender: (text, row, index) => {
            return (
              <span>
                {row.commandStateCode === 1 ? (
                  <a-progress
                    class="progress-warp"
                    percent={text}
                    status="active"
                  />
                ) : row.commandStateCode === 2 ? (
                  <a-progress class="progress-warp" percent={text} />
                ) : row.commandStateCode === 4 ? (
                  <a-progress class="progress-warp" strokeColor='#FFA53D' format={()=><a-icon style="color:#FFA53D" type="exclamation-circle" />} percent={text} />
                ) : (
                  <a-progress
                    class="progress-warp"
                    percent={text}
                    status="exception"
                  />
                )}
              </span>
            );
          },
        },
      ];
      if (this.currentPage === 1) {
        arr.push(
          {
            title: "开始时间",
            key: "createTime",
            dataIndex: "createTime",
            width: 180,
          },
          {
            title: "持续时间",
            key: "durationTime",
            dataIndex: "durationTime",
            width: 160,
          }
        );
      }
      if (this.currentPage === 3) {
        arr.push({
          title: "日志信息",
          key: "resultMsg",
          dataIndex: "resultMsg",
          // width: 140,
          customRender: (text, row, index) => {
            return (
              <span
                class="flex-container command-name"
                onClick={() => this.seeDetail(row)}
              >
                查看日志
              </span>
            );
          },
        });
      }
      return arr;
    },
  },
  methods: {
    changeType(type) {
      this.hostType = type;
    },
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.pollingSearch();
    },
    getServiceList(flag) {
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      };
      if (this.currentPage === 2) params.commandId = this.commandId;
      if (this.currentPage === 3) {
        params.hostname = this.hostname;
        params.commandHostId = this.commandHostId;
      }
      const ajaxApi =
        this.currentPage === 1
          ? global.API.getServiceCommandlist
          : this.currentPage === 2
            ? global.API.getServiceHostList
            : global.API.getServiceRoleOrderList;
      // todo：这个接口地址需要替换
      this.$axiosPost(ajaxApi, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        this.pagination.total = res.total;
      });
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      let arr = row.filter((item) => item.commandStateCode !== 3);
      this.selectedRow = arr;
      this.selectedRowKeys = selectedRowKeys;
    },
    goBack() {
      clearInterval(this.timer1);
      clearInterval(this.timer2);
      clearInterval(this.timer3);
      this.currentPage--;
      this.loading = true;
      if (this.currentPage === 2) {
        this.title = this.commandName;
      }
      if (this.currentPage === 1) {
        this.title = "安装并启动服务";
      }
      if (this.currentPage === 3) {
        this.title = this.hostname;
      }
      this.dataSource = [];
      this.pagination.total = 0;
      this.pagination.current = 1;
      this.pollingSearch();
    },
    seeDetail(row) {
      clearInterval(this.timer1);
      clearInterval(this.timer2);
      clearInterval(this.timer3);
      this.pagination.current = 1;
      if (this.currentPage === 3) {
        this.loading = true;
        this.hostname = row.hostname;
        this.hostCommandId = row.hostCommandId;
        this.getLog();
        return false;
      }
      this.currentPage++;
      this.loading = true;
      if (this.currentPage === 2) {
        this.commandName = row.commandName;
        this.title = row.commandName;
        this.commandId = row.commandId;
      }
      if (this.currentPage === 3) {
        this.title = row.hostname;
        this.commandHostId = row.commandHostId;
        this.hostname = row.hostname;
      }
      this.dataSource = [];
      this.pagination.total = 0;
      this.pollingSearch();
    },
    getLog() {
      this.$axiosPost(global.API.getHostCommandLog, {
        hostCommandId: this.hostCommandId,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      }).then((res) => {
        this.loading = false
        this.logData = res.data;
        this.currentPage++;
        this.title = "查看日志";
      });
    },
    // 三秒去刷一下
    pollingSearch() {
      this.getServiceList(); // 先立马刷一次
      let self = this;
      if (self[`timer${this.currentPage}`])
        clearInterval(self[`timer${this.currentPage}`]);
      self[`timer${this.currentPage}`] = setInterval(() => {
        self.getServiceList(true);
      }, global.intervalTime);
    },
    // 重试
    retryHost(row) {
      let commandIds = "";
      if (row === "all") {
        if (this.selectedRowKeys.length < 1) {
          this.$message.warning("请至少选择一条命令！");
          return false;
        }
        if (this.selectedRow.length > 0) {
          this.$message.warning("目前只支持失败的命令进行重试操作！");
          return false;
        }
        commandIds = this.selectedRowKeys.join(",");
      } else {
        commandIds = row.commandId;
      }
      const params = {
        commandIds,
        commandType: this.steps.commandType,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      };
      this.$axiosPost(global.API.startExecuteCommand, params).then((res) => {
        this.selectedRowKeys = [];
        this.$message.success(`操作成功`);
        this.pollingSearch();
      });
    },
    // 取消
    cancelHost(row) {},
    // 主机环境校验是否完成 是否可以进入下一步
    async dispatcherHostAgentCompleted(callback) {
      const params = {
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      };
      // 等待网络请求结束
      let flag = await this.$axiosPost(
        global.API.dispatcherHostAgentCompleted,
        params
      );
      // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
      if (callback) {
        callback(flag);
      }
    },
  },
  mounted() {
    this.pollingSearch();
  },
  beforeDestroy() {
    clearInterval(this.timer1);
    clearInterval(this.timer2);
    clearInterval(this.timer3);
  },
};
</script>
<style lang="less" scoped>
.steps8 {
  .status-num {
    span {
      margin: 0 4px;
      font-size: 14px;
      color: #555555;
      letter-spacing: 0;
      font-weight: 400;
      cursor: pointer;
    }
    span.host-selected {
      color: @primary-color;
      span {
        color: @primary-color;
      }
    }
  }
  .progress-warp {
    width: 80%;
  }
  .command-name {
    color: @primary-color;
    cursor: pointer;
    &:hover {
      text-decoration: underline;
    }
  }
  .cluster-setting-footer {
    display: flex;
    justify-content: flex-end;
  }
  .close-x {
    cursor: pointer;
  }
}
</style>