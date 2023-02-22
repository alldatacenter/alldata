<!--
 * @Author: mjzhu
 * @Date: 2022-06-13 14:04:05
 * @LastEditTime: 2022-10-28 11:34:34
 * @FilePath: \ddh-ui\src\components\steps\index.vue
-->
<template>
  <div class="steps-container">
    <div class="lf">
        <a-steps direction="vertical" :current="currentSteps - 1">
        <a-step v-for= "(item) in stepsList" :key="item" :title="item"></a-step>
        <!-- <a-step title="安装主机"></a-step>
        <a-step title="主机环境校验" />
        <a-step title="主机Agent分发" />
        <a-step title="选择服务" />
        <a-step title="分配服务Master角色" />
        <a-step title="分配服务Worker与Client角色" />
        <a-step title="服务配置" />
        <a-step title="安装并启动服务" /> -->
      </a-steps>
    </div>
    <div class="rf">
      <StepsRf :currentSteps="currentSteps" :stepsType="stepsType" :interval="interval" :stepsList="stepsList" :serviceData="steps4Data" />
    </div>
  </div>
</template>
<script>
import StepsRf from './stpesRf.vue'
import { mapActions, mapState } from "vuex";

export default {
  name: "ConfigCluster",
  props: { 
    stepsType: {
      type: String,
      default: 'cluster',
    },
    steps4Data: Object,
    clusterId: Number
  },
  components: {StepsRf},
  provide () {
    return {
      currentStepsAdd: this.currentStepsAdd,
      currentStepsSub: this.currentStepsSub,
      clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId // 需要更换
    }
  },
  watch: {
    stepsType: {
      handler(val) {
        let list = ['安装主机', '主机环境校验', '主机Agent分发', '选择服务', '分配服务Master角色', '分配服务Worker与Client角色', '服务配置', '安装并启动服务' ]
        if (this.stepsType === 'hostManage') {
          list = list.splice(0, 3)
          this.currentSteps = 1
        }
        if (this.stepsType === 'addService') {
          // this.currentSteps = 4t
          this.interval = 3
        }
        if (this.stepsType === 'service-example') {
          this.interval = 4
        }
      },
      immediate: true
    }
  },
  mounted () {
    console.log(this.setting, 'setting', this.clusterId)
  },
  data() {
    return {
      interval: 0,
      currentSteps: 1,
    };
  },
  computed: {
    stepsList () {
      let list = ['安装主机', '主机环境校验', '主机Agent分发', '选择服务', '分配服务Master角色', '分配服务Worker与Client角色', '服务配置', '安装并启动服务' ]
      if (this.stepsType === 'hostManage')list =  list.splice(0, 3)
      if (this.stepsType === 'addService')list =  list.splice(3, list.length)
      if (this.stepsType === 'service-example')list =  list.splice(4, list.length)
      return list
    },
    ...mapState({
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
  },
  methods: {
    currentStepsAdd () {
      this.currentSteps ++
    },
    currentStepsSub () {
      this.currentSteps --
    }
  }
};
</script>
<style lang="less" scoped>
.steps-container {
  display: flex;
  height: 860px;
  .lf {
    width: 216px;
    border-right: 1px solid #e3e4e6;
    padding: 32px 20px;
    /deep/ .ant-steps-vertical > .ant-steps-item > .ant-steps-item-container > .ant-steps-item-tail {
      padding: 23px 0 0;
      left: 11px;
    }
    /deep/ .ant-steps-item {
      height: 80px;
      .ant-steps-item-title {
        line-height: 23px;
        font-size: 14px;
        color: #666666;
        letter-spacing: 0.39px;
        font-weight: 500;
      }
      .ant-steps-item-icon {
        width: 23px;
        height: 23px;
        line-height: 23px;
        // border-color: rgb(209,212,217);
        font-size: 12px;
        margin-right: 10px;
      }
    }
    // 等待还未到的
   /deep/ .ant-steps-item-wait {
      .ant-steps-item-icon {
        .ant-steps-icon {
          color: #666666;
          letter-spacing: 0;
          font-weight: 400;
        }
      }
    }
    /deep/ .ant-steps-item-active {
      .ant-steps-item-title {
        line-height: 23px;
        font-size: 14px;
        color: #111111;
        letter-spacing: 0.39px;
        font-weight: 600;
      }
    }
    // 灰色的线
    /deep/ .ant-steps-item-process > .ant-steps-item-container > .ant-steps-item-tail::after {
      background: #D1D4D9;
    }
  }
  .rf {
    flex: 1;
    padding: 32px 0 32px 30px;
  }
}
</style>