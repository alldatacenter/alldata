<template>
  <view class="securityCenter">
    <u-cell-group>
      <u-cell-item title="修改密码" @click="navigateTo('/pages/mine/set/securityCenter/editPassword')"></u-cell-item>
    </u-cell-group>
  </view>
</template>

<script>
export default {
  data() {
    return {
      mobile: "", //存储手机号
    };
  },

  methods: {
    navigateTo(url) {
      uni.navigateTo({
        url: url,
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.securityCenter {
  .u-cell {
    line-height: normal;
  }
}
</style>
