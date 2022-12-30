<template>
  <div class="model-item" v-if="element && element.key">
    <!-- 轮播图模块，包括个人信息，快捷导航模块 -->
    <template v-if="element.type == 'carousel'">
      <model-carousel
        :data="element"
        class="mb_20 width_1200_auto"
      ></model-carousel>
    </template>
    <template v-if="element.type == 'carousel1'">
      <model-carousel1 :data="element" class="mb_20"></model-carousel1>
    </template>
    <template v-if="element.type == 'carousel2'">
      <model-carousel2
        :data="element"
        class="mb_20 width_1200_auto"
      ></model-carousel2>
    </template>
    <!-- 热门广告 -->
    <template v-if="element.type == 'hotAdvert'">
      <div class="mb_20 width_1200_auto">
        <img
          style="display: block"
          class="hover-pointer"
          :src="element.options.list[0].img"
          @click="linkTo(element.options.list[0].url)"
          width="1200"
          alt=""
        />
      </div>
      <ul class="advert-list width_1200_auto">
        <template v-for="(item, index) in element.options.list">
          <li
            v-if="index !== 0"
            @click="linkTo(item.url)"
            class="hover-pointer"
            :key="index"
          >
            <img :src="item.img" width="230" height="190" alt="" />
          </li>
        </template>
      </ul>
    </template>
    <!-- 限时秒杀 待完善 -->
    <template v-if="element.type == 'seckill' && element.options.list.length">
      <seckill :data="element" class="mb_20 width_1200_auto"></seckill>
    </template>
    <!-- 折扣广告 -->
    <template v-if="element.type == 'discountAdvert'">
      <div
        class="discountAdvert"
        :style="{
          backgroundImage:
            'url(' + require('@/assets/images/decorate.png') + ')',
        }"
      >
        <img
          @click="linkTo(item.url)"
          class="hover-pointer"
          v-for="(item, index) in element.options.classification"
          :key="index"
          :src="item.img"
          width="190"
          height="210"
          alt=""
        />
        <img
          @click="linkTo(item.url)"
          class="hover-pointer"
          v-for="(item, index) in element.options.brandList"
          :key="'discount' + index"
          :src="item.img"
          width="240"
          height="105"
          alt=""
        />
      </div>
    </template>

    <!-- 好货推荐 -->
    <template v-if="element.type == 'recommend'">
      <recommend :data="element" class="mb_20 width_1200_auto"></recommend>
    </template>
    <!-- 新品排行 -->
    <template v-if="element.type == 'newGoodsSort'">
      <new-goods-sort
        :data="element"
        class="mb_20 width_1200_auto"
      ></new-goods-sort>
    </template>
    <!-- 首页广告 -->
    <template v-if="element.type == 'firstAdvert'">
      <first-page-advert
        :data="element"
        class="mb_20 width_1200_auto"
      ></first-page-advert>
    </template>
    <!-- 横幅广告 -->
    <template v-if="element.type == 'bannerAdvert'">
      <div style="width: 100%; text-align: center">
        <img
          width="1200"
          class="hover-pointer mb_20"
          @click="linkTo(element.options.url)"
          :src="element.options.img"
          alt=""
        />
      </div>
    </template>
    <template v-if="element.type == 'notEnough'"
        >
      <not-enough
        :data="element"
        class="mb_20 width_1200_auto"
      ></not-enough>
    </template>
  </div>
</template>

<script>
import ModelCarousel from "./modelList/Carousel.vue";
import ModelCarousel1 from "./modelList/Carousel1.vue";
import ModelCarousel2 from "./modelList/Carousel2.vue";
import FirstPageAdvert from "./modelList/FirstPageAdvert.vue";
import NewGoodsSort from "./modelList/NewGoodsSort.vue";
import Recommend from "./modelList/Recommend.vue";
import NotEnough from "./modelList/NotEnough.vue";
import Seckill from "./modelList/Seckill.vue";

export default {
  name: "modelFormItem",
  props: ["element", "select", "index", "data"],
  components: {
    ModelCarousel,
    ModelCarousel1,
    ModelCarousel2,
    Recommend,
    NewGoodsSort,
    FirstPageAdvert,
    NotEnough,
    Seckill,
  },
  data() {
    return {
      showModal: false, // 控制模态框显隐
      selected: {}, // 已选数据
    };
  },
};
</script>
<style lang="scss" scoped>
.model-item {
  position: relative;
  margin-bottom: 10px;
}

/** 热门广告 */
.advert-list {
  background: $theme_color;
  height: 200px;
  display: flex;
  justify-content: space-around;
  padding: 3px 10px;
  > li {
    img {
      cursor: pointer;
      border-radius: 10px;
      transition: all 150ms ease-in-out;
      &:hover {
        transform: translateY(-3px);
        box-shadow: rgba(0, 0, 0, 0.4) 0px 5px 20px 0px;
      }
    }
  }
}

/** 折扣广告 */
.discountAdvert {
  width: 1300px;
  height: 566px;
  margin: 0 auto;
  margin-bottom: 20px;
  background-repeat: no-repeat;
  position: relative;
  left: -47px;
  padding-left: 295px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  img {
    margin-top: 10px;
    margin-right: 10px;
    transition: all 150ms ease-in-out;
    &:hover {
      box-shadow: 0 5px 12px 0 rgba(0, 0, 0, 0.4);
      transform: translateY(-2px);
    }
  }
}

.width_1200_auto {
  width: 1200px;
  margin: 0 auto;
  background-color: #fff;
}
</style>
