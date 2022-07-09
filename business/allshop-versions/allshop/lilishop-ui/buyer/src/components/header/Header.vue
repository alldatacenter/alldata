<template>
  <div class="box">
    <div class="nav">
      <ul class="location">
        <li v-if="$route.path.includes('home')" style="margin-left:10px"><router-link to="/">首页</router-link></li>
      </ul>
      <ul class="detail">
        <li class="first" v-show="!userInfo.username">
          <router-link :to="`/signUp`">
            <span style="border:none">立即注册</span>
          </router-link>
        </li>
        <li v-show="!userInfo.username">
          <router-link :to="`/login?rePath=${$route.path}&query=${JSON.stringify($route.query)}`">
            <span style="border:none">请登录</span>
          </router-link>
        </li>
        <li v-show="!!userInfo.username">
          <div class="username-p">
            <div>
              <Avatar class="person-icon" :src="userInfo.face" icon="person" size="small" />
              <span class="username">{{ userInfo.nickName? userInfo.nickName : userInfo.username | secrecyMobile }}</span>
            </div>
            <transition name='fade'>
              <ul class="drop-items">
                <li @click="goUserCenter('/home')">我的主页</li>
                <li @click="goUserCenter('/home/Coupons')">优惠券</li>
                <li @click="goUserCenter('/home/Favorites')">我的收藏</li>
                <li @click="signOutFun">退出登录</li>
              </ul>
            </transition>
          </div>
        </li>
        <li @click="goUserCenter('/home/MyOrder')"><span class="nav-item hover-color">我的订单</span></li>
        <li @click="goUserCenter('/home/MyTracks')"><span class="nav-item hover-color">我的足迹</span></li>
        <li @click="goUserCenter('/home/MsgList')"><span class="nav-item hover-color">我的消息</span></li>
        <li v-if="$route.name !== 'Cart'" style="position:relative;">
          <i class="cart-badge" v-show="Number(cartNum)">{{cartNum < 100 ? cartNum : '99'}}</i>
          <Dropdown placement="bottom-start">
            <router-link to="/cart" target="_blank">
              <span @mouseenter="getCartList">
                <Icon size="18" type="ios-cart-outline"></Icon>
                购物车
              </span>
            </router-link>
            <DropdownMenu slot="list">
              <div class="shopping-cart-null" style="width:200px" v-show="shoppingCart.length <= 0">
                <Icon type="ios-cart-outline" class="cart-null-icon"></Icon>
                <span>你的购物车没有宝贝哦</span>
                <span>赶快去添加商品吧~</span>
              </div>
              <div class="shopping-cart-list" v-show="shoppingCart.length > 0">
                <div class="shopping-cart-box" v-for="(item, index) in shoppingCart" @click="goToPay" :key="index">
                  <div class="shopping-cart-img">
                    <img :src="item.goodsSku.thumbnail" class="hover-pointer" />
                  </div>
                  <div class="shopping-cart-info">
                    <div class="shopping-cart-title ">
                      <p class="hover-pointer goods-title ellipsis">{{ item.goodsSku.goodsName }}</p>
                    </div>
                    <div class="shopping-cart-detail">
                      <p>
                        数量:
                        <span class="shopping-cart-text">{{ item.num }}</span>
                        价钱:
                        <span class="shopping-cart-text">{{ item.purchasePrice | unitPrice('￥') }}</span>
                      </p>
                    </div>
                  </div>
                </div>
                <div class="go-to-buy">
                  <Button type="error" size="small" @click="goToPay">去结账</Button>
                </div>
              </div>
            </DropdownMenu>
          </Dropdown>
        </li>
        <li>
          <span class="nav-item" @click="shopEntry">店铺入驻</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
import storage from '@/plugins/storage.js';
import { cartGoodsAll } from '@/api/cart.js';
import { logout } from '@/api/account.js';
export default {
  name: 'M-Header',
  created () {
    if (storage.getItem('userInfo')) {
      this.userInfo = JSON.parse(storage.getItem('userInfo'));
    }
  },

  data () {
    return {
      userInfo: {}, // 用户信息
      shoppingCart: [] // 购物车
    };
  },
  computed: {
    // 购物车商品数量
    cartNum () {
      return this.$store.state.cartNum;
    }
  },
  methods: {
    goToPay () { // 跳转购物车
      let url = this.$router.resolve({
        path: '/cart'
      });
      window.open(url.href, '_blank');
    },
    myInfo () { // 跳转会员中心
      let url = this.$router.resolve({
        path: '/home'
      });
      window.open(url.href, '_blank');
    },
    signOutFun () { // 退出登录
      logout().then(res => {
        storage.removeItem('accessToken');
        storage.removeItem('refreshToken');
        storage.removeItem('userInfo');
        storage.removeItem('cartNum');
        this.$store.commit('SET_CARTNUM', 0)
        this.$router.push('/login');
      });
    },
    goUserCenter (path) {
      // 跳转我的订单，我的足迹、收藏等
      if (this.userInfo.username) {
        this.$router.push({ path: path });
      } else {
        this.$Modal.confirm({
          title: '请登录',
          content: '<p>请登录后执行此操作</p>',
          okText: '立即登录',
          cancelText: '继续浏览',
          onOk: () => {
            this.$router.push({
              path: '/login',
              query: {
                rePath: this.$router.history.current.path,
                query: JSON.stringify(this.$router.history.current.query)
              }
            });
          }
        });
      }
    },
    shopEntry () {
      // 店铺入驻
      if (storage.getItem('accessToken')) {
        let routeUrl = this.$router.resolve({
          path: '/shopEntry',
          query: { id: 1 }
        });
        window.open(routeUrl.href, '_blank');
      } else {
        this.$router.push('login');
      }
    },
    getCartList () {
      // 获取购物车列表
      if (this.userInfo.username) {
        cartGoodsAll().then((res) => {
          this.shoppingCart = res.result.skuList;
          this.$store.commit('SET_CARTNUM', this.shoppingCart.length);
          this.Cookies.setItem('cartNum', this.shoppingCart.length);
        });
      }
    }
  }
};
</script>

