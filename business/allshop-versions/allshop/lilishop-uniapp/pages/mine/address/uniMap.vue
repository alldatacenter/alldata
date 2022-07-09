<template></template>
<script>
import { getAddressCode } from "@/api/address";
import config from '@/config/config'
export default {
  data() {
    return {
      config
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    // 初始化地图并且调用
    initMap() {
      let that = this;
      uni.chooseLocation({
        success: function (res) {
          /**获取地址详情地址 */
          that.posToCity(res.latitude, res.longitude).then((val) => {
            /**获取地址code */
            getAddressCode(
              val.regeocode.addressComponent.citycode,
              val.regeocode.addressComponent.township
            ).then((code) => {
              that.$emit("callback", { ...val, ...res, ...code });
              that.$emit("close");
            });
          });
        },
        fail() {
          that.$emit("close");
        },
      });
    },

    // 根据当前客户端判断展示不同类型数据
    init() {
      // #ifdef MP-WEIXIN
      this.wechatMap();
      // #endif
      // #ifndef MP-WEIXIN
      this.initMap();
      // #endif
    },

    // 如果是微信小程序单独走微信小程序授权模式
    wechatMap() {
      let that = this;
      uni.authorize({
        scope: "scope.userLocation",
        success() {
          // 允许授权
          that.initMap();
        },
        fail() {
          //拒绝授权
          uni.showModal({
            content: "检测到您没打开获取地址功能权限，是否去设置打开？",
            confirmText: "确认",
            cancelText: "取消",
            success: (res) => {
              if (res.confirm) {
                // 打开设置好后重新刷新地图
                uni.openSetting({
                  success: (res) => {
                    that.initMap();
                  },
                });
              } else {
                // 取消后关闭
                that.$emit("close");
                return false;
              }
            },
          });
          return false;
        },
      });
    },
    // 获取城市的数据
    posToCity(latitude, longitude) {
      return new Promise((resolve, reject) => {
        uni.request({
          url: `https://restapi.amap.com/v3/geocode/regeo`,
          method: "GET",
          data: {
            key: config.aMapKey, //web服务的key
            location: `${longitude},${latitude}`,
          },
          success: ({ data }) => {
            const { status, info } = data;
            if (status === "1") {
              resolve(data);
            } else {
              reject(info);
            }
          },
          fail: (err) => {
            reject(err);
          },
        });
      });
    },
  },
};
</script>
<style lang="scss" scoped>
</style>