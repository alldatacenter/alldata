<template>
  <div class="shop">
    <div class="wap-content">
      <div class="query-wrapper">
        <div class="query-item">
          <div>店铺名称</div>
          <Input placeholder="请输入店铺名称" @on-clear="shopsData=[]; params.storeName=''; params.pageNumber =1; init()" @on-enter="()=>{shopsData=[]; params.pageNumber =1; init();}" icon="ios-search" clearable style="width: 150px"
            v-model="params.storeName" />
        </div>

        <div class="query-item">
          <Button type="primary" @click="shopsData=[];params.pageNumber =1; init();" icon="ios-search">搜索</Button>
        </div>
      </div>
      <div>
        <Scroll class="wap-content-list" :on-reach-bottom="handleReachBottom" :distance-to-edge="23">
          <div class="wap-content-item" @click="clickShop(item,index)" :class="{ active:selected == index }" v-for="(item, index) in shopsData" :key="index">
            <div>
              <img class="shop-logo" :src="item.storeLogo" alt="" />
            </div>
            <div class="wap-content-desc">
              <div class="wap-content-desc-title">{{ item.storeName }}</div>

              <div class="self-operated" :class="{'theme_color':item.selfOperated }">{{ item.selfOperated ? '自营' : '非自营' }}</div>
              <div class="wap-sku" :class="{'theme_color':(item.storeDisable === 'OPEN' ? true : false) }">{{ item.storeDisable === 'OPEN' ? '开启中' : '未开启' }}</div>
            </div>
          </div>
          <Spin size="large" fix v-if="loading"></Spin>
        </Scroll>
      </div>
    </div>
  </div>
</template>
<script>
import { getShopListData } from "@/api/shops.js";
export default {
  data() {
    return {
      loading: false, // 加载状态
      total: "", // 总数
      params: { // 请求参数
        pageNumber: 1,
        pageSize: 10,
        storeDisable: "OPEN",
        storeName: "",
      },
      shopsData: [], // 店铺数据
      selected: 9999999999, //设置一个不可能选中的index
    };
  },
  watch: {},

  created() {
    this.init();
  },
  methods: {
    handleReachBottom() {
      setTimeout(() => {
        if (this.params.pageNumber * this.params.pageSize <= this.total) {
          this.params.pageNumber++;
          this.init();
        }
      }, 1500);
    },
    init() {
      this.loading = true;
      getShopListData(this.params).then((res) => {
        if (res.success) {
          /**
           * 解决数据请求中，滚动栏会一直上下跳动
           */
          this.total = res.result.total;

          this.shopsData.push(...res.result.records);

          this.loading = false;
        }
      });
    },
    clickShop(val, i) {
      this.selected = i;
      val = { ...val, ___type: "shops" };
      this.$emit("selected", [val]);
    },
  },
};
</script>
<style lang="scss" scoped>
@import "../style.scss";
.shop {
  display: flex;
}
.self-operated {
  font-size: 12px;
  color: #999;
}
.wap-content-list {
  flex-wrap: wrap;
}
.shop-logo {
  object-fit: cover;
}
.wap-content-item {
}
.active {
  background: url("../../../assets/selected.png") no-repeat;
  background-position: right;
  background-size: 10%;
}
</style>
