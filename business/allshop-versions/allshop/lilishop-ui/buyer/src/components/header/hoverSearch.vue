<template>
  <div class="scroll-show">
    <div class="content clearfix">
      <cateNav class="cate" :hover="true" :showNavBar="false"></cateNav>
      <Search
        class="search-con"
        :hover="true"
        :showLogo="false"
        :showTag="false"
      ></Search>
      <Icon
        type="ios-cart-outline"
        @click="goCartList"
        class="cart-icon"
        @mouseenter.native="getCartList"
      />
      <i class="cart-badge">{{ cartNum < 100 ? cartNum : "99" }}</i>
    </div>
    <hr class="hr"/>
  </div>
</template>
<script>
import { cartCount } from "@/api/cart.js";
import storage from "@/plugins/storage.js";
export default {
  data() {
    return {
      userInfo: {}, // 用户信息
    };
  },
  computed: {
    cartNum() {
      // 购物车商品数量
      return this.$store.state.cartNum;
    },
  },
  methods: {
    goCartList() {
      // 跳转购物车页面
      let routerUrl = this.$router.resolve({
        path: "/cart",
      });
      window.open(routerUrl.href, "_blank");
    },
    getCartList() {
      // 获取购物车列表
      if (storage.getItem("userInfo")) {
        cartCount().then((res) => {
          this.$store.commit("SET_CARTNUM", res.result);
          this.Cookies.setItem("cartNum", res.result);
        });
      }
    },
  },
  mounted() {
    if (storage.getItem("userInfo")) {
      this.userInfo = JSON.parse(storage.getItem("userInfo"));
    }
  },
};
</script>
<style lang="scss" scoped>
.hr{
  height:1px;
  background:$theme_color;
}
.content {
  width: 1200px;
  height: 40px;
  margin: 10px auto;
  position: relative;
}
.cate {
  float: left;
  width: 200px !important;
}
.search-con {
  float: left;
  width: 800px;
  overflow: hidden;
  margin-top: -27px;
}
.cart-icon {
  width: 30px;
  float: left;
  font-size: 25px;
  margin-top: 8px;
  color: $theme_color;
  z-index: 1;
  position: relative;
  &:hover {
    cursor: pointer;
  }
}
.cart-badge {
  position: absolute;
  font-style: normal;
  right: 165px;
  display: block;
  background-color: $theme_color;
  color: #fff;
  font-size: 12px;
  width: 17px;
  height: 17px;
  border-radius: 10px;
  line-height: 17px;
  text-align: center;
  z-index: 5;
  top: 3px;
}
</style>
