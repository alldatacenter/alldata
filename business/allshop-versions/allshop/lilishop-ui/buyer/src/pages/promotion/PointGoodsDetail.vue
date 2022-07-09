<template>
  <div style="background:#fff;">
    <BaseHeader></BaseHeader>
    <Search style="border-bottom:2px solid red;"></Search>
    <!-- <drawer></drawer> -->
    <div class="base-width cate-container">
      <Breadcrumb>
        <BreadcrumbItem to="/">首页</BreadcrumbItem>
        <BreadcrumbItem>{{goodsMsg.pointsGoodsCategoryName}}</BreadcrumbItem>
      </Breadcrumb>
    </div>
    <!-- 商品信息展示 -->
    <div class="item-detail-show">
      <!-- 详情左侧展示数据、图片，收藏、举报 -->
      <div class="item-detail-left">
        <!-- 大图、放大镜 -->
        <div class="item-detail-big-img">
          <pic-zoom v-if="goodsSku.thumbnail" :url="goodsSku.thumbnail" :scale="2"></pic-zoom>
        </div>
        <div class="item-detail-img-row">
          <div class="item-detail-img-small">
            <img :src="goodsSku.thumbnail" />
          </div>
        </div>
      </div>
      <!-- 右侧商品信息、活动信息、操作展示 -->
      <div class="item-detail-right">
        <div class="item-detail-title">
          <p>{{ goodsSku.goodsName }}</p>
        </div>
        <div class="sell-point">
          {{goodsSku.sellingPoint}}
        </div>
        <!-- 商品详细 -->
        <div class="item-detail-price-row">
          <div class="item-price-left">
            <!-- 商品原价 -->
            <div class="item-price-row">
              <p>
                <span class="item-price-title">积 &nbsp;&nbsp;&nbsp;&nbsp;分</span>
                <span class="item-price">{{goodsMsg.points}}</span>
              </p>
            </div>
          </div>
        </div>
        <div class="add-buy-car-box">
          <div class="item-select">
            <div class="item-select-title">
              <p>数量</p>
            </div>
            <div class="item-select-row">
              <InputNumber :min="1" :disabled="goodsSku.quantity === 0" v-model="count"></InputNumber>
              <span class="inventory"> 库存{{goodsSku.quantity}}</span>
            </div>
          </div>
          <div class="item-select" v-if="goodsSku.goodsType !== 'VIRTUAL_GOODS' && goodsSku.weight !== 0">
            <div class="item-select-title">
              <p>重量</p>
            </div>
            <div class="item-select-row">
              <span class="inventory"> {{goodsSku.weight}}kg</span>
            </div>
          </div>
          <div class="add-buy-car">
            <Button type="error" :loading="loading" :disabled="goodsSku.quantity === 0" @click="pointBuy">积分购买</Button>
          </div>
        </div>
      </div>
    </div>
    <!-- 商品详情 -->
    <div class="base-width item-intro" ref="itemIntroGoods">
      <div>商品介绍</div>
      <div v-html="goodsSku.intro" class="mt_10 ml_10" v-if="goodsSku.intro"></div>
      <div v-else style="margin:20px;">暂无商品介绍</div>
    </div>
    <Spin size="large" fix v-if="isLoading"></Spin>
    <BaseFooter></BaseFooter>
  </div>
</template>

