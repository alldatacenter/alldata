<template>
  <div class="wrapper">
    <!-- uni 中不能使用 vue component 所以用if判断每个组件 -->
    <div v-for="(item, index) in pageData.list" :key="index">
      <!-- 搜索栏，如果在楼层装修顶部则会自动浮动，否则不浮动 -->
      <u-navbar
        class="navbar"
        v-if="item.type == 'search'"
        :is-back="false"
        :is-fixed="index === 1 ? false : true"
      >
        <search style="width: 100%" :res="item.options" />
        <!-- #ifndef H5 -->
        <!-- 扫码功能 不兼容h5 详情文档: https://uniapp.dcloud.io/api/system/barcode?id=scancode -->
        <div slot="right" class="navbar-right">
          <u-icon name="scan" @click="scan()" color="#666" size="50"></u-icon>
        </div>
        <!-- #endif -->
      </u-navbar>
      <carousel v-if="item.type == 'carousel'" :res="item.options" />
      <titleLayout v-if="item.type == 'title'" :res="item.options" />
      <leftOneRightTwo
        v-if="item.type == 'leftOneRightTwo'"
        :res="item.options"
      />
      <leftTwoRightOne
        v-if="item.type == 'leftTwoRightOne'"
        :res="item.options"
      />
      <topOneBottomTwo
        v-if="item.type == 'topOneBottomTwo'"
        :res="item.options"
      />
      <topTwoBottomOne
        v-if="item.type == 'topTwoBottomOne'"
        :res="item.options"
      />
      <flexThree v-if="item.type == 'flexThree'" :res="item.options" />
      <flexFive v-if="item.type == 'flexFive'" :res="item.options" />
      <flexFour v-if="item.type == 'flexFour'" :res="item.options" />
      <flexTwo v-if="item.type == 'flexTwo'" :res="item.options" />
      <textPicture v-if="item.type == 'textPicture'" :res="item.options" />
      <menuLayout v-if="item.type == 'menu'" :res="item.options" />
      <flexOne v-if="item.type == 'flexOne'" :res="item.options" />
      <goods v-if="item.type == 'goods'" :res="item.options" />
      <group v-if="item.type == 'group'" :res="item.options" />
      <notice v-if="item.type == 'notice'" :res="item.options" />
      <promotions v-if="item.type == 'promotionDetail'" :res="item.options" />
      <!-- <joinGroup v-if="item.type == 'joinGroup'" :res="item.options" /> -->
      <!-- <integral v-if="item.type == 'integral'" :res="item.options" /> -->
      <!-- <spike v-if="item.type == 'spike'" :res="item.options" /> -->
    </div>
    <u-no-network></u-no-network>
  </div>
</template>

<script>
// 引用组件
import tpl_banner from "@/pages/tabbar/home/template/tpl_banner"; //导航栏模块
import tpl_title from "@/pages/tabbar/home/template/tpl_title"; //标题栏模块
import tpl_left_one_right_two from "@/pages/tabbar/home/template/tpl_left_one_right_two"; //左一右二模块
import tpl_left_two_right_one from "@/pages/tabbar/home/template/tpl_left_two_right_one"; //左二右一模块
import tpl_top_one_bottom_two from "@/pages/tabbar/home/template/tpl_top_one_bottom_two"; //上一下二模块
import tpl_top_two_bottom_one from "@/pages/tabbar/home/template/tpl_top_two_bottom_one"; //上二下一模块
import tpl_flex_one from "@/pages/tabbar/home/template/tpl_flex_one"; //单行图片模块
import tpl_flex_two from "@/pages/tabbar/home/template/tpl_flex_two"; //两张横图模块
import tpl_flex_three from "@/pages/tabbar/home/template/tpl_flex_three"; //三列单行图片模块
import tpl_flex_five from "@/pages/tabbar/home/template/tpl_flex_five"; //五列单行图片模块
import tpl_flex_four from "@/pages/tabbar/home/template/tpl_flex_four"; //四列单行图片模块
import tpl_text_picture from "@/pages/tabbar/home/template/tpl_text_picture"; //文字图片模板
import tpl_menu from "@/pages/tabbar/home/template/tpl_menu"; //五列菜单模块
import tpl_search from "@/pages/tabbar/home/template/tpl_search"; //搜索栏
import tpl_group from "@/pages/tabbar/home/template/tpl_group"; //
import tpl_goods from "@/pages/tabbar/home/template/tpl_goods"; //商品分类以及分类中的商品
// 结束引用组件
import { getFloorData } from "@/api/home"; //获取楼层装修接口
import permision from "@/js_sdk/wa-permission/permission.js"; //权限工具类
import config from "@/config/config";
import tpl_notice from "@/pages/tabbar/home/template/tpl_notice"; //标题栏模块
import tpl_promotions from "@/pages/tabbar/home/template/tpl_promotions_detail"; //标题栏模块

