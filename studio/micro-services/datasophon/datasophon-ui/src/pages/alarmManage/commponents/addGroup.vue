<!--
 * @Author: mjzhu
 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-07-13 18:35:19
 * @FilePath: \ddh-ui\src\pages\alarmManage\commponents\addGroup.vue
-->
<template>
  <div style="padding-top: 20px">
    <a-form
      :label-col="labelCol"
      :wrapper-col="wrapperCol" 
      :form="form"
      class="p0-32-10-32 form-content"
    >
      <a-form-item label="告警组名称">
        <a-input
          id="error"
          v-decorator="[
            'alertGroupName',
            { rules: [{ required: true, message: '告警组名称不能为空!' }] },
          ]"
          placeholder="请输入告警组名称"
        />
      </a-form-item>
      <a-form-item label="告警组类别">
          <a-select v-decorator="['alertGroupCategory', { rules: [{ required: true, message: '告警组类别不能为空!' }]}]"  placeholder="请选择告警组类别">
               <a-select-option :value="item.serviceName" v-for="(item,index) in cateList" :key="index">{{item.serviceName}}</a-select-option>
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
    detail: {
      type: Object,
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
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
      cateList: [] //告警组类别列表
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
            "alertGroupName": values.alertGroupName, 
            "alertGroupCategory": values.alertGroupCategory,
            clusterId: this.clusterId
          }
          if (JSON.stringify(this.detail) !== '{}') params.id = this.detail.id
          this.loading = true;
          const ajaxApi = JSON.stringify(this.detail) !== '{}' ? global.API.saveGroup : global.API.saveGroup
          this.$axiosJsonPost(ajaxApi, params).then((res) => {  
            this.loading = false;
            if (res.code === 200) {
              this.$message.success('保存成功', 2)
              this.$destroyAll();
              _this.callBack();
            }
          }).catch((err) => {});
        }
      });
    },
    getAlarmCate() {
      this.$axiosPost(global.API.getAlarmCate, {clusterId: this.clusterId}).then((res) => {
        if (res.code === 200) {
          this.cateList = res.data
          if (JSON.stringify(this.detail) !== '{}') {
            this.form.getFieldsValue(['alertGroupName', 'alertGroupCategory', 'clusterCode'])
            this.form.setFieldsValue({
              alertGroupName:this.detail.alertGroupName,
              alertGroupCategory: this.detail.alertGroupCategory,
              clusterCode: this.detail.clusterCode
            })
          }
        }
      })
    }
  },
  mounted() {
    this.getAlarmCate()
  },
};
</script>
<style lang="less" scoped>
</style>
