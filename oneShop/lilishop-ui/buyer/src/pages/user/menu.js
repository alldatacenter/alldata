//  * icon  图标
//  * title 一级菜单
//  * display 是否显示菜单
//  * menus 菜单栏
//  * path路径  router 配置的

// 订单中心
const order = [{
  icon: '',
  title: '订单中心',
  menus: [{
    icon: '',
    title: '我的订单',
    path: 'MyOrder'
  },
  {
    icon: '',
    title: '收货地址',
    path: 'MyAddress'
  },
  {
    icon: '',
    title: '售后订单',
    path: 'AfterSale'
  }
  ],
  display: true
}];

// 会员中心
const member = [{
  icon: '',
  title: '会员中心',
  menus: [{
    icon: '',
    title: '用户信息',
    path: 'Profile'
  },
  {
    icon: '',
    title: '账户安全',
    path: 'AccountSafe'
  },
  {
    icon: '',
    title: '我的消息',
    path: 'MsgList'
  },
  {
    icon: '',
    title: '我的足迹',
    path: 'MyTracks'
  },

  {
    icon: '',
    title: '我的收藏',
    path: 'Favorites'
  },
  {
    icon: '',
    title: '分销推荐',
    path: 'Distribution'
  },
  {
    icon: '',
    title: '我的评论',
    path: 'CommentList'
  },
  {
    icon: '',
    title: '我的投诉',
    path: 'ComplainList'
  },
  {
    icon: '',
    title: '我的积分',
    path: 'Point'
  }
  ],
  display: true
}];

// 账户中心
const user = [{
  icon: '',
  title: '账户中心',
  menus: [{
    icon: '',
    title: '我的优惠券',
    path: 'Coupons'
  },
  {
    icon: '',
    title: '资金管理',
    path: 'MoneyManagement'
  }],
  display: true
}]

// 活动中心
// const activity = [{
//   icon: '',
//   title: '活动中心',
//   menus: [
//     // {
//     //   icon: '',
//     //   title: '拍卖活动',
//     //   path: 'MyOrder',
//     // },
//     // {
//     //   icon: '',
//     //   title: '夺宝奇兵',
//     //   path: 'MyOrder',
//     // },
//   ],
//   display: true
// }]

// 批发中心
// const wholesale = [
//   {
//     icon: '',
//     title: '批发中心',
//     menus: [
//       {
//         icon: '',
//         title: '我的采购单',
//         path: 'MyOrder',
//       },
//       {
//         icon: '',
//         title: '采购退货单',
//         path: 'MyOrder',
//       },
//       {
//         icon: '',
//         title: '我的求购单',
//         path: 'MyOrder',
//       },
//
//     ],
//     display: true
//   }
// ]

// 店铺
// const shop = [
//   {
//     icon: '',
//     title: '店铺管理',
//     menus: [
//       {
//         icon: '',
//         title: '店铺后台',
//         path: 'MyOrder',
//       },
//       {
//         icon: '',
//         title: '商家等级',
//         path: 'MyOrder',
//       },
//     ],
//     display: true
//   }
// ]
// wholesale[0], shop[0]
let menuList = []
menuList.push(order[0], member[0], user[0])

export default menuList
