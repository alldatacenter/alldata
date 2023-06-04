<template>
  <a-layout :class="['admin-layout', 'beauty-scroll']">
    <admin-header :class="[{'fixed-tabs': fixedTabs, 'fixed-header': fixedHeader, 'multi-page': multiPage}]" :style="headerStyle" :menuData="headMenuData" :collapsed="collapsed" @toggleCollapse="toggleCollapse"/>
    <a-layout-header :class="['virtual-header', {'fixed-tabs' : fixedTabs, 'fixed-header': fixedHeader, 'multi-page': multiPage}]" v-show="fixedHeader"></a-layout-header>
    <a-layout class="admin-layout-main beauty-scroll">
      <drawer v-if="isMobile" v-model="drawerOpen">
          <side-menu :theme="theme.mode" :menuData="menuDataOptions" :collapsed="false" :collapsible="false" @menuSelect="onMenuSelect"/>
      </drawer>
      <side-menu :class="[fixedSideBar ? 'fixed-side' : '']" :style="{paddingTop: isCluster === 'isCluster' ? '10px': 0}" :theme="theme.mode" v-else-if="layout === 'side' || layout === 'mix'" :menuData="sideMenuData" :collapsed="collapsed" :collapsible="true" />
      <div v-if="fixedSideBar && !isMobile" :style="`width: ${sideMenuWidth}; min-width: ${sideMenuWidth};max-width: ${sideMenuWidth};`" class="virtual-side"></div>
      <drawer v-if="!hideSetting" v-model="showSetting" placement="right">
        <div class="setting" slot="handler">
          <a-icon :type="showSetting ? 'close' : 'setting'"/>
        </div>
        <setting />
      </drawer>
      <a-layout-content class="admin-layout-content" :style="`min-height: ${minHeight}px;`">
        <div class="breadcrumb">
          <a-breadcrumb>
            <a-breadcrumb-item :key="index" v-for="(item, index) in breadcrumb">
              <span>{{item}}</span>
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div style="position: relative">
          <slot></slot>
        </div>
      </a-layout-content>
      <!-- <a-layout-footer style="padding: 0px">
        <page-footer :link-list="footerLinks" :copyright="copyright" />
      </a-layout-footer> -->
    </a-layout>
  </a-layout>
</template>

<script>
import AdminHeader from './header/AdminHeader'
// import PageFooter from './footer/PageFooter'
import Drawer from '../components/tool/Drawer'
import SideMenu from '../components/menu/SideMenu'
import Setting from '../components/setting/Setting'
import {mapState, mapMutations, mapGetters} from 'vuex'
import {getI18nKey} from '@/utils/routerUtil'
import menu from '../components/menu/menu'

// const minHeight = window.innerHeight - 64 - 122

