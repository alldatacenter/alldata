<!--
 * @Author: mjzhu
 * @Date: 2022-06-08 11:38:30
 * @LastEditTime: 2022-07-14 16:22:01
 * @FilePath: \ddh-ui\src\pages\alarmManage\commponents\addMetric.vue
-->
<template>
  <div style="padding-top: 20px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="p0-32-10-32 form-content">
      <a-form-item label="告警指标名称">
        <a-input v-decorator="[
            'alertQuotaName',
            { rules: [{ required: true, message: '告警指标名称不能为空!' }] },
          ]" placeholder="请输入告警指标名称" />
      </a-form-item>
      <a-form-item label="指标表达式">
        <a-input v-decorator="[
            'alertExpr',
            { rules: [{ required: true, message: '指标表达式不能为空!' }] },
          ]" placeholder="请输入指标表达式" />
      </a-form-item>
      <!-- <a-form-item label="告警组类别">
        <a-select v-decorator="['serviceCategory', { rules: [{ required: true, message: '告警组类别不能为空!' }]}]" placeholder="请选择告警组类别">
          <a-select-option :value="item.frameCode" v-for="(item,index) in frameList" :key="index">{{item.frameCode}}</a-select-option>
        </a-select>
      </a-form-item>-->
      <a-form-item label="比较方式">
        <a-select v-decorator="['compareMethod', { rules: [{ required: true, message: '比较方式不能为空!' }]}]" placeholder="请选择比较方式">
          <a-select-option :value="item.value" v-for="(item,index) in compareMethodList" :key="index">{{item.label}}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="告警阀值">
        <a-input v-decorator="[
            'alertThreshold',
            { rules: [{ required: true, message: '告警阀值不能为空!' }] },
          ]" placeholder="请输入告警阀值" />
      </a-form-item>
      <a-form-item label="告警级别">
        <a-select v-decorator="['alertLevel', { rules: [{ required: true, message: '告警级别不能为空!' }]}]" placeholder="请选择告警级别">
          <a-select-option :value="item.value" v-for="(item,index) in alertLevelList" :key="index">{{item.label}}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="告警组">
        <a-select v-decorator="['alertGroupId', { rules: [{ required: true, message: '告警组不能为空!' }]}]" placeholder="请选择告警组" @change="getRoleList">
          <a-select-option :value="item.id" v-for="(item,index) in groupList" :key="index">{{item.alertGroupName}}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="绑定角色">
        <a-select v-decorator="['serviceRoleName', { rules: [{ required: true, message: '绑定角色不能为空!' }]}]" placeholder="请选择绑定角色">
          <a-select-option :value="item.serviceRoleName" v-for="(item,index) in roleList" :key="index">{{item.serviceRoleName}}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="通知组">
        <a-select v-decorator="['noticeGroupId', { rules: [{ required: true, message: '通知组不能为空!' }]}]" placeholder="请选择通知组">
          <a-select-option :value="item.value" v-for="(item,index) in noticeList" :key="index">{{item.label}}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="告警策略">
        <a-radio-group v-decorator="['alertTactic', { rules: [{ required: true, message: '告警策略不能为空!' }]}]" placeholder="请选择告警策略">
          <a-radio :value="1">单次</a-radio>
          <a-radio :value="2">连续</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="间隔时长(分钟)">
        <a-input v-decorator="[
            'intervalDuration',
            { rules: [{ required: true, message: '间隔时长(分钟)不能为空!' }] },
          ]" placeholder="请输入间隔时长(分钟)" />
      </a-form-item>
      <a-form-item label="触发时长(秒)">
        <a-input v-decorator="[
            'triggerDuration',
            { rules: [{ required: true, message: '触发时长(秒)不能为空!' }] },
          ]" placeholder="请输入触发时长(秒)" />
      </a-form-item>
      <a-form-item label="告警建议">
        <a-input type="textarea" v-decorator="[
            'alertAdvice',
            { rules: [{ required: true, message: '告警建议不能为空!' }] },
          ]" placeholder="请输入告警建议" />
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
      compareMethodList: [
        {
          label: "!=",
          value: "!=",
        },
        {
          label: ">",
          value: ">",
        },
        {
          label: "<",
          value: "<",
        },
      ],
      alertTacticList: [
        {
          label: "单次",
          value: 1,
        },
        {
          label: "持续",
          value: 2,
        },
      ],
      alertLevelList: [
        {
          label: "警告",
          value: "warning",
        },
        {
          label: "异常",
          value: "exception",
        },
      ],
      noticeList: [
        {
          label: "数据开发组",
          value: 1,
        },
      ],
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      groupList: [],
      roleList: [],
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
      frameList: [], //告警组类别列表
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
            ...values,
          };
          if (JSON.stringify(this.detail) !== "{}") params.id = this.detail.id;
          this.loading = true;
          const ajaxApi =
            JSON.stringify(this.detail) !== "{}"
              ? global.API.updateMetric
              : global.API.saveMetric;
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
    getAlarmGroupList() {
      const params = {
        pageSize: 1000,
        page: 1,
        clusterId: this.clusterId || "",
      };
      this.$axiosPost(global.API.getAlarmGroupList, params).then((res) => {
        this.groupList = res.data;
        if (JSON.stringify(this.detail) !== "{}") {
          this.form.getFieldsValue([
            "alertQuotaName",
            "alertExpr",
            "compareMethod",
            "alertThreshold",
            "alertGroupId",
            "noticeGroupId",
            "alertTactic",
            "intervalDuration",
            "alertAdvice",
            "alertLevel",
            "triggerDuration",
          ]);
          this.form.setFieldsValue({
            alertQuotaName: this.detail.alertQuotaName,
            alertExpr: this.detail.alertExpr,
            compareMethod: this.detail.compareMethod,
            alertThreshold: this.detail.alertThreshold,
            alertGroupId: this.detail.alertGroupId,
            noticeGroupId: this.detail.noticeGroupId,
            alertTactic: this.detail.alertTactic,
            intervalDuration: this.detail.intervalDuration,
            alertAdvice: this.detail.alertAdvice,
            alertLevel: this.detail.alertLevel,
            triggerDuration: this.detail.triggerDuration,
          });
          if (this.detail.alertGroupId) {
            this.getRoleList(this.detail.alertGroupId);
          }
        } else {
          this.form.getFieldsValue(["alertTactic"]);
          this.form.setFieldsValue({
            alertTactic: 1,
          });
        }
      });
    },
    getRoleList(e) {
      this.roleList = [];
      this.form.getFieldsValue(["serviceRoleName"]);
      this.form.setFieldsValue({
        serviceRoleName: undefined,
      });
      const params = {
        alertGroupId: e,
        clusterId: this.clusterId || "",
      };
      this.$axiosPost(global.API.getAlarmRole, params).then((res) => {
        this.roleList = res.data;
        if (JSON.stringify(this.detail) !== "{}" && this.detail.alertGroupId) {
          this.form.getFieldsValue(["serviceRoleName"]);
          this.form.setFieldsValue({
            serviceRoleName: this.detail.serviceRoleName || undefined,
          });
        }
      });
    },
  },
  mounted() {
    this.getAlarmGroupList();
  },
};
</script>
<style lang="less" scoped>
</style>
