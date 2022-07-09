<template>
  <div class="layout">
    <Form
      ref="formValidate"
      :label-width="150"
      label-position="right"
      :model="formValidate"
      :rules="ruleValidate"
    >
      <FormItem label="appId" prop="appId">
        <Input class="w200" v-model="formValidate.appId" />
        <p style="color: red">*APP应用 AppID 非必填</p>
      </FormItem>
      <FormItem label="mpAppId" prop="mpAppId">
        <Input class="w200" v-model="formValidate.mpAppId" />
        <p style="color: red">*小程序 AppID 非必填</p>
      </FormItem>
      <FormItem label="serviceAppId" prop="serviceAppId">
        <Input class="w200" v-model="formValidate.serviceAppId" />
        <p style="color: red">*服务号 AppID 非必填</p>
      </FormItem>
      <FormItem label="mchId" prop="mchId">
        <Input class="w200" v-model="formValidate.mchId" />
      </FormItem>
      <FormItem label="apiKey3" prop="apiKey3">
        <Input v-model="formValidate.apiKey3" />
      </FormItem>
      <FormItem label="apiclient_cert_p12" class="label-item" prop="apiclient_cert_p12">
        <Input v-model="formValidate.apiclient_cert_p12" />
      </FormItem>
      <FormItem label="apiclient_cert_pem" prop="apiclient_cert_pem">
        <Input v-model="formValidate.apiclient_cert_pem" />
      </FormItem>
      <FormItem label="apiclient_key" prop="apiclient_key">
        <Input v-model="formValidate.apiclient_key" />
      </FormItem>

      <FormItem label="serialNumber" prop="serialNumber">
        <Input v-model="formValidate.serialNumber" />
      </FormItem>
      <div class="label-btns">
        <Button type="primary" @click="submit('formValidate')">保存</Button>
      </div>
    </Form>
  </div>
</template>
<script>
import { setSetting } from "@/api/index";
import { handleSubmit } from "../setting/validate";

export default {
  data() {
    return {
      ruleValidate: {}, // 验证规则
      formValidate: {}, // 表单数据
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
    // 保存设置
    setupSetting() {
      setSetting(this.type, this.formValidate).then((res) => {
        if (res.success) {
          this.$Message.success("保存成功!");
        } else {
          this.$Message.error("保存失败!");
        }
      });
    },
    // 实例化数据
    init() {
      this.res = JSON.parse(this.res);

      this.$set(this, "formValidate", { ...this.res });
      Object.keys(this.formValidate).forEach((item) => {
        if (item.indexOf("pId") < 0) {
          this.ruleValidate[item] = [
            {
              required: true,
              message: "请填写必填项",
              trigger: "blur",
            },
          ];
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "../setting/style.scss";

.label-item {
  display: flex;
}

.w200 {
  /deep/ .ivu-input {
    width: 250px !important;
    margin: 0 10px;
  }
}

/deep/ .ivu-input {
  width: 450px !important;
  margin: 0 10px;
}

.ivu-input-wrapper {
  width: 450px;
  margin-right: 10px;
}
</style>
