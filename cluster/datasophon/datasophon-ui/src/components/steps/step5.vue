<!--
 * @Author: mjzhu
 * @describe: step5-分配服务Master角色 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:09
 * @FilePath: \ddh-ui\src\components\steps\step5.vue
-->
<template>
  <a-spin :spinning="loading" style="min-height: 600px">
    <div class="steps5 steps">
      <div class="steps-title flex-bewteen-container">
        <span>分配服务Master角色</span>
      </div>
      <div class="mgt16 steps-body">
        <CommonTemplate ref="commonTemplateRef" :steps4Data="steps4Data" :templateData="templateData" />
      </div>
    </div>
  </a-spin>
</template>
<script>
import CommonTemplate from "@/components/commonTemplate/index";

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  components: { CommonTemplate },
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      loading: false,
      templateData: [],
      saveData: [],
      hostList: [],
    };
  },
  methods: {
    // 去除字符串里面的数字
    deleteNum(str, key) {
      let reg = /[0-9]+/g;
      let str1 = str.replace(reg, "");
      let str2 = str1.replace(key, "");
      return str2;
    },
    handleSubmit(callback) {
      this.$refs.commonTemplateRef.form.validateFields(async (err, values) => {
        if (!err) {
          // 处理表单数据 将相同的key处理成数组
          let formData = {};
          let saveParam = [];
          for (var k in values) {
            const key = this.deleteNum(k, "multipleSelect");
            if (k.includes("multipleSelect")) {
              if (Object.prototype.hasOwnProperty.call(formData, key)) {
                formData[`${key}`].push(values[k]);
              } else {
                formData[`${key}`] = [values[k]];
              }
            } else {
              if (
                Object.prototype.toString.call(values[k]) === "[object Array]"
              ) {
                formData[`${k}`] = values[k];
              } else {
                formData[`${k}`] = [values[k]];
              }
            }
          }
          for (var label in formData) {
            saveParam.push({
              serviceRole: label,
              hosts: formData[label],
            });
            this.templateData.forEach((item) => {
              if (item.label === label) {
                item.value = formData[label];
              }
            });
          }
          // 等待网络请求结束
          let res = await this.$axiosJsonPost(
            global.API.saveServiceRoleHostMapping + `/${this.clusterId}`,
            saveParam
          );
          // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
          if (callback) {
            callback(res);
          }
        } else {
          if (callback) {
            callback({ code: 0 });
          }
        }
      });
    },
    getServiceRoleList() {
      const self = this;
      const params = {
        clusterId: this.clusterId,
        serviceIds: this.steps4Data.serviceIds.join(",") || "",
        serviceRoleType: 1, // 传1查的是Master角色
      };
      this.$axiosPost(global.API.getServiceRoleList, params).then((res) => {
        self.templateData = self.handlerData(res.data);
        self.loading = false;
      });
    },
    getAllHost() {
      this.loading = true;
      const params = {
        clusterId: this.clusterId,
      };
      this.$axiosPost(global.API.getAllHost, params).then((res) => {
        let arr = [];
        res.data.map((item) => {
          arr.push(item.hostname);
        });
        this.hostList = arr;
        this.getServiceRoleList();
      });
    },
    handlerData(data) {
      let arr = [];
      data.map((item) => {
        arr.push({
          label: item.serviceRoleName,
          name: item.serviceRoleName,
          value: item.hosts ? item.hosts : this.hostList.length > 1 ? this.hostList[0] : undefined,
          defaultValue: item.hosts ? item.hosts : this.hostList.length > 1 ? this.hostList[0] : undefined,
          selectValue: this.hostList,
          type: item.cardinality === "1" ? "select" : "multipleSelect",
          isHidden: false,
          required: item.serviceRoleType === "master",
        });
      });
      return arr;
    },
  },
  mounted() {
    this.getAllHost();
  },
};
</script>
<style lang="less" scoped>
</style>