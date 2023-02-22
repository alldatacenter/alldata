<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-06-20 14:25:07
 * @FilePath: \ddh-ui\src\pages\colonyManage\clusterList\index.vue
-->
<template>
  <div class="frame-list">
    <a-tabs default-active-key="1" @change="callback">
      <a-tab-pane v-for="(item, index) in frameList" :key="index+1" :tab="item.frameCode">
        <a-table :columns="loadTable()" :loading="loading" class="release-table-custom" :dataSource="item.frameServiceList" rowKey="id" :pagination="false"></a-table>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
export default {
  name: "FrameList",
  data() {
    return {
      loading: false,
      frameList: [],
      tableColumns: [
        { title: "序号", key: "index" },
        { title: "服务", key: "serviceName" },
        { title: "版本", key: "serviceVersion" },
        { title: "描述", key: "serviceDesc", ellipsis: true },
      ],
    };
  },
  methods: {
    callback(key) {
      console.log(key);
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
            } else {
              return <span title={record[item.key]}> {record[item.key]} </span>;
            }
          },
        };
      });
    },
    getFrameList() {
      this.$axiosPost(global.API.getFrameList, {}).then((res) => {
        if (res.code === 200) {
          this.frameList = res.data;
        }
      });
    },
  },
  mounted() {
    this.getFrameList();
  },
};
</script>

<style lang="less" scoped>
.frame-list {
  background: #fff;
  padding: 20px;
}
</style>
