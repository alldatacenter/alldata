<!--
 * @Author: mjzhu
 * @describe: 
 * @Date: 2022-06-20 20:34:13
 * @LastEditTime: 2022-10-25 20:20:25
 * @FilePath: \ddh-ui\src\components\menu\clusterMenu.vue
-->
<template>
  <a-menu class="cluster-menu" :mode="mode" :inlineCollapsed="collapsed" :theme="menuTheme" :defaultSelectedKeys="['overview']" :selectedKeys="selectedKeys" :openKeys="sOpenKeys" @click="handleClick" @openChange="openChange" :style="{'min-width': collapsed ? '50px' : '',}">
    <template v-for="(item) in options">
      <template v-if="!item.children.length">
        <a-menu-item :key="item.fullPath">
          <span v-if="collapsed">{{ item.name }}</span>
          <router-link :to="{ path: item.fullPath }">
            <span class="flex-container" v-if="!collapsed">
              <svg-icon :icon-class="item.meta.icon" class="collapsed-icon anticon" :style="{width: '14px',height: '14px',lineHeight: '0px'}" />
              {{ item.name}}
            </span>
          </router-link>
        </a-menu-item>
      </template>
      <template v-else>
        <a-sub-menu :key="item.fullPath">
          <span slot="title">
            <div class="flex-bewteen-container">
              <span class="flex-container">
                <svg-icon :icon-class="item.meta.icon" class="collapsed-icon anticon" :style="{width: '14px',height: '14px',lineHeight: '0px'}" />
                {{item.name}}
            </span>
            <div v-if="item.path === 'service-manage'">
              <serviceOption />
            </div>
            </div>
          </span>
          <a-menu-item v-for="(subItem) in item.children" :key="subItem.fullPath" style="padding-left: 24px" class="cluster-menu-subitem">
            <router-link :to="{ path: subItem.fullPath }">
              <div class="flex-bewteen-container cluster-menu-item">
                <div class="flex-container cluster-menu-item-left">
                  <span :class="['circle-point', 'mgr10', subItem.meta.obj? subItem.meta.obj.serviceStateCode === 1 ? 'hide-point' : subItem.meta.obj.serviceStateCode === 2 ? 'success-point': subItem.meta.obj.serviceStateCode === 3 ? 'configured-point': 'error-point' : '']"></span>
                  <span class="service-name" :style="getServiceClassNameStyle(subItem.meta.obj)" :title="subItem.label">{{subItem.label}}</span>
                </div>
                <div v-if="subItem.path.includes('service-list')" class="cluster-menu-item-right">
                  <!-- 告警 -->
                  <span v-if="subItem.meta.obj && [3,4].includes(subItem.meta.obj.serviceStateCode) && subItem.meta.obj.alertNum > 0" :class="[subItem.meta.obj ? subItem.meta.obj.serviceStateCode === 4 ? 'error-status-color': 'configured-status-color':'']" @click="showGj(subItem.meta.obj)">
                   <span v-show="alarmManageVisible">
                    <svg-icon class="icon-gj" icon-class="gaojing"></svg-icon>
                    {{subItem.meta.obj ? subItem.meta.obj.alertNum ? subItem.meta.obj.alertNum : 0 : 0}}
                   </span>
                  </span>
                  <!-- 重启 -->
                  <!-- 服务对比 -->
                  <a-icon v-if="subItem.meta.obj && subItem.meta.obj.needRestart" type="sync" class="menu-sub-icon" @click="textCompare" />
                  <!-- <a-icon  type="sync" class="menu-sub-icon" @click="textCompare" /> -->
                  <a-popover trigger="hover" placement="rightTop" class="popover-index" overlayClassName="popover-index" :content="()=> getMoreMenu(subItem)">
                    <a-icon type="more" class="cluster-more menu-sub-icon" />
                  </a-popover>
                </div>
              </div>
            </router-link>
          </a-menu-item>
        </a-sub-menu>
      </template>
    </template>
  </a-menu>
</template>

