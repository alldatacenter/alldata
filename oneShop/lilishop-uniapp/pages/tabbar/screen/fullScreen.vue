<template>
   <u-modal v-model="show" cancelText="不同意" confirmText="同意" showCancelButton="btnShow" title="服务协议和隐私政策" @confirm="confirm" @cancel="cancel">
    <view class="u-update-content">
      请您务必审慎阅读,充分理解“服务协议”和“隐私政策”各条款，
      包括但不限于：为了更好的向你提供服务，我们需要收集你的设备标识,
      操作日志等信息用于分析，优化应用性能。 您可阅读你可阅读
      <a @click="gotoLink">《服务协议》</a>
      和
      <a @click="gotoB"> 《隐私政策》</a>了解详细信息。
      如果您同意，请点击下面按钮开始接受我们的服务。
    </view>
  </u-modal>
</template>

<script>
import storage from "@/utils/storage";
export default {
  created() {
    //先进入 created
    // if (storage.getShow()) {
    //   //展示的话进入  true
    //   console.log(this.show); //如果上面没读缓存  此时 this.show 为true
    //   if (!this.show) {
    //     //如果等于 false 了 就跳到主页
    //     this.show = storage.getShow(); //这里就为false
    //     setTimeout(() => {
    //       //然后这里就跳转到  首页
    //       uni.reLaunch({
    //         //跳转到 首页
    //         url: "/pages/tabbar/home/index",
    //       });
    //     }, 500);
    //   }
    // }
  },
  data() {
    return {
      show: true, //展示
      btnShow:true,
      a: "",
    };
  },
  //   onReady() {
  //     this.show = true;
  //   },
  methods: {
    gotoLink() {
      uni.navigateTo({
        //点击跳转到浏览器
        url:
          "/pages/tabbar/home/web-view?src=https://pc-b2b2c.pickmall.cn/article/detail?id=1371992704333905920",
      });
    },
    gotoB() {
      uni.navigateTo({
        url:
          "/pages/tabbar/home/web-view?src=https://pc-b2b2c.pickmall.cn/article/detail?id=1371779927900160000",
      });
    },
     //取消
    cancel(){
      // #ifdef APP-PLUS
		 	 const threadClass = plus.ios.importClass("NSThread");
		 	 const mainThread = plus.ios.invoke(threadClass, "mainThread");
		 	 plus.ios.invoke(mainThread, "exit")
		 // #endif
    },
    confirm() {
      //点击
      this.show = false; // 让这个框为false
      storage.setShow(this.show); //存入缓存
      if (!this.show) {
        // 他如果 不展示  就跳转到主页
        setTimeout(() => {
          uni.reLaunch({
            //跳转到 首页
            url: "/pages/tabbar/home/index",
          });
        }, 500);
      }
    },
  },
};
</script>

<style scoped>
.u-update-content {
  font-size: 26rpx;
  padding: 30rpx;
}
a {
  text-decoration: blue;
  color: blue;
}
</style>
