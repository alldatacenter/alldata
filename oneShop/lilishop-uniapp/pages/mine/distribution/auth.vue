<template>
  <view class="wrapper">
    <view>
      <h4>实名认证（请上传真实的个人信息，认证通过后将无法修改）</h4>
      <view>
        <u-form :model="ruleForm" label-width="200rpx" ref="uForm">
          <u-form-item label="姓名" prop="name">
            <u-input v-model="ruleForm.name" placeholder="请输入您的真实姓名" />
          </u-form-item>
          <u-form-item label="身份证" prop="idNumber">
            <u-input v-model="ruleForm.idNumber" placeholder="请输入身份证号码" />
          </u-form-item>
          <u-form-item label="银行开户行" prop="settlementBankBranchName">
            <u-input v-model="ruleForm.settlementBankBranchName" type="text" placeholder="请输入银行开户行" />
          </u-form-item>
          <u-form-item label="银行开户名" prop="settlementBankAccountName">
            <u-input v-model="ruleForm.settlementBankAccountName" type="text" placeholder="请输入银行开户名" />
          </u-form-item>
          <u-form-item label="银行账号" prop="settlementBankAccountNum">
            <u-input v-model="ruleForm.settlementBankAccountNum" type="text" placeholder="请输入银行账号" />
          </u-form-item>
          <!-- <u-form-item label="身份证正面照" prop="name">
            <u-upload></u-upload>
          </u-form-item>
          <u-form-item label="身份证反面照" prop="name">
            <u-upload></u-upload>
          </u-form-item>
          <u-form-item label="手持身份证照" prop="name">
            <u-upload></u-upload>
          </u-form-item> -->
        </u-form>
        <u-button :customStyle="{'background':$lightColor,'color':'#fff' }" @click="submit">提交</u-button>
      </view>
    </view>

    <view class="tips">
      <view>您提交的信息正在审核</view>
      <view>提交认证申请后，工作人员将在三个工作日进行核对完成审核</view>
    </view>
  </view>
</template>
<script>
import { applyDistribution } from "@/api/goods";
import { checkBankno } from "@/utils/Foundation";
export default {
  data() {
    return {
      ruleForm: {
        name: "",
        idNumber: "",
        settlementBankBranchName: "", // 银行开户行
        settlementBankAccountName: "", //银行开户名
        settlementBankAccountNum: "", //银行账号
      },
      rules: {
        name: [
          {
            required: true,
            message: "请输入姓名",
            // 可以单个或者同时写两个触发验证方式
            trigger: "blur",
          },
          {
            // 自定义验证函数，见上说明
            validator: (rule, value, callback) => {
              // 上面有说，返回true表示校验通过，返回false表示不通过
              // this.$u.test.mobile()就是返回true或者false的
              return this.$u.test.chinese(value);
            },
            message: "姓名输入不正确",
            // 触发器可以同时用blur和change
            trigger: ["change", "blur"],
          },
        ],
        // 银行开户行
        settlementBankBranchName: [
          {
            required: true,
            message: "银行开户行",
            // 可以单个或者同时写两个触发验证方式
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              // 上面有说，返回true表示校验通过，返回false表示不通过
              // this.$u.test.mobile()就是返回true或者false的
              return this.$u.test.chinese(value);
            },
            message: "银行开户行不正确",
            // 触发器可以同时用blur和change
            trigger: ["change", "blur"],
          },
        ], //银行开户名
        settlementBankAccountName: [
          {
            required: true,
            message: "银行开户名",
            // 可以单个或者同时写两个触发验证方式
            trigger: "blur",
          },
        ],
        //银行账号
        settlementBankAccountNum: [
          {
            required: true,
            message: "银行账号不正确",
            // 可以单个或者同时写两个触发验证方式
            trigger: "blur",
          },
          {
            validator: (rule, value, callback) => {
              // 上面有说，返回true表示校验通过，返回false表示不通过
              // this.$u.test.mobile()就是返回true或者false的
              return checkBankno(value);
            },
            message: "银行账号不正确",
          },
        ],
        idNumber: [
          {
            required: true,
            message: "请输入身份证",
            // 可以单个或者同时写两个触发验证方式
            trigger: "blur",
          },
          {
            // 自定义验证函数，见上说明
            validator: (rule, value, callback) => {
              // 上面有说，返回true表示校验通过，返回false表示不通过
              // this.$u.test.mobile()就是返回true或者false的
              return this.$u.test.idCard(value);
            },
            message: "身份证号码不正确",
            // 触发器可以同时用blur和change
            trigger: ["change", "blur"],
          },
        ],
      },
    };
  },
  methods: {
    submit() {
      this.$refs.uForm.validate((valid) => {
        if (valid) {
          applyDistribution(this.ruleForm).then((res) => {
            if (res.data.success) {
              uni.showToast({
                title: "认证提交成功!",
                duration: 2000,
                icon: "none",
              });

              setTimeout(() => {
                uni.navigateBack();
              }, 500);
            } else {
              uni.showToast({
                title: res.data.message,
                duration: 2000,
                icon: "none",
              });
            }
          });
        } else {
          uni.showToast({
            title: "请填写有效信息",
            duration: 2000,
            icon: "none",
          });
        }
      });
    },
  },
  onReady() {
    this.$refs.uForm.setRules(this.rules);
  },
};
</script>
<style lang="scss" scoped>
.wrapper {
  padding: 32rpx;
}
.tips {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: #999;
}
</style>