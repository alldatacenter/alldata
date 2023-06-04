<!--
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-06-23 15:24:29
 * @LastEditTime: 2022-10-25 20:09:13
 * @FilePath: \ddh-ui\src\components\menu\serviceOption.vue
-->
<template>
  <div @click.stop>
    <a-popover trigger="hover" placement="rightTop" class="popover-service" overlayClassName="popover-service" :content="()=> getMoreOptions()">
      <a-icon type="more" class="cluster-more" style="top: -28px" />
    </a-popover>
    <!-- 配置集群的modal -->
    <a-modal v-if="visible" title :visible="visible" class="service-option-modal" :maskClosable="false" :closable="false" :width="1576" :confirm-loading="confirmLoading" @cancel="handleCancel" :footer="null">
      <Steps :clusterId="clusterId" stepsType="addService" />
    </a-modal>
  </div>
</template>
<script>
import Steps from "@/components/steps";
import { mapMutations, mapState } from 'vuex'

export default {
  provide() {
    return {
      handleCancel: this.handleCancel,
      onSearch: () => {},
    };
  },
  components: { Steps },
  data() {
    return {
      visible: false,
      confirmLoading: false,
      clusterId: Number(localStorage.getItem("clusterId") || -1),
    };
  },
  computed: {
    ...mapState({
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    handleCancel(e) {
      this.visible = false;
    },
    getMoreOptions() {
      let arr = [
        { name: "添加服务", key: "addService" },
        { name: "启动所有", key: "startAll" },
        { name: "停止所有", key: "stopAll" },
        { name: "重启所有需要重启的服务", key: "restartAll" },
      ];
      return arr.map((item, index) => {
        return (
          <div key={index}>
            <a
              class="more-menu-btn"
              style="border-width:0px;min-width:100px;"
              onClick={() => this.optionService(item)}
            >
              {item.name}
            </a>
          </div>
        );
      });
    },
    optionService(item) {
      if (item.key === "addService") {
        this.addService();
      } else {
        this.optServices(item);
      }
    },
    // 添加服务
    addService() {
      this.visible = true;
    },
    optServices(item) {
      this.$confirm({
        width: 450,
        title: () => {
          return (
            <div style="font-size: 22px;">
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content: (
          <div style="margin-top:20px">
            <div style="padding:0 65px;font-size: 16px;color: #555555;">
              {'确认' + (item.key=='startAll'?'启动所有':item.key=='stopAll'?'停止所有':item.key=='restartAll'?'重启所有需要重启的服务':"") +'吗？'}
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.openServices(item)}
              >
                确定
              </a-button>
              <a-button
                style="margin-right:10px;"
                onClick={() => this.$destroyAll()}
              >
                取消
              </a-button>
            </div>
          </div>
        ),
        icon: () => {
          return <div />;
        },
        closable: true,
      });
    
    },
    openServices(item) {
      let params = {
        clusterId: this.setting.clusterId,
        commandType: item.key === "stopAll" ? "STOP_SERVICE" : item.key === "startAll" ? "START_SERVICE" : "RESTART_SERVICE",
        serviceInstanceIds: "",
      };
      let serviceInstanceIds = [];
      const menuData = JSON.parse(localStorage.getItem("menuData")) || [];
      const arr =
        menuData.filter((item) => item.path === "service-manage") || [];
      if (arr.length > 0) {
        arr[0].children.map((child) => {
          if (item.key === "restartAll") {
            if (child.meta.obj.needRestart) {
              serviceInstanceIds.push(child.meta.obj.id);
            }
          } else {
            serviceInstanceIds.push(child.meta.obj.id);
          }
        });
      }
      params.serviceInstanceIds = serviceInstanceIds.join(",");
      this.$axiosPost(global.API.generateServiceCommand, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          // todo: 打开头部那个setting栏
          this.$destroyAll()
          this.showClusterSetting(true)
        }
      });
    },
  },
};
</script>
<style lang="less" scoped>
.popover-service {
  // margin-left: 31px;
  .more-menu-btn {
    font-size: 14px;
    color: #555555;
    letter-spacing: 0.39px;
    line-height: 32px;
    font-weight: 400;
    &:hover {
      color: @primary-color;
    }
  }
  /deep/ .ant-popover-inner-content {
    text-align: left;
    padding: 12px 16px;
  }
}
.service-option-modal {
  /deep/ .ant-modal {
    top: 61px;
    .ant-modal-body {
      padding: 0;
    }
  }
  /deep/ .ant-modal-content {
    border-radius: 4px;
  }
}
</style>