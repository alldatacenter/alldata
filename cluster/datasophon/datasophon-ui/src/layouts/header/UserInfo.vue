<!--
 * @Author: mjzhu
 * @Date: 2022-06-09 17:05:39
 * @LastEditTime: 2022-06-09 17:20:58
 * @FilePath: \ddh-ui\src\layouts\header\UserInfo.vue
-->
<template>
  <div class="user-info">
    <a-descriptions class="user-warp" title="" :bordered="true" :column='1'>
      <a-descriptions-item label="用户名称">{{user.username || '-'}}</a-descriptions-item>
      <a-descriptions-item label="邮箱地址">{{user.email}}</a-descriptions-item>
      <a-descriptions-item label="手机号码">{{user.phone}}</a-descriptions-item>
    </a-descriptions>
  </div>
</template>
<script>

export default {
  data() {
    return {
      user: {}
    };
  },
  methods: {},
  mounted () {
    const user = localStorage.getItem(process.env.VUE_APP_USER_KEY)
    this.user = JSON.parse(user)
  }
};
</script>
<style lang="less" scoped>
.user-info {
  padding: 0 32px;
  .user-warp {
    margin: 16px 0;
  }
}
</style>