<script>
import Search from '@/components/Search';
import PicZoom from 'vue-piczoom';
import { addCartGoods } from '@/api/cart.js';
import { pointGoodsDetail } from '@/api/promotion';
export default {
  name: 'PointGoodsDetail',
  beforeRouteEnter (to, from, next) {
    window.scrollTo(0, 0);
    next();
  },
  created () {
    this.getGoodsDetail();
  },
  mounted () {
    window.addEventListener('scroll', this.handleScroll)
  },
  data () {
    return {
      goodsMsg: {}, // 商品信息
      goodsSku: {}, // 商品sku
      isLoading: false, // 加载状态
      categoryBar: [], // 分类
      onceFlag: true, // 只调用一次
      count: 1, // 购买商品数量
      loading: false // 提交加载状态
    };
  },
  methods: {
    // 获取积分商品详情
    getGoodsDetail () {
      this.isLoading = true;
      pointGoodsDetail(this.$route.query.id).then((res) => {
        this.isLoading = false;
        if (res.success) {
          this.goodsMsg = res.result;
          this.goodsSku = res.result.goodsSku
        } else {
          this.$Message.error(res.message)
          this.$router.push('/')
        }
      }).catch(() => {
        this.$router.push('/')
      });
    },
    pointBuy () {
      const params = {
        num: this.count,
        skuId: this.goodsMsg.skuId,
        cartType: 'POINTS'
      };
      this.loading = true;
      addCartGoods(params).then(res => {
        this.loading = false;
        if (res.success) {
          this.$router.push({path: '/pay', query: {way: params.cartType}});
        } else {
          this.$Message.warning(res.message);
        }
      }).catch(() => {
        this.loading = false;
      });
    },
    handleScroll () { // 监听页面滚动
      if (this.onceFlag) {
        this.$nextTick(() => {
          this.changeHeight()
        });
        this.onceFlag = false
      }
    },
    changeHeight () { // 设置商品详情高度
      let goodsDetailCon = document.querySelector('.item-intro')
      let heightCss = window.getComputedStyle(goodsDetailCon).height;
      heightCss = parseInt(heightCss.substr(0, heightCss.length - 2)) + 89;
      this.$refs.itemIntroGoods.style.height = heightCss + 'px';
    }
  },
  components: {
    Search, PicZoom
  }
};
</script>
<style scoped lang="scss">
.base-width{
  width: 1200px;
  margin: 0 auto;
  position: relative;
}
.cate-container{
  background-color: #eee;
  height: 30px;
  line-height: 30px;
  padding-left: 10px;
  margin-top: 10px;
}
// 商品图片，价格等
.item-detail-show {
  width: 1200px;
  margin: 0 auto;
  padding: 30px;
  display: flex;
  flex-direction: row;
}
.item-detail-left {
  width: 350px;
  margin-right: 30px;
}

.item-detail-big-img {
  width: 350px;
  height: 350px;
  box-shadow: 0px 0px 8px $border_color;
  cursor: pointer;
  img {
    width: 100%;
  }
}

.item-detail-img-row {
  margin-top: 15px;
  display: flex;
}

.item-detail-img-small {
  width: 68px;
  height: 68px;
  box-shadow: 0px 0px 8px #ccc;
  cursor: pointer;
  margin-left: 5px;
  img {
    height: 100%;
    width: 100%;
  }
}
/*商品选购详情*/
.item-detail-right {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-detail-title p {
  @include content_color($light_content_color);
  font-weight: bold;
  font-size: 20px;
  padding: 8px 0;
}

.item-detail-express {
  font-size: 14px;
  padding: 2px 3px;
  border-radius: 3px;
  background-color: $theme_color;
  color: #fff;
}

/*商品标签*/
.item-detail-tag {
  padding: 8px 0;
  font-size: 12px;
  color: $theme_color;
}

/*价格详情等*/
.item-detail-price-row {
  padding: 10px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  background: url("../../assets/images/goodsDetail/price-bg.png");
}

.item-price-left {
  display: flex;
  flex-direction: column;
}

.item-price-title {
  color: #999999;
  font-size: 14px;
  margin-right: 15px;
}

.item-price-row {
  margin: 5px 0px;
}

.item-price {
  color: $theme_color;
  font-size: 20px;
  cursor: pointer;
}
.item-price-old {
  color: gray;
  text-decoration: line-through;
  font-size: 14px;
  margin-left: 5px;
}
.add-buy-car-box {
  width: 100%;
  margin-top: 15px;
  border-top: 1px dotted $border_color;
}

.add-buy-car {
  margin-top: 15px;
  >*{
    margin: 0 4px;
  }
}
.item-select {
  display: flex;
  flex-direction: row;
  margin-top: 15px;
}

.item-select-title {
  @include content_color($light_content_color);
  font-size: 14px;
  margin-right: 15px;
  width: 60px;
}

.item-select-column {
  display: flex;
  flex-wrap: wrap;
  flex: 1;
}

.item-select-row {
  margin-bottom: 8px;
}

.item-select-box {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 5px;
  margin-right: 8px;
  @include background_color($light_background_color);
  border: 0.5px solid $border_color;
  cursor: pointer;
  @include content_color($light_content_color);
}

.item-select-box:hover {
  border: 0.5px solid $theme_color;
}

.item-select-box-active {
  border: 0.5px solid $theme_color;
}

.item-select-intro p {
  margin: 0px;
  padding: 5px;
}
.sell-point {
  font-size: 12px;
  color: red;
  margin-bottom: 5px;
}
// 商品详情
.item-intro {
  margin-top: 10px;
  >div:nth-child(1) {
    height: 40px;
    line-height: 40px;
    background-color: #eee;
    padding-left: 20px;
  }
}
</style>
