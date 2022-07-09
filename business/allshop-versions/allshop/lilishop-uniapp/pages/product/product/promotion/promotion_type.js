const promotion = [
  {
    title: "积分活动",
    value: "POINT",
  },
  {
    title: "单品立减",
    value: "MINUS",
  },
  {
    title: "团购",
    value: "GROUPBUY",
  },
  {
    title: "积分换购",
    value: "EXCHANGE",
  },
  {
    title: "第二件半价",
    value: "HALF_PRICE",
  },
  {
    title: "满减优惠",
    value: "FULL_DISCOUNT",
  },
  {
    title: "限时抢购",
    value: "SECKILL",
  },
  {
    title: "拼团",
    value: "PINTUAN",
  },
  {
    title: "优惠券",
    value: "COUPON",
  },
];
export default promotion
/**格式化 */
export function formatType(val){
    if(val != undefined){
        promotion.forEach(item=>{
            if(val == item.value){
                return item.title
            }
        })
    }
}