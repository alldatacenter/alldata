<!--
 * @Author: mjzhu
 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-11-25 17:00:35
 * @FilePath: \ddh-ui\src\pages\hostManage\addLabel.vue
-->
<template>
  <div style="padding-top: 20px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="p0-32-10-32 form-content">
      <a-form-item v-if="type ==='add'" label="标签名称">
        <a-input id="error" v-decorator="[
            'nodeLabel',
            { rules: [{ required: true, message: '标签名称不能为空!' }, { validator: checkName }] },
          ]" placeholder="请输入标签名称" />
      </a-form-item>
      <a-form-item v-if="type !== 'add'" label="标签">
        <a-select v-decorator="['nodeLabelId', { rules: [{ required: true, message: '标签不能为空!' }]}]" placeholder="请选择标签">
          <a-select-option :value="item.id" v-for="(item,index) in cateList" :key="index">{{item.nodeLabel}}</a-select-option>
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
    type: String,
    hostIds: {
        type: Array,
        default: () => {
            return []
        }
    },
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
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
      cateList: [], //告警组类别列表
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
        if (!err) {
          if (this.type === 'add') this.savelabel(values)
          if (this.type === 'del') this.delLabel(values)
          if (this.type === 'handLabel') this.handLabel(values)
        }
      });
    },
    handLabel(values) {
      const _this = this
      const params = {
        nodeLabelId: values.nodeLabelId,
        hostIds: this.hostIds
      };
      this.loading = true;
      const ajaxApi = global.API.assginLabel;
      this.$axiosPost(ajaxApi, params)
        .then((res) => {
          this.loading = false;
          if (res.code === 200) {
            this.$message.success("分配成功", 2);
            this.$destroyAll();
            _this.callBack();
          }
        })
        .catch((err) => {});
    },
    delLabel(values) {
      const params = {
        nodeLabelId: values.nodeLabelId,
      };
      this.loading = true;
      const ajaxApi = global.API.deleteLabel;
      this.$axiosPost(ajaxApi, params)
        .then((res) => {
          this.loading = false;
          if (res.code === 200) {
            this.$message.success("删除成功", 2);
            this.$destroyAll();
            // _this.callBack();
          }
        })
        .catch((err) => {});
    },
    savelabel(values) {
      const params = {
        nodeLabel: values.nodeLabel,
        clusterId: this.clusterId,
      };
      this.loading = true;
      const ajaxApi = global.API.saveLabel;
      this.$axiosPost(ajaxApi, params)
        .then((res) => {
          this.loading = false;
          if (res.code === 200) {
            this.$message.success("保存成功", 2);
            this.$destroyAll();
            // _this.callBack();
          }
        })
        .catch((err) => {});
    },
    getLabelList() {
      this.$axiosPost(global.API.getLabelList, {
        clusterId: this.clusterId,
      }).then((res) => {
        if (res.code === 200) {
          this.cateList = res.data;
        }
      });
    },
  },
  mounted() {
    this.type !== "add" && this.getLabelList();
  },
};
</script>
<style lang="less" scoped>
</style>
