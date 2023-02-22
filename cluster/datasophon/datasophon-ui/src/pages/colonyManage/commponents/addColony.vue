<!--
 * @Author: mjzhu
 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-06-15 17:06:20
 * @FilePath: \ddh-ui\src\pages\colonyManage\commponents\addColony.vue
-->
<template>
  <div style="padding-top: 20px">
    <a-form
      :label-col="labelCol"
      :wrapper-col="wrapperCol"
      :form="form"
      class="p0-32-10-32 form-content"
    >
      <a-form-item label="集群名称">
        <a-input
          id="error"
          v-decorator="[
            'clusterName',
            { rules: [{ required: true, message: '集群名称不能为空!' }] },
          ]"
          placeholder="请输入集群名称"
        />
      </a-form-item>
      <a-form-item label="集群编码">
        <a-input
          id="error"
          v-decorator="[
            'clusterCode',
            { rules: [{ required: true, message: '集群编码不能为空!' }] },
          ]"
          placeholder="请输入集群编码"
        />
      </a-form-item>
      <a-form-item label="集群框架">
          <a-select v-decorator="['clusterFrame', { rules: [{ required: true, message: '集群框架不能为空!' }]}]"  placeholder="请选择集群框架">
               <a-select-option :value="item.frameCode" v-for="(item,index) in frameList" :key="index">{{item.frameCode}}</a-select-option>
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
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
      frameList: [] //集群框架列表
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
        console.log(values);
        if (!err) {
          const params = {
            "clusterName": values.clusterName, 
            "clusterCode": values.clusterCode, 
            "clusterFrame": values.clusterFrame
          }
          if (JSON.stringify(this.detail) !== '{}') params.id = this.detail.id
          this.loading = true;
          const ajaxApi = JSON.stringify(this.detail) !== '{}' ? global.API.updateColony : global.API.saveColony
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
    getFrameList() {
      this.$axiosPost(global.API.getFrameList, {}).then((res) => {
        if (res.code === 200) {
          this.frameList = res.data
          if (JSON.stringify(this.detail) !== '{}') {
            this.form.getFieldsValue(['clusterName', 'clusterFrame', 'clusterCode'])
            this.form.setFieldsValue({
              clusterName:this.detail.clusterName,
              clusterFrame: this.detail.clusterFrame,
              clusterCode: this.detail.clusterCode
            })
          }
        }
      })
    }
  },
  mounted() {
    this.getFrameList()
  },
};
</script>
<style lang="less" scoped>
</style>
