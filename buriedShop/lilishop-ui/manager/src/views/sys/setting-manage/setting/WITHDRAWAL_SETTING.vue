<template>
  <div class="layout">

    <Form ref="formValidate" :label-width="150" label-position="right" :model="formValidate" :rules="ruleValidate">
      <FormItem label="提现审核是否开启">
        <i-switch v-model="formValidate.apply" style="margin-top:7px;"><span slot="open">开</span>
          <span slot="close">关</span>
        </i-switch>

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
      formValidate: { // 表单数据
        apply: true,
      },

      switchTitle: "提现审核是否开启", // 切换title
    };
  },
  created() {
    this.init();
  },
  props: ["res", "type"],
  methods: {
    // 保存
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
    },
  },
};
</script>

<style lang="scss" scoped>
@import "./style.scss";
/deep/ .ivu-form-item-content{
  align-items: center;
  padding-bottom: 5px;
}
</style>
