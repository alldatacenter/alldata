<!--
 * @Author: mjzhu
 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2022-10-25 17:23:55
 * @FilePath: \ddh-ui\src\pages\alarmManage\metric.vue
-->

<template>
  <div class="alarm-metric ">
    <a-card class="mgb16 card-shadow">
      <a-row type="flex" align="middle">
        <a-col :span="16">
          <a-input placeholder="请输入指标名称" class="w252 mgr12" @change="(value) => getVal(value, 'quotaName')" allowClear />
          <a-select placeholder="请选择告警组" class="w252 mgr12" allowClear @change="(value) => getVal(value, 'alertGroupId')">
            <a-select-option :value="item.id" v-for="(item,index) in groupList" :key="index">{{item.alertGroupName}}</a-select-option>
          </a-select>
          <a-button class type="primary" icon="search" @click="onSearch"></a-button>
        </a-col>
        <a-col :span="8" style="text-align: right">
          <a-button style="margin-right: 10px;" type="primary" @click="handleMenuStart">启用指标</a-button>
          <a-button style="margin-right: 10px;" type="primary" @click="addGroup({})">新建指标</a-button>
          <!-- <a-dropdown>
          <a-menu slot="overlay" @click="handleMenuClick">
            <a-menu-item key="start">启用</a-menu-item>
            <a-menu-item key="stop">停用</a-menu-item>
            <a-menu-item key="del">删除</a-menu-item>
          </a-menu>
          <a-button class type="primary">
            选择操作
            <a-icon type="down" />
          </a-button>
          </a-dropdown>-->
        </a-col>
      </a-row>
    </a-card>
    <a-card class="card-shadow">
      <div class="table-info steps-body">
        <!-- :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" -->
        <a-table @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" rowKey="id" :pagination="pagination" :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" ></a-table>
      </div>
    </a-card>
  </div>
</template>

<script>
import AddMetric from "./commponents/addMetric.vue";
import { mapActions, mapState } from "vuex";

export default {
  name: "ALARMMETRIC",
  components: {},
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
      confirmLoading: false,
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
      groupList: [],
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
          title: "指标名称",
          key: "alertQuotaName",
          dataIndex: "alertQuotaName",
        },
        { title: "比较方式", key: "compareMethod", dataIndex: "compareMethod" },
        {
          title: "告警阀值",
          key: "alertThreshold",
          dataIndex: "alertThreshold",
        },
        { title: "告警组", key: "alertGroupName", dataIndex: "alertGroupName" },
        { title: "通知组", key: "noticeGroupId", dataIndex: "noticeGroupId" },
        {
          title: "状态",
          key: "quotaState",
          dataIndex: "quotaState",
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                <span
                  class={[
                    "circle-point",
                    row.quotaStateCode === 1
                      ? "success-point"
                      : row.quotaStateCode === 2
                        ? "error-point"
                        : "grey-point",
                  ]}
                />
                {text}
              </span>
            );
          },
        },
        {
          title: "操作",
          key: "action",
          width: 140,
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                <a class="btn-opt" onClick={() => this.addGroup(row)}>
                  编辑
                </a>
                <a-divider type="vertical" />
                <a class="btn-opt" onClick={() => this.delMetric(row)}>
                  删除
                </a>
              </span>
            );
          },
        },
      ],
    };
  },
  watch: {
    clusterId: {
      handler (val, oldVal) {
        if (val !== oldVal) {
          debugger
          this.onSearch()
        }
      },
    }
  },
  computed: {
    ...mapState({
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
    clusterId () {
      return this.setting.clusterId
    }
  },
  methods: {
    handleCancel(e) {
      this.visible = false;
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      this.selectedRowKeys = selectedRowKeys;
    },
    handleMenuStart(){
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
              确认启用吗？
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.quotaStart()}
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
    quotaStart(){
      let params = {
        alertQuotaIds: this.selectedRowKeys.join(","),
        clusterId:this.clusterId
      };
      this.$axiosPost(global.API.quotaStart, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.$destroyAll();
          this.selectedRowKeys = [];
        }
      });
    },
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.getAlarmMerticList();
    },
    getVal(val, filed) {
      if (filed === "alertGroupId") this.changeAlertFlag = true;
      this.params[`${filed}`] =
        filed === "alertGroupId" ? val : val.target.value;
    },
    //   查询
    onSearch() {
      this.pagination.current = 1;
      this.getAlarmMerticList();
    },
    handleMenuClick(key) {
      if (key.key === "del") {
        this.delExample();
        return false;
      }
    },
    addGroup(obj) {
      const self = this;
      let width = 700;
      let title =
        JSON.stringify(obj) !== "{}" ? "编辑告警指标" : "新建告警指标";
      let content = (
        <AddMetric detail={obj} callBack={() => self.getAlarmMerticList()} />
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
    delMetric(row) {
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
      this.$axiosPostUpload(global.API.deleteMetric, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.onSearch();
          this.$destroyAll();
        }
      });
    },
    getAlarmMerticList() {
      this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId || "",
        quotaName: this.params.quotaName || "",
        alertGroupId: this.changeAlertFlag
          ? this.params.alertGroupId || ""
          : this.$route.query.groupId || "",
      };
      this.$axiosPost(global.API.getAlarmMerticList, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        this.pagination.total = res.total;
      });
    },
    getAlarmGroupList() {
      const params = {
        pageSize: 1000,
        page: 1,
        clusterId: this.clusterId || "",
      };
      this.$axiosPost(global.API.getAlarmGroupList, params).then((res) => {
        this.groupList = res.data;
      });
    },
  },
  mounted() {
    this.getAlarmMerticList();
    this.getAlarmGroupList();
  },
};
</script>

<style lang="less" scoped>
.alarm-metric {
  background: #f5f7f8;
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
