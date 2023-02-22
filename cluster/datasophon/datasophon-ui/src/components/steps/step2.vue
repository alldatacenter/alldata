<!--
 * @Author: mjzhu
 * @describe: step2-主机环境校验 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:04
 * @FilePath: \ddh-ui\src\components\steps\step2.vue
-->
<template>
  <div class="steps2 steps">
    <div class="steps-title flex-bewteen-container pdr30">
      <span>主机环境校验</span>
      <a-button type="primary" @click="retryEnvironment('all')">全部重试</a-button>
    </div>
    <div class="table-info mgt16 steps-body pdr30">
      <a-table @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" rowKey="hostname" :pagination="pagination"></a-table>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps1Data: Object,
  },
  data() {
    return {
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 100,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["100", "200", "500", "1000"],
        showTotal: (total) => `共 ${total} 条`,
      },
      timer: null,
      dataSource: [],
      loading: false,
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
        { title: "主机", key: "hostname", dataIndex: "hostname" },
        {
          title: "当前受管",
          key: "managed",
          dataIndex: "managed",
          customRender: (text, row, index) => {
            return <span>{text ? "是" : "否"}</span>;
          },
        },
        {
          title: "检测结果",
          key: "phone",
          dataIndex: "phone",
          customRender: (text, row, index) => {
            return row.checkResult ? (
              <span class="flex-container">
                <svg-icon
                  icon-class={
                    row.checkResult.code === 10001
                      ? "success-status"
                      : row.checkResult.code === 10000
                        ? "success-status"
                        : "error-status"
                  }
                  class={
                    row.checkResult.code === 10001
                      ? "success-status-color"
                      : row.checkResult.code === 10000
                        ? "running-status-color"
                        : "configured-status-color"
                  }
                />
                <span class="mgl5">{row.checkResult.msg}</span>
              </span>
            ) : (
              <span>-</span>
            );
          },
        },
        {
          title: "操作",
          key: "action",
          width: 140,
          customRender: (text, row, index) => {
            return row.userType !== 1 ? (
              <span class="flex-container">
                <a class="btn-opt" onClick={() => this.retryEnvironment(row)}>
                  重试
                </a>
              </span>
            ) : (
              <span class="flex-container">
                <a class="btn-opt" style="color: #bbb">
                  重试
                </a>
              </span>
            );
          },
        },
      ],
    };
  },
  methods: {
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.pollingSearch();
    },
    getEnvironmentList(flag) {
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId,
        ...this.steps1Data,
      };
      this.$axiosPost(global.API.analysisHostList, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        this.pagination.total = res.total;
      });
    },
    // 三秒去刷一下
    pollingSearch() {
      this.getEnvironmentList(); // 先立马刷一次
      let self = this;
      if (self.timer) clearInterval(self.timer);
      self.timer = setInterval(() => {
        self.getEnvironmentList(true);
      }, global.intervalTime);
    },
    //表格选择
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    retryEnvironment(row) {
      let hostnames = "";
      if (row === "all") {
        if (this.selectedRowKeys.length < 1) {
          this.$message.warning("请至少选择一台主机！");
          return false;
        }
        hostnames = this.selectedRowKeys.join(",");
      } else {
        hostnames = row.hostname;
      }
      const params = {
        hostnames,
        clusterId: this.clusterId,
        sshUser: this.steps1Data.sshUser,
        sshPort: this.steps1Data.sshPort,
      };
      this.$axiosPost(global.API.rehostCheck, params).then((res) => {
        this.selectedRowKeys = [];
        this.$message.success(`操作成功`);
        this.pollingSearch();
      });
    },
    // 主机环境校验是否完成 是否可以进入下一步
    async hostCheckCompleted(callback) {
      const params = {
        clusterId: this.clusterId,
      };
      // 等待网络请求结束
      let flag = await this.$axiosPost(global.API.hostCheckCompleted, params);
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
</style>