export default {
  data() {
    return {
      config,
      pageData: "", //楼层页面数据
      isIos: "",
    };
  },
  components: {
    carousel: tpl_banner,
    titleLayout: tpl_title,
    leftOneRightTwo: tpl_left_one_right_two,
    leftTwoRightOne: tpl_left_two_right_one,
    topOneBottomTwo: tpl_top_one_bottom_two,
    topTwoBottomOne: tpl_top_two_bottom_one,
    flexThree: tpl_flex_three,
    flexFive: tpl_flex_five,
    flexFour: tpl_flex_four,
    flexTwo: tpl_flex_two,
    textPicture: tpl_text_picture,
    menuLayout: tpl_menu,
    search: tpl_search,
    flexOne: tpl_flex_one,
    goods: tpl_goods,
    group: tpl_group,
    notice: tpl_notice,
    promotions: tpl_promotions,
  },

  mounted() {
    this.init();
    // #ifdef MP-WEIXIN
    // 小程序默认分享
    uni.showShareMenu({ withShareTicket: true });
    // #endif
  },

  methods: {
    /**
     * 实例化首页数据楼层
     */
    init() {
      this.pageData = "";
      getFloorData().then((res) => {
        if (res.data.success) {
          this.pageData = JSON.parse(res.data.result.pageData);
        }
      });
    },

    /**
     * TODO 扫码功能后续还会后续增加
     * 应该实现的功能目前计划有：
     * 扫描商品跳转商品页面
     * 扫描活动跳转活动页面
     * 扫描二维码登录
     * 扫描其他站信息 弹出提示，返回首页。
     */
    seacnCode() {
      uni.scanCode({
        success: function (res) {
          let path = encodeURIComponent(res.result);

          // WX_CODE 为小程序码
          if (res.scanType == "WX_CODE") {
            console.log(res);
            uni.navigateTo({
              url: `/${res.path}`,
            });
          } else {
            config.scanAuthNavigation.forEach((src) => {
              if (res.result.indexOf(src) != -1) {
                uni.navigateTo({
                  url: `/${res.result.substring(src.length)}`,
                });
              } else {
                setTimeout(() => {
                  uni.navigateTo({
                    url: "/pages/tabbar/home/web-view?src=" + path,
                  });
                }, 100);
              }
            });
          }
        },
      });
    },

    /**
     * 提示获取权限
     */
    tipsGetSettings() {
      uni.showModal({
        title: "提示",
        content: "您已经关闭相机权限,去设置",
        success: function (res) {
          if (res.confirm) {
            if (this.isIos) {
              plus.runtime.openURL("app-settings:");
            } else {
              permision.gotoAppPermissionSetting();
            }
          }
        },
      });
    },

    /**
     * 唤醒客户端扫码
     * 没权限去申请权限，有权限获取扫码功能
     */
    async scan() {
      // #ifdef APP-PLUS
      this.isIos = plus.os.name == "iOS";
      // 判断是否是Ios
      if (this.isIos) {
        const iosFirstCamera = uni.getStorageSync("iosFirstCamera"); //是不是第一次开启相机
        if (iosFirstCamera !== "false") {
          uni.setStorageSync("iosFirstCamera", "false"); //设为false就代表不是第一次开启相机
          this.seacnCode();
        } else {
          if (permision.judgeIosPermission("camera")) {
            this.seacnCode();
          } else {
            // 没有权限提醒是否去申请权限
            this.tipsGetSettings();
          }
        }
      } else {
        /**
         * TODO 安卓 权限已经授权了，调用api总是显示用户已永久拒绝申请。人傻了
         * TODO 如果xdm有更好的办法请在 https://gitee.com/beijing_hongye_huicheng/lilishop/issues 提下谢谢
         */
        this.seacnCode();
      }

      // #endif

      // #ifdef MP-WEIXIN
      this.seacnCode();
      // #endif
    },
  },
};
</script>

<style scoped lang="scss">
.navbar-right {
  padding: 0 16rpx 0 0;
}
</style>
