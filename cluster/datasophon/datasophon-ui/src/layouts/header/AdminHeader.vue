<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-10-25 19:14:40
 * @FilePath: \ddh-ui\src\layouts\header\AdminHeader.vue
-->
<template>
  <a-layout-header :class="[headerTheme, 'admin-header']">
    <div :class="['admin-header-wide', layout, pageWidth]">
      <div v-if="isMobile || layout === 'head'" :class="['logo', isMobile ? null : 'pc', headerTheme]">
        <img width="32" src="@/assets/img/brand.png" />
        <h1 v-if="!isMobile">{{systemName}}</h1>
      </div>
      <div :class="['logo', theme]">
        <div class="flex-container">
          <img src="@/assets/img/brand.png" alt />
          <h1>{{systemName}}</h1>
        </div>
      </div>
      <a-divider v-if="isMobile" type="vertical" />
      <!-- <a-icon v-if="layout !== 'head'" class="trigger" :type="collapsed ? 'menu-unfold' : 'menu-fold'" @click="toggleCollapse"/> -->
      <div v-if="layout !== 'side' && !isMobile" class="admin-header-menu" :style="`width: ${menuWidth};`">
        <i-menu class="head-menu" :theme="headerTheme" mode="horizontal" :options="menuData" @select="onSelect" />
      </div>
      <div :class="['admin-header-right', headerTheme]">
        <cluster-setting v-if="isCluster === 'isCluster'" />
        <alarm-manage  v-if="isCluster === 'isCluster'"/>
        <a-dropdown v-if="isCluster === 'isCluster'" class="lang header-item">
          <div style="display: flex;align-items: center;">
            <span class="system-name">{{currentCluster.name}}</span>
            <a-icon type="caret-down" style="font-size: 18px;color:#fff" />
          </div>
          <!-- <a-menu @click="val => setLang(val.key)" :selected-keys="[lang]" slot="overlay">
            <a-menu-item v-for=" lang in langList" :key="lang.key">{{lang.key.toLowerCase() + ' ' + lang.name}}</a-menu-item>
          </a-menu> -->
          <a-menu @click="val => changeCluster(val)" :selectedKeys="[currentCluster.clusterId]" slot="overlay">
            <a-menu-item v-for=" lang in runningCluster" :key="lang.value">{{lang.label}}</a-menu-item>
          </a-menu>
        </a-dropdown>
        <a-dropdown v-if="isCluster === 'isCluster'" class="lang header-item">
          <div style="display: flex;align-items: center;">
            <span class="system-name">{{lang}}</span>
            <a-icon type="caret-down" style="font-size: 18px;color:#fff" />
          </div>
          <a-menu @click="val => setLang(val.key)" :selected-keys="[lang]" slot="overlay">
            <a-menu-item v-for=" l in langList" :key="l.key">{{l.key.toLowerCase() + ' ' + l.name}}</a-menu-item>
          </a-menu>
        </a-dropdown>
        <header-avatar class="header-item" />
      </div>
    </div>
  </a-layout-header>
</template>

<script>
import HeaderAvatar from "./HeaderAvatar";
import IMenu from "@/components/menu/menu";
import ClusterSetting from './clusterSetting';
import AlarmManage from './alarmManage.vue'
import { mapState, mapMutations, mapGetters } from "vuex";

export default {
  name: "AdminHeader",
  components: { IMenu, HeaderAvatar, ClusterSetting  ,AlarmManage},
  props: ["collapsed", "menuData"],
  data() {
    return {
      langList: [
        { key: "CN", name: "简体中文", alias: "简体" },
        // {key: 'HK', name: '繁體中文', alias: '繁體'},
        { key: "US", name: "English", alias: "English" },
      ],
      searchActive: false,
    };
  },
  computed: {
    ...mapGetters('setting', ['isCluster', 'runningCluster', 'clusterId']),
    ...mapState("setting", [
      "theme",
      "isMobile",
      "layout",
      "systemName",
      "lang",
      "pageWidth",
    ]),
    headerTheme() {
      if (
        this.layout == "side" &&
        this.theme.mode == "dark" &&
        !this.isMobile
      ) {
        return "light";
      }
      return this.theme.mode;
    },
    langAlias() {
      let lang = this.langList.find((item) => item.key == this.lang);
      return lang.alias;
    },
    menuWidth() {
      const { layout, searchActive } = this;
      const headWidth = layout === "head" ? "100% - 188px" : "100%";
      const extraWidth = searchActive ? "600px" : "400px";
      return `calc(${headWidth} - ${extraWidth})`;
    },
    currentCluster () {
      let arr = this.runningCluster.filter(item => item.value === Number(this.clusterId)) || []
      return {
        name: arr.length > 0 ? arr[0].label : '',
        clusterId: this.clusterId
      }
    }
  },
  methods: {
    toggleCollapse() {
      this.$emit("toggleCollapse");
    },
    onSelect(obj) {
      this.$emit("menuSelect", obj);
    },
    // 切换运行中的集群
    changeCluster (val) {
      if (this.clusterId === val.key) return false
      this.setClusterId(val.key)
      // todo: 是否需要立马去刷新服务列表
      this.$store.dispatch('setting/getRunningClusterList')
      if (window.location.hash !== '#/overview') this.$router.push("/overview");
    },
    ...mapMutations("setting", ["setLang", "setClusterId"]),
  },
};
</script>

<style lang="less" scoped>
@import "index";
.system-name {
  font-size: 14px;
  color: #fff;
  letter-spacing: 0.39px;
  font-weight: 500;
  margin-right: 5px;
}
</style>
