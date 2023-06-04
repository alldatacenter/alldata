<!--
 * @Author: mjzhu
 * @describe: step1-安装主机 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-06-20 14:45:22
 * @FilePath: \ddh-ui\src\components\steps\step1.vue
-->
<template>
  <div class="steps1 steps">
    <div class="steps-title">为集群安装主机</div>
    <div class="steps-tips mgt20 mgb16">
      <a-icon class="steps-tips-icon mgr5" type="exclamation-circle" />提示：使用IP或主机名输入主机列表，按逗号分隔或使用主机域批量添加主机，例如：10.3.144.[19-23]
    </div>
    <div class="form-content steps-body">
      <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form">
        <a-form-item label :label-col="labelCol1" :wrapper-col="wrapperCol1">
          <a-textarea v-decorator="[
            'hosts',
            {initialValue: steps1.hosts, rules: [{ required: true, message: '主机列表不能为空!' }] },
          ]" placeholder="请输入主机列表..." />
        </a-form-item>
        <a-form-item label="SSH用户名">
          <a-input v-decorator="[
            `sshUser`,
            { initialValue: steps1.sshUser,rules: [{ required: true, message: `SSH用户名不能为空!` }] },
          ]" placeholder="请输入SSH用户名" />
        </a-form-item>
        <a-form-item label="SSH端口">
          <a-input v-decorator="['sshPort', {initialValue: steps1.sshPort, rules: [{ required: true, message: 'SSH端口不能为空!' }] }]" placeholder="请输入SSH端口" />
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub"],
  props: {
    steps1: Object,
  },
  data() {
    return {
      autosize1: { minRows: 2, maxRows: 4 },
      labelCol: {
        xs: { span: 24 },
        sm: { span: 3 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 21 },
      },
      labelCol1: {
        xs: { span: 24 },
        sm: { span: 0 },
      },
      wrapperCol1: {
        xs: { span: 24 },
        sm: { span: 24 },
      },
      form: this.$form.createForm(this),
    };
  },
};
</script>
<style lang="less" scoped>
</style>