<style scoped lang="scss">
.shopping-cart-detail,
.shopping-cart-text,
.shopping-cart-info,
.nav a,
.location,
.first,
.username,
.shopping-cart-null span {
 
}

.box {
  width: 100%;
  font-size: 12px!important;
  // height: 35px;
  @include background_color($light_white_background_color);
}
.nav {
  margin: 0 auto;
  width: 1200px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}
.nav ul {
  list-style: none;
}
.nav li {
  float: left;
  font-size: 12px;
  line-height: 35px;
  margin-right: 10px;
  font-weight: bold;
}
.nav a,
.nav-item {
  text-decoration: none;
  padding-left: 14px;
  border-left: 1px solid #ccc;
  color: #999;
  cursor: pointer;
}
.location a {
  border-left: none;
}
.nav a:hover {
  color: $theme_color;
}

.icon {
  color: gray;
  vertical-align: middle;
}

.first a:first-child {
  padding-left: 3px;
  border-left: none;
}
.city {
  padding: 10px 15px;
}
.city-item {
  font-weight: bold;
  cursor: pointer;
  padding: 5px;
}
.city-item:hover {
  color: $theme_color;
}
.person-icon {
  color: $theme_color;
  background-color: #f0cdb2;
}

.shopping-cart-list {
  padding: 10px 15px;
  box-sizing: border-box;
  max-height: 300px;
  overflow: scroll;
}
.shopping-cart-box {
  margin: 8px 0px;
  margin-top: 15px;
  padding-bottom: 15px;
  height: 40px;
  display: flex;
  align-items: center;
  border-bottom: 1px #ccc dotted;
}
.shopping-cart-box:first-child {
  margin-top: 8px;
}
.shopping-cart-img {
  margin-right: 15px;
  width: 40px;
  height: 40px;
}
.shopping-cart-img img {
  width: 100%;
}
.shopping-cart-info {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-content: space-between;
  width: 200px;
  overflow: hidden;
  font-size: 12px;
  line-height: 20px;
}

.go-to-buy {
  display: flex;
  justify-content: flex-end;
}
.shopping-cart-null {
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.cart-null-icon {
  font-size: 38px;
  margin-bottom: 15px;
}
.shopping-cart-null span {
  font-size: 12px;
  line-height: 16px;
}
.username-p {
  position: relative;
  div{
    cursor: pointer;
    >span{
      margin-left: 5px;
    }
  }
  .drop-items {
    position: absolute;
    display: none;
    top: 45px;
    left: 0;
    right: 0;
    margin: 0 auto;
    padding: 5px 10px;
    z-index: 20;
    height: 150px;
    background-color: #fff;
    width: 80px;
    border: 1px solid #eee;
    box-shadow: 2px 2px 7px #999;

    li {
      color: rgb(107, 106, 106);
      width: 100%;
      border-bottom:1px solid rgb(207, 206, 206);
      font-weight: normal;
      text-align: center;
      &:last-child{
        border: none;
      }
      &:hover{
        cursor: pointer;
        color: $theme_color;
      }
    }

    &::before{
      position: absolute;
      top: -20px;
      left: 30px;
      content: '';
      width: 0;
      height: 0;
      border: 10px solid #999;
      border-color:  transparent transparent #fff transparent ;
    }

    &::after{
      content: '';
      position: absolute;
      width: 80px;
      height: 20px;
      top: -20px;
      left: 0;
    }
  }
  &:hover{
    .drop-items{
      display: block;
    }
  }
}

.my-page {
  padding: 3px 5px;
  width: 180px;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
.my-page a {
  margin: 0px;
  padding: 0px;
  border: none;
}
.my-info {
  padding: 5px;
  width: 50%;
  height: 100%;
  text-align: center;
  cursor: pointer;
}
.my-info:hover {
  box-shadow: 0px 0px 5px #ccc;
}
.my-info i {
  font-size: 28px;
}
.my-info p {
  font-size: 12px;
}
.sign-out {
  padding: 5px;
  width: 50%;
  height: 100%;
  text-align: center;
  cursor: pointer;
}
.sign-out:hover {
  box-shadow: 0px 0px 5px $border_color;
}
.sign-out i {
  font-size: 28px;
}
.sign-out p {
  font-size: 12px;
}
.goods-title:hover {
  color: $theme_color;
}
.cart-badge {
  position: absolute;
  right: -8px;
  font-style: normal;
  background-color: $theme_color;
  color: #fff;
  font-size: 12px;
  width: 17px;
  height: 17px;
  border-radius: 10px;
  line-height: 17px;
  text-align: center;
  z-index: 3;
  top: 3px;
}
</style>