export default {
  name: 'AdminLayout',
  components: {Setting, SideMenu, Drawer, AdminHeader},
  data () {
    return {
      minHeight: window.innerHeight - 64 - 122,
      collapsed: false,
      showSetting: false,
      drawerOpen: false,
      menuDataOptions: []
    }
  },
  provide() {
    return {
      adminLayout: this
    }
  },
  watch: {
    $route(val) {
      this.setActivated(val)
    },
    layout() {
      this.setActivated(this.$route)
    },
    isMobile(val) {
      if(!val) {
        this.drawerOpen = false
      }
    },
    isCluster: {
      handler(val) {
        this.menuDataOptions = this.hanlderMenuData()
      },
      immediate: true
    },
    // menuData
    menuData: {
      handler(val) {
        this.menuDataOptions = this.hanlderMenuData()
      },
      immediate: true,
      deep: true
    }
  },
  computed: {
    ...mapState('setting', ['isMobile', 'theme', 'layout', 'footerLinks', 'copyright', 'fixedHeader', 'fixedSideBar',
      'fixedTabs', 'hideSetting', 'multiPage']),
    ...mapGetters('setting', ['firstMenu', 'subMenu', 'isCluster', 'menuData']),
    sideMenuWidth() {
      return this.collapsed ? '80px' : '225px'
    },
    headerStyle() {
      let width = (this.fixedHeader && this.layout !== 'head' && !this.isMobile) ? `calc(100% - ${this.sideMenuWidth})` : '100%'
      let position = this.fixedHeader ? 'fixed' : 'static'
      return `width: ${width}; position: ${position};`
    },
    headMenuData() {
      const {layout, menuDataOptions, firstMenu} = this
      return layout === 'mix' ? firstMenu : menuDataOptions
    },
    sideMenuData() {
      const {layout, menuDataOptions, subMenu} = this
      return layout === 'mix' ? subMenu : menuDataOptions
    },
    breadcrumb() {
      let page = this.page
      let breadcrumb = page && page.breadcrumb
      if (breadcrumb) {
        let i18nBreadcrumb = []
        breadcrumb.forEach(item => {
          i18nBreadcrumb.push(this.$t(item))
        })
        return i18nBreadcrumb
      } else {
        return this.getRouteBreadcrumb()
      }
    },
  },
  methods: {
    ...mapMutations('setting', ['correctPageMinHeight', 'setActivatedFirst']),
    toggleCollapse () {
      this.collapsed = !this.collapsed
    },
    hanlderMenuData () {
      // let menuData = this.$store.state.setting.menuData
      let menuData = JSON.parse(localStorage.getItem('menuData'))
      const isCluster = localStorage.getItem('isCluster') || ''
      let arr = menuData.filter(item => item.meta.isCluster === isCluster)
      return arr
    },
    onMenuSelect () {
      this.toggleCollapse()
    },
    getRouteBreadcrumb() {
      let routes = this.$route.matched
      const path = this.$route.path
      let breadcrumb = []
      routes.filter(item => path.includes(item.path))
        .forEach(route => {
          const path = route.path.length === 0 ? '/home' : route.path
          breadcrumb.push(this.$t(getI18nKey(path)))
        })
      let pageTitle = this.page && this.page.title
      if (this.customTitle || pageTitle) {
        breadcrumb[breadcrumb.length - 1] = this.customTitle || pageTitle
      }
      breadcrumb.shift()
      // 去匹配动态的路由名称
      if (this.$route && this.$route.params && this.$route.params.serviceId) {
        let name = ''
        const serviceId = this.$route.params.serviceId || ''
        const menuData = JSON.parse(localStorage.getItem('menuData')) || []
        const arr = menuData.filter(item => item.path === 'service-manage')
        if (arr.length > 0) {
          arr[0].children.map(item => {
            if (item.meta.params.serviceId == serviceId) name = item.name
          })
          breadcrumb.push(name)
        }
      }
      return breadcrumb
    },
    setActivated(route) {
      if (this.layout === 'mix') {
        let matched = route.matched
        matched = matched.slice(0, matched.length - 1)
        const {firstMenu} = this
        for (let menu of firstMenu) {
          if (matched.findIndex(item => item.path === menu.fullPath) !== -1) {
            this.setActivatedFirst(menu.fullPath)
            break
          }
        }
      }
    }
  },
  created() {
    this.correctPageMinHeight(this.minHeight - 24)
    this.setActivated(this.$route)
  },
  beforeDestroy() {
    this.correctPageMinHeight(-this.minHeight + 24)
  }
}
</script>

<style lang="less" scoped>
  .admin-layout{
    .side-menu{
      &.fixed-side{
        position: fixed;
        height: calc(100vh - 47px);
        left: 0;
        top: 47px;
      }
    }
    .virtual-side{
      transition: all 0.2s;
    }
    .virtual-header{
      transition: all 0.2s;
      opacity: 0;
      &.fixed-tabs.multi-page:not(.fixed-header){
        height: 0;
      }
    }
    .admin-layout-main{
      .admin-header{
        top: 0;
        right: 0;
        overflow: hidden;
        transition: all 0.2s;
        &.fixed-tabs.multi-page:not(.fixed-header){
          height: 0;
        }
      }
    }
    .admin-layout-content{
      overflow-y: auto;
      padding: 20px;
      background: rgb(238, 240, 245);
      // background: #f5f7f8;
      /*overflow-x: hidden;*/
      /*min-height: calc(100vh - 64px - 122px);*/
      max-height: calc(100vh - 48px);
      height: calc(100vh - 48px);
      scrollbar-color: @primary-color @primary-2;
      scrollbar-width: thin;
      -ms-overflow-style:none;
      position: relative;
      &::-webkit-scrollbar{
        width: 3px;
        height: 1px;
      }
      &::-webkit-scrollbar-thumb {
        border-radius: 3px;
        background: @primary-color;
      }
      &::-webkit-scrollbar-track {
        -webkit-box-shadow: inset 0 0 1px rgba(0,0,0,0);
        border-radius: 3px;
        background: @primary-3;
      }
    }
    .setting{
      background-color: @primary-color;
      color: @base-bg-color;
      border-radius: 5px 0 0 5px;
      line-height: 40px;
      font-size: 22px;
      width: 40px;
      height: 40px;
      box-shadow: -2px 0 8px @shadow-color;
    }
    // 面包屑
    .breadcrumb {
      margin-top: -16px;
      margin-bottom: 4px;
      /deep/ .ant-breadcrumb-link {
        font-size: 12px;
      }
    }
  }
</style>
