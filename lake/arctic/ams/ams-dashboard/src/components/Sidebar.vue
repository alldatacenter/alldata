<template>
  <div :class="{'side-bar-collapsed': collapsed}" class="side-bar">
    <div :class="{'logo-collapsed': collapsed}" @mouseenter="toggleTablesMenu(false)" @click="viewIntroduce" class="logo g-flex-ac">
      <img src="../assets/images/logo.svg" class="logo-img" alt="">
      <img v-show="!collapsed" src="../assets/images/arctic-dashboard.svg" class="arctic-name" alt="">
    </div>
    <a-menu
      v-model:selectedKeys="selectedKeys"
      mode="inline"
      theme="dark"
      :inline-collapsed="collapsed"
    >
      <a-menu-item v-for="item in menuList" :key="item.key" @click="navClick(item)" @mouseenter="mouseenter(item)" :class="{'active-color': (store.isShowTablesMenu && item.key === 'tables'), 'table-item-tab': item.key === 'tables'}">
        <template #icon>
          <svg-icon :icon-class="item.icon" class="svg-icon" />
        </template>
        <span>{{ $t(item.title) }}</span>
      </a-menu-item>
    </a-menu>
    <a-button type="link" @click="toggleCollapsed" class="toggle-btn">
      <MenuUnfoldOutlined v-if="collapsed" />
      <MenuFoldOutlined v-else />
    </a-button>
    <div @click.self="toggleTablesMenu(false)" v-if="store.isShowTablesMenu && !hasToken" @mouseleave="toggleTablesMenu(false)" @mouseenter="toggleTablesMenu(true)" :class="{'collapsed-sub-menu': collapsed}" class="tables-menu-wrap">
      <TableMenu @goCreatePage="goCreatePage" />
    </div>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, nextTick, reactive, ref, toRefs, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import useStore from '@/store/index'
import TableMenu from '@/components/tables-sub-menu/TablesMenu.vue'
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { getQueryString } from '@/utils'

interface MenuItem {
  key: string
  title: string
  icon: string
}

export default defineComponent({
  name: 'Sidebar',
  components: {
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    TableMenu
  },
  setup () {
    const { t } = useI18n()
    const router = useRouter()
    const route = useRoute()
    const store = useStore()

    const state = reactive({
      collapsed: false,
      selectedKeys: [] as string[]
    })
    const hasToken = computed(() => {
      return !!(getQueryString('token') || '')
    })
    const timer = ref(0)
    const menuList = computed(() => {
      const menu: MenuItem[] = [
        {
          key: 'tables',
          title: t('tables'),
          icon: 'TableOutlined'
        }
      ]
      const allMenu : MenuItem[] = [
        {
          key: 'tables',
          title: t('tables'),
          icon: 'tables'
        },
        {
          key: 'catalogs',
          title: t('catalogs'),
          icon: 'catalogs'
        },
        {
          key: 'optimizers',
          title: t('optimizers'),
          icon: 'optimizers'
        },
        {
          key: 'terminal',
          title: t('terminal'),
          icon: 'terminal'
        },
        {
          key: 'settings',
          title: t('settings'),
          icon: 'settings'
        }
      ]
      return hasToken.value ? menu : allMenu
    })

    const setCurMenu = () => {
      const pathArr = route.path.split('/')
      if (route.path) {
        const routePath = [pathArr[1]]
        state.selectedKeys = routePath.includes('hive-tables') ? ['tables'] : routePath
      }
    }
    watchEffect(() => {
      setCurMenu()
    })
    const toggleCollapsed = () => {
      state.collapsed = !state.collapsed
      // The stretch animation is 300 ms and the trigger method needs to be delayed here
      setTimeout(() => {
        window.dispatchEvent(new Event('resize'))
      }, 300)
    }

    const navClick = (item:MenuItem) => {
      if (item.key === 'tables') {
        nextTick(() => {
          setCurMenu()
        })
        return
      }
      router.replace({
        path: `/${item.key}`
      })
      nextTick(() => {
        setCurMenu()
      })
    }

    const mouseenter = (item:MenuItem) => {
      toggleTablesMenu(item.key === 'tables')
    }

    const goCreatePage = () => {
      toggleTablesMenu(false)
      router.push({
        path: '/tables/create'
      })
    }

    const toggleTablesMenu = (flag = false) => {
      if (hasToken.value) {
        return
      }
      timer.value && clearTimeout(timer.value)
      const time = flag ? 0 : 200
      timer.value = setTimeout(() => {
        store.updateTablesMenu(flag)
      }, time)
    }

    const viewIntroduce = () => {
      router.push({
        path: '/introduce'
      })
    }

    return {
      ...toRefs(state),
      hasToken,
      menuList,
      toggleCollapsed,
      navClick,
      mouseenter,
      store,
      toggleTablesMenu,
      goCreatePage,
      viewIntroduce
    }
  }
})
</script>

<style lang="less" scoped>
  .side-bar {
    position: relative;
    height: 100%;
    transition: width 0.3s;
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    :deep(.ant-menu) {
      height: 100%;
      width: 200px;
      &.ant-menu-inline-collapsed {
        width: 64px;
        .logo {
          padding-left: 14px;
        }
        .toggle-btn {
          position: absolute;
          right: -68px;
          top: 8px;
          font-size: 18px;
          padding: 0 24px;
        }
      }
    }
    :deep(.ant-menu-item) {
      margin: 0;
      padding-left: 22px !important;
      .ant-menu-title-content {
        width: 100%;
        margin-left: 12px;
      }
      &.active {
        background-color: @primary-color;
        color: #fff;
      }
      &.active-color {
        color: #fff;
        background-color: @dark-bg-color;
      }
      &:hover {
        color: #fff;
      }
      &.table-item-tab:hover {
        background-color: @dark-bg-color;
      }
    }
    .logo {
      padding: 12px 0 12px 16px;
      overflow: hidden;
      background-color: #001529;
      cursor: pointer;
    }
    .logo-img {
      width: 32px;
      height: 32px;
    }
    .arctic-name {
      width: 112px;
      margin: 4px 0 0 8px;
    }
    .toggle-btn {
      position: absolute;
      right: -68px;
      top: 8px;
      font-size: 18px;
      padding: 0 24px;
    }
    .svg-icon {
      font-size: 16px;
    }
  }
  .tables-menu-wrap {
    position: absolute;
    top: 0;
    left: 200px;
    right: 0;
    bottom: 0;
    z-index: 100;
    &.collapsed-sub-menu {
      left: 64px;
    }
  }
</style>
