<template>
  <div class="layout">
    <div class="row" v-for="(client,clientIndex) in supportForm.clients" :key="clientIndex">

      <div class="col" v-if="client ==item.client" v-for="(item,index) in  formValidate" :key="index">
        <Card :padding="0">
          <div>

            <!-- app -->
            <div class="icon-item" v-if="client ==item.client &&item.client== 'APP'">
              <img class="icon" src="../../../../assets/setting/app.svg" alt="" srcset="">
            </div>
            <div class="icon-item" v-if="  client ==item.client &&  item.client== 'PC'">
              <!-- pc -->
              <img class="icon" src="../../../../assets/setting/pc.svg" alt="" srcset="">

            </div>
            <div class="icon-item" v-if="  client ==item.client &&  item.client== 'WECHAT_MP'">
              <!-- 小程序 -->

              <img class="icon" src="../../../../assets/setting/wechat_mp.svg" alt="" srcset="">
            </div>

            <div class="icon-item" v-if=" client ==item.client &&  item.client== 'H5'">
              <!-- h5 -->

              <img class="icon" src="../../../../assets/setting/h5.svg" alt="" srcset="">
            </div>
            <div class='pay-title' v-if=" client ==item.client "> {{way[item.client]}}</div>

            <div v-if=" client ==item.client ">
              <Divider orientation="left">支付设置</Divider>

              <div class="pay-list">
                <CheckboxGroup @on-change="handleChangePayType" v-model="item.supports">

                  <Checkbox v-for="(support,i)  in supportForm.payments" :key="i" :label="support">

                    {{payWay[support] || support}}

                  </Checkbox>
                </CheckboxGroup>
              </div>
            </div>

          </div>
        </Card>

      </div>

    </div>
  </div>
</template>
<script>
import { setSetting } from "@/api/index";
import { handleSubmit } from "../setting/validate";
import { getPaymentSupportForm } from "@/api/setting";
export default {
  data() {
    return {
      ruleValidate: {}, // 验证规则
      way: { // 类型
        APP: "移动应用端",
        H5: "移动端",
        WECHAT_MP: "小程序端",
        PC: "PC端",
      },
      formValidate: {}, // 表单数据
      // key obj
      payWay: { // 支付方式
        ALIPAY: "支付宝支付",
        WECHAT: "微信支付",
        WALLET: "余额支付",
      },
      supportForm: "", // 支持的支付方式
    };
  },
  props: ["res", "type"],
  created() {
    this.init();
  },
  methods: {
    submit(name) {
      let that = this;
      if (handleSubmit(that, name)) {
        this.setupSetting();
      }
    },
    // 修改支付设置
    handleChangePayType(val) {
      this.$Modal.confirm({
        title: "修改支付设置",
        content: "您是否修改此项？",
        loading: true,
        onOk: () => {
          this.setupSetting();
        },
        onCancel: () => {
          val.splice(val.length - 1, 1);
        },
      });
    },
    // 保存设置
    setupSetting() {
      setSetting(this.type, { paymentSupportItems: this.formValidate }).then(
        (res) => {
          if (res.success) {
            this.$Message.success("保存成功!");
            this.$Modal.remove();
          } else {
            this.$Message.error("保存失败!");
            this.$Modal.remove();
          }
        }
      );
    },
    // 实例化数据
    async init() {
      this.formValidate = JSON.parse(this.res).paymentSupportItems;

      console.log(this.formValidate);

      await getPaymentSupportForm().then((res) => {
        // res.result.payments = ["H5", "PC"];
        this.supportForm = res.result;
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "../setting/style.scss";
.pay-title {
  text-align: center;
  margin: 10px 0;
}
.layout {
  padding: 20px;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-start;
}
.row {
  width: 300px;
  margin-right: 20px;
  display: flex;
  margin-bottom: 20px;
  /deep/ .ivu-card-body {
    padding: 0 16px !important;
  }
}

.label-item {
  display: flex;
}
.label-item {
  display: flex;
  align-items: center;
}
.pay-list {
  display: flex;
  justify-content: center;
  padding-bottom: 10px;
}
.icon-item {
  width: 100%;
  padding: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}
.ivu-form-item {
  display: flex;

  align-items: center;
}
.ivu-row {
  width: 100%;
}
.icon {
  width: 100px;
  height: 100px;
}
</style>
