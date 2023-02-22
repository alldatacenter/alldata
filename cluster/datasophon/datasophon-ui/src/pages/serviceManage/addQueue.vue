<!--
 * @Author: mjzhu
 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-07-14 16:20:20
 * @FilePath: \ddh-ui\src\pages\serviceManage\addQueue.vue
-->
<template>
  <div style="padding-top: 20px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="p0-32-10-32 form-content">
      <a-form-item label="队列名称">
        <a-input id="error" v-decorator="[
            'queueName',
            { rules: [{ required: true, message: '队列名称不能为空!' }, { validator: checkName }] },
          ]" placeholder="请输入队列名称" />
      </a-form-item>
      <a-form-item label="最小资源数" style="margin-bottom: 0px" :required="true">
        <a-row type="flex" style="position: relative">
          <a-col :span="9">
            <a-form-item>
              <a-input v-decorator="[
                    `minCore`,
                    {
                    rules: [
                      {
                        required: true,
                        message: `最小内核数不能为空!`,
                      },
                      { validator: checkNumber }
                    ],
                  }
                  ]" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="2" style="text-align: right">Core</a-col>
          <a-col :span="2" style></a-col>
          <a-col :span="9">
            <a-form-item>
              <a-input v-decorator="[
                    `minMem`,
                    {
                    rules: [
                      {
                        required: true,
                        message: `最小内存数不能为空`,
                      },
                      { validator: checkNumber }
                    ],
                  }
                  ]" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="2" style="text-align: right">GB</a-col>
        </a-row>
      </a-form-item>
      <a-form-item label="最大资源数" style="margin-bottom: 0px" :required="true">
        <a-row type="flex" style="position: relative">
          <a-col :span="9">
            <a-form-item>
              <a-input v-decorator="[
                    `maxCore`,
                    {
                    rules: [
                      {
                        required: true,
                        message: `最大内核数不能为空!`,
                      },
                      { validator: checkNumber }
                    ],
                  }
                  ]" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="2" style="text-align: right">Core</a-col>
          <a-col :span="2" style></a-col>
          <a-col :span="9">
            <a-form-item>
              <a-input v-decorator="[
                    `maxMem`,
                    {
                    rules: [
                      {
                        required: true,
                        message: `最大内存数不能为空`,
                      },
                      { validator: checkNumber }
                    ],
                  }
                  ]" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="2" style="text-align: right">GB</a-col>
        </a-row>
      </a-form-item>
      <a-form-item label="最多同时运行应用数">
        <a-input id="error" v-decorator="[
            'appNum',
            { rules: [{ required: true, message: '最多同时运行应用数不能为空!' }, { validator: checkNumber }] },
          ]" placeholder="请输入最多同时运行应用数" />
      </a-form-item>
      <a-form-item label="资源分配策略">
        <a-select v-decorator="['schedulePolicy', { rules: [{ required: true, message: '资源分配策略不能为空!' }]}]" placeholder="请选择资源分配策略">
          <a-select-option :value="item.label" v-for="(item,index) in schedulePolicyList" :key="index">{{item.label}}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="权重">
        <a-input id="error" v-decorator="[
            'weight',
            { rules: [{ required: true, message: '权重不能为空!' }, { validator: checkNumber }] },
          ]" placeholder="请输入权重" />
      </a-form-item>
      <a-form-item label="队列中AM占用最大比例">
        <a-input id="error" v-decorator="[
            'amShare',
            { rules: [{ required: true, message: '队列中AM占用最大比例不能为空!' }, { validator: checkFloat }] },
          ]" placeholder="请输入队列中AM占用最大比例" />
      </a-form-item>
      <a-form-item label="是否允许队列抢占资源">
        <a-switch v-decorator="[`allowPreemption`, { valuePropName: 'checked' }]"></a-switch>
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
      schedulePolicyList: [
        {
          label: "fair",
        },
        {
          label: "fifo",
        },
        {
          label: "drf",
        },
      ],
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      groupList: [],
      roleList: [],
      labelCol: {
        xs: { span: 24 },
        sm: { span: 6 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 18 },
      },
      form: this.$form.createForm(this),
      value1: "",
      loading: false,
    };
  },
  watch: {},
  methods: {
    //
    checkNumber(rule, value, callback) {
      var reg = /^[1-9]\d*$/;
      if (!reg.test(value) && value) {
        callback(
          new Error("请输入正整数")
        );
      }
      callback();
    },

    checkFloat (rule, value, callback) {
      var reg = /^(([0-9])|([0-9]([0-9]+)))(.[0-9]+)?$/;
      if (!reg.test(value) && value) {
        callback(
          new Error("请输入正数")
        );
      }
      if (Number(value) === 0) {
        callback(
          new Error("请输入正数")
        );
      }
      callback();
    },
    checkName(rule, value, callback) {
      var reg = /^(?!_)(?!.*?_$)[a-zA-Z0-9_]+$/;
      if (!reg.test(value) && value) {
        callback(
          new Error("名称只能是数字、字母、下划线且不能以下划线开头和结尾")
        );
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
            ...values,
            clusterId: this.clusterId,
            allowPreemption: values.allowPreemption ? 1 : 2,
          };
          if (JSON.stringify(this.detail) !== "{}") params.id = this.detail.id;
          this.loading = true;
          const ajaxApi =
            JSON.stringify(this.detail) !== "{}"
              ? global.API.updateQueue
              : global.API.saveQueue;
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
    initData() {
      if (JSON.stringify(this.detail) !== "{}") {
        this.form.getFieldsValue([
          "queueName",
          "minCore",
          "minMem",
          "maxCore",
          "maxMem",
          "appNum",
          "schedulePolicy",
          "allowPreemption",
          "amShare",
          "weight",
        ]);
        this.form.setFieldsValue({
          queueName: this.detail.queueName,
          minCore: this.detail.minCore,
          minMem: this.detail.minMem,
          maxCore: this.detail.maxCore,
          maxMem: this.detail.maxMem,
          appNum: this.detail.appNum,
          schedulePolicy: this.detail.schedulePolicy,
          amShare: this.detail.amShare,
          weight: this.detail.weight,
          allowPreemption: this.detail.allowPreemption === 1,
        });
      } else {
        this.form.getFieldsValue([
          "amShare",
          "weight",
          "schedulePolicy",
          "appNum",
          "allowPreemption",
        ]);
        this.form.setFieldsValue({
          amShare: 0.1,
          weight: 1,
          schedulePolicy: this.schedulePolicyList[0].label,
          appNum: 50,
          allowPreemption: false,
        });
      }
    },
  },
  mounted() {
    this.initData();
  },
};
</script>
<style lang="less" scoped>
</style>
