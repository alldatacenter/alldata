<!--
 * @Author: mjzhu
 * @describe: step6-服务配置
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-10-31 16:00:46
 * @FilePath: \ddh-ui\src\components\steps\step7.vue
-->
<template>
  <div class="steps7 steps">
    <div class="steps-title flex-bewteen-container">
      <span>服务配置</span>
    </div>
    <a-button class="btn-save" type="primary" @click="handleSubmit">保存</a-button>
    <a-spin :spinning="loading" style="position: relative;">
      <a-tabs v-model="serviceNameKey" @change="callback" style="max-width: 1330px; position: relative;">
        <a-tab-pane v-for="item in SERVICENAMES" :key="item" :tab="item" :forceRender="true">
          <!-- <div class="mgt16 steps-body">
            <CommonTemplate :ref="'CommonTemplateRef'+item" :steps4Data="steps4Data" :templateData="templateProps(item)" />
          </div>-->
        </a-tab-pane>
      </a-tabs>
      <div :class="['steps-body', serviceNameKey === item ?'steps-container': '']" v-for="item in SERVICENAMES" :key="item">
        <!-- :class="[serviceNameKey === item ?'steps-container show-template' : 'steps-container hide-template']" -->
        <CommonTemplate :ref="'CommonTemplateRef'+item" :class="[serviceNameKey === item ?'steps-container show-template' : 'steps-container hide-template', item+'warp']" :steps4Data="steps4Data" :templateData="templateProps(item)" />
      </div>
    </a-spin>
  </div>
