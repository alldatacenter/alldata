<template>
  <div>
    <div class="userBox">
      <div class="box">
        <!-- 我的订单组件 -->
        <myOrderPage :homePage="true" />
      </div>
      <div class="box">
        <!-- 近期收藏 -->
        <myFavorites :homePage="true" />
      </div>
      <div class="box">
      </div>
    </div>
  </div>
</template>

<script>
import myOrderPage from '@/pages/home/orderCenter/MyOrder'
import myFavorites from '@/pages/home/memberCenter/Favorites'

export default {
  name: 'main',
  components: {
    myOrderPage,
    myFavorites
  }
}
</script>

<style scoped lang="scss">
.userBox {
  padding: 0 0 20px 0;
}
</style>
