// TODO 第一版本暂无此功能 后续优化以及更新
<template>
  <view class="face-login">
    <u-cell-group>
      <u-cell-item class="border-top" :arrow="false" title="面容登录">
        <u-switch slot="right-icon" @change="faceSwitchChange" active-color="#1abc9c" size="40" v-model="checked"></u-switch>
      </u-cell-item>
    </u-cell-group>
    <view class="describe">开启后可使用面容认证完成快捷登录，设置仅对本机生效。</view>
  </view>
</template>

<script>
import storage from "@/utils/storage.js";
import { setBiolofy } from "@/api/passport.js";

export default {
  data() {
    return {
      lightColor: this.$lightColor,
      checked: true,
    };
  },
  methods: {
    faceSwitchChange(value) {
      if (value === true) {
        const res = uni.getSystemInfoSync();
        plus.device.getInfo({
          success: function (e) {
            let params = {
              mobile_type: res.model,
              secret_key: e.uuid,
            };
            setBiolofy(params).then((res) => {
              if (res.statusCode === 200) {
                storage.setFaceLogin(true);
              }
            });
          },
          fail: function (e) {
            //plus.nativeUI.toast('获取设备信息错误：' + JSON.stringify(e));
            console.error("getDeviceInfo failed: " + JSON.stringify(e));
          },
        });
      } else {
        storage.setFaceLogin(false);
      }
    },
  },
  onLoad() {
    // #ifdef APP-PLUS
    uni.checkIsSupportSoterAuthentication({
      success(res) {
        if (!res.supportMode.find((e) => e === "facial")) {
          plus.nativeUI.toast("此设备不支持面部识别");
          uni.navigateBack();
        }
        uni.checkIsSoterEnrolledInDevice({
          checkAuthMode: "facial",
          success(_res) {
            if (!_res.isEnrolled) {
              plus.nativeUI.toast("此设备未录入面部信息");
              uni.navigateBack();
            }
          },
          fail(_err) {
            // plus.nativeUI.toast(JSON.stringify(_err));
            uni.navigateBack();
          },
        });
      },
      fail(err) {
        // plus.nativeUI.toast(JSON.stringify(err));
        uni.navigateBack();
      },
    });
    this.checked = storage.getFaceLogin() || false;
    // #endif
  },
};
</script>

<style lang="scss" scoped>
.face-login {
}
</style>
