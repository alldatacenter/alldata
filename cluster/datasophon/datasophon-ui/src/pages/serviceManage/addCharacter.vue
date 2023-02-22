
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
            'characterGroupName',
            { rules: [{ required: true, message: '角色组名称不能为空!' }] },
          ]"
          placeholder="请输入角色组名称"
        />
      </a-form-item>
      <!-- <a-form-item label="角色类型">
           <a-select v-decorator="['characterGroupCategory', { rules: [{ required: true, message: '角色类型不能为空!' }]}]"  placeholder="请选择告角色类型">
               <a-select-option :value="item.serviceRoleName" v-for="(item,index) in cateList" :key="index">{{item.serviceRoleName}}</a-select-option>
          </a-select>
      </a-form-item> -->
       <a-form-item label="角色组列表">
           <a-select v-decorator="['characterGroupId', { rules: [{ required: true, message: '角色组列表不能为空!' }]}]"  placeholder="请选择告角色组列表">
               <a-select-option :value="item.id" v-for="(item,index) in GroupList" :key="index">{{item.roleGroupName}}</a-select-option>
          </a-select>
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
    serviceId: {
      type: Object,
      default: function () {
        return {};
      },
    },
    edit:{
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
            "roleGroupName": values.characterGroupName, 
            "roleGroupId": values.characterGroupId,
            serviceInstanceId: this.serviceId.id
          }
          this.loading = true;
          this.$axiosPost(global.API.addRoleGroupSave, params).then((res) => {  
            this.loading = false;
            if (res.code !== 200) return
            this.$message.success('保存角色组成功')
            this.$destroyAll();
            _this.callBack();
          }).catch((err) => {});
        }
      });
    },
    getServiceRoleType() {
      const params={
        serviceInstanceId :this.serviceId.id
      }
      //角色组类型
      // this.$axiosPost(global.API.getServiceRoleType, params).then((res) => {
      //   if (res.code !== 200) return
      //   this.cateList = res.data
      // }
      // ) 
      //角色组列表
      this.$axiosPost(global.API.getRoleGroupList, params).then((res) => {
        if (res.code !== 200) return  //this.$message.error('获取角色组列表失败')
        this.GroupList = res.data
        
      })
    }
  },
  mounted() {
    this.getServiceRoleType()
  },
};
</script>
<style lang="less" scoped>
</style>
