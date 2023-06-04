<!--
 * @Author: mjzhu
 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2023-01-03 16:56:20
 * @FilePath: \ddh-ui\src\pages\serviceManage\queue.vue
-->

<template>
  <div class="queue-page">
    <div class="flex-container" v-if="showGraph" style="position: absolute;top: 0px;right: 0px;z-index: 400000;">
      <!-- <a-button style="margin-right: 10px; margin-bottom: 20px" type="primary" @click="addQueue({})">新建队列</a-button> -->
      <a-button style="margin-right: 10px; margin-bottom: 20px" type="primary" @click="refreshQueues">刷新队列到Yarn</a-button>
    </div>
    <div class="table-info steps-body">
      <!-- :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" -->
      <a-table v-if="!showGraph" @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" rowKey="id" :pagination="pagination"></a-table>
      <QueueGraph v-else />
    </div>
  </div>
</template>

<script>
import AddQueue from "./addQueue.vue";
import QueueGraph from './queueGraph.vue'
export default {
  name: "Queue",
  components: {
    QueueGraph
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
      visible: false,
      showGraph:false,
      confirmLoading: false,
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      changeAlertFlag: false,
      dataSource: [],
      selectedRowKeys: [],
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
        {
          title: "队列名称",
          key: "queueName",
          dataIndex: "queueName",
        },
        {
          title: "最小资源数",
          key: "minMem",
          dataIndex: "minMem",
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                {row.minCore}Core, {row.minMem}GB
              </span>
            );
          },
        },
        {
          title: "最大资源数",
          key: "maxCore",
          dataIndex: "maxCore",
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                {row.maxCore}Core, {row.maxMem}GB
              </span>
            );
          },
        },
        {
          title: "资源分配策略",
          key: "schedulePolicy",
          dataIndex: "schedulePolicy",
        },
        { title: "权重", key: "weight", dataIndex: "weight" },
        {
          title: "是否允许资源被抢占",
          key: "allowPreemption",
          dataIndex: "allowPreemption",
          customRender: (text, row, index) => {
            return <span class="flex-container">{text === 1 ? '是' : '否'}</span>;
          },
        },
        {
          title: "AM占用比例",
          key: "amShare",
          dataIndex: "amShare",
          customRender: (text, row, index) => {
            return <span class="flex-container">{text}</span>;
          },
        },
        {
          title: "操作",
          key: "action",
          width: 140,
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                <a class="btn-opt" onClick={() => this.addQueue(row)}>
                  编辑
                </a>
                <a-divider type="vertical" />
                <a class="btn-opt" onClick={() => this.deleteQueue(row)}>
                  删除
                </a>
              </span>
            );
          },
        },
      ],
    };
  },
  computed: {},
  methods: {
    handleCancel(e) {
      this.visible = false;
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      this.selectedRowKeys = selectedRowKeys;
    },
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.getQueueList();
    },
    getVal(val, filed) {
      if (filed === "alertGroupId") this.changeAlertFlag = true;
      this.params[`${filed}`] =
        filed === "alertGroupId" ? val : val.target.value;
    },
    //   查询
    onSearch() {
      this.pagination.current = 1;
      this.getQueueList();
    },
    refreshQueues() {
      this.$axiosPost(global.API.refreshQueues, {
        clusterId: this.clusterId,
      }).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功", 2);
          this.getQueueList();
        }
      });
    },
    addQueue(obj) {
      const self = this;
      let width = 800;
      let title = JSON.stringify(obj) !== "{}" ? "编辑队列" : "新建队列";
      let content = (
        <AddQueue detail={obj} callBack={() => self.getQueueList()} />
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
    deleteQueue(row) {
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
                onClick={() => this.confirmGroup(row)}
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
    confirmGroup(row) {
      const params = JSON.stringify([row.id]);
      this.$axiosPostUpload(global.API.deleteQueue, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.onSearch();
          this.$destroyAll();
        }
      });
    },
    getQueueList() {
      this.loading = true;
      this.showGraph = false;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId || "",
      };
      this.$axiosPost('/ddh/cluster/yarn/scheduler/info', {clusterId: this.clusterId}).then((res) => {
        this.showGraph = res.data == 'capacity';
        if(res.data && res.data == 'fair'){
          this.$axiosPost(global.API.getQueueList, params).then((res) => {
            this.loading = false;
            this.dataSource = res.data;
            this.pagination.total = res.total;
          });
        }
      });
    },
  },
  mounted() {
    this.getQueueList();
  },
  activated() {
    this.getQueueList();
  },
};
</script>

<style lang="less" scoped>
.queue-page {
  position: relative;
  background: #fff;
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
  .grey-point {
    background: grey;
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