<script>
import fastEqual from "fast-deep-equal";
import serviceOption from './serviceOption.vue';
import _ from 'lodash';
import alarmModal from '@/components/alarmModal'
import TextCompare from './commponents/textCompare.vue'
import { mapMutations ,mapState} from 'vuex'
const toRoutesMap = (routes) => {
  const map = {};
  routes.forEach((route) => {
    map[route.fullPath] = route;
    if (route.children && route.children.length > 0) {
      const childrenMap = toRoutesMap(route.children);
      Object.assign(map, childrenMap);
    }
  });
  return map;
};
export default {
  components: { serviceOption },
  props: {
    options: {
      type: Array,
      required: true,
    },
    theme: {
      type: String,
      required: false,
      default: "dark",
    },
    mode: {
      type: String,
      required: false,
      default: "inline",
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false,
    },
    i18n: Object,
    openKeys: Array,
  },
  data() {
    return {
      selectedKeys: [],
      sOpenKeys: [],
      cachedOpenKeys: [],
    };
  },
  created() {
    this.updateMenu()
  },
  watch: {
    $route: function () {
      this.updateMenu();
    },
  },
  computed: {
    menuTheme() {
      return this.theme == "light" ? this.theme : "dark";
    },
    routesMap() {
      return toRoutesMap(this.options);
    },
    ...mapState('setting', ['alarmManageVisible', "clusterId"])
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting" ]),
    textCompare(){
      const self = this;
      let width = 1200;
      let title = "服务版本对比";
      let serviceId = {id:this.$route.params.serviceId || ""}
      let content = (
        <TextCompare  serviceId={serviceId} callBack={() => self.updateMenu()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    getServiceClassNameStyle (obj) {
      // 如果有重启的tubiao没有告警的图标
      if (obj && obj.needRestart && (![3,4].includes(obj.serviceStateCode) && obj.alertNum === 0)) {
        return {
          'max-width': '116px'
        }
      }
      // 如果没有重启的tubiao有告警的图标
      if (obj && !obj.needRestart && ([3,4].includes(obj.serviceStateCode) && obj.alertNum > 0)) {
        return {
          'max-width': '116px'
        }
      }
      // 如果有重启的tubiao有告警的图标
      if (obj && obj.needRestart && ([3,4].includes(obj.serviceStateCode) && obj.alertNum > 0)) {
        return {
          'max-width': '106px'
        }
      }
      // 如果没有重启的tubiao没有告警的图标
      return {
        'max-width': '126px'
      }
    },
    updateMenu() {
      this.selectedKeys = this.getSelectedKeys();
      console.log(this.selectedKeys);
      let openKeys = this.selectedKeys.filter((item) => item !== "");
      openKeys = openKeys.slice(0, openKeys.length - 1);
      this.sOpenKeys = openKeys
      if(this.selectedKeys.includes('/overview') ||this.selectedKeys.includes('/host-manage') ||this.selectedKeys.includes('/alarm-manage') ){
        this.sOpenKeys.push('/service-manage')
      }
      
      if (!fastEqual(openKeys, this.sOpenKeys)) {
        this.collapsed || this.mode === "horizontal"
          ? (this.cachedOpenKeys = openKeys)
          : (this.sOpenKeys = openKeys);
      }
    },
    getSelectedKeys() {
      let matches = this.$route.matched;
      console.log(matches);
      let arr = []
      matches.map(item => {
        arr.push(item)
      })
      const route = matches[matches.length - 1];
      let chose = this.routesMap[route.path];
      if (chose && chose.meta && chose.meta.highlight) {
        chose = this.routesMap[chose.meta.highlight];
        const resolve = this.$router.resolve({ path: chose.fullPath });
        matches = (resolve.resolved && resolve.resolved.matched) || matches;
      }
      let selectedKeys = []
      if ((this.$route.params && this.$route.params.serviceId)) {
        let arr2 = arr.splice(0, matches.length-1)
        arr2.push(this.$route)
        arr2.map((item) => {
          selectedKeys.push(item.path)
        });
      } else {
        matches.map((item) => {
          selectedKeys.push(item.path)
        });
      }
      return selectedKeys
    },
    getMoreMenu(props) {
      let arr = [
        { name: "启动", key: "start" },
        { name: "停止", key: "stop" },
        { name: "重启", key: "restart" },
        { name: "删除", key: "del" },
        // { name: "添加角色实例", key: "add" },
        // { name: "下载客户端配置", key: "downLoad" },
      ];
      // if (props.meta.obj.needRestart) arr.splice(2, 0, { name: "重启", key: "restart" })
      return arr.map((item, index) => {
        return (
          <div key={index}>
            <a
              class="more-menu-btn"
              style="border-width:0px;min-width:100px;"
              onClick={() => this.openServices(item, props)}
            >
              {item.name}
            </a>
          </div>
        );
      });
    },
    openServices(item,props) {
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
              {'确认' + (item.key=='start'?'开启':item.key=='stop'?'停止':item.key=='restart'?'重启':item.key=='del'?'删除':"") +'吗？'}
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.optServices(item, props)}
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
    delService(id){
      this.$axiosPost('/ddh/cluster/service/instance/delete', {serviceInstanceId: id,}).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.$destroyAll();
          this.$router.push({path: '/overview'})
        }
      });
    },
    optServices(item, props) {
      if(item.key === "del"){
        this.delService(props.meta.obj.id);
        return
      }
      let params = {
        clusterId: this.clusterId,
        commandType: item.key === "stop" ? "STOP_SERVICE" : item.key === "start" ? "START_SERVICE" : "RESTART_SERVICE",
        serviceInstanceIds: props.meta.obj.id,
      };
      this.$axiosPost(global.API.generateServiceCommand, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.$destroyAll();
          this.showClusterSetting(true)
        }
      });
    },
    showGj (meunItem) {
      let width = 1000;
      let title = "告警详情";
      let content = (
        <alarmModal serviceInstanceId={meunItem.id} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    handleClick(e) {
      this.selectedKeys = [];
      this.selectedKeys.push(e.key);
      this.$emit("select", e);
    },
    openChange (val) {
      this.sOpenKeys = val
    }
  },
};
</script>
<style lang="less" scoped>
.cluster-menu {
  .cluster-menu-item {
    &-left {
      .circle-point {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        display: block;
        z-index: 1000;
      }
      .service-name {
        cursor: pointer;
        max-width: 100px;
        overflow:hidden;
        white-space:nowrap;
        text-overflow:ellipsis;
      }
      .hide-point {
        visibility: hidden;
      }
      .success-point {
        background: @success-status-color;
      }
      .error-point {
        background: @error-status-color;
      }
      .configured-point {
        background: @configured-status-color;
      }
    }

    &-right {
      // width: 40px;
      position: relative;
      display: flex;
      justify-content: space-between;
      align-items: center;
      .icon-gj {
        position: relative;
        top: -2px;
      }
      .menu-sub-icon{
        position: relative;
        // top: 2px;
        margin: 0 6px 0 8px;
      }
      .cluster-more {
          margin-right: 0px;
          margin-left: 0px;
        // right: -10px;
      }
    }
  }
}
/deep/.ant-popover-placement-rightTop
  > .ant-popover-content
  > .ant-popover-arrow {
  display: none;
}
.popover-index {
  // margin-left: 5px;
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
</style>