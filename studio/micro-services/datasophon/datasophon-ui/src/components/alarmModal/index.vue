<!--
 * @Author: mjzhu
 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2022-10-25 17:29:15
 * @FilePath: \ddh-ui\src\components\alarmModal\index.vue
-->

<template>
  <div class="alarmModal">
    <div class="table-info steps-body">
      <a-config-provider :locale="zh_CN">
      <a-table :columns="columns" :loading="loading" :dataSource="dataSource" rowKey="id" :scroll="!alarmAll? {y: 400} : {}" :pagination="alarmAll?pagination:false" @change="tableChange" ></a-table>
      </a-config-provider>
    </div>
  </div>
</template>

<script>
import zh_CN from "ant-design-vue/lib/locale-provider/zh_CN";
export default {
  name: "ALARMMODAL",
  provide() {
    return {
      handleCancel: this.handleCancel,
      onSearch: () => {}
    };
  },
  props: {
    serviceInstanceId: {
      Type:Number,
      default:0
    },
    alarmAll:Boolean
  },
  data() {
    return {
      zh_CN,
      clusterId: Number(localStorage.getItem("clusterId") || -1) ,
      dataSource: [],
      loading: false,
      columns: [
        {
          title: "主机",
          key: "hostname",
          width: 120,
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                <svg-icon
                  icon-class='gaojing'
                  class={[
                    "mgr6 top-2",
                    row.alertLevel === 'warning'
                      ? "configured-status-color" : "error-status-color"
                  ]}
                />
                {row.hostname}
              </span>
            );
          },
        },
        {
          title: "告警组",
          width: 120,
          key: "alertGroupName",
          dataIndex: "alertGroupName",
          ellipsis: true,
        },
        {
          title: "告警指标",
          key: "alertTargetName",
          dataIndex: "alertTargetName",
        },
        { title: "告警详情", key: "alertInfo", dataIndex: "alertInfo",ellipsis: true },
        { title: "建议操作", key: "alertAdvice", dataIndex: "alertAdvice",ellipsis: true, }
      ],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
    };
  },
  computed: {},
  methods: {
    handleCancel(e) {
      this.visible = false;
    },
    getAlertList() {
      this.loading = true;
      if(!this.alarmAll){
        const params = {
          serviceInstanceId: this.serviceInstanceId,
        };
        this.$axiosPost(global.API.getAlertList, params).then((res) => {
          this.loading = false;
          console.log(res);
          this.dataSource = res.data;
          // this.pagination.total = res.total;
        });
      }else{
        const params = {
          clusterId: this.clusterId,
          pageSize: this.pagination.pageSize,
          page: this.pagination.current,
        };
        this.$axiosPost(global.API.getAllAlertList, params).then((res) => {
          this.loading = false;
          this.dataSource = res.data;
          this.pagination.total = res.total;
        });
      }
    },
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.getAlertList();
    },
  },
  mounted() {
    this.getAlertList();
    console.log(this.alarmAll);
  },
};
</script>

<style lang="less" scoped>
.alarmModal {
  padding:20px 32px;
  background: #fff;
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
    color: #0264c8;
    letter-spacing: 0;
    font-weight: 400;
    margin: 0 5px;
  }
  .top-2 {
    position: relative;
    top: -2px;
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
