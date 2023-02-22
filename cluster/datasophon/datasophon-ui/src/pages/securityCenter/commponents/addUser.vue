<!--
 * @Author: mjzhu
 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-07-14 15:12:08
 * @FilePath: \ddh-ui\src\pages\securityCenter\commponents\addUser.vue
-->
<template>
  <div style="padding-top: 10px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="p0-32-10-32 form-content">
      <a-form-item label="用户名称">
        <a-input v-decorator="[
            'username',
            { rules: [{ required: true, message: '用户名称不能为空!' }, { validator: checkName }] },
          ]" placeholder="请输入用户名称" />
      </a-form-item>
      <a-form-item label="用户密码">
        <a-input type="password" :disabled="editFlag" v-decorator="['password',{ rules: [{ required: true, message: '用户密码不能为空!' }] }]" placeholder="请输入用户密码" />
      </a-form-item>
      <a-form-item label="邮    箱">
        <a-input v-decorator="['email',{ rules: [{ required: true, message: '邮箱不能为空!' },{pattern: new RegExp(/\w{3,}(\.\w+)*@[A-z0-9]+(\.[A-z]{2,5}){1,2}/), message: '请输入正确的邮箱地址'}] }]" placeholder="请输入邮箱" />
      </a-form-item>
      <a-form-item label="手机号码">
        <a-input v-decorator="['phone',{ rules: [{ required: true, message: '手机号码不能为空!' }] }]" placeholder="请输入手机号码" />
      </a-form-item>
    </a-form>
    <div class="ant-modal-confirm-btns-new">
      <a-button style="margin-right: 10px" type="primary" @click.stop="handleSubmit" :loading="loading">确认</a-button>
      <a-button @click.stop="formCancel">取消</a-button>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    detail: {
      type: Object,
      default: function () {
        return {};
      },
    },
    callBack: Function,
  },
  data() {
    return {
      editFlag: false,
      labelCol: {
        xs: { span: 24 },
        sm: { span: 6 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 18 },
      },
      form: this.$form.createForm(this),
      loading: false,
    };
  },
  watch: {},
  methods: {
    checkName(rule, value, callback) {
      var reg = /[\u4E00-\u9FA5]|[\uFE30-\uFFA0]/g;
      if (reg.test(value)) {
        callback(new Error("名称中不能包含中文"));
      }
      if (/\s/g.test(value)) {
        callback(new Error("名称中不能包含空格"));
      }
      callback();
    },
    formCancel() {
      this.$destroyAll();
    },
    handleSubmit(e) {
      const _this = this;
      e.preventDefault();
      this.form.validateFields((err, values) => {
        console.log(values);
        if (!err) {
          const params = {
            username: values.username,
            password: values.password,
            email: values.email,
            phone: values.phone,
          };
          if (JSON.stringify(this.detail) !== "{}") {
            params.id = this.detail.id;
            // delete params.password;
          }
          this.loading = true;
          const ajaxApi =
            JSON.stringify(this.detail) !== "{}"
              ? global.API.updateUser
              : global.API.addUser;
          this.$axiosJsonPost(ajaxApi, params)
            .then((res) => {
              this.loading = false;
              if (res.code === 200) {
                this.$message.success("保存成功", 2);
                this.$destroyAll();
                _this.callBack();
              }
            })
            .catch((err) => {});
        }
      });
    },
    echoUSer() {
      if (JSON.stringify(this.detail) !== "{}") {
      // this.editFlag = true;
        this.form.getFieldsValue(["username", "phone", "password", "email"]);
        this.form.setFieldsValue({
          username: this.detail.username,
          phone: this.detail.phone,
          //password: this.detail.password,
          password: '',
          email: this.detail.email,
        });
      } else {
        this.form.getFieldsValue(["username", "phone", "password", "email"]);
        this.form.setFieldsValue({
          username: '',
          phone: '',
          password: '',
          email: '',
        });
      }
    },
  },
  mounted() {
    this.echoUSer();
  },
};
</script>
<style lang="less" scoped>
</style>
