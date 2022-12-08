<template>
  <div style="background: #fff">
    <BaseHeader></BaseHeader>
    <Search></Search>
    <drawer></drawer>
    <ShopHeader :detail="storeMsg"></ShopHeader>
    <div class="shop-item-path">
      <div class="shop-nav-container">
        <Breadcrumb>
          <BreadcrumbItem to="/">首页</BreadcrumbItem>
          <BreadcrumbItem
            v-for="(item, index) in categoryBar"
            :to="goGoodsList(index)"
            target="_blank"
            :key="index"
          >
            {{ item.name }}
          </BreadcrumbItem>
        </Breadcrumb>
        <div class="store-collect">
          <span class="mr_10" v-if="goodsMsg.data">
            <router-link :to="'Merchant?id=' + goodsMsg.data.storeId">{{
              goodsMsg.data.storeName
            }}</router-link>
          </span>
          <span @click="collect">
            <Icon
              type="ios-heart"
              :color="storeCollected ? '#ed3f14' : '#666'"
            />
            {{ storeCollected ? "已收藏店铺" : "收藏店铺" }}
          </span>
          <span class="ml_10" @click="IMService()">联系客服</span>
        </div>
      </div>
    </div>

    <!-- 商品信息展示 -->
    <ShowGoods
      @handleClickSku="targetClickSku"
      v-if="goodsMsg.data"
      :detail="goodsMsg"
    ></ShowGoods>
    <!-- 商品详细展示 -->
    <ShowGoodsDetail v-if="goodsMsg.data" :detail="goodsMsg"></ShowGoodsDetail>

    <Spin size="large" fix v-if="isLoading"></Spin>
    <BaseFooter></BaseFooter>
  </div>
</template>

<script>
import Search from "@/components/Search";
import ShopHeader from "@/components/header/ShopHeader";
import ShowGoods from "@/components/goodsDetail/ShowGoods";
import ShowGoodsDetail from "@/components/goodsDetail/ShowGoodsDetail";
import { goodsSkuDetail } from "@/api/goods";
import {
  cancelCollect,
  collectGoods,
  isCollection,
  getGoodsDistribution,
} from "@/api/member";
import { getDetailById } from "@/api/shopentry";
import { getIMDetail } from "@/api/common";
import Storage from "../plugins/storage";
export default {
  name: "GoodsDetail",
  beforeRouteEnter(to, from, next) {
    window.scrollTo(0, 0);
    next();
  },
  created() {
    this.getGoodsDetail();
  },
  data() {
    return {
      goodsMsg: {}, // 商品信息
      isLoading: false, // 加载状态
      categoryBar: [], // 分类
      storeCollected: false, // 商品收藏
      storeMsg: {}, // 店铺信息
      IMLink: "",
    };
  },
  methods: {
    // 跳转im客服
    async IMService() {
      // 获取访问Token
      let accessToken = Storage.getItem("accessToken");
      await this.getIMDetailMethods();
      if (!accessToken) {
        this.$Message.error("请登录后再联系客服");
        return;
      }
      window.open(
        this.IMLink +
          "?token=" +
          accessToken +
          "&id=" +
          this.goodsMsg.data.storeId
      );
    },
    // 获取im信息
    async getIMDetailMethods() {
      let res = await getIMDetail();
      if (res.success) {
        this.IMLink = res.result;
      }
    },
    // 点击规格
    targetClickSku(val) {
      this.getGoodsDetail(val);
    },
    // 获取商品详情
    getGoodsDetail(val) {
      this.isLoading = true;
      const params = val || this.$route.query;

      // 分销员id
      let distributionId =
        params && params.distributionId
          ? params.distributionId
          : this.Cookies.getItem("distributionId");
      // 如果有分销信息
      if (distributionId) {
        console.log(distributionId);
        // 先存储
        this.Cookies.setItem("distributionId", params.distributionId);
        let _this = this;
        // 绑定关系
        getGoodsDistribution(params.distributionId).then((res) => {
          // 绑定成功，则清除关系
          if (res.success) {
            _this.Cookies.removeItem("distributionId");
          }
        });
      }

      goodsSkuDetail(params)
        .then((res) => {
          this.isLoading = false;
          if (res.success) {
            const result = res.result;
            const cateName = res.result.categoryName;
            const cateId = result.data.categoryPath.split(",");
            const cateArr = [];
            cateId.forEach((e, index) => {
              // 插入分类id和name
              cateArr.push({
                id: e,
                name: cateName[index],
              });
            });
            this.categoryBar = cateArr;
            this.$set(this, "goodsMsg", res.result);
            // 判断是否收藏
            if (this.Cookies.getItem("userInfo")) {
              isCollection("STORE", this.goodsMsg.data.storeId).then((res) => {
                if (res.success && res.result) {
                  this.storeCollected = true;
                }
              });
            }
            if (!this.storeMsg) {
              // 获取店铺信息
              getDetailById(this.goodsMsg.data.storeId).then((res) => {
                if (res.success) {
                  this.storeMsg = res.result;
                }
              });
            }
          } else {
            this.$Message.error(res.message);
            this.$router.push("/");
          }
        })
        .catch((e) => {
          this.$router.push("/");
        });
    },
    goGoodsList(currIndex) {
      // 跳转商品列表
      const arr = [];
      this.categoryBar.forEach((e, index) => {
        if (index <= currIndex) {
          arr.push(e.id);
        }
      });
      return location.origin + "/goodsList?categoryId=" + arr.toString();
    },
    async collect() {
      // 收藏店铺
      if (this.storeCollected) {
        let cancel = await cancelCollect("STORE", this.goodsMsg.data.storeId);
        if (cancel.success) {
          this.$Message.success("已取消收藏");
          this.storeCollected = false;
        }
      } else {
        let collect = await collectGoods("STORE", this.goodsMsg.data.storeId);
        if (collect.code === 200) {
          this.storeCollected = true;
          this.$Message.success("收藏店铺成功,可以前往个人中心我的收藏查看");
        }
      }
    },
  },
  watch: {},
  components: {
    Search,
    ShopHeader,
    ShowGoods,
    ShowGoodsDetail,
  },
};
</script>
<style scoped lang="scss">
.shop-item-path {
  height: 38px;
  @include background_color($light_background_color);
  line-height: 38px;
  color: #2c2c2c;
}

.shop-nav-container {
  width: 1200px;
  margin: 0 auto;
  position: relative;

  .store-collect {
    position: absolute;
    right: 20px;
    top: 0;
    color: #999;

    span {
      &:hover {
        cursor: pointer;
        color: $theme_color;
      }
    }
  }
}
</style>
