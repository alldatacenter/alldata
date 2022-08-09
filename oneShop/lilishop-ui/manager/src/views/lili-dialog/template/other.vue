<template>
  <div>
    <Row :gutter="30">
      <Col
        span="4"
        v-for="(item, index) in linkList"
        :key="index"
        v-if="
          (item.title !== '拼团频道' && item.title !== '签到') ||
          $route.name !== 'renovation'
        "
      >
        <div
          class="card"
          :class="{ active: selectedIndex == index }"
          @click="handleLink(item, index)"
        >
          <Icon size="24" :type="item.icon" />
          <p>{{ item.title }}</p>
        </div>
      </Col>
      <!-- 外部链接，只有pc端跳转 -->
      <Col span="4">
        <div
          class="card"
          :class="{ active: selectedIndex == linkList.length }"
          @click="handleLink(linkItem, linkList.length)"
        >
          <Icon size="24" :type="linkItem.icon" />
          <p>{{ linkItem.title }}</p>
        </div>
      </Col>
    </Row>
  </div>
</template>
<script>
export default {
  data() {
    return {
      linkList: [
        // 链接列表
        {
          title: "首页",
          icon: "md-home",
          ___type: "home",
        },
        {
          title: "购物车",
          icon: "md-cart",
          ___type: "cart",
        },
        {
          title: "收藏商品",
          icon: "md-heart",
          ___type: "collection",
        },
        {
          title: "我的订单",
          icon: "md-document",
          ___type: "order",
        },
        {
          title: "个人中心",
          icon: "md-person",
          ___type: "user",
        },
        {
          title: "拼团频道",
          icon: "md-flame",
          ___type: "group",
        },
        {
          title: "秒杀频道",
          icon: "md-flame",
          ___type: "seckill",
        },
        {
          title: "领券中心",
          icon: "md-pricetag",
          ___type: "coupon",
        },
        {
          title: "签到",
          icon: "md-happy",
          ___type: "sign",
        },
        {
          title: "小程序直播",
          icon: "ios-videocam",
          ___type: "live",
        },
        {
          title: "砍价",
          icon: "md-share-alt",
          ___type: "kanjia",
        },
        {
          title: "积分商城",
          icon: "ios-basket",
          ___type: "point",
        },
      ],
      linkItem: {
        title: "外部链接",
        icon: "ios-link",
        ___type: "link",
        url: "",
      },
      linkVisible: false, // 是否显示外部链接
      selectedIndex: 9999999, // 已选index
    };
  },
  methods: {
    handleLink(val, index) {
      val = { ...val, ___type: "other" };
      this.selectedIndex = index;
      this.$emit("selected", [val]);
    },
  },
};
</script>
<style lang="scss" scoped>
@import "../style.scss";
.card {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  padding: 20px 0;
  margin: 10px 0;
  text-align: center;
  transition: 0.35s;
  cursor: pointer;
  /deep/ p {
    margin: 10px 0;
  }
  border: 1px solid #ededed;
}
.card:hover {
  background: #ededed;
}
.active {
  background: #ededed;
}
</style>
