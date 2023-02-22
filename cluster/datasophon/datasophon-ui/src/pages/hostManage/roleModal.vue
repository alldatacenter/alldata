<!--
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-07-15 15:04:12
 * @LastEditTime: 2022-07-15 17:28:48
 * @FilePath: \ddh-ui\src\pages\hostManage\roleModal.vue
-->

<template>
  <div class="role-model">
    <a-spin :spinning="loading">
      <div class="flex-container flex-warp">
        <span v-for="(item, index) in dataSource" :key="index" class="flex-container role-item">
          <span :class="['circle-point',
          item.serviceRoleStateCode === 1
          ? 'success-point'
          : item.serviceRoleStateCode === 2
          ? 'error-point'
          : 'configured-point']" />
          {{item.serviceRoleName}}
        </span>
      </div>
    </a-spin>
    <div class="ant-modal-confirm-btns-new">
      <a-button @click.stop="formCancel">关闭</a-button>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    detail: Object,
  },
  data() {
    return {
      loading: false,
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      dataSource: [],
    };
  },
  watch: {},
  methods: {
    formCancel() {
      this.$destroyAll();
    },
    getRoleListByHostname() {
      this.loading = true;
      const params = {
        clusterId: this.clusterId || "",
        hostname: this.detail.hostname || "",
      };
      this.$axiosPost(global.API.getRoleListByHostname, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
      });
    },
  },
  mounted() {
    this.getRoleListByHostname();
  },
};
</script>
<style lang="less" scoped>
.role-model {
  padding: 10px 0 0 32px;
  .flex-warp {
    flex-wrap: wrap;
      max-height: 500px;
      overflow-y: auto;
    .role-item {
      // width: 33%;
      padding: 0 16px 10px 0;
    }
  }
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
  .configured-point {
    background: @configured-status-color;
  }
}
</style>
