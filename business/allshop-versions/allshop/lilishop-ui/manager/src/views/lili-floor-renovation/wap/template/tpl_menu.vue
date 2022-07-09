<template>
  <div class="layout">
    <div class="menu-list">
      <div class="menu-item" v-for="(item, index) in res.list" :key="index">
        <div>
          <img class="menu-img" :src="item.img" alt="" />
        </div>
        <div class="menu-title">{{ item.title }}</div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: ["res"],
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
    margin: 10px 0;
  }
}
.menu-img {
  width: 44px;
  height: 44px;
}
.menu-title {
  font-size: 12px;
}
</style>
