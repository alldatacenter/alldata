
// 订单状态
export const orderStatusList = [
  {
    name: '全部',
    status: 'ALL'
  },
  {
    name: '未付款',
    status: 'UNPAID'
  },
  {
    name: '已付款',
    status: 'PAID'
  },
  {
    name: '待发货',
    status: 'UNDELIVERED'
  },
  {
    name: '已发货',
    status: 'DELIVERED'
  },
  {
    name: '已完成',
    status: 'COMPLETED'
  },
  {
    name: '待核验',
    status: 'TAKE'
  },
  {
    name: '已取消',
    status: 'CANCELLED'
  }
]
// 订单售后状态
export const afterSaleStatusList = [
  {
    name: '申请中',
    status: 'APPLY'
  },
  {
    name: '通过',
    status: 'PASS'
  },
  {
    name: '拒绝',
    status: 'REFUSE'
  },
  {
    name: '买家退货，待卖家收货',
    status: 'BUYER_RETURN'
  },
  {
    name: '卖家确认收货',
    status: 'SELLER_CONFIRM'
  },
  {
    name: '卖家终止售后',
    status: 'SELLER_TERMINATION'
  },
  {
    name: '买家确认收货',
    status: 'BUYER_CONFIRM'
  },
  {
    name: '买家取消售后',
    status: 'BUYER_CANCEL'
  },
  {
    name: '完成售后',
    status: 'COMPLETE'
  },
  {
    name: '等待平台退款',
    status: 'WAIT_REFUND'
  }
]
