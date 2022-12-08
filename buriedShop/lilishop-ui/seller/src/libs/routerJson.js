/**
 * 商家端左侧菜单栏静态数据
 */

export const result = [{
    name: "lili",
    level: 0,
    type: -1,
    title: "商品",
    path: "",
    component: "",
    children: [{
      name: "goods-list",
      level: 1,
      type: 0,
      title: "商品管理",
      path: "",
      component: "Main",
      children: [{
          name: "goods-operation",
          level: 2,
          type: 0,
          title: "商品发布",
          path: "goods-operation",
          component: "goods-operation",
          children: null,
        },
        {
          name: "goods",
          level: 2,
          type: 0,
          title: "商品列表",
          path: "goods",
          component: "goods/goods-seller/goods",
          children: null,
        },
        {
          path: "template-goods",
          props: {
            type: "TEMPLATE"
          },
          level: 2,
          type: 0,
          title: "商品模版",
          component: "goods/goods-seller/draftGoods",
          name: "template-goods",
          children: null,
        },
        {
          name: "category",
          level: 2,
          type: 0,
          title: "店铺分类",
          path: "category",
          component: "goods/goods-manage/category",
          children: null,
        }
      ]
    }]
  },
  {
    name: "order",
    level: 0,
    type: -1,
    title: "订单",
    path: "",
    component: "",
    description: "",
    children: [{
        name: "order",
        level: 1,
        type: 0,
        title: "订单管理",
        path: "/order",
        component: "Main",
        children: [{
            name: "orderList",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "商品订单",
            path: "orderList",
            component: "order/order/orderList",
            children: null,
          },
          {
            name: "virtualOrderList",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "虚拟订单",
            path: "virtualOrderList",
            component: "order/order/virtualOrderList",
            children: null
          }
        ]
      },
      {
        name: "memberComment",
        level: 1,
        type: 0,
        title: "评价管理",
        path: "/order",
        component: "Main",
        children: [{
          name: "memberComment",
          level: 2,
          type: 0,
          title: "评价管理",
          path: "memberComment",
          component: "member/memberComment",
          children: null,
        }]
      },
      {
        name: "order",
        level: 1,
        type: 0,
        title: "售后管理",
        path: "/order",
        component: "Main",
        children: [{
            name: "returnGoodsOrder",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "退货管理",
            path: "returnGoodsOrder",
            component: "order/after-order/returnGoodsOrder",
            children: null
          },
          {
            name: "returnMoneyOrder",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "退款管理",
            path: "returnMoneyOrder",
            component: "order/after-order/returnMoneyOrder",
            children: null
          },
          {
            name: "orderComplaint",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "投诉管理",
            path: "orderComplaint",
            component: "order/after-order/orderComplaint",
            children: null
          }
        ]
      }
    ]
  },
  {
    name: "bill",
    level: 0,
    type: -1,
    title: "财务",
    path: "",
    component: "",
    children: [{
        name: "bill",
        level: 1,
        type: 0,
        title: "财务管理",
        path: "/bill",
        component: "Main",
        children: [{
            name: "accountStatementBill",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "财务对账",
            path: "accountStatementBill",
            component: "shop/bill/accountStatementBill",
            children: null
          },
          {
            name: "storeBill",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "店铺结算",
            path: "storeBill",
            component: "shop/bill/storeBill",
            children: null
          }
        ]
      },
      {
        name: "receiptManager",
        level: 1,
        type: 0,
        title: "发票管理",
        path: "/bill",
        component: "Main",
        children: [{
          name: "receipt",
          level: 2,
          type: 0,
          title: "发票管理",
          path: "receipt",
          component: "order/receiptManager/receipt",
          children: null
        }]
      }
    ]
  },
  {
    name: "promotion",
    level: 0,
    type: -1,
    title: "营销",
    path: "",
    component: "",
    children: [{
        name: "promotion",
        level: 1,
        type: 0,
        title: "平台活动",
        path: "/promotion",
        component: "Main",
        children: [{
            name: "pintuan",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "拼团活动",
            path: "pintuan",
            component: "promotion/pintuan/pintuan",
            children: null
          },
          {
            name: "seckill",
            level: 2,
            type: 0,
            keepAlive: true,
            title: "秒杀活动",
            path: "seckill",
            component: "promotion/seckill/seckill",
            children: null
          }
        ]
      },
      {
        name: "lives",
        level: 1,
        type: 0,
        title: "直播活动",
        path: "/promotion",
        component: "Main",
        children: [{
            name: "live",
            level: 2,
            type: 0,
            title: "直播管理",
            path: "live",
            component: "promotion/live/live",
            children: null
          },
          {
            name: "liveGoods",
            level: 2,
            type: 0,
            title: "直播商品",
            path: "liveGoods",
            component: "promotion/live/liveGoods",
            children: null
          }
        ]
      },
      {
        name: "storePromotion",
        level: 1,
        type: 0,
        title: "商家活动",
        path: "/storePromotion",
        component: "Main",
        children: [{
            name: "full-cut",
            level: 2,
            type: 0,
            title: "满额活动",
            path: "full-discount",
            component: "promotion/full-discount/full-discount",
            children: null
          },
          {
            name: "coupon",
            level: 2,
            type: 0,
            title: "优惠券",
            path: "coupon",
            component: "promotion/coupon/coupon",
            children: null
          },
        ]
      },
      {
        name: "distribution",
        level: 1,
        type: 0,
        title: "分销管理",
        path: "/distribution",
        component: "Main",
        children: [{
            name: "distributionGoods",
            level: 2,
            type: 0,
            title: "分销商品",
            path: "distributionGoods",
            component: "distribution/distributionGoods",
            children: null
          },
          {
            name: "distributionOrder",
            level: 2,
            type: 0,
            title: "分销订单",
            path: "distributionOrder",
            component: "distribution/distributionOrder",
            children: null
          }
        ]
      }
    ]
  },
  {
    name: "statistics",
    level: 0,
    type: -1,
    title: "统计",
    path: "",
    component: "",
    children: [{
      name: "statistics",
      level: 1,
      type: 0,
      title: "统计管理",
      path: "/statistics",
      component: "Main",
      children: [{
          name: "goodsStatistics",
          level: 2,
          type: 0,
          title: "商品统计",
          path: "goodsStatistics",
          component: "statistics/goods",
          children: null
        },
        {
          name: "orderStatistics",
          level: 2,
          type: 0,
          title: "订单统计",
          path: "orderStatistics",
          component: "statistics/order",
          children: null
        },
        {
          name: "trafficStatistics",
          level: 2,
          type: 0,
          title: "流量统计",
          path: "trafficStatistics",
          component: "statistics/traffic",
          children: null
        },
      ]
    }]
  },
  {
    name: "settings",
    level: 0,
    type: -1,
    title: "设置",
    path: "",
    component: "",
    description: "",
    children: [{
        name: "user-manage",
        level: 1,
        type: 0,
        title: "页面设置",
        path: "user-manage",
        component: "sys/user-manage/userManage",
        children: []
      },
      {
        name: "ship",
        level: 1,
        type: 0,
        title: "配送设置",
        path: "",
        component: "Main",
        children: [{
            name: "shipTemplate",
            level: 2,
            type: 0,
            title: "配送模板",
            path: "shipTemplate",
            component: "shop/ship/shipTemplate",
            children: null
          },
          {
            name: "logistics",
            level: 2,
            type: 0,
            title: "物流公司",
            path: "logistics",
            component: "shop/ship/logistics",
            children: null
          }
        ]
      },
      {
        name: "shop",
        level: 1,
        type: 0,
        title: "店铺管理",
        path: "/shop",
        component: "Main",
        description: "",
        children: [{
            name: "shopSetting",
            level: 2,
            type: 0,
            title: "店铺设置",
            path: "shopSetting",
            component: "shop/shopSetting",
            children: null,
          },
          // {
          //   name: "shopAddress",
          //   level: 2,
          //   type: 0,
          //   title: "自提管理",
          //   path: "shopAddress",
          //   component: "shop/shopAddress",
          //   children: null,
          // }
        ]
      }
    ]
  },
  {
    name: "messages",
    level: 0,
    type: -1,
    title: "消息",
    path: "",
    component: "",
    children: [{
      name: "user-manage",
      level: 1,
      type: 0,
      title: "系统消息",
      path: "user-manage",
      component: "sys/user-manage/userManage",
      children: [{
        name: "message_index",
        level: 2,
        type: 0,
        title: "系统消息",
        path: "message",
        component: "message",
        children: null
      }]
    }]
  }
];
