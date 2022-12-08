<template>
  <div class="content-drawer">
    <div v-if="title === '购物车'" class="cart-con">
      <ul>
        <li v-for="(goods,goodsIndex) in cartList" :key="goodsIndex">
          <div>
            <img :src="goods.goodsSku.thumbnail" width="90" height="90" alt="">
          </div>
          <div>
            <p class="hover-color" @click="linkTo(`/goodsDetail?skuId=${goods.goodsSku.id}&goodsId=${goods.goodsSku.goodsId}`)">{{goods.goodsSku.goodsName}}</p>
            <p class="price">{{goods.goodsSku.price | unitPrice('￥')}}<span>&nbsp; x{{goods.num}}</span></p>
          </div>
          <span class="del hover-color" @click="delGoods(goods.goodsSku.id)">删除</span>
        </li>
      </ul>
      <Button size="large" class="mt_10" type="primary" @click="linkTo('/cart')" long>去购物车结算</Button>
    </div>

    <div v-else-if="title === '我的订单'" class="order-con">
      <ul>
        <li v-for="(order,orderIndex) in orderList" :key="orderIndex">
          <div class="order-status"><span>{{filterOrderStatus(order.orderStatus)}}</span><span>{{order.createTime}}</span></div>
          <div class="goods-img">
            <img :src="img.image"
              @click="linkTo(`/goodsDetail?skuId=${img.skuId}&goodsId=${img.goodsId}`)"
              v-for="(img,imgIndex) in order.orderItems"
              :key="imgIndex" width="40" height="40" alt="">
          </div>
          <div class="order-handle"><span>{{ order.flowPrice | unitPrice("￥") }}</span><span class="hover-color" @click="linkTo(`home/OrderDetail?sn=${order.sn}`)">查看订单</span></div>
        </li>
      </ul>
      <Button type="primary" @click="linkTo('/home/MyOrder')" long>查看全部订单</Button>
    </div>

    <div v-else-if="title === '优惠券'" class="coupon-con">
      <ul class="coupon-list">
          <li v-for="(coupon, index) in couponList" class="coupon-item" :key="index">
            <div class="c-left">
              <div>
                <span v-if="coupon.couponType === 'PRICE'" class="fontsize_12 global_color">￥<span class="price">{{coupon.price | unitPrice}}</span></span>
                <span v-if="coupon.couponType === 'DISCOUNT'" class="fontsize_12 global_color"><span class="price">{{coupon.couponDiscount}}</span>折</span>
                <span class="describe">满{{coupon.consumeThreshold}}元可用</span>
              </div>
              <p>使用范围：{{useScope(coupon.scopeType, coupon.storeName)}}</p>
              <p>有效期：{{coupon.endTime}}</p>
            </div>
            <b></b>
            <a class="c-right" @click="receive(coupon)">立即领取</a>
            <i class="circle-top"></i>
            <i class="circle-bottom"></i>
          </li>
        </ul>
    </div>
    <div v-else-if="title === '我的足迹'" class="tracks-con">
      <ul>
        <li v-for="(track,trackIndex) in tracksList" :key="trackIndex">
          <img :src="track.thumbnail" :alt="track.thumbnail" @click="linkTo(`/goodsDetail?skuId=${track.id}&goodsId=${track.goodsId}`)" width="100" height="100">
          <div @click="addToCart(track.id)">加入购物车</div>
          <p class="global_color">{{track.price | unitPrice('￥')}}</p>
        </li>
      </ul>
      <div class="hover-color" style="text-align:center;" @click="linkTo('/home/MyTracks')">查看更多>></div>
    </div>
    <div v-else-if="title === '我的收藏'" class="collect-con">
      <ul>
        <li v-for="(collect,collectIndex) in collectList" :key="collectIndex">
          <img :src="collect.image" :alt="collect.image" @click="linkTo(`/goodsDetail?skuId=${collect.skuId}&goodsId=${collect.goodsId}`)" width="100" height="100">
          <div @click="addToCart(collect.skuId)">加入购物车</div>
          <span class="del-icon" @click.stop="cancelCollect(collect.skuId)">
            <Icon type="md-trash" />
          </span>
          <p class="global_color">{{collect.price | unitPrice('￥')}}</p>
        </li>
      </ul>
      <div class="hover-color" style="text-align:center;" @click="linkTo('/home/Favorites')">查看更多>></div>
    </div>
    <Spin v-if="loading" fix></Spin>
  </div>
</template>

