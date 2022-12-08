<template>
  <div class="model-carousel1" :style="{background: bgColor}">
    <div class="nav-body clearfix">
      <!-- 侧边导航 -->
      <div class="nav-side"></div>
      <div class="nav-content">
        <!-- 轮播图 -->
        <Carousel autoplay @on-change="autoChange">
          <CarouselItem v-for="(item, index) in data.options.list" :key="index" >
            <div style="overflow: hidden">
              <img :src="item.img" width="1200" height="470" />
            </div>
          </CarouselItem>
        </Carousel>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'modelCarousel1',
  props: ['data'],
  data () {
    return {
      showModal: false, // modal显隐
      selected: null, // 已选数据
      picModelFlag: false, // 选择图片modal
      bgColor: '#fff' // 轮播背景色
    };
  },
  mounted () {
    this.bgColor = this.data.options.list[0].bgColor
  },
  methods: {
    // 自动切换时改变背景色
    autoChange (oVal, val) {
      this.bgColor = this.data.options.list[val].bgColor
    }
  }
};
</script>

<style scoped lang="scss">
.model-carousel1 {
  width: 100%;
  height: 470px;
  background: #fff;
}

/*大的导航信息，包含导航，幻灯片等*/
.nav-body {
  width: 1200px;
  height: 470px;
  margin: 0px auto;
}
.nav-side {
  height: 470px;
  width: 200px;
  padding: 0px;
  color: #fff;
  line-height: 470px;
  text-align: center;
  position: absolute;
  z-index: 1;
}

/*导航内容*/
.nav-content {
  width: 1200px;
  height: 470px;
  overflow: hidden;
  float: left;
  position: relative;
}
</style>
