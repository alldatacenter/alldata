<template>
  <div class="layout">
    <div class="search" @click="handleSearch">
      <u-icon name="search"></u-icon>
      {{ res.list[0].title }}
    </div>
  </div>
</template>
<script>
export default {
  title:"搜索栏",
  props: ["res"],
  methods: {
    handleSearch() {
      uni.navigateTo({
        url: "/pages/navigation/search/searchPage",
      });
    },
  },
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";

.search {
  height: 64rpx;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ededed;
}

.layout {
  background: #fff;
  padding: 0 16rpx;
}
</style>