<script>
import {cartGoodsAll, delCartGoods, addCartGoods, cartCount} from '@/api/cart.js'
import { getOrderList } from '@/api/order';
import {couponList, receiveCoupon, tracksList, collectList, cancelCollect} from '@/api/member.js'
export default {
  name: 'Drawer',
  props: {
    title: {
      default: '',
      type: String
    }
  },
  watch: {
    title (val) {
      switch (val) {
        case '购物车':
          this.getCartList()
          break;
        case '我的订单':
          this.getOrderList()
          break;
        case '我的足迹':
          this.getTracksList()
          break;
        case '优惠券':
          this.getCouponList()
          break;
        case '我的收藏':
          this.getCollectList()
          break;
      }
    }
  },
  data () {
    return {
      loading: false, // 控制spin显隐
      cartList: [], // 购物车列表
      couponList: [], // 优惠券列表
      orderList: [], // 订单列表
      collectList: [], // 收藏列表
      tracksList: [], // 足迹列表
      orderStatusList: [ // 订单状态
        {
          name: '未付款',
          status: 'UNPAID'
        },
        {
          name: '已付款',
          status: 'PAID'
        },
        {
          name: '待发货',
          status: 'UNDELIVERED'
        },
        {
          name: '已发货',
          status: 'DELIVERED'
        },
        {
          name: '已完成',
          status: 'COMPLETED'
        },
        {
          name: '待核验',
          status: 'TAKE'
        },
        {
          name: '已取消',
          status: 'CANCELLED'
        }
      ]
    };
  },
  methods: {
    getCartList () { // 获取购物车列表
      this.loading = true
      cartGoodsAll().then(res => {
        this.loading = false
        this.cartList = res.result.skuList
      })
    },
    // 删除商品
    delGoods (id) {
      delCartGoods({ skuIds: id }).then((res) => {
        if (res.success) {
          this.$Message.success('删除成功');
          this.getCartList();
          cartCount().then(res => {
            this.$store.commit('SET_CARTNUM', res.result)
            this.Cookies.setItem('cartNum', res.result)
          })
        } else {
          this.$Message.error(res.message);
        }
      });
    },
    filterOrderStatus (status) { // 获取订单状态中文
      const ob = this.orderStatusList.filter(e => { return e.status === status });
      return ob[0].name
    },
    receive (item) { // 领取优惠券
      receiveCoupon(item.id).then(res => {
        if (res.success) {
          this.$Modal.confirm({
            title: '领取优惠券',
            content: '<p>优惠券领取成功，可到我的优惠券页面查看</p>',
            okText: '我的优惠券',
            cancelText: '立即使用',
            onOk: () => {
              this.$router.push('/home/Coupons')
            },
            onCancel: () => {
              if (item.storeId !== '0') {
                this.$router.push({path: '/merchant', query: {id: item.storeId}})
              } else {
                if (item.scopeType === 'PORTION_GOODS_CATEGORY') {
                  this.$router.push({path: '/goodsList', query: {categoryId: item.scopeId}})
                } else {
                  this.$router.push({path: '/goodsList'})
                }
              }
            }
          });
        }
      })
    },
    useScope (type, storeName) { // 判断优惠券使用范围
      let shop = '平台';
      let goods = '全部商品'
      if (storeName !== 'platform') shop = storeName
      switch (type) {
        case 'ALL':
          goods = '全部商品'
          break;
        case 'PORTION_GOODS':
          goods = '部分商品'
          break;
        case 'PORTION_GOODS_CATEGORY':
          goods = '部分分类商品'
          break;
      }
      return `${shop}${goods}可用`
    },
    addToCart (id) { // 添加商品到购物车
      const params = {
        num: 1,
        skuId: id
      }
      this.loading = true;
      addCartGoods(params).then(res => {
        this.loading = false;
        if (res.success) {
          this.$Message.success('商品已成功添加到购物车')
        } else {
          this.$Message.warning(res.message);
        }
      }).catch(() => { this.loading = false });
    },
    getCouponList () { // 获取优惠券列表
      // this.loading = true;
      const params = {
        pageNumber: 1,
        pageSize: 10
      }
      couponList(params).then(res => {
        this.loading = false
        if (res.success) {
          this.couponList = res.result.records
        }
      }).catch(() => { this.loading = false })
    },
    getOrderList () { // 获取订单列表
      this.loading = true
      const params = {
        pageNumber: 1,
        pageSize: 10,
        tag: 'ALL'
      }
      getOrderList(params).then(res => {
        this.loading = false
        if (res.success) {
          this.orderList = res.result.records;
        }
      });
    },
    getCollectList () { // 获取收藏列表
      const params = {
        pageNumber: 1,
        pageSize: 10,
        type: 'GOODS'
      }
      this.loading = true
      collectList(params).then(res => {
        this.loading = false
        this.collectList = res.result.records
      })
    },
    cancelCollect (id) { // 取消商品收藏
      cancelCollect('GOODS', id).then(res => {
        if (res.success) {
          this.$Message.success('取消收藏成功')
          this.getCollectList();
        }
      })
    },
    getTracksList () { // 获取足迹列表
      const params = {
        pageNumber: 1,
        pageSize: 20
      }
      this.loading = true
      tracksList(params).then(res => {
        this.tracksList = res.result.records
        this.loading = false
      }).catch(() => { this.loading = false })
    }
  }
};
</script>

<style scoped lang="scss">
@import '../../assets/styles/coupon.scss';
@import './drawer.scss';
.coupon-item{
  overflow: hidden;
  background-color: #fff;
  height: 120px;
  .c-left{
    padding: 15px;
  }
  .c-right{
    width: 38px;
    padding: 13px;
    font-size: 14px;
  }
  i{
    right: 30px;
    background-color: #eee;
  }
}
</style>
