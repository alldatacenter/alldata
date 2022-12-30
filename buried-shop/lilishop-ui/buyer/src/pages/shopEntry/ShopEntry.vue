<template>
  <div class="shop-entry">
    <div style="height: 20px"></div>
    <div class="content">
      <h1>店铺入驻</h1>
      <Steps :current="currentIndex" class="margin">
        <Step title="企业资质信息"></Step>
        <Step title="财务资质信息"></Step>
        <Step title="其他信息"></Step>
        <Step title="提交审核"></Step>
      </Steps>
      <first-apply v-if="currentIndex == 0 && dataReview" :content="firstData" @change="nextPage"></first-apply>

      <second-apply v-if="currentIndex == 1 && dataReview" :content="secondData" @change="nextPage"></second-apply>

      <third-apply v-if="currentIndex == 2 && dataReview" :content="thirdData" @change="nextPage"></third-apply>

      <div class="success-page" v-if="currentIndex == 3">
        <span v-if="storeDisable == '' || storeDisable == 'APPLYING'">入驻申请提交成功，等待平台审核</span>
        <span v-if="storeDisable == 'OPEN'">申请已通过，请联系管理员</span>
        <span v-if="storeDisable == 'CLOSED'">店铺已关闭，重申请联系管理员</span>
        <span v-if="storeDisable == 'REFUSED'">审核未通过,请修改资质信息，如有疑问请联系管理员</span>
      </div>
      <Button v-if="currentIndex === 3" @click="$router.push('/')">返回</Button>
      <Button type="primary" @click='currentIndex = 0'
        v-if="storeDisable === 'REFUSED' && currentIndex === 3">重新申请</Button>
    </div>

    <Modal title="店铺入驻协议" v-model="showAgreement" width="1200" :closable="false" :mask-closable="false">
      <div class="agreeent-con" v-html="agreementCon"></div>

      <div slot="footer" style="text-align: center">
        <p>
          <Checkbox v-model="checked">我已同意以上协议</Checkbox>
        </p>
        <Button type="primary" :disabled="!checked" class="margin" @click="showAgreement = false">同意协议填写资质信息</Button>
      </div>
    </Modal>
  </div>
</template>
<script>
import { agreement, applyStatus } from "@/api/shopentry";
import firstApply from "./FirstApply";
import secondApply from "./SecondApply";
import thirdApply from "./ThirdApply";
export default {
  components: {
    firstApply,
    secondApply,
    thirdApply,
  },
  data() {
    return {
      currentIndex: 0, // 当前步骤
      showAgreement: false, // 协议显示
      agreementCon: "", // 协议内容
      checked: false, // 选中协议
      firstData: {}, // 第一步数据
      secondData: {}, // 第二步数据
      thirdData: {}, // 第三步数据
      storeDisable: "", // APPLY OPEN 开店中 CLOSED 关闭 REFUSED 拒绝 APPLYING 申请中，审核
      dataReview: true, // 根据接口返回判断是否可展示数据
    };
  },
  methods: {
    getArticle() {
      // 入驻协议
      agreement().then((res) => {
        this.agreementCon = res.result.content;
      });
    },
    getData(status) {
      // 获取已填写店铺信息
      applyStatus().then((res) => {
        if (res.success) {
          if (!res.result) {
            this.showAgreement = true;
          } else {
            this.dataReview = false;
            let data = res.result;
            let first = [
              "companyAddressPath",
              "companyAddress",
              "companyAddressIdPath",
              "companyEmail",
              "companyName",
              "employeeNum",
              "companyPhone",
              "legalId",
              "legalName",
              "licencePhoto",
              "legalPhoto",
              "licenseNum",
              "linkName",
              "linkPhone",
              "registeredCapital",
              "scope",
            ];
            let second = [
              "settlementBankAccountName",
              "settlementBankAccountNum",
              "settlementBankBranchName",
              "settlementBankJointName",
            ];
            let third = [
              "goodsManagementCategory",
              "storeCenter",
              "storeDesc",
              "storeLogo",
              "storeName",
              "storeAddressIdPath",
              "storeAddressPath",
              "storeAddressDetail",
            ];

            this.storeDisable = data.storeDisable;

            first.forEach((e) => {
              this.firstData[e] = data[e];
            });
            second.forEach((e) => {
              this.secondData[e] = data[e];
            });
            third.forEach((e) => {
              this.thirdData[e] = data[e];
            });
            if (status === "init") {
              if (this.storeDisable === "APPLY") {
                this.currentIndex = 0;
              } else {
                this.currentIndex = 3;
              }
            }
            this.$nextTick(() => {
              this.dataReview = true;
              this.$forceUpdate();
            });
          }
        }
      });
    },
    // 下一步
    nextPage(step) {
      this.currentIndex = step;
      this.getData("next");
    },
  },
  mounted() {
    this.getData("init");
    this.getArticle();
  },
};
</script>
<style lang="scss" scoped>
.content {
  width: 1200px;
  margin: 0 auto;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  min-height: 500px;
  border-radius: 20px;
  background: #fff;
  padding: 10px 20px;

  h1 {
    
    margin-top: 20px;
  }
}

.margin {
  margin: 30px 0;
}
.agreeent-con {
  max-height: 500px;
}
.success-page {
  height: 500px;
  width: 100%;
  line-height: 500px;
  text-align: center;
  font-size: 20px;
}
.shop-entry {
  min-height: 100vh;
  padding: 32px 0;
}
</style>
