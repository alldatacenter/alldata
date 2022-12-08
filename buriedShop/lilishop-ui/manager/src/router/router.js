import Main from "@/views/Main.vue";

const config = require('@/config/index')
// 不作为Main组件的子页面展示的页面单独写，如下
export const loginRouter = {
  path: "/login",
  name: "login",
  meta: {
    title: `登录 - ${config.title}运营后台`
  },
  component: () => import("@/views/login.vue")
};

// 作为Main组件的子页面展示但是不在左侧菜单显示的路由写在otherRouter里
export const otherRouter = {
  path: "/",
  name: "otherRouter",
  redirect: "/home",
  component: Main,
  children: [
    {
      path: "home",
      title: '首页',
      name: "home_index",
      component: () => import("@/views/home/home.vue")
    },
    {
      path: "personal-center",
      title: "个人中心",
      name: "personal-center",
      meta: { title: "个人中心" },
      component: () => import("@/views/personal-center/personal-center.vue")
    },
    {
      path: "change-password",
      title: "修改密码",
      name: "change_password",
      component: () => import("@/views/change-password/change-password.vue")
    },
    {
      path: "category",
      title: "类型列表",
      name: "category",
      component: () => import("@/views/goods/goods-manage/category.vue")
    },
    {
      path: "parameter",
      title: "参数绑定",
      name: "parameter",
      component: () => import("@/views/goods/goods-manage/parameter.vue")
    },
    {
      path: "shop",
      title: "店铺列表",
      name: "shop",
      component: () => import("@/views/seller/shop/shopList.vue")
    },
    {
      path: "shop-operation",
      title: "店铺操作",
      name: "shop-operation",
      component: () => import("@/views/seller/shop/shopOperation.vue")
    },
    {
      path: "shop-detail",
      title: "店铺详细",
      name: "shop-detail",
      component: () => import("@/views/seller/shop/shopDetail.vue")
    },
    {
      path: "bill-detail",
      title: "结算单详情",
      name: "bill-detail",
      component: () => import("@/views/seller/bill/bill-detail.vue")
    },
    {
      path: "apply-goods",
      title: "商品审核列表",
      name: "apply-goods",
      component: () => import("@/views/goods/goods-info/goodsApply.vue")
    },
    {
      path: "manager-goods",
      title: "管理端商品列表",
      name: "manager-goods",
      component: () => import("@/views/goods/goods-info/goods.vue")
    },
    {
      path: "goods-brand",
      title: "商品品牌",
      name: "goods-brand",
      component: () => import("@/views/goods/goods-manage/brand.vue")
    },
    {
      path: "goods-category",
      title: "商品分类",
      name: "goods-category",
      component: () => import("@/views/goods/goods-manage/category.vue")
    },
    {
      path: "goods-parameter",
      title: "商品参数",
      name: "goods-parameter",
      component: () => import("@/views/goods/goods-manage/parameter.vue")
    },
    {
      path: "goods-spec",
      title: "商品参数",
      name: "goods-spec",
      component: () => import("@/views/goods/goods-manage/spec.vue")
    },
    {
      path: "order-complaint-detail",
      title: "投诉详情",
      name: "order-complaint-detail",
      component: () =>
        import("@/views/order/after-order/orderComplaintDetail.vue")
    },
    {
      path: "order-list",
      title: "订单列表",
      name: "order-list",
      component: () => import("@/views/order/order/orderList.vue")
    },
    {
      path: "order-detail",
      title: "订单详情",
      name: "order-detail",
      component: () => import("@/views/order/order/orderDetail.vue")
    },
    {
      path: "after-order-detail",
      title: "售后单详情",
      name: "after-order-detail",
      component: () =>
        import("@/views/order/after-order/afterSaleOrderDetail.vue")
    },
    {
      path: "shop-operation",
      title: "店铺详情",
      name: "shop-operation",
      component: () => import("@/views/seller/shop/shopOperation.vue")
    },
    {
      path: "member-detail",
      title: "会员详情",
      name: "member-detail",
      component: () => import("@/views/member/list/memberDetail.vue")
    },

    {
      path: "goods/goods-info/goodsDetail",
      title: "商品详情",
      name: "goods-detail",
      component: () => import("@/views/goods/goods-info/goodsDetail.vue")
    },
    {
      path: "promotions/add-points-goods",
      title: "添加积分商品",
      name: "add-points-goods",
      component: () =>
        import("@/views/promotions/points-goods/points-goods-add.vue")
    },
    {
      path: "promotions/edit-points-goods",
      title: "修改积分商品",
      name: "edit-points-goods",
      component: () =>
        import("@/views/promotions/points-goods/points-goods-edit.vue")
    },
    {
      path: "promotions/manager-points-goods-category",
      title: "积分商品分类",
      name: "manager-points-goods-category",
      component: () =>
        import("@/views/promotions/points-goods-category/points-goods-category.vue")
    },
    {
      path: "promotions/add-kanJia-activity-goods",
      title: "添加砍价活动",
      name: "add-kanJia-activity-goods",
      component: () => import("@/views/promotions/kanjia/kanjia-activity-add-goods.vue")
    },
    {
      path: "promotions/edit-kanJia-activity-goods",
      title: "修改砍价活动",
      name: "edit-kanJia-activity-goods",
      component: () => import("@/views/promotions/kanjia/kanjia-activity-edit-goods.vue")
    },
    {
      path: "promotions/manager-coupon",
      title: "平台优惠券",
      name: "manager-coupon",
      component: () => import("@/views/promotions/coupon/coupon.vue")
    },
    {
      path: "promotions/add-platform-coupon",
      title: "添加平台优惠券",
      name: "add-platform-coupon",
      component: () => import("@/views/promotions/coupon/coupon-publish.vue")
    },
    {
      path: "promotions/edit-platform-coupon",
      title: "编辑平台优惠券",
      name: "edit-platform-coupon",
      component: () => import("@/views/promotions/coupon/coupon-publish.vue")
    },
    {
      path: "promotions/add-coupon-activity",
      title: "添加优惠券活动",
      name: "add-coupon-activity",
      component: () => import("@/views/promotions/coupon-activity/coupon-publish.vue")
    },
    {
      path: "promotions/edit-coupon-activity",
      title: "编辑平台优惠券活动",
      name: "edit-coupon-activity",
      component: () => import("@/views/promotions/coupon-activity/coupon-publish.vue")
    },
    {
      path: "promotions/coupon-activity-info",
      title: "券活动详情",
      name: "coupon-activity-info",
      component: () => import("@/views/promotions/coupon-activity/coupon-info.vue")
    },
    {
      path: "promotions/manager-pintuan",
      title: "平台拼团",
      name: "manager-pintuan",
      component: () => import("@/views/promotions/pintuan/pintuan.vue")
    },
    {
      path: "promotions/pintuan/pintuan-goods",
      title: "拼团商品",
      name: "pintuan-goods",
      component: () => import("@/views/promotions/pintuan/pintuan-goods.vue")
    },
    {
      path: "promotions/full-discount-detail",
      title: "满减满折详情",
      name: "full-discount-detail",
      component: () => import("@/views/promotions/full-discount/full-discount-detail.vue")
    },
    {
      path: "promotions/seckill/manager-seckill-add",
      title: "编辑秒杀活动",
      name: "manager-seckill-add",
      component: () => import("@/views/promotions/seckill/seckill-add.vue")
    },
    {
      path: "promotions/seckill/seckill-goods",
      title: "秒杀商品",
      name: "seckill-goods",
      component: () => import("@/views/promotions/seckill/seckill-goods.vue")
    },
    {
      path: "/floorList/renovation",
      title: "编辑模板",
      name: "renovation",
      component: () => import("@/views/lili-floor-renovation/renovation.vue")
    },
    {
      path: "/floorList/main",
      title: "编辑模板",
      name: "main",
      component: () => import("@/views/lili-floor-renovation/wap/main.vue")
    },
    {
      path: "add-sms-sign",
      title: "短信签名",
      name: "add-sms-sign",
      component: () => import("@/views/sys/message/smsSign.vue")
    },
    {
      path: "live-detail",
      title: "查看直播",
      name: "live-detail",
      component: () => import("@/views/promotions/live/live-detail.vue")
    }
  ]
};

export const page404 = {
  path: "/*",
  name: "error-404",
  meta: {
    title: "404-页面不存在"
  },
  component: () => import("@/views/error-page/404.vue")
};

export const page403 = {
  path: "/403",
  meta: {
    title: "403-权限不足"
  },
  name: "error-403",
  component: () => import("@/views/error-page/403.vue")
};

export const page500 = {
  path: "/500",
  meta: {
    title: "500-服务端错误"
  },
  name: "error-500",
  component: () => import("@/views/error-page/500.vue")
};

// 所有上面定义的路由都要写在下面的routers里
export const routers = [loginRouter, otherRouter, page500, page403];
