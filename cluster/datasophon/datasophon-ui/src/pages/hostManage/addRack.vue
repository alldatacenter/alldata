<!--
 * @Author: mjzhu
 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-11-25 17:00:30
 * @FilePath: \ddh-ui\src\pages\hostManage\addRack.vue
-->
<template>
  <div style="padding-top: 20px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="p0-32-10-32 form-content">
      <a-form-item v-if="type ==='add'" label="机架名称">
        <a-input id="error" v-decorator="[
            'rack',
            { rules: [{ required: true, message: '机架名称不能为空!' }, { validator: checkName }]  },
          ]" placeholder="请输入机架名称" />
      </a-form-item>
      <a-form-item v-if="type !== 'add'" label="机架">
        <a-select v-decorator="['rack', { rules: [{ required: true, message: '机架不能为空!' }]}]" placeholder="请选择机架">
          <a-select-option :value="item.rack" v-for="(item,index) in cateList" :key="index">{{item.rack}}</a-select-option>
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
          if (this.type === 'add') this.saveRack(values)
          if (this.type === 'del') this.delRack(values)
          if (this.type === 'handRack') this.handRack(values)
        }
      });
    },
    handRack(values) {
      const _this = this
      const params = {
        rack: values.rack,
        hostIds: this.hostIds,
        clusterId: this.clusterId
      };
      this.loading = true;
      const ajaxApi = global.API.assginRack;
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
    delRack(values) {
      const params = {
        rackId: values.rack,
      };
      this.loading = true;
      const ajaxApi = global.API.deleteRack;
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
    saveRack(values) {
      const params = {
        rack: values.rack,
        clusterId: this.clusterId,
      };
      this.loading = true;
      const ajaxApi = global.API.saveRack;
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
    getRackList() {
      this.$axiosPost(global.API.getRackList, {
        clusterId: this.clusterId,
      }).then((res) => {
        if (res.code === 200) {
          this.cateList = res.data;
        }
      });
    },
  },
  mounted() {
    this.type !== "add" && this.getRackList();
  },
};
</script>
<style lang="less" scoped>
</style>
