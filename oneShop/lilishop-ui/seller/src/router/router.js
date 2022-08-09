import Main from "@/views/Main.vue";

const config = require('@/config/index')
// 不作为Main组件的子页面展示的页面单独写，如下
export const loginRouter = {
  path: "/login",
  name: "login",
  meta: {
    title: `登录 - ${config.title}商家后台`
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
      title: "首页",
      name: "home_index",
      component: () => import("@/views/home/home.vue")
    },
    {
      path: "change-password",
      title: "修改密码",
      name: "change_pass",
      component: () => import("@/views/change-password/change-password.vue")
    },
    {
      path: "message",
      title: "消息中心",
      name: "message_index",
      component: () => import("@/views/message/message.vue")
    },
    {
      path: "goods-operation",
      title: "添加商品",
      name: "goods-operation",
      component: () => import("@/views/goods/goods-seller/goodsOperation.vue")
    },
    {
      path: "goods-operation-edit",
      title: "编辑商品",
      name: "goods-operation-edit",
      component: () => import("@/views/goods/goods-seller/goodsOperation.vue")
    },
    {
      path: "goods-template-operation-edit",
      title: "编辑模版",
      name: "goods-template-operation-edit",
      component: () => import("@/views/goods/goods-seller/goodsOperation.vue")
    },
    {
      path: "goods-draft-operation-edit",
      title: "编辑草稿",
      name: "goods-draft-operation-edit",
      component: () => import("@/views/goods/goods-seller/goodsOperation.vue")
    },
    {
      path: "add-coupon",
      title: "店铺优惠券",
      name: "add-coupon",
      component: () => import("@/views/promotion/coupon/coupon-publish.vue")
    },
    {
      path: "add-live",
      title: "创建直播",
      name: "add-live",
      component: () => import("@/views/promotion/live/addLive.vue")
    },
    {
      path: "bill-detail",
      title: "结算单详情",
      name: "bill-detail",
      component: () => import("@/views/shop/bill/billDetail.vue")
    },
    {
      path: "seckill-goods",
      title: "限时抢购商品",
      name: "seckill-goods",
      component: () => import("@/views/promotion/seckill/seckill-goods.vue")
    },
    {
      path: "pintuan-goods",
      title: "拼团商品",
      name: "pintuan-goods",
      component: () => import("@/views/promotion/pintuan/pintuan-goods.vue")
    },
    {
      path: "pintuan-edit",
      title: "新增拼团",
      name: "pintuan-edit",
      component: () => import("@/views/promotion/pintuan/pintuan-edit.vue")
    },
    {
      path: "order-detail",
      title: "订单详情",
      name: "order-detail",
      component: () => import("@/views/order/order/orderDetail.vue")
    },
    {
      path: "order-complaint-detail",
      title: "投诉详情",
      name: "order-complaint-detail",
      component: () =>
        import("@/views/order/after-order/orderComplaintDetail.vue")
    },
    {
      path: "return-goods-order-detail",
      title: "售后详情",
      name: "return-goods-order-detail",
      component: () =>
        import("@/views/order/after-order/reurnGoodsOrderDetail.vue")
    },
    {
      path: "full-discount-detail",
      title: "添加满额活动",
      name: "full-discount-detail",
      component: () => import("@/views/promotion/full-discount/full-discount-add.vue")
    },
    {
      path: "export-order-deliver",
      title: "发货",
      name: "export-order-deliver",
      component: () => import("@/views/order/order/exportOrderDeliver.vue")
    },
    {
      path: "order-detail",
      title: "订单详情",
      name: "order-detail",
      component: () => import("@/views/order/order/orderDetail.vue")
    }

    // {
    //   path: "/*",
    //   name: "error-404",
    //   meta: {
    //     title: "404-页面不存在"
    //   },
    //   component: () => import("@/views/error-page/404.vue")
    // }
  ]
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
