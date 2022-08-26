<template>
  <div class="coupon-center">
    <BaseHeader></BaseHeader>
    <div class="content">
      <div>
        <div class="coupon-title">
          <router-link to="/">
            <img src="../assets/images/logo.png" width="120" alt="">
          </router-link>
          <p>领券中心</p>
          <Input search style="width:400px" @on-search='search' enter-button="搜索" placeholder="搜索优惠券" />
        </div>

        <div class="fontsize_18 recommend">推荐好券</div>

        <empty v-if="list.length===0" />
        <ul class="coupon-list" v-else>
          <li v-for="(item, index) in list" class="coupon-item" :key="index">
            <div class="c-left">
              <div>
                <span v-if="item.couponType === 'PRICE'" class="fontsize_12 global_color">￥<span class="price">{{item.price | unitPrice}}</span></span>
                <span v-if="item.couponType === 'DISCOUNT'" class="fontsize_12 global_color"><span class="price">{{item.couponDiscount}}</span>折</span>
                <span class="describe">满{{item.consumeThreshold}}元可用</span>
              </div>
              <p>使用范围：{{useScope(item.scopeType, item.storeName)}}</p>
              <p>有效期：{{item.endTime}}</p>
            </div>
            <b></b>
            <a class="c-right" @click="receive(item)">立即领取</a>
            <i class="circle-top"></i>
            <i class="circle-bottom"></i>
          </li>
        </ul>
        <Page :total="total" @on-change="changePageNum"
          class="pageration"
          @on-page-size-change="changePageSize"
          :page-size="params.pageSize"
          show-total
          show-sizer>
        </Page>
      </div>
    </div>
    <BaseFooter></BaseFooter>
  </div>
</template>
<script>
import {couponList, receiveCoupon} from '@/api/member.js'
export default {
  data () {
    return {
      list: [], // 优惠券列表
      total: 0, // 优惠券总数
      params: { // 请求参数
        getType: 'FREE',
        pageNumber: 1,
        pageSize: 20
      }
    }
  },
  methods: {
    // 搜索优惠券
    search (item) {
      this.params.couponName = item
      this.params.pageNumber = 1
      this.getList()
    },
    // 获取优惠券列表
    getList () {
      this.$Spin.show()
      couponList(this.params).then(res => {
        this.$Spin.hide()
        this.loading = false
        if (res.success) {
          this.list = res.result.records
          this.total = res.result.total
        }
      }).catch(() => { this.$Spin.hide() })
    },
    // 分页 改变页码
    changePageNum (val) {
      this.params.pageNumber = val;
      this.getList()
    },
    // 分页 改变每页数
    changePageSize (val) {
      this.params.pageNumber = 1;
      this.params.pageSize = val;
      this.getList()
    },
    // 领取优惠券
    receive (item) {
      receiveCoupon(item.id).then(res => {
        if (res.success) {
          this.$Modal.confirm({
            title: '领取优惠券',
            content: '<p>优惠券领取成功，可到我的优惠券页面查看</p>',
            okText: '我的优惠券',
            cancelText: '立即使用',
            closable: true,
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
    // 优惠券可用范围
    useScope (type, storeName) {
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
    }
  },
  mounted () {
    this.getList()
  }
}
</script>
<style lang="scss" scoped>
  @import '../assets/styles/coupon.scss';
  .content{
    width: 100%;
    background-color: #fff;

    > div {
      margin: 10px auto;
      width: 1200px;
    }
  }
  .coupon-title {
    display: flex;
    align-items: center;

    p{
      font-size: 18px;
      margin-right: 500px;
    }
    border-bottom: 2px solid $theme_color;
  }
  .recommend {
    margin: 20px auto;
    font-weight: bold;
    width: 200px;
    text-align: center;
  }
  .coupon-item {
    b{
      background: url('../assets/images/small-circle.png') top left repeat-y;
    }
  }
  .pageration {
    text-align: right;
    padding-bottom: 10px;
  }
</style>
