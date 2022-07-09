<template>
  <div class="user-point">
    <!-- <div class="point-rule">积分规则</div> -->
    <div class="point-wrapper">
      <u-image shape="circle" :lazy-load="true" width="100" height="100"
        :src="userInfo.face || '/static/missing-face.png'"></u-image>
      <div class="whether-point">
        <div>你的可用积分：<span class="point">{{userInfo.point || 0}}</span></div>
      </div>
    </div>
  </div>
</template>
<script>
import { getUserInfo } from "@/api/members";
export default {
  data() {
    return {
      userInfo: {},
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    async init() {
      let res = await getUserInfo();
      if (res.data.success) {
        this.userInfo = res.data.result;
      }
    },
  },
};
</script>
<style lang="scss" scoped>
.user-point {
  padding: 0 20rpx;
  height: 300rpx;
  background: url("/static/point-bg.png") no-repeat;
  background-size: 100%;
}
.point {
  font-size: 40rpx;
}
.point-rule {
  color: #fff;
  display: flex;
  justify-content: flex-end;
  padding: 20rpx 0;
}
.point-wrapper {
  padding-top: 80rpx;
  display: flex;
}
.whether-point {
  color: #fff;
  margin-left: 30rpx;
  font-size: 36rpx;
  font-weight: bold;
}
</style>