</template>
<script>
import CommonTemplate from "@/components/commonTemplate/index";
import { mapActions, mapState } from "vuex";
import { de } from "date-fns/locale";

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
      // "ZOOKEEPER": [], "HDFS": [], "YARN": []
      templateObj: {},
      saveData: [],
      hostList: [],
      serviceNameKey: "",
      SERVICENAMES: [],
      selectKeys: [],
      // serviceContainerHeight: 0,
    };
  },
  watch: {
    serviceContainerHeight(val) {
      console.log(val);
    },
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
    serviceContainerHeight() {
      const className = this.serviceNameKey + "warp";
      const height = document.getElementsByClassName(className)[0];
      return height;
    },
  },
  methods: {
    templateProps(item) {
      return this.templateObj[item];
    },
    ...mapActions("steps", ["setCommandType", "setCommandIds"]),
    callback(key) {
      this.serviceNameKey = key;
      if (this.selectKeys.includes(key)) return false;
      this.selectKeys.push(key);
      // this.getServiceConfigOption();
    },
    // 去除字符串里面的数字
    deleteNum(str, key) {
      let reg = /[0-9]+/g;
      let str1 = str.replace(reg, "");
      let str2 = str1.replace(key, "");
      return str2;
    },
    handlearrayWithData(a) {
      let obj = {};
      let arr = [];
      for (var k in a) {
        if (k.includes("arrayWith")) {
          let key = "";
          if (k.includes("arrayWithKey")) {
            key = k.split("arrayWithKey")[0];
            arr.push(key);
          }
          if (k.includes("arrayWithVal")) {
            key = k.split("arrayWithVal")[0];
            arr.push(key);
          }
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      for (var f in obj) {
        let keys = [];
        let vals = [];
        for (var i in a) {
          if (i.includes(f)) {
            if (i.includes("arrayWithKey")) {
              keys.push(i);
            }
            if (i.includes("arrayWithVal")) {
              vals.push(i);
            }
          }
        }
        keys.map((item, index) => {
          obj[f].push({
            [`${a[item]}`]: a[vals[index]],
          });
        });
      }
      return obj;
    },
    handleMultipleData(a) {
      let obj = {};
      let arr = [];
      for (var k in a) {
        if (k.includes("multiple")) {
          let key = k.split("multiple")[0];
          arr.push(key);
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      // obj{ a: , b: }
      for (var f in obj) {
        let vals = [];
        for (var i in a) {
          if (i.includes(f)) {
            if (i.includes("multiple")) {
              vals.push(i);
            }
          }
        }
        vals.map((item, index) => {
          obj[f].push(a[vals[index]]);
        });
      }
      return obj;
    },
    // 单个标签页的保存
    handleSubmit() {
      this.templateData = this.templateObj[`${this.serviceNameKey}`];
      this.$refs[
        `CommonTemplateRef${this.serviceNameKey}`
      ][0].form.validateFields(async (err, values) => {
        if (!err) {
          let param = _.cloneDeep(this.templateData);
          const arrayWithData = this.handlearrayWithData(values);
          const multipleData = this.handleMultipleData(values);
          const formData = { ...values, ...arrayWithData, ...multipleData };
          for (var name in formData) {
            param.forEach((item) => {
              if (item.name === name) {
                item.value = formData[name];
              }
            });
          }
          param.forEach((item) => {
            item.name = item.name.replaceAll("!", ".");
          });
          let filterParam = param.filter(
            (item) => !(!item.required && item.hidden)
          );
          // 处理表单数据 将相同的key处理成数组
          let saveParam = {
            clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
            serviceName: this.serviceNameKey,
            serviceConfig: JSON.stringify(filterParam),
          };
          // // 等待网络请求结束
          let res = await this.$axiosPost(
            global.API.saveServiceConfig,
            saveParam
          );
          if (res.code === 200) {
            this.$message.success("保存成功");
          }
        }
      });
    },
    getServiceConfigOption() {
      this.loading = true;
      const self = this;
      this.SERVICENAMES.map((item) => {
        const params = {
          clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
          serviceName: item,
        };
        this.$axiosPost(global.API.getServiceConfigOption, params).then(
          (res) => {
            if (res.code === 200) {
              self.templateObj[item] = self.handlerTemplate(res.data);
              self.loading = false;
            }
            // self.templateData = this.handlerTemplate(res.data);
          }
        );
      });
    },
    handlerTemplate(data) {
      data.forEach((item) => {
        item.name = item.name.replaceAll(".", "!");
      });
      return data;
    },
    checkAllForm() {
      const self = this;
      let num = 0;
      for (var i = 0; i < self.SERVICENAMES.length; i++) {
        const item = self.SERVICENAMES[i];
        self.$refs[`CommonTemplateRef${item}`][0].form.validateFields(
          (err, values) => {
            if (err) {
              self.serviceNameKey = item;
              num++;
            }
          }
        );
        if (num > 0) break;
      }
      return num > 0;
    },
    submitAllServices(callback) {
      let promiseArr = [];
      this.SERVICENAMES.forEach((item) => {
        //todo 目前只有一个节点
        let p = null;
        p = new Promise((resolve) => {
          let serviceNameKey = item;
          this.templateData = this.templateObj[`${serviceNameKey}`];
          this.$refs[
            `CommonTemplateRef${serviceNameKey}`
          ][0].form.validateFields(async (err, values) => {
            if (!err) {
              let param = _.cloneDeep(this.templateData);
              const arrayWithData = this.handlearrayWithData(values);
              const multipleData = this.handleMultipleData(values);
              const formData = { ...values, ...arrayWithData, ...multipleData };
              for (var name in formData) {
                param.forEach((item) => {
                  if (item.name === name) {
                    item.value = formData[name];
                  }
                });
              }
              param.forEach((item) => {
                item.name = item.name.replaceAll("!", ".");
              });
              let filterParam = param.filter(
                (item) => !(!item.required && item.hidden)
              );
              // 处理表单数据 将相同的key处理成数组
              let saveParam = {
                clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
                serviceName: serviceNameKey,
                serviceConfig: JSON.stringify(filterParam),
              };
              // // 等待网络请求结束
              let res = await this.$axiosPost(
                global.API.saveServiceConfig,
                saveParam
              );
              resolve({ ...res, name: serviceNameKey });
            }
          });
        });
        if (p) promiseArr.push(p);
      });
      Promise.all(promiseArr).then(async (res) => {
        let num = 0;
        res.map((item) => {
          if (item.code !== 200) {
            this.$message.warnning(`${res.name}配置失败`);
            num++;
          }
        });
        if (num > 0) {
          let res = { code: 0 };
          callback(res)
          return false
        }
        let params = {
          clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
        };
        let a = false;
        if (a) {
          params.commandIds = this.steps.commandIds;
          params.commandType = this.steps.commandType;
          // 直接启动
          res = await this.$axiosPost(global.API.startExecuteCommand, params);
          if (callback) {
            callback(res);
          }
        } else {
          // 先调用生成指令再去启动
          params.serviceNames = this.SERVICENAMES;
          params.commandType = this.steps.commandType;
          let result = await this.$axiosPost(global.API.generateCommand, params);
          params.commandIds = result.data;
          this.setCommandIds(result.data);
          delete params.servicenames;
          res = await this.$axiosPost(global.API.startExecuteCommand, params);
          if (callback) {
            callback(res);
          }
        }
      });
    },
    //  从第七步进入第八步的请求
    async nextSteps(callback) {
      let res = { code: 0 };
      const flag = this.checkAllForm();
      if (flag && callback) {
        callback(res);
        return false;
      }
      // 如果所有的表单校验成功了 那么就把所有的tab页去保存一下
      this.submitAllServices(callback);
    },
  },
  created() {
    this.SERVICENAMES = this.steps4Data.serviceNames.map(
      (item) => item.serviceName
    );
    this.serviceNameKey = this.SERVICENAMES[0];
    this.selectKeys.push(this.serviceNameKey);
    this.SERVICENAMES.map((item) => {
      this.templateObj[`${item}`] = [];
    });
  },
  mounted() {
    this.getServiceConfigOption();
  },
};
</script>
<style lang="less" scoped>
/deep/ .ant-tabs {
  max-width: 1442px;
  .ant-tabs-bar {
    margin-right: 32px;
  }
}
.steps7 {
  /deep/ .ant-spin-container {
    position: relative;
  }
  .steps-body {
    // max-width: 1444px;
    position: absolute;
    top: 60px;
    left: 0;
    right: 0;
    bottom: 0;
    // overflow-y: hidden!important;;
    // max-height: 640px;
    height: 600px;
  }
  .steps-container {
    // max-height: 640px;
    // height: 600px;
    // overflow-y: hidden;
    z-index: 10;
  }
  .show-template {
    z-index: 1;
    opacity: 1;
  }
  .hide-template {
    z-index: 0;
    opacity: 0;
  }
  .btn-save {
    position: absolute;
    right: 32px;
    z-index: 1000;
  }
}
</style> 