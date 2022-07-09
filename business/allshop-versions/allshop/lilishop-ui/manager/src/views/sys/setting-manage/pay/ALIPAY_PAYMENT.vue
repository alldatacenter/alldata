<template>
  <div class="layout">
    <Form ref="formValidate" :label-width="160" label-position="right" :model="formValidate" :rules="ruleValidate">
      <FormItem label="appId" prop="appId">
        <Input class="w200" v-model="formValidate.appId" />
      </FormItem>
      <FormItem label="certPath" prop="certPath">
        <Input  v-model="formValidate.certPath" />
      </FormItem>
      <FormItem label="alipayPublicCertPath" prop="alipayPublicCertPath">
        <Input v-model="formValidate.alipayPublicCertPath" />
      </FormItem>
      <FormItem label="privateKey" class="label-item" prop="privateKey">
        <Input v-model="formValidate.privateKey" />
      </FormItem>
      <FormItem label="rootCertPath" prop="rootCertPath">
        <Input v-model="formValidate.rootCertPath" />
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
      formValidate: { // 表单数据
        accessKeyId: "",
        accessKeySecret: "",
        bucketName: "",
        picLocation: "",
        endPoint: "",
      },
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
        this.ruleValidate[item] = [
          {
            required: true,
            message: "请填写必填项",
            trigger: "blur",
          },
        ];
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
