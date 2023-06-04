
<template>
  <div style="padding-top: 20px">
    <a-form
      :label-col="labelCol"
      :wrapper-col="wrapperCol"
      :form="form"
      class="p0-32-10-32 form-content"
    >
      <a-form-item label="角色组名称">
        <a-input
          id="error"
          v-decorator="[
            'roleGroupName',
            { rules: [{ required: true, message: '角色组名称不能为空!' }] },
          ]"
          placeholder="请输入角色组名称"
        />
      </a-form-item>
    </a-form>
    <div class="ant-modal-confirm-btns-new">
      <a-button
        style="margin-right: 10px"
        type="primary"
        @click.stop="handleSubmit"
        :loading="loading"
        >确认</a-button
      >
      <a-button @click.stop="formCancel">取消</a-button>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    grouopObj:{
      type:Object,
      default: function () {
        return {};
      },
    },
    callBack:Function
  },
  data() {
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 5 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 19 },
      },
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
      cateList: [], //类型
      GroupList:[]  //列表
    };
  },
  watch: {},
  methods: {
    formCancel() {
      this.$destroyAll();
    },
    handleSubmit(e) {
      const _this = this
      e.preventDefault();
      this.form.validateFields((err, values) => {
        if (!err) {
          const params = {
            "roleGroupName": values.roleGroupName, 
            "roleGroupId": this.grouopObj.id,
          }
          this.loading = true;
          this.$axiosPost(global.API.reNameGroup, params).then((res) => {  
            this.loading = false;
            if (res.code !== 200) return
            this.$message.success('修改成功')
            this.$destroyAll();
            _this.callBack(params);
          }).catch((err) => {});
        }
      });
    },
    initData () {
      if (JSON.stringify(this.grouopObj) !== "{}") {
        this.form.getFieldsValue([
          "roleGroupName"
        ]);
        this.form.setFieldsValue({
          roleGroupName: this.grouopObj.roleGroupName
        });
      } 
    }
  },
  mounted() {
    this.initData()
  },
};
</script>
<style lang="less" scoped>
</style>
