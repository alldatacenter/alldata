<template>
  <div class="layout">
    <Carousel class="carousel" v-if="res"  autoplay :autoplay-speed="5000">
      <CarouselItem v-for="(item, index) in res.list" :key="index">
        <div>
          <img class="image-mode" :src="item.img" />
        </div>
      </CarouselItem>
    </Carousel>
  </div>
</template>
<script>
export default {
  title: "导航栏",
  props: ["res"],
  watch: {
    res: {
      handler(newValue, oldValue) {
          this.$set(this,'res',newValue);
      },
      deep: true
    }
  }
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.carousel,
.image-mode {
  height: 150px;
}
</style>