<!--
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-07-15 15:04:12
 * @LastEditTime: 2022-07-15 15:51:30
 * @FilePath: \ddh-ui\src\pages\hostManage\distributionRack.vue
-->

<template>
  <div style="padding-top: 20px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="p0-32-10-32 form-content">
      <a-form-item label="机架">
        <a-select v-decorator="['rack', { rules: [{ required: true, message: '机架不能为空!' }]}]" placeholder="请选择机架">
          <a-select-option :value="item.rack" v-for="(item,index) in frameList" :key="index">{{item.rack}}</a-select-option>
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
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      frameList: [], //集群框架列表
    };
  },
  watch: {},
  methods: {
    formCancel() {
      this.$destroyAll();
    },
    handleSubmit(e) {
      const _this = this;
      e.preventDefault();
      this.form.validateFields((err, values) => {
        if (!err) {
          const params = {
            rack: values.rack,
            id: this.clusterId,
          };
          this.loading = true;
          const ajaxApi = global.API.updateRack;
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
    getRack() {
      this.$axiosPost(global.API.getRack, {}).then((res) => {
        if (res.code === 200) {
          this.frameList = res.data;
        }
      });
    },
  },
  mounted() {
    this.getRack();
  },
};
</script>
<style lang="less" scoped>
</style>
