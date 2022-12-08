<template>
  <div class="content-goods-publish">
    <div class="success" style="text-align: left">
      <h1>恭喜您，商品发布成功!</h1>
      <div class="goToGoodsList" @click="gotoGoodsList">
        <a>去店铺查看商品列表>></a>
      </div>
      <div class="operation">
        <h3>您还可以：</h3>
        <div>
          1、继续
          <a @click="gotoBack">发布商品</a>
        </div>
        <div>
          2、进入卖家中心，管理
          <a @click="gotoGoodsList">商品列表</a>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  methods: {
     // 跳转商品列表
    gotoGoodsList() {
      this.$router.push({name: "goods"});
    },
    // 刷新页面
    gotoBack() {
      this.$router.go();
    },
  }
}
</script>
<style lang="scss" scoped>
  @import "./addGoods.scss";
</style>