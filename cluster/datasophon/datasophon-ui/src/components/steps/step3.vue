<!--
 * @Author: mjzhu
 * @describe: step3-主机Agent分发 
 * @Date: 2022-06-13 16:35:02 
 * @LastEditTime: 2022-07-01 15:16:56
 * @FilePath: \ddh-ui\src\components\steps\step3.vue
-->
<template>
  <div class="steps3 steps">
    <div class="steps-title flex-bewteen-container pdr30">
      <span>主机Agent分发</span>
      <div class="flex-bewteen-container">
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
        </div> -->
        <a-button type="primary" @click="retryHost('all')">全部重试</a-button>
      </div>
    </div>
    <div class="table-info mgt16 steps-body pdr30">
      <a-table @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" rowKey="hostname" :pagination="pagination"></a-table>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  data() {
    return {
      hostType: "all",
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
      timer: null,
      loading: false,
      columns: [
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
        { title: "主机", key: "hostname", dataIndex: "hostname", width: 300 },
        {
          title: "进度",
          key: "progress",
          dataIndex: "progress",
          customRender: (text, row, index) => {
            return (
              <span>
                {row.installStateCode === 1 ? (
                  <a-progress class="progress-warp" percent={text} status="active" />
                ) : row.installStateCode === 2 ? (
                  <a-progress class="progress-warp" percent={text} />
                ) : (
                  <a-progress class="progress-warp" percent={text} status="exception" />
                )}
              </span>
            );
          },
        },
        { title: "进度信息", key: "message", dataIndex: "message", width: 120,
          customRender: (text, row, index) => {
            return (
              <span>
                {text || ''}
              </span>
            );
          } },
        {
          title: "操作",
          key: "action",
          width: 140,
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                {row.installStateCode === 3 ? (
                  <a class="btn-opt" onClick={() => this.retryHost(row)}>
                    重试
                  </a>
                ) : (
                  <a class="btn-opt" style="color: #bbb">
                    重试
                  </a>
                )}
              </span>
            );
          },
        },
      ],
    };
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
    getAgentList(flag) {
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId,
      };
      // todo：这个接口地址需要替换
      this.$axiosPost(global.API.dispatcherHostAgentList, params).then(
        (res) => {
          this.loading = false;
          this.dataSource = res.data;
          this.pagination.total = res.total;
        }
      );
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      let arr = row.filter(item => item.installStateCode !== 3)
      this.selectedRow = arr
      this.selectedRowKeys = selectedRowKeys;
    },
    // 三秒去刷一下
    pollingSearch() {
      this.getAgentList(); // 先立马刷一次
      let self = this;
      if (self.timer) clearInterval(self.timer);
      self.timer = setInterval(() => {
        self.getAgentList(true);
      }, global.intervalTime);
    },
    // 重试
    retryHost(row) {
      let hostnames = "";
      if (row === "all") {
        if (this.selectedRowKeys.length < 1) {
          this.$message.warning("请至少选择一台主机！");
          return false;
        }
        if (this.selectedRow.length > 0) {
          this.$message.warning("目前只支持失败的主机进行重试操作！");
          return false;
        }
        hostnames = this.selectedRowKeys.join(",");
      } else {
        hostnames = row.hostname;
      }
      const params = {
        hostnames,
        clusterId: this.clusterId,
      };
      this.$axiosPost(global.API.reStartDispatcherHostAgent, params).then(
        (res) => {
          this.selectedRowKeys = [];
          this.$message.success(`操作成功`);
          this.pollingSearch();
        }
      );
    },
    // 取消
    cancelHost(row) {},
    // 主机环境校验是否完成 是否可以进入下一步
    async dispatcherHostAgentCompleted(callback) {
      const params = {
        clusterId: this.clusterId,
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
    clearInterval(this.timer);
  },
};
</script>
<style lang="less" scoped>
.steps3 {
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
    width: 70%;
  }
}
</style>