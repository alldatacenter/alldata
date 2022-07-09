<template>
  <div class="layout">
    <Form ref="formValidate" :label-width="150" label-position="right" :model="formValidate" :rules="ruleValidate">
      <FormItem label="ebusinessID" prop="ebusinessID">
        <Input v-model="formValidate.ebusinessID" />
      </FormItem>
      <FormItem label="appKey" prop="appKey">
        <Input class="label-appkey" v-model="formValidate.appKey" />
      </FormItem>
      <FormItem label="reqURL" prop="reqURL">
        <Input v-model="formValidate.reqURL" />
      </FormItem>
      <div class="label-btns">
        <Button type="primary" @click="submit('formValidate')">保存</Button>

      </div>
    </Form>
  </div>
</template>
<script>
import { setSetting } from "@/api/index";
import { handleSubmit } from "./validate";
export default {
  data() {
    return {
      ruleValidate: {}, // 验证规则
      formValidate: { ebusinessID: "", reqURL: "", appKey: "" }, // 表单数据
    };
  },
  props: ["res",'type'],
  created() {
    this.init();
  },
  methods: {
    // 验证
    submit(name) {
      let that = this;
       if( handleSubmit(that, name )){
        this.setupSetting()
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
@import "./style.scss";
.label-item {
  display: flex;
}
/deep/ .ivu-input {
  width: 300px !important;
  margin: 0 10px;
}
.ivu-input-wrapper {
  width: 300px;
  margin-right: 10px;
}
</style>
