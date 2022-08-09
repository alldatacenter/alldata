<template>

  <div class="index">
    <u-modal v-model="show" :show-title="false" :show-confirm-button="false" mask-close-able>
      <view class="slot-content">
        <image @click="downLoad()" class="img" :src="imgUrl" />
        <div class="canvas-hide">
          <!-- #ifdef MP-WEIXIN -->
          <canvas id="canvas" type="2d" style="width: 600px; height: 960px" />
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <canvas canvas-id="canvas" id="canvas" style="width: 600px; height: 960px" />
          <!-- #endif -->
        </div>
      </view>
    </u-modal>

  </div>
</template>
<script>
// 引入绘制插件
import DrawPoster from "@/js_sdk/u-draw-poster";

export default {
  data: () => ({
    imgUrl: "", //绘制出来的图片路径
    show: false, //是否展示模态框
    dp: {}, //绘制的dp对象，用于存储绘制等一些方法。
    logo: require("@/pages/passport/static/logo-title.png"), //本地logo地址
  }),

  props: {
    /**
     * 父级传参的数据
     */
    res: {
      type: null,
      default: "",
    },
  },
  onUnload() {},

  methods: {
    /**
     * 解决微信小程序中图片模糊问题
     */
    // #ifdef MP-WEIXIN
    st2: (size) => size * 2,
    // #endif

    // #ifndef MP-WEIXIN
    st2: (size) => size,
    // #endif

    /**
     * 保存图片
     */
    downLoad() {
      uni.saveImageToPhotosAlbum({
        filePath: this.imgUrl,
        success: function () {
          uni.showToast({
            title: "保存成功！",
            icon: "none",
          });
        },
        fail: function () {
          uni.showToast({
            title: "保存失败，请稍后重试！",
            icon: "none",
          });
        },
      });
    },

    /**
     * 创建canvas
     */
    async init() {
      this.show = true;
      this.dp = await DrawPoster.build({
        selector: "canvas",
        componentThis: this,
        loading: true,
        debugging: true,
      });
      let dp = this.dp;
      // #ifdef MP-WEIXIN
      // 用于微信小程序中画布错乱问题
      dp.canvas.width = this.st2(600);
      dp.canvas.height = this.st2(960);
      // #endif
      this.draw(dp);
    },

    async draw(dp) {
      const { width, height, background, title } = this.res.container;
      const { code, img, price } = this.res.bottom;

      // /** 绘制背景 */
      await dp.draw((ctx) => {
        ctx.fillStyle = background;
        ctx.fillRoundRect(
          this.st2(0),
          this.st2(0),
          this.st2(width),
          this.st2(height),
          this.st2(12)
        );
        ctx.clip();
      });
      /** 绘制图片 */
      dp.draw(async (ctx) => {
        await Promise.all([
          // 绘制Logo
          ctx.drawImage(
            this.logo,
            this.st2(175),
            this.st2(0),
            this.st2(256),
            this.st2(144)
          ),
          // 中间图片
          ctx.drawImage(
            img,
            this.st2(100),
            this.st2(150),
            this.st2(400),
            this.st2(400)
          ),
          // 二维码
          ctx.drawImage(
            code,
            this.st2(39),
            this.st2(750),
            this.st2(150),
            this.st2(150)
          ),
        ]);
      });

      /** 绘制中间文字*/
      await dp.draw((ctx) => {
        ctx.fillStyle = "#333";
        ctx.font = `bold ${this.st2(24)}px PingFang SC`;
        ctx.textAlign = "center";
        ctx.fillWarpText({
          text: title,
          maxWidth: this.st2(500),
          x: this.st2(300),
          y: this.st2(600),
          layer: 1,
        });

        ctx.fillStyle = "#ff3c2a";
        ctx.font = `${this.st2(38)}px PingFang SC`;
        ctx.textAlign = "center";
        ctx.fillText(price, this.st2(300), this.st2(680));
      });

      // /** 绘制底部文字 */
      await dp.draw((ctx) => {
        ctx.fillStyle = "#666";
        ctx.font = `${this.st2(24)}px PingFang SC`;
        ctx.fillText("长按图片，识别二维码", this.st2(200), this.st2(866));
        ctx.fillStyle = "#666";
        ctx.font = `${this.st2(24)}px PingFang SC`;
        ctx.fillText("查看商品详情", this.st2(200), this.st2(900));
      });

      this.imgUrl = await dp.createImagePath();

      // console.log(posterImgUrl)
    },
  },

  async mounted() {
    this.init();
  },
};
</script>

<style lang="scss" scoped>
page,
.index {
  height: 100%;
}
.canvas-hide {
  /* 1 */
  position: fixed;
  right: 100vw;
  bottom: 100vh;
  /* 2 */
  z-index: -9999;
  /* 3 */
  opacity: 0;
}
.index {
  position: relative;
  text-align: center;
  background: rgba($color: grey, $alpha: 0.2);
}

image {
  display: block;
}
.img {
  width: 600rpx;
  height: 960rpx;
}
</style>
