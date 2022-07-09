<template>
  <div class="layout">
    <div class="join-list">
      <div class="join-title">
        <div>{{ res.list[0].title }}</div>

        <div>更多</div>
      </div>
      <div class="join-box">
        <div class="join-item" v-for="item in 4" :key="item">
          <div class="item-img-box">
            <img
              class="item-img"
              src="https://picsum.photos/id/268/200/200"
              alt
            />
           
          </div>
          <div class="item-price">
            <span>20积分</span>
          </div>
          <div class="item-line-through">
            <span>30积分</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: ["res"]
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.join-box {
  display: flex;
}
.item-price {
  > span {
    font-size: 15px;
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
    font-size: 16px;
    font-weight: bold;
  }
  > div:nth-of-type(2) {
    font-size: 12px;
    color: #999;
  }
}
</style>