<template>
  <div class="shop-box">
    <div class="shop-container">
      <div class="shop-title-content">
        <img class="hover-pointer" @click="linkTo(`/merchant?id=${storeDetail.storeId}`)" :src="storeDetail.storeLogo" height="40" alt="">
        <p><router-link :to="`/merchant?id=${storeDetail.storeId}`">{{ storeDetail.storeName }}</router-link></p>
        <div class="ml_20" v-html="storeDetail.storeDesc"></div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ShopHeader',
  props: {
    detail: {
      type: Object,
      default: null
    }
  },
  computed: {
    storeDetail () { // 店铺详情
      return this.detail;
    }
  }
};
</script>

<style scoped>
/* 店铺介绍 */
.shop-box {
  width: 100%;
  height: 50px;
  background-color: #484848;
}

.shop-container {
  width: 1200px;
  height: 100%;
  margin: 0px auto;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}

.shop-title-content {
  display: flex;
  align-items: center;
}

.shop-title-content p {
  font-size: 20px;
  margin-left: 20px;
}
.shop-title-content a {
  color: #fff;
}

</style>
