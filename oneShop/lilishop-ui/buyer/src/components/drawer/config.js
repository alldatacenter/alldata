const config = {
  closable: true, // 是否显示右上角关闭按钮
  mask: true, // 是否显示遮罩层

  menuList: [{
    icon: 'md-person', // menu的icon
    title: '会员中心', // menu的标题
    titleShow: false,
    path: '', // menu点击的路径
    display: false // 是否显示此menu
  },
  {
    icon: 'ios-cart', // menu的icon
    title: '购物车', // menu的标题
    path: '', // menu点击的路径
    display: true, // 是否显示此menu
    badge: 12,
    titleShow: true
  },
  {
    icon: 'md-clipboard', // menu的icon
    title: '我的订单', // menu的标题
    path: '', // menu点击的路径
    display: true, // 是否显示此menu
    badge: '',
    titleShow: false
  },
  {
    icon: 'md-pricetag', // menu的icon
    title: '优惠券', // menu的标题
    path: '', // menu点击的路径
    display: true, // 是否显示此menu
    badge: '',
    titleShow: false
  },
  {
    icon: 'logo-usd', // menu的icon
    title: '我的资产', // menu的标题
    path: '', // menu点击的路径
    display: false, // 是否显示此menu
    badge: '',
    titleShow: false
  },
  {
    icon: 'ios-eye', // menu的icon
    title: '我的足迹', // menu的标题
    path: '', // menu点击的路径
    display: true, // 是否显示此menu
    badge: '',
    titleShow: false
  },
  {
    icon: 'md-star', // menu的icon
    title: '我的收藏', // menu的标题
    path: '', // menu点击的路径
    display: true, // 是否显示此menu
    badge: '',
    titleShow: false
  }
  ]
}

export default config
