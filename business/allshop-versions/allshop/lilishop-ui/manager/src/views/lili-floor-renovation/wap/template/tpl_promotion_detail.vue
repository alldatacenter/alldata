<template>
  <div class="layout">
    <div class="join-list">
      <div
        v-for="(item, index) in res.list"
        :key="index"
        class="join-list-item"
      >
        <div>
          <div class="join-title">
            <div>{{ item.title }}</div>
            <div
              class="sub"
              :style="{
                backgroundColor: item.bk_color,
                color: item.color1,
                borderColor: item.bk_color,
              }"
            >
              {{ item.title1 }}
            </div>
          </div>
          <div class="join-box">
            <div
              class="join-item"
              v-for="(i, _index) in item.data"
              :key="_index"
            >
              <div class="item-img-box">
                <img
                  class="item-img"
                  :src="i.thumbnail ? i.thumbnail : i.goodsImage"
                  alt
                />
              </div>
              <div
                class="ellipsis"
                :class="{ 'max-width': res.list.length <= 1 }"
              >
                {{ i.goodsName ? i.goodsName : i.name }}
              </div>
              <div class="item-price">
                <span>￥{{ i.price ? i.price : i.originalPrice }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: ["res"],
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.join-box {
  display: flex;
}
.join-list {
  width: 100%;
  display: flex;
  overflow: hidden;
}
.join-list-item {
  flex: 1;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 54px; // 大于1个活动
  font-size: 11px;
}
.max-width {
  width: 158px !important;
}
.item-price {
  > span {
    font-size: 14px;
    font-weight: 500;
    color: #e1212b;
  }
}
.join-item {
  flex: 1;
}
.item-img {
  width: 75px;
  height: 75px;
  margin: 0 auto;
  display: block;
}
.item-img-box {
  position: relative;
}
.item-line-through {
  > span {
    font-size: 10px;
    font-weight: 400;
    text-decoration: line-through;
    color: #999;
  }
}
.item-position-tips {
  position: absolute;
  right: 0;
  color: #fff;
  font-size: 12px;
  bottom: 0;
}
.join-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  height: 50px;
  > div:nth-of-type(1) {
    font-size: 15px;
    font-weight: bold;
  }
  > div:nth-of-type(2) {
    font-size: 10px;
    color: #fff;
    border: 2px solid;
    border-radius: 8px;
    text-align: center;
    width: 30%;
  }
  .sub {
    background-color: #e1212b;
    margin-right: 40px;
  }
}
</style>