<!--
 * @Author: mjzhu
 * @Date: 2022-05-24 10:28:22
 * @LastEditTime: 2022-06-28 10:02:53
 * @FilePath: \ddh-ui\src\components\menu\SideMenu.vue
-->
<template>
  <a-layout-sider :theme="sideTheme" :class="['side-menu', 'beauty-scroll', isMobile ? null : 'shadow']" width="225px" :collapsible="collapsible" v-model="collapsed" :trigger="null">
    <ClusterMenu v-if="isCluster === 'isCluster'" :theme="theme" :collapsed="collapsed" :options="menuData" @select="onSelect" class="menu" /> 
    <i-menu v-else :theme="theme" :collapsed="collapsed" :options="menuData" @select="onSelect" class="menu"/>
  </a-layout-sider>
</template>

<script>
import IMenu from './menu'
import ClusterMenu from './clusterMenu'
import {mapState, mapGetters} from 'vuex'
export default {
  name: 'SideMenu',
  components: {IMenu, ClusterMenu},
  props: {
    collapsible: {
      type: Boolean,
      required: false,
      default: false
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false
    },
    menuData: {
      type: Array,
      required: true
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    }
  },
  computed: {
    sideTheme() {
      return this.theme == 'light' ? this.theme : 'dark'
    },
    ...mapState('setting', ['isMobile', 'systemName']),
    ...mapGetters('setting', ['isCluster'])
  },
  methods: {
    onSelect (obj) {
      console.log(obj);
      this.$emit('menuSelect', obj)
    }
  }
}
</script>

<style lang="less" scoped>
@import "index";
</style>
