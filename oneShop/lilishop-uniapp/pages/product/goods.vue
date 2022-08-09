<template>
  <div class="main-page">
    <!-- #ifdef APP-PLUS -->
    <view class="status_bar"></view>
    <!-- #endif -->
    <!-- 仅h5有效 打开App -->

    <!-- 分享 -->
    <shares
      v-if="enableShare && goodsDetail.id"
      :skuId="this.routerVal.id"
      :goodsId="this.routerVal.goodsId"
      :link="
        '/pages/product/goods?id=' +
        this.routerVal.id +
        '&goodsId=' +
        this.routerVal.goodsId
      "
      :thumbnail="goodsDetail.thumbnail"
      :goodsName="goodsDetail.goodsName"
      type="goods"
      @close="enableShare = false"
    />
    <popups
      v-model="popupsSwitch"
      @tapPopup="handleNavbarList"
      :popData="navbarListData"
      :x="navbarListX"
      :y="navbarListY"
      placement="top-start"
    />
    <view class="index">
      <!-- topBar -->
      <u-navbar
        :background="navbar"
        :is-back="false"
        :class="headerFlag ? 'header' : 'header bg-none scroll-hide'"
      >
        <div class="headerRow">
          <div class="backs">
            <u-icon @click="back()" name="arrow-left" class="icon-back"></u-icon>

            <u-icon
              name="list"
              @click="popupsSwitch = !popupsSwitch"
              class="icon-list"
            ></u-icon>
          </div>
          <div class="headerList" :class="headerFlag ? 'tab-bar' : 'tab-bar scroll-hide'">
            <div class="headerRow">
              <div
                class="nav-item"
                v-for="header in headerList"
                :key="header.id"
                :class="{ cur: scrollId === header.id }"
                @click="headerTab(header.id)"
              >
                {{ header.text }}
              </div>
            </div>
          </div>
        </div>
      </u-navbar>

      <u-navbar
        :border-bottom="false"
        v-show="!headerFlag"
        class="header-only-back"
        :background="navbarOnlyBack"
        :is-back="false"
      >
        <div>
          <div class="bg-back">
            <u-icon
              size="40"
              @click="back()"
              name="arrow-left"
              class="icon-back"
            ></u-icon>
            <u-icon
              size="40"
              @click="popupsSwitch = !popupsSwitch"
              name="list"
              class="icon-list"
            ></u-icon>
          </div>
        </div>
      </u-navbar>
    </view>

    <view
      class="product-container"
      :style="{ height: productRefHeight }"
      ref="productRef"
      id="productRef"
    >
      <scroll-view
        scroll-anchoring
        enableBackToTop="true"
        scroll-with-animation
        scroll-y
        class="scroll-page"
        :scroll-top="tabScrollTop"
        @scroll="pageScroll"
      >
        <view>
          <!-- 轮播图 -->
          <GoodsSwiper id="main1" :res="imgList" />

          <!-- 促销活动条 -->
          <PromotionAssembleLayout
            v-if="PromotionList"
            :detail="goodsDetail"
            :res="PromotionList"
          />

          <view class="card-box top-radius-0" id="main2">
            <!-- 活动不显示价钱 -->
            <view v-if="isSeckill || isGroup" class="desc-bold -goods-msg">
              <view class="-goods-flex">
                <view class="desc-bold">
                  {{ goodsDetail.goodsName || "" }}
                </view>
                <view class="favorite" @click="clickFavorite(goodsDetail.id)">
                  <u-icon
                    size="30"
                    :color="favorite ? '#f2270c' : '#262626'"
                    :name="favorite ? 'heart-fill' : 'heart'"
                  >
                  </u-icon>
                  <view :style="{ color: favorite ? '#f2270c' : '#262626' }">{{
                    favorite ? "已收藏" : "收藏"
                  }}</view>
                </view>
              </view>
              <!-- 商品描述 -->
              <view class="-goods-desc">
                {{ goodsDetail.sellingPoint || "" }}
              </view>
            </view>
            <view v-else class="-goods-msg">
              <!-- 没有拼团，秒杀等活动的情况下 -->
              <view>
                <view class="-goods-flex">
                  <!-- 如果有积分显示积分 -->
                  <view class="-goods-price" v-if="goodsDetail.price != undefined">
                    <span v-if="pointDetail.points">
                      <span class="price">{{ pointDetail.points }}</span>
                      <span>积分</span>
                    </span>

                    <span v-else>
													<span v-if="wholesaleList.length">
														<span>¥</span><span class="price">{{ formatPrice(wholesaleList[wholesaleList.length-1].price)[0] }}</span>.{{ formatPrice(wholesaleList[wholesaleList.length-1].price)[1] }}
														~
														<span>¥</span><span class="price">{{ formatPrice(wholesaleList[0].price)[0] }}</span>.{{ formatPrice(wholesaleList[0].price)[1] }}
													</span>
													<span v-else>
														<span>¥</span><span class="price">{{ formatPrice(goodsDetail.price)[0] }}</span>.{{ formatPrice(goodsDetail.price)[1] }}
													</span>
                    </span>
                  </view>
                  <view class="-goods-price" v-else>
                    ¥<span class="price">0 </span>.00
                  </view>

                  <view class="icons share" @click="shareChange()">
                    <u-icon size="30" name="share-fill"></u-icon>
                    <view>分享</view>
                  </view>
                  <view class="icons" @click="clickFavorite(goodsDetail.id)">
                    <u-icon
                      size="30"
                      :color="favorite ? '#f2270c' : '#262626'"
                      :name="favorite ? 'heart-fill' : 'heart'"
                    ></u-icon>
                    <view :style="{ color: favorite ? '#f2270c' : '#262626' }">{{
                      favorite ? "已收藏" : "收藏"
                    }}</view>
                  </view>
                </view>
                <view class="-goods-name desc-bold">
                  {{ goodsDetail.goodsName || "" }}
                </view>
                <view class="-goods-desc">
                  {{ goodsDetail.sellingPoint || "" }}
                </view>
              </view>
            </view>
          </view>

          <view class="card-box">
            <view class="card-flex" @click="shutMask(1)">
              <view class="card-title"> 促销 </view>
              <view class="card-content">
                <span v-if="PromotionList && emptyPromotion()">暂无促销信息</span>
                <PromotionLayout v-else @shutMasks="shutMask" :res="PromotionList" />
              </view>
              <view class="card-bottom">
                <u-icon name="more-dot-fill"></u-icon>
              </view>
            </view>
          </view>

          <!-- 拼团用户列表 -->
          <PromotionAssembleListLayout
            v-if="isGroup"
            @to-assemble-buy-now="toAssembleBuyNow"
            :res="PromotionList"
          />

          <!-- 配置地址 如果是虚拟产品的时候不展示 -->
          <view class="card-box" v-if="goodsDetail.goodsType != 'VIRTUAL_GOODS'">
            <view class="card-flex" @click="shutMask(4)">
              <view class="card-title"> 已选 </view>
              <view class="card-content">
                <span v-if="selectedGoods.spec"
                  >{{ selectedGoods.spec.specName }}-{{
                    selectedGoods.spec.specValue
                  }}</span
                >
                <span v-else>默认</span>
              </view>
              <view class="card-bottom">
                <u-icon name="more-dot-fill"></u-icon>
              </view>
            </view>
            <view class="card-flex" @click="shutMask(3)">
              <view class="card-title"> 送至</view>
              <view class="card-content">
                <span v-if="delivery">{{
                  delivery.consigneeAddressPath | clearStrComma
                }}</span>
                <span v-else>暂无地址信息</span>
              </view>
              <view class="card-bottom">
                <u-icon name="more-dot-fill"></u-icon>
              </view>
            </view>
          </view>

          <!-- 评价 -->
          <Evaluation id="main5" :goodsDetail="goodsDetail" />

          <!-- 店铺推荐 -->
          <storeLayout
            id="main7"
            :storeDetail="storeDetail"
            :goodsDetail="goodsDetail"
            :res="recommendList"
          />

          <!-- 宝贝详情 -->
          <GoodsIntro
            id="main9"
            :res="goodsDetail"
            :goodsParams="goodsParams"
            :goodsId="goodsDetail.goodsId"
            v-if="goodsDetail.id"
          />

          <!-- 宝贝推荐 -->
          <GoodsRecommend id="main11" :res="likeGoodsList" />
        </view>
      </scroll-view>

      <view class="page-bottom mp-iphonex-bottom" id="pageBottom">
        <view class="icon-btn">
          <view class="icon-btn-item" @click="navigateToStore(goodsDetail.storeId)">
            <u-icon size="34" class="red" name="home-fill"></u-icon>
            <view class="red icon-btn-name">店铺</view>
          </view>
          <view class="icon-btn-item" @click="linkMsgDetail()">
            <u-icon size="34" name="kefu-ermai"></u-icon>
            <view class="icon-btn-name">客服</view>
          </view>
          <view class="icon-btn-item" @click="reluchToCart()">
            <u-icon size="34" name="storeping-cart"></u-icon>
            <view class="icon-btn-name">购物车</view>
            <view v-if="nums && nums > 0" class="num-icon">{{ nums }}</view>
          </view>
        </view>
        <!-- 正常结算页面 -->
        <view class="detail-btn" v-if="!isGroup">
          <view
            class="to-store-car to-store-btn"
            v-if="goodsDetail.goodsType != 'VIRTUAL_GOODS'"
            @click="shutMask(4)"
          >
            加入购物车</view
          >
          <view class="to-buy to-store-btn" @click="shutMask(4, 'buy')">立即购买</view>
          <view class="to-store-car to-store-btn" v-if="startTimer">暂未开始</view>
        </view>
        <!-- 拼团结算 -->
        <view class="detail-btn" v-else>
          <view class="to-store-car pt-buy to-store-btn" @click="shutMask(4, 'buy')">
            <view>￥{{ goodsDetail.price | unitPrice }}</view>
            <view>单独购买</view>
          </view>
          <view class="to-buy pt-buy to-store-btn" @click="toAssembleBuyNow">
            <view>￥{{ goodsDetail.promotionPrice | unitPrice }}</view>
            <view>拼团价格</view>
          </view>
        </view>
      </view>
      <!-- 规格-模态层弹窗 -->
      <view class="spec">
        <!-- 促销弹窗 -->
        <u-popup
          v-model="promotionShow"
          :height="setup.height"
          :mode="setup.mode"
          :border-radius="setup.radius"
          @close="promotionShow = false"
          :mask-close-able="setup.close"
          closeable
        >
          <view class="header-title">优惠</view>
          <view class="cuxiao">
            <scroll-view class="scroll_mask" :scroll-y="true">
              <view class="con-cuxiao">
                <view class="cuxiao-title">促销活动</view>
                <PromotionDetailsLayout :res="PromotionList" />
              </view>
              <view class="con-cuxiao coupons">
                <view class="cuxiao-title">可领优惠券</view>
                <PromotionCoupon @getCoupon="getCoupon" :res="PromotionList" />
              </view>
            </scroll-view>
          </view>
        </u-popup>

        <!-- 配送地址弹窗 -->
        <popupAddress
          @closeAddress="closePopupAddress"
          @deliveryData="deliveryFun"
          v-if="goodsDetail.id"
          :goodsId="goodsDetail.id"
          :addressFlag="addressFlag"
        />

        <!-- 商品规格  商品详情，以及默认参与活动的id-->
        <popupGoods
          :addr="delivery"
          ref="popupGoods"
          @changed="changedGoods"
          @closeBuy="closePopupBuy"
          @queryCart="cartCount()"
          :goodsDetail="goodsDetail"
          :goodsSpec="goodsSpec"
          :isGroup="isGroup"
          :id="productId"
          v-if="goodsDetail.id"
          :pointDetail="pointDetail"
					:wholesaleList="wholesaleList"
          @handleClickSku="selectSku"
          :buyMask="buyMask"
        />
      </view>
    </view>
  </div>
