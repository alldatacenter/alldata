<!--
 * @Author: mjzhu
 * @describe: step4-选择服务 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-06-28 15:34:21
 * @FilePath: \ddh-ui\src\components\steps\step4.vue
-->
<template>
  <div class="steps4 steps">
    <div class="steps-title flex-bewteen-container pdr30">
      <span>选择服务</span>
    </div>
    <div class="table-info mgt16 steps-body pdr30">
      <a-table @change="tableChange" :columns="columns" :loading="loading" :pagination="false" :dataSource="dataSource" :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange, getCheckboxProps:getCheckboxProps}" rowKey="id"></a-table>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      selectedRowKeys: [],
      selectedRowNames: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
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
        { title: "服务", key: "serviceName", dataIndex: "serviceName" },
        {
          title: "描述",
          key: "serviceDesc",
          dataIndex: "serviceDesc",
        },
        {
          title: "版本",
          key: "serviceVersion",
          dataIndex: "serviceVersion",
        },
      ],
    };
  },
  methods: {
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.getServiceList();
    },
    getServiceList() {
      this.loading = true;
      const params = {
        clusterId: this.clusterId,
      };
      const self = this;
      // todo：这个接口地址需要替换
      this.$axiosPost(global.API.getServiceList, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        let arr = this.dataSource.filter(item => item.installed)
        if (arr.length > 0) {
          arr.map(childItem => {
            this.selectedRowKeys.push(childItem.id)
            this.selectedRowNames.push({
              serviceId: childItem.id,
              serviceName: childItem.serviceName
            })
          })
        }
        self.steps4Data.serviceIds.map(item => {
          this.selectedRowKeys.push(item)
        })
        self.steps4Data.serviceNames.map(item => {
          this.selectedRowNames.push({
            serviceId: item.id,
            serviceName: item.serviceName
          })
        })
      });
    },
    getCheckboxProps (record) {
      return {
        props: {
          disabled: record.installed
        }
      }
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      this.selectedRowKeys = selectedRowKeys;
      let arr = [];
      row.map((item) => {
        arr.push({
          serviceName: item.serviceName,
          serviceId: item.id
        });
      });
      this.selectedRowNames = arr;
    },
  },
  mounted() {
    this.getServiceList();
  },
};
</script>
<style lang="less" scoped>
</style>