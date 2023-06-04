<!--
 * @Author: mjzhu
 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2022-10-25 17:25:35
 * @FilePath: \ddh-ui\src\pages\alarmManage\group.vue
-->

<template>
  <div class="alarm-group">
    <a-card class="mgb16 card-shadow ">
      <a-row type="flex" align="middle">
        <a-col :span="16">
          <a-input placeholder="请输入告警组名称" v-model="alertGroupName" class="w252 mgr12" allowClear />
          <a-button class type="primary" icon="search" @click="onSearch"></a-button>
        </a-col>
        <a-col :span="8" style="text-align: right">
          <a-button style="margin-right: 10px;" type="primary" @click="addGroup({})">新建告警组</a-button>
          <!-- <a-dropdown>
          <a-menu slot="overlay" @click="handleMenuClick">
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
        <a-table @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" rowKey="id" :pagination="pagination" ></a-table>
      </div>
    </a-card>
  </div>
</template>

<script>
import AddGroup from "./commponents/addGroup.vue";
import { mapActions, mapState } from "vuex";

export default {
  name: "ALARMGROUP",
  components: {},
  provide() {
    return {
      handleCancel: this.handleCancel,
      onSearch: this.onSearch,
    };
  },
  data() {
    return {
      alertGroupName: '',
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
          title: "名称",
          key: "alertGroupName",
          dataIndex: "alertGroupName",
        },
        {
          title: "模板类别",
          key: "alertGroupCategory",
          dataIndex: "alertGroupCategory",
        },
        {
          title: "告警指标数",
          key: "alertQuotaNum",
          dataIndex: "alertQuotaNum",
        },
        {
          title: "操作",
          key: "action",
          width: 200,
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                <a class="btn-opt" onClick={() => this.bindMetric(row)}>
                  查看告警指标
                </a>
                <a-divider type="vertical" />
                <a class="btn-opt" onClick={() => this.delGroup(row)}>
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
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.getAlarmGroupList();
    },
    getVal(val, filed) {
      this.params[`${filed}`] = val.target.value;
    },
    //   查询
    onSearch() {
      this.pagination.current = 1;
      this.getAlarmGroupList();
    },
    bindMetric(row) {
      this.$router.push({
        path: "metric",
        query: { groupId: row.id },
      });
    },
    handleMenuClick(key) {
      if (key.key === "del") {
        this.delExample();
        return false;
      }
    },
    addGroup(obj) {
      const self = this;
      let width = 520;
      let title = JSON.stringify(obj) !== "{}" ? "编辑" : "新建告警组";
      let content = (
        <AddGroup detail={obj} callBack={() => self.getAlarmGroupList()} />
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
    delGroup(row) {
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
      this.$axiosPostUpload(global.API.deleteGroup, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.onSearch();
          this.$destroyAll();
        }
      });
    },
    getAlarmGroupList() {
      this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId || "",
        alertGroupName:this.alertGroupName
      };
      this.$axiosPost(global.API.getAlarmGroupList, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        this.pagination.total = res.total;
      });
    },
  },
  mounted() {
    this.getAlarmGroupList();
  },
};
</script>

<style lang="less" scoped>
.alarm-group {
  background: #f5f7f8;
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
