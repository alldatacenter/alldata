<template>
  <view class="wrapper">
    <u-tabs
      :list="list"
      :is-scroll="false"
      :current="current"
      @change="change"
      :active-color="$lightColor"
    ></u-tabs>

  

    <!-- 推广人资料 -->
    <view class="message">
      <u-form :model="ruleForm" label-width="250rpx" ref="uForm">
        <u-form-item label="会员昵称" prop="name">
          <u-input v-model="ruleForm.name" />
        </u-form-item>
        <u-form-item label="账户类型" prop="name"> </u-form-item>
        <u-form-item
          label="收款人姓名"
          placeholder="请输入收款人姓名"
          prop="name"
        >
          <u-input v-model="ruleForm.name" />
        </u-form-item>
        <u-form-item
          label="收款账号"
          placeholder="请输入收款人账号"
          prop="name"
        >
          <u-input v-model="ruleForm.name" />
        </u-form-item>
        <u-form-item
          label="银行名称"
          placeholder="请输入开户银行支行名称"
          prop="name"
        >
          <u-input v-model="ruleForm.name" />
        </u-form-item>
      </u-form>
      <u-button  :customStyle="{'background':$lightColor,'color':'#fff' }"  @click="submit">提交</u-button>
    </view>
  </view>
</template>
<script>
export default {
  components: {},
  // 必须要在onReady生命周期，因为onLoad生命周期组件可能尚未创建完毕
  onReady() {
    this.$refs.uForm.setRules(this.rules);
  },
  data() {
    return {
      current:0,
      list: [
        {
          name: "推广人资料",
        },
        {
          name: "平台审核",
        },
        {
          name: "完成",
        },
      ],
      ruleForm: {
        name: "",
        radio: "",
      },
      rules: {
        name: [
          {
            required: true,
            message: "请输入姓名",
            // 可以单个或者同时写两个触发验证方式
            trigger: "blur",
          },
        ],
      },
    };
  },
};
</script>
<style lang="scss" scoped>
.menu {
  height: 88rpx;
  line-height: 88rpx;
  background: $main-color;
  display: flex;
  > .menu-item {
    flex: 1;
    text-align: center;
    color: $light-color;
  }
}
.active {
  color: #fff !important;
}
.message {
  padding: 0 32rpx;
}
</style>
