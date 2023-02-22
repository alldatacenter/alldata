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
      <a-form-item label="主用户组">
        <a-select showSearch allowClear 
          v-decorator="['mainGroupId', {rules: [{ required: true, message: '用户组不能为空' }]}]" placeholder="请选择用户组"  >
          <a-select-option v-for="list in groupList" :key="list.id" :value="list.id">  {{list.groupName}} </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="附属用户组">
        <a-select showSearch allowClear mode="multiple"
          v-decorator="['usergroup', {rules: [{ required: false }]}]" placeholder="请选择用户组"  >
          <a-select-option v-for="list in groupList" :key="list.id" :value="list.id">  {{list.groupName}} </a-select-option>
        </a-select>
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
      groupList:[],
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
    getGroupList() {
      this.loading = true;
      const params = {
        pageSize: 1000,
        page: 1,
        groupName: "",
      };
      this.$axiosPost('/ddh/cluster/group/list', params).then((res) => {
        this.loading = false;
        this.groupList = res.data;
      });
    },
    handleSubmit(e) {
      const _this = this;
      e.preventDefault();
      this.form.validateFields((err, values) => {
        console.log(values);
        if (!err) {
          debugger
          const params = {
            clusterId:localStorage.getItem("clusterId"),
            username: values.username,
            mainGroupId: values.mainGroupId,
            otherGroupIds: values.usergroup == null ?"" :values.usergroup.join(',')
          };
          this.loading = true;
          this.$axiosPost('/ddh/cluster/user/create', params)
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
  created(){
    this.getGroupList();
  }
};
</script>
<style lang="less" scoped>
</style>
