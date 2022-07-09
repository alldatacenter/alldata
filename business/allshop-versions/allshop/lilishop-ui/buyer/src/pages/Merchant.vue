<template>
  <div class="merchant">
    <BaseHeader />
    <!-- 搜索栏 -->
    <Search :store="true" @search="search"></Search>
    <!-- 店铺logo -->
    <div class="shop-logo">
      <div>
        <img :src="storeMsg.storeLogo" height="80" alt="">
        <div>
          <p>{{storeMsg.storeName || 'xx店铺'}}</p>
          <p class="ellipsis" :alt="storeMsg.storeDesc" v-html="storeMsg.storeDesc"></p>
        </div>
        <div>
          <span class="hover-pointer" @click="collect"><Icon type="ios-heart" :color="storeCollected ? '#ed3f14' : '#fff'" />{{storeCollected?'已收藏店铺':'收藏店铺'}}</span>
          <span style="width:80px" class="hover-pointer ml_10" @click="connectCs(storeMsg.yzfSign)"><Icon custom="icomoon icon-customer-service"  />联系客服</span>
        </div>
      </div>
    </div>
    <div  class="store-category">
      <ul class="cate-list">
        <li class="cate-item" @click="searchByCate({id:'', labelName: '店铺推荐'})">首页</li>
        <li class="cate-item" v-for="(cate, index) in cateList" :key="index">
          <Dropdown v-if="cate.children.length">
            <div @click.self="searchByCate(cate)">{{cate.labelName}} <Icon type="ios-arrow-down"></Icon></div>
            <DropdownMenu slot="list">
                <DropdownItem @click.native="searchByCate(sec)" :name="sec.id" v-for="sec in cate.children" :key="sec.id">{{sec.labelName}}</DropdownItem>
            </DropdownMenu>
          </Dropdown>
          <span v-else @click.self="searchByCate(cate)">{{cate.labelName}}</span>
        </li>
      </ul>
    </div>

    <div class="promotion-decorate">{{cateName}}</div>

    <div class="goods-list">
      <empty v-if="goodsList.length === 0" />
      <div
        v-else
        class="goods-show-info"
        v-for="(item, index) in goodsList"
        :key="index"
        @click="goGoodsDetail(item.content.id, item.content.goodsId)"
      >
        <div class="goods-show-img">
          <img width="220" height="220" :src="item.content.thumbnail" />
        </div>
        <div class="goods-show-price">
          <span>
            <span class="seckill-price text-danger">{{
              item.content.price | unitPrice("￥")
            }}</span>
          </span>
        </div>
        <div class="goods-show-detail">
          <span>{{ item.content.goodsName }}</span>
        </div>
        <div class="goods-show-num">
          已有<span>{{ item.content.commentNum || 0 }}</span>人评价
        </div>
      </div>
    </div>
    <div class="goods-page">
      <Page
        show-sizer
        @on-change="changePageNum"
        @on-page-size-change="changePageSize"
        :total="total"
        :page-size="params.pageSize"
      ></Page>
    </div>
    <BaseFooter />
  </div>
</template>

<script>
import {getDetailById, getCateById} from '@/api/shopentry'
import { cancelCollect, collectGoods, isCollection } from '@/api/member';
import {goodsList} from '@/api/goods';
export default {
  name: 'Merchant',
  data () {
    return {
      storeMsg: {}, // 店铺信息
      cateList: [], // 店铺分裂
      goodsList: [], // 商品列表
      total: 0, // 商品数量
      params: { // 请求参数
        pageNumber: 1,
        pageSize: 20,
        keyword: '',
        storeId: this.$route.query.id,
        storeCatId: ''
      },
      cateName: '店铺推荐', // 分类名称
      storeCollected: false // 是否收藏
    }
  },
  created () {
    this.getStoreMsg()
    this.getCateList()
    this.getGoodsList()
  },
  methods: {
    getStoreMsg () { // 店铺信息
      getDetailById(this.$route.query.id).then(res => {
        if (res.success) {
          this.storeMsg = res.result
          document.title = this.storeMsg.storeName
          if (this.Cookies.getItem('userInfo')) {
            isCollection('STORE', this.storeMsg.storeId).then(res => {
              if (res.success && res.result) {
                this.storeCollected = true;
              }
            })
          }
        }
      })
    },
    getCateList () { // 店铺分类
      getCateById(this.$route.query.id).then(res => {
        if (res.success) {
          this.cateList = res.result
        }
      })
    },
    getGoodsList () { // 商品信息
      goodsList(this.params).then((res) => {
        if (res.success) {
          this.goodsList = res.result.content;
          this.total = res.result.totalElements;
        }
      }).catch(() => {
      });
    },
    goGoodsDetail (skuId, goodsId) {
      // 跳转商品详情
      let routeUrl = this.$router.resolve({
        path: '/goodsDetail',
        query: { skuId, goodsId }
      });
      window.open(routeUrl.href, '_blank');
    },
    search (val) { // 搜索本店商品
      console.log(val);
      this.params.keyword = val
      this.getGoodsList()
    },
    searchByCate (cate) { // 搜索同分类下商品
      this.params.storeCatId = cate.id
      this.cateName = cate.labelName
      this.getGoodsList()
    },
    // 分页 修改页码
    changePageNum (val) {
      this.params.pageNumber = val;
      this.getGoodsList();
    },
    // 分页 修改页数
    changePageSize (val) {
      this.params.pageNumber = 1;
      this.params.pageSize = val;
      this.getGoodsList();
    },
    async collect () { // 收藏店铺
      if (this.storeCollected) {
        let cancel = await cancelCollect('STORE', this.storeMsg.storeId)
        if (cancel.success) {
          this.$Message.success('已取消收藏')
          this.storeCollected = false;
        }
      } else {
        let collect = await collectGoods('STORE', this.storeMsg.storeId);
        if (collect.code === 200) {
          this.storeCollected = true;
          this.$Message.success('收藏店铺成功,可以前往个人中心我的收藏查看');
        }
      }
    }
  }
};
</script>

<style scoped lang="scss">
@import '../assets/styles/goodsList.scss';
.merchant {
  margin: 0 auto;
}
.shop-logo {
  position: relative;
  width: 100%;
  background-color: #666;
  padding: 4px;
  color: #fff;
  >div{
    display: flex;
    width: 1200px;
    margin: 0 auto;
    align-items: center;

    >div:nth-child(2){
      margin-left: 10px;
      flex: 1;
    }

    >div:nth-child(3){
      width: 200px;
    }
  }
  p:nth-child(1) {
    font-size: 20px;
  }
  p:nth-child(2){
    font-size: 14px;
    max-height: 40px;
    max-width: 400px;
  }
}

.store-category {
  background-color: #005aa0;
  color: #fff;
  .cate-list{
    width: 1200px;
    margin: 0 auto ;
    clear: left;
    height: 30px;
    line-height: 30px;
    .cate-item {
      margin-right: 25px;
      float: left;
    }
    .cate-item:hover{
      cursor: pointer;
    }
  }
}
.promotion-decorate::before,.promotion-decorate::after{
  background-image: url('/src/assets/images/sprite@2x.png');
}
</style>
