<template>
  <div>
    <BaseHeader></BaseHeader>
    <Search></Search>
    <cateNav></cateNav>
    <!-- 添加成功提示 -->
    <div class="add-info-box-container">
      <div class="add-info-box">
        <div class="add-info-detail">
          <div class="add-info-title">
            <p>
              <i class="fa fa-check-circle"></i> 商品已成功加入购物车！</p>
          </div>
          <div class="add-info-box-row">
            <div class="add-info-img">
              <img :src="this.detail.thumbnail" alt="">
            </div>
            <div class="add-info-intro">
              <p>{{detail.goodsName}}</p>
              <p class="add-info-intro-detail">数量：{{$route.query.count}}</p>
            </div>
          </div>
        </div>
        <div class="car-btn-group">
          <div></div>
          <div class="car-btn-row">
            <router-link :to="`/goodsDetail?skuId=${detail.id}&goodsId=${detail.goodsId}`">
              <button class="btn-car hover-pointer btn-car-to-detail">查看商品详情</button>
            </router-link>
            <router-link to="/cart">
              <button class="btn-car hover-pointer btn-car-to-pay">去购物车结算></button>
            </router-link>
          </div>
        </div>
      </div>
    </div>
    <BaseFooter></BaseFooter>
  </div>
</template>

<script>
export default {
  name: 'ShoppingCart',
  beforeRouteEnter (to, from, next) {
    window.scrollTo(0, 0);
    next();
  },
  data () {
    return {
      detail: {} // 商品详情
    };
  },
  mounted () {
    let detail = this.$route.query.detail;
    if (detail) this.detail = detail;
  }
};
</script>

<style scoped lang="scss">
/****************************加入购物车页面开始*****************************/
.add-info-box-container{
  width: 100%;
  background-color: #F5F5F5;
}
.add-info-box{
  width: 1200px;
  margin: 0px auto;
  padding: 15px 0px;
  display: flex;
    flex-direction: row;
    justify-content: space-between;
}
.add-info-detail{
  display: flex;
    flex-direction: column;
}
.add-info-title{
  font-size: 25px;
  color: #71b247;
}
.add-info-box-row{
  display: flex;
    flex-direction: row;
    margin-top: 15px;
}
.add-info-img{
  width: 56px;
  height: 56px;
  margin-right: 15px;
}
.add-info-img img{
  width: 100%;
}
.add-info-intro{
  display: flex;
    flex-direction: column;
    justify-content: center;
}
.add-info-intro-detail{
  font-size: 12px;
  color: #999999;
}
.car-btn-group{
  display: flex;
    flex-direction: column;
    justify-content: space-between;
}
/*按钮*/
.btn-car{
  padding: 8px 10px;
  font-size: 16px;
  border-radius: 0px;
  border:none;
  margin-right: 15px;
}
.btn-car-to-detail{
  background-color: #fff;
  color: $theme_color;
  border:1px solid #fff;
}
.btn-car-to-detail:hover{
border:1px solid $theme_color;
}
.btn-car-to-pay{
  background-color: $theme_color;
  color: #fff;
  border:1px solid $theme_color;
}
.btn-car-to-pay:hover{
  background-color: $theme_color;
  border:1px solid $theme_color;
}
/*其他用户购买*/
.other-user-buy-box{
  width: 90%;
  margin: 0px auto;
  display: flex;
    flex-direction: column;
}
.other-user-buy-title{
  margin-top: 25px;
  font-size: 14px;
  color: #666;
  font-weight: bold;
}
.other-user-buy-row{
  margin-top: 25px;
  display: flex;
    flex-direction: row;
    justify-content: space-between;
}
.other-user-buy-item-box{
  display: flex;
    flex-direction: row;
}
.other-user-buy-item-img{
  width: 96px;
  height: 96px;
}
.other-user-buy-item-img img{
  width: 100%;
}
.other-buy-detail-box{
  width: 160px;
  margin-left: 15px;
  display: flex;
    flex-direction: column;
}
.other-buy-title{
  font-size: 12px;
}
.other-buy-title a{
  color: #2c2c2c;
  text-decoration: none;
}
.other-buy-price{
  font-size: 12px;
  font-weight: bold;
  color: $theme_color;
}
.other-buy-btn{
  padding: 3px 10px;
  color: $theme_color;
  font-size: 12px;
  border: 1px solid $theme_color;
  border-radius: 0px;
  background-color: #fff;
}
.other-buy-btn:hover{
  color: $theme_color;
  border: 1px solid $theme_color;
}
/****************************加入购物车页面结束*****************************/
</style>