</template>

<script>
/************接口API***************/
import { getGoods, getGoodsList, getMpScene, getGoodsDistribution } from "@/api/goods.js";
import * as API_trade from "@/api/trade.js";
import * as API_Members from "@/api/members.js";
import * as API_store from "@/api/store.js";
import { getIMDetail } from "@/api/common";
import { modelNavigateTo } from "@/pages/tabbar/home/template/tpl.js";
/************请求存储***************/
import storage from "@/utils/storage.js";

/************组件***************/
import PromotionLayout from "./product/promotion/-promotion"; //促销组件
import PromotionDetailsLayout from "./product/promotion/-promotion-details"; //促销活动详情
import PromotionAssembleLayout from "./product/promotion/-promotion-assemble-promotions"; //促销活动条
import PromotionAssembleListLayout from "./product/promotion/-promotion-assemble-list"; //拼团用户列表
import PromotionCoupon from "./product/promotion/-promotion-coupon"; //优惠券组件
import GoodsIntro from "./product/goods/-goods-intro"; //商品介绍组件
import GoodsRecommend from "./product/goods/-goods-recommend"; //宝贝推荐
import storeLayout from "./product/shop/-shop"; //店铺组件
import Evaluation from "./product/evaluation/-evaluation"; //评价组件
import GoodsSwiper from "./product/goods/-goods-swiper"; //轮播图组件
import popupGoods from "@/components/m-buy/goods"; //购物车商品的模块
import popupAddress from "./product/popup/address"; //地址选择模块
import shares from "@/components/m-share/index"; //分享
import popups from "@/components/popups/popups"; //气泡框
import setup from "./product/popup/popup";
export default {
  components: {
    popups,
    shares,
    PromotionLayout,
    PromotionDetailsLayout,
    PromotionAssembleLayout,
    PromotionAssembleListLayout,
    PromotionCoupon,
    GoodsIntro,
    GoodsRecommend,
    storeLayout,
    Evaluation,
    GoodsSwiper,
    popupGoods,
    popupAddress,
  },
  data() {
    return {
      setup,
      promotionShow: false, //弹窗开关
      // #ifdef H5
      navbarListX: 110, //导航栏列表栏x轴
      navbarListY: 80, //导航栏列表栏y轴
      // #endif
      // #ifdef MP-WEIXIN
      navbarListX: 100, //导航栏列表栏x轴
      navbarListY: 140, //导航栏列表栏y轴
      // #endif
      // #ifdef APP-PLUS
      navbarListX: 120, //导航栏列表栏x轴
      navbarListY: 170, //导航栏列表栏y轴
      // #endif
      navbarListData: [
        //导航栏列表栏数据
        {
          title: "首页",
          icon: "home-fill",
          ___type: "other",
        },
        {
          title: "购物车",
          icon: "bag-fill",
          ___type: "other",
        },
        {
          title: "搜索",
          icon: "search",
          ___type: "category",
        },
        {
          title: "个人中心",
          icon: "account-fill",
          ___type: "other",
        },
      ],
      popupsSwitch: false, //导航栏列表栏开关
      enableShare: false,
      selectedGoods: "", //选择的商品规格昵称
      isGroup: false, // 是否是拼团活动
      isSeckill:false, // 是否秒杀活动
      pointDetail: "", // 是否是积分商品
      assemble: "", //拼团的sku
      navbarOnlyBack: {
        background: "transparent",
      },
      navbar: {
        background: "#fff",
      },

      productRefHeight: "",
      header: {
        top: 0,
        height: 50,
      },
      goodsParams: [], // 商品参数
      headerFlag: false, //顶部导航显示与否
      headerList: [
        //顶部导航文字按照规则来 详情全局搜索
        {
          text: "商品",
          id: "1",
        },
        {
          text: "评价",
          id: "2",
        },
        {
          text: "详情",
          id: "3",
        },
        {
          text: "推荐",
          id: "4",
        },
      ],
      tabScrollTop: null,
      scrollArr: [],
      scrollId: "1",

      scrollFlag: true,
      current: "1", //当前显示的轮播图页

      goodsDetail: {}, //商品数据
      goodsSpec: "", //规格数据
      imgList: [], //轮播图数据
      favorite: false, //收藏与否flag
      recommendList: [], //推荐列表
      // maskFlag: false, //模态显示与否
      goodsInfo: false, //商品介绍弹窗
      addressFlag: false, //配送地址弹窗
      buyMask: false, //添加购物车直接购买，查看已选 弹窗

      num: 1, //添加到购物车的数量

      skuId: "", //
      storeDetail: "", //店铺基本信息,

      // 店铺信息
      storeParams: {
        pageNumber: 1,
        pageSize: 10,
      },

      likeGoodsList: "", //相似商品列表
      PromotionList: "", //活动,促销，列表
      specList: [],
      skusCombination: [],
      selectedSpec: [],
      nums: 0,
      delivery: "",

      exchange: {},
      productId: 0,

      startTimer: false, //未开启 是false

      routerVal: "",
      IMLink: "", // IM地址
			wholesaleList:[]
    };
  },

  computed: {
	// udesk IM 
    IM() {
      return this.IMLink + this.storeDetail.merchantEuid;
    },
  },

  watch: {
    isGroup(val) {
      if (val) {
        let timer = setInterval(() => {
          this.$refs.popupGoods.buyType = "PINTUAN";
          clearInterval(timer);
        }, 100);
      } else {
        this.$refs.popupGoods.buyType = "";
      }
    },
  },
  mounted() {
    const { windowHeight } = uni.getSystemInfoSync();
    let bottomHeight = 0;
    let topHeight = 0;
    uni.getSystemInfo({
      success: function (res) {
        // res - 各种参数
        let bottom = uni.createSelectorQuery().select(".page-bottom");
        bottom
          .boundingClientRect(function (data) {
            if (data && data.height) {
              //data - 各种参数
              bottomHeight = data.height; // 获取元素宽度
            }
          })
          .exec();
        let top = uni.createSelectorQuery().select(".header");
        top
          .boundingClientRect(function (data) {
            if (data && data.height) {
              //data - 各种参数
              topHeight = data.height; // 获取元素宽度
            }
          })
          .exec();
      },
    });

    this.productRefHeight = windowHeight - bottomHeight + "px";
  },
  async onLoad(options) {
    this.routerVal = options;
    // #ifdef MP-WEIXIN
    // 小程序默认分享
    uni.showShareMenu({
      withShareTicket: true,
      menus: ["shareAppMessage", "shareTimeline"],
    });
    // #endif
  },
  async onShow() {
    this.goodsDetail = {};
    //如果有参数ids说明事分销短连接，需要获取参数
    if (this.routerVal.scene) {
      getMpScene(this.routerVal.scene).then((res) => {
        if (res.data.success) {
          let data = res.data.result.split(","); // skuId,goodsId,distributionId
          this.init(data[0], data[1], data[2]);
        }
      });
    } else {
      this.init(this.routerVal.id, this.routerVal.goodsId, this.routerVal.distributionId);
    }
  },
  // #ifdef MP-WEIXIN
  onShareAppMessage(res) {
    return {
      path: this.share(),
      title: `[好友推荐]${this.goodsDetail.goodsName}`,
      imageUrl: this.goodsDetail.goodsGalleryList[0],
    };
  },
  // #endif
  methods: {
    share() {
      return `/pages/product/goods?id=${this.routerVal.id}&goodsId=${this.routerVal.goodsId}`;
    },
    /**
     * 导航栏列表栏
     */
    handleNavbarList(val) {
      modelNavigateTo({ url: val });
    },

    /**
     * 循环出当前促销是否为空
     */
    emptyPromotion() {
      if (
        this.PromotionList == "" ||
        this.PromotionList == null ||
        this.PromotionList == []
      ) {
        return true;
      }
    },
    selectSku(idObj) {
      this.init(idObj.skuId, idObj.goodsId);
    },
    /**
     * 初始化信息
     */
    async init(id, goodsId, distributionId = "") {
      this.isGroup = false; //初始化拼团
      this.productId = id; // skuId
      // 这里请求获取到页面数据  解析数据

      let response = await getGoods(id, goodsId);

      if (!response.data.success) {
        setTimeout(() => {
          uni.navigateBack();
        }, 500);
      }
      // 这里是绑定分销员
      if (distributionId || this.$store.state.distributionId) {
        let disResult = await getGoodsDistribution(distributionId);
        if (!disResult.data.success || disResult.statusCode == 403) {
          this.$store.state.distributionId = distributionId;
        }
      }
      /**商品信息以及规格信息存储 */
      this.goodsDetail = response.data.result.data;
      this.wholesaleList = response.data.result.wholesaleList;
      this.goodsSpec = response.data.result.specs;
      this.PromotionList = response.data.result.promotionMap;
      this.goodsParams = response.data.result.goodsParamsDTOList || [];

      // 判断是否拼团活动或者积分商品 如果有则显示拼团活动信息
      this.PromotionList &&
        Object.keys(this.PromotionList).forEach((item) => {
          // 拼团商品
          if (item.indexOf("PINTUAN") == 0) {
            this.isGroup = true;
          }
          // 积分
          if (item.indexOf("POINTS_GOODS") == 0) {
            this.pointDetail = this.PromotionList[item];
          }
          // 秒杀
          if (item.indexOf("SECKILL") == 0) {
            this.isSeckill = true
          }
        });
      // 轮播图
      this.imgList = this.goodsDetail.goodsGalleryList;

      // 获取店铺基本信息
      this.getStoreBaseInfoFun(this.goodsDetail.storeId);

      // 获取购物车
      this.cartCount();

      // 获取店铺推荐商品
      this.getStoreRecommend();

      // 获取商品列表
      this.getOtherLikeGoods();
      // 获取商品是否已被收藏 如果未登录不获取

      if (this.$options.filters.isLogin("auth")) {
        this.getGoodsCollectionFun(this.goodsDetail.id);
      }
      // 获取IM 需要的话使用
      // this.getIMDetailMethods();
    },

    async getIMDetailMethods() {
      let res = await getIMDetail();
      if (res.data.success) {
        this.IMLink = res.data.result;
      }
    },

    linkMsgDetail() {
      // lili 基础客服
	
	  uni.navigateTo({
		url: `/pages/tabbar/home/web-view?IM=${this.storeDetail.storeId}`,
	  });
		
		// udesk 代码  
		// if (this.storeDetail.merchantEuid) {
		//   uni.navigateTo({
		//     url: `/pages/tabbar/home/web-view?src=${this.IM}`,
		//   });
		// }
		  
		  
        // 客服 云智服代码 
        // // #ifdef MP-WEIXIN
        // const params = {
        //   storeName: this.storeDetail.storeName,
        //   goodsName: this.goodsDetail.goodsName,
        //   goodsId: this.goodsDetail.goodsId,
        //   goodsImg: this.goodsDetail.thumbnail,
        //   price: this.goodsDetail.promotionPrice || this.goodsDetail.price,
        //   // originalPrice: this.goodsDetail.original || this.goodsDetail.price,
        //   uuid: storage.getUuid(),
        //   token: storage.getAccessToken(),
        //   sign: this.storeDetail.yzfSign,
        //   mpSign: this.storeDetail.yzfMpSign,
        // };
        // uni.navigateTo({
        //   url:
        //     "/pages/product/customerservice/index?params=" +
        //     encodeURIComponent(JSON.stringify(params)),
        // });
        // // #endif
        // // #ifndef MP-WEIXIN
        // const sign = this.storeDetail.yzfSign;
        // uni.navigateTo({
        //   url:
        //     "/pages/tabbar/home/web-view?src=https://yzf.qq.com/xv/web/static/chat/index.html?sign=" +
        //     sign,
        // });
        // // #endif
    
    },
    // 格式化金钱  1999 --> [1999,00]
    formatPrice(val) {
      if (typeof val == "undefined") {
        return val;
      }
      return val.toFixed(2).split(".");
    },

    /**选择商品 */
    changedGoods(val) {
      this.selectedGoods = val;
    },

    /**  点击子级地址回调参数*/
    deliveryFun(val) {
      this.delivery = val;
    },
    /**
     * 地址子级关闭回调
     */
    closePopupAddress(val) {
      this.addressFlag = val;
      // this.maskFlag = false;
    },
    /**
     * 商品规格子级关闭回调
     */
    closePopupBuy(val) {
      this.buyMask = val;
      // this.maskFlag = false;
    },

    /** 参与拼团  创建拼团 */
    toAssembleBuyNow(order) {
      this.shutMask(4, "PINTUAN", order);
    },
    /**
     * 查看购物车
     */
    reluchToCart() {
      let obj = {
        from: "product",
        id: this.productId,
      };
      storage.setCartBackbtn(obj);
      uni.switchTab({
        url: "/pages/tabbar/cart/cartList",
      });
    },

    /**
     * 查询购物车总数量
     */
    cartCount() {
      if (storage.getHasLogin()) {
        API_trade.getCartNum().then((res) => {
          this.nums = res.data.result;
        });
      }
    },

    /**
     * 返回
     */
    back() {
      if (getCurrentPages().length == 1) {
        uni.switchTab({
          url: "/pages/tabbar/home/index",
        });
      } else {
        uni.navigateBack();
      }
    },

    /**
     * 获取店铺信息
     */
    getStoreBaseInfoFun(id) {
      API_store.getStoreBaseInfo(id).then((res) => {
        if (res.data.success) {
          this.storeDetail = res.data.result;
        }
      });
    },

    /**
     * 删除收藏店铺
     */
    deleteGoodsCollectionFun(id) {
      API_Members.deleteGoodsCollection(id).then((res) => {
        if (res.statusCode == 200) {
          uni.showToast({
            title: "商品已取消收藏!",
            icon: "none",
          });
          this.favorite = !this.favorite;
        }
      });
    },

    /**
     * 获取商品是否已被收藏
     */
    getGoodsCollectionFun(goodsId) {
      if (storage.getHasLogin()) {
        API_Members.getGoodsIsCollect("GOODS", goodsId).then((res) => {
          this.favorite = res.data.result;
        });
      }
    },

    /**
     * 获取店铺推荐商品列表
     */
    getStoreRecommend() {
      getGoodsList({
        pageNumber: 1,
        pageSize: 6,
        storeId: this.goodsDetail.storeId,
        recommend: true,
      }).then((res) => {
        this.recommendList = res.data.result.content;
      });
    },

    /**
     * 获取相似商品列表
     *
     */
    getOtherLikeGoods() {
      getGoodsList({
        pageNumber: 1,
        pageSize: 10,
        category: this.goodsDetail.categoryId,
        keyword: this.goodsDetail.name,
      }).then((res) => {
        this.likeGoodsList = res.data.result.content;
      });
    },

    /**
     * 领取优惠券
     */
    receiveCouponsFun(id) {
      API_Members.receiveCoupons(id).then((res) => {
        uni.showToast({
          title: res.data.message,
          icon: "none",
        });
      });
    },

    /**
     * 跳转到店铺页面
     */
    navigateToStore(store_id) {
      uni.navigateTo({
        url: `/pages/product/shopPage?id=` + store_id,
      });
    },

    /**
     * 获取优惠券按钮
     */
    getCoupon(item) {
      this.receiveCouponsFun(item.id);
    },

    /**
     * 规格弹窗开关
     */
    shutMask(flag, buyFlag, type) {
      this.promotionShow = false;
      this.buyMask = false;
      this.addressFlag = false;
      if (flag) {
        switch (flag) {
          case 1: //优惠券弹窗
            this.promotionShow = true;

            break;
          case 3:
            this.addressFlag = true;
            break;
          case 4: //添加购物车直接购买，查看已选 弹窗
            // 判断是否是一个规格

            this.buyMask = true;
            if (buyFlag == "PINTUAN") {
              if (type.orderSn) {
                this.$refs.popupGoods.parentOrder = type;
              }
              this.$refs.popupGoods.buyType = "PINTUAN";
            }
            if (buyFlag == "buy") {
              this.$refs.popupGoods.buyType = "";
            }

            break;
        }
      }
    },

    /**
     * 收藏
     */
    clickFavorite(id) {
      if (this.favorite) {
        // 取消收藏
        this.deleteGoodsCollectionFun(id);
        return false;
      }
      API_Members.collectionGoods("GOODS", id).then((res) => {
        if (res.data.success) {
          uni.showToast({
            title: "收藏成功!",
            icon: "none",
          });
        }
      });
      this.favorite = !this.favorite;
    },

    /**
     * 顶部header显示或隐藏
     */
    pageScroll(e) {
      if (this.scrollFlag) {
        this.calcSize();
      }
      if (e.detail.scrollTop > 200) {
        //当距离大于200时显示回到顶部按钮
        this.headerFlag = true;
      } else {
        //当距离小于200时隐藏回到顶部按钮
        this.headerFlag = false;
      }
      if (e.detail.scrollTop < this.scrollArr[0] - 10) {
        this.scrollId = "1";
      }
      if (e.detail.scrollTop > this.scrollArr[1] - 10) {
        this.scrollId = "2";
      }
      if (e.detail.scrollTop > this.scrollArr[2] - 10) {
        this.scrollId = "3";
      }
      if (e.detail.scrollTop > this.scrollArr[3] - 10) {
        this.scrollId = "4";
      }
    },

    /**
     * 计算每个要跳转到的模块高度信息
     */
    calcSize() {
      let h = 0;
      let that = this;
      let arr = [
        "main1",
        "main2",
        "main3",
        "main4",
        "main5",
        "main6",
        "main7",
        "main8",
        "main9",
        "main10",
        "main11",
      ];
      arr.forEach((item) => {
        let view = uni.createSelectorQuery().select("#" + item);
        view
          .fields(
            {
              size: true,
            },
            (data) => {
              if (
                item === "main1" ||
                item === "main5" ||
                item === "main9" ||
                item === "main11"
              ) {
                that.scrollArr.push(h);
              }
              if (data && data.height) {
                h += data.height;
              }
            }
          )
          .exec();
      });
      this.scrollFlag = false;
    },

    /**
     * 点击顶部跳转到对应位置
     */
    headerTab(id) {
      if (this.scrollFlag) {
        this.calcSize();
      }
      this.scrollId = id;

      this.$nextTick(() => {
        this.tabScrollTop = this.scrollArr[id - 1];
      });
    },

    /**
     * 点击分享
     */
    async shareChange() {
      this.enableShare = true;
    },
  },
};
</script>

<style lang="scss" scoped>
// #ifdef MP-WEIXIN
@import "./product/mp-goods.scss";
// #endif

@import "./product/style.scss";
@import "./product/product.scss";
</style>
