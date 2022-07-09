<template>
  <div class="layout">
    <div class="menu-list">
      <div
        class="menu-item"
        @click="modelNavigateTo(item)"
        v-for="(item, index) in res.list"
        :key="index"
      >
        <div>
          <u-image
            width="88rpx"
            height="88rpx"
            class="menu-img"
            :src="item.img"
          >
            <u-loading slot="loading"></u-loading>
          </u-image>
        </div>
        <div class="menu-title">{{ item.title }}</div>
      </div>
    </div>
  </div>
</template>
<script>
import { modelNavigateTo } from "./tpl";
export default {
  title: "五列菜单",
  props: ["res"],
  data() {
    return {
      modelNavigateTo,
    };
  },
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.menu-list {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;

  > .menu-item {
    text-align: center;
    width: 20%;
    // flex: 1;
    margin: 20rpx 0;
  }
}
.menu-img {
  display: flex;
  margin: 0 auto;
  width: 88rpx;
  height: 88rpx;
}
.menu-title {
  font-size: 24rpx;
}
</style>