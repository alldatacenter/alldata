<template>
  <div class="wrapper">
    <p>公司名称：{{storeData.companyName}}</p>
    <p>公司地址：{{storeData.companyAddress}}</p>
    <p>公司地址地区：{{storeData.companyAddressPath}}</p>
    <p>员工人数：{{storeData.employeeNum}}</p>
    <p>法定经营范围：{{storeData.scope}}</p>
    <u-image @click="preview()" width="100%" height="400rpx" mode="aspectFit" :src="storeData.licencePhoto"></u-image>
  </div>
</template>

<script>
import { getLicencePhoto } from "@/api/store.js";
export default {
  data() {
    return {
      storeData: {},
    };
  },
  onLoad(options) {
    this.getStoreLicencePhoto(options.id);
  },
  methods: {
    async getStoreLicencePhoto(id) {
      let res = await getLicencePhoto(id);
      if (res.data.success) {
        this.storeData = res.data.result;
      }
    },

    /**
     * 点击图片放大或保存
     */
    preview() {
      uni.previewImage({
        current: 0,
        urls: [this.storeData.licencePhoto],
        longPressActions: {
          itemList: ["保存图片"],
          success: function (data) {},
          fail: function (err) {},
        },
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.wrapper {
  padding: 32rpx;
  > p {
    margin: 20rpx 0;
  }
}
</style>