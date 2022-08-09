// TODO 第一版本暂无此功能 后续优化以及更新
<template>
  <view class="finger">
    <u-cell-group>
      <u-cell-item class="border-top" :arrow="false" title="指纹登录">
        <u-switch slot="right-icon" @change="fingerSwitchChange" :active-color="lightColor" size="40" v-model="checked"></u-switch>
      </u-cell-item>
    </u-cell-group>
    <view class="describe">开启后可使用指纹认证完成快捷登录，设置仅对本机生效。如需修改指纹，请在系统设置中操作。</view>
  </view>
</template>

<script>
import storage from "@/utils/storage.js";
import { setBiolofy } from "@/api/passport.js";

export default {
  data() {
    return {
      lightColor: this.$lightColor,
      checked: false,
    };
  },
  methods: {
    fingerSwitchChange(value) {
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
                storage.setFingerLogin(true);
              }
            });
          },
          fail: function (e) {
            console.error("getDeviceInfo failed: " + JSON.stringify(e));
          },
        });
      } else {
        storage.setFingerLogin(false);
      }
    },
  },
  onLoad() {
    // #ifdef APP-PLUS
    if (!plus.fingerprint.isSupport()) {
      plus.nativeUI.toast("此设备不支持指纹识别");
      uni.navigateBack();
    }
    if (!plus.fingerprint.isKeyguardSecure()) {
      plus.nativeUI.toast("此设备未设置密码锁屏");
      uni.navigateBack();
    }
    if (!plus.fingerprint.isEnrolledFingerprints()) {
      plus.nativeUI.toast("此设备未录入指纹");
      uni.navigateBack();
    }
    this.checked = storage.getFingerLogin() || false;
    // #endif
  },
};
</script>

<style scoped lang="scss">
</style>
