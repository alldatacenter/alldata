<template>
  <div class="feedBack">
    <div class="feedBack-box">
      <h4>猜你想问</h4>
      <div class="feedBack-item" :class="{'active':feedBack.type == item.value }" @click="handleClick(index)" v-for="(item,index) in list" :key="index">
        {{item.text}}
      </div>
    </div>

    <div class="feedBack-box">
      <h4>问题反馈 <span style="margin-left:10rpx;" v-if="feedBack.type">@{{ list.find(item=>{return item.value == feedBack.type }).text  }}</span></h4>
      <u-input class="field-input" height="500" :border-bottom="false" v-model="feedBack.context" type="textarea" placeholder="请输入反馈信息">
      </u-input>
    </div>

    <!-- 上传凭证 -->
    <div class="feedBack-box">
      <view class="opt-view">
        <view class="img-title">上传凭证（最多5张）</view>
        <view class="images-view">
          <u-upload :header=" { accessToken: storage.getAccessToken() }" :action="action" width="150" @on-uploaded="onUploaded" :max-count="5" :show-progress="false"></u-upload>
        </view>
      </view>
    </div>

    <div class="feedBack-box">
      <h4>手机号</h4>
      <u-input :border-bottom="false" v-model="feedBack.mobile" placeholder="请输入您的手机号">
      </u-input>
    </div>

    <div class="submit" @click="submit()">提交</div>
  </div>

</template>

<script>
import storage from "@/utils/storage.js";
import config from "@/config/config";
import { feedBack } from "@/api/members.js";
import { upload } from "@/api/common.js";
export default {
  data() {
    return {
      storage,
      config,
      feedBack: {
        type: "FUNCTION", //默认反馈问题为 '功能相关'
      },
      action: upload, //图片上传地址
      list: [
        { text: "功能相关", value: "FUNCTION" },
        { text: "优化反馈", value: "OPTIMIZE" },
        { text: "其他", value: "OTHER" },
      ],
    };
  },
  methods: {
    // 点击反馈内容
    handleClick(index) {
      this.$set(this.feedBack, "type", this.list[index].value);
    },

    //图片上传
    onUploaded(lists) {
      let images = [];
      lists.forEach((item) => {
        images.push(item.response.result);
      });
      this.feedBack.images = images.join(",");
    },

    /**
     * 提交意见反馈
     */
    submit() {
      if (!this.feedBack.type) {
        uni.showToast({
          title: "请填写反馈类型",
          duration: 2000,
          icon: "none",
        });
        return false;
      }
      if (!this.feedBack.context) {
        uni.showToast({
          title: "请填写反馈类型",
          duration: 2000,
          icon: "none",
        });
        return false;
      }
      if (this.feedBack.mobile && !this.$u.test.mobile(this.feedBack.mobile)) {
        uni.showToast({
          title: "请填写您的正确手机号",
          duration: 2000,
          icon: "none",
        });
        return false;
      }
      /** 提交 */
      feedBack(this.feedBack).then((res) => {
        if (res.data.success) {
          uni.showToast({
            title: "提交成功!",
            duration: 2000,
            icon: "none",
          });
          setTimeout(() => {
            uni.navigateBack({
              delta: 1,
            });
          }, 500);
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.submit {
  text-align: center;
  background: $light-color;
  height: 70rpx;
  line-height: 70rpx;
  color: #fff;
  width: 92%;
  margin-bottom: 100rpx;
  margin: 0 auto;
  border-radius: 100px;
}
.active{
  color: $light-color !important;
  font-weight: bold;
}
.feedBack {
  padding-bottom: 100rpx;
}
.feedBack-box {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 40rpx;
}
/deep/ .u-input__textarea {
  padding: 12px;
}
.feedBack-box:nth-of-type(1) {
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}
.feedBack-item {
  margin: 20rpx 0;
  font-size: 24rpx;
  color: #666;
}
h4 {
  font-size: 30rpx;
}
.field-input {
  margin: 20rpx 0;
  padding: 20rpx 0;
  background: #fafafa;
  border-radius: 0.6em;
}
</style>
