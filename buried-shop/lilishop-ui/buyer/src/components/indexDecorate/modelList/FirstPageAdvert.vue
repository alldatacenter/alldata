<template>
  <div class="first-page-advert">
    <div
      class="item hover-pointer"
      @click="linkTo(item.url)"
      :style="{
        backgroundImage: `linear-gradient(to right, ${item.fromColor}, ${item.toColor})`,
      }"
      v-for="(item, index) in options.list"
      :key="index"
    >
      <div>
        <span class="line top-line"></span>
        <p>{{ item.name }}</p>
        <span class="line btm-line"></span>
        <p>{{ item.describe }}</p>
      </div>
      <img :src="item.img" width="170" height="170" alt="" />
    </div>
  </div>
</template>
<script>
export default {
  props: {
    data: {
      type: Object,
      default: null
    }
  },
  data () {
    return {
      options: this.data.options // 装修数据
    };
  }
};
</script>
<style lang="scss" scoped>
.first-page-advert {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  // margin-top: -10px;
  .item {
    width: 393px;
    height: 170px;
    margin-top: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    img {
      margin-left: 20px;
      transition: transform 0.5s, -webkit-transform 0.5s, -moz-transform 0.5s;
      &:hover {
        transform: translateX(-5px);
      }
    }

    &:nth-of-type(1),
    &:nth-of-type(2),
    &:nth-of-type(3) {
      margin-top: 0;
    }

    p:nth-of-type(1) {
      margin: 3px 0;
      font-size: 18px;
      color: #fff;
    }
    p:nth-of-type(2) {
      margin-top: 3px;
      color: #fff;
    }
  }
  .line {
    position: relative;
    display: block;
    height: 2px;
    background: url(../../../assets/images/festival_icon.png);
    z-index: 1;
  }
  .top-line {
    width: 78px;
    background-position: -1px -3px;
  }
  .btm-line {
    background-position: 0 -11px;
    width: 154px;
  }
}
.modal-top-advert {
  align-items: start;
  padding: 0 30px;
  .exhibition {
    width: 300px;
    height: 50px;
  }
}
</style>
