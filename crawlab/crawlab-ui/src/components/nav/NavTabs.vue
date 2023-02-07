<template>
  <div class="nav-tabs">
    <el-tooltip
      v-if="toggle"
      :content="collapsed ? t('components.nav.tabs.toggle.expand') : t('components.nav.tabs.toggle.collapse')"
    >
      <div class="toggle" @click="onToggle">
        <font-awesome-icon :icon="collapsed ? ['fa', 'indent'] : ['fa', 'outdent']"/>
      </div>
    </el-tooltip>
    <el-menu
      :default-active="activeKey"
      mode="horizontal"
      @select="onSelect"
    >
      <el-menu-item
        v-for="item in items"
        :key="item.id"
        :class="getClassName(item)"
        :index="item.id"
        :style="item.style"
        :disabled="item.disabled"
      >
        <el-tooltip :content="item.tooltip" :disabled="!item.tooltip">
          <template v-if="!!item.icon">
            <font-awesome-icon :icon="item.icon"/>
          </template>
          <template v-else>
            {{ t(item.title) }}
          </template>
        </el-tooltip>
      </el-menu-item>
    </el-menu>
    <div class="extra">
      <slot name="extra">
      </slot>
    </div>
  </div>
</template>
<script lang="ts">
import {defineComponent, PropType} from 'vue';
import {useI18n} from 'vue-i18n';
import {emptyArrayFunc} from '@/utils/func';

export default defineComponent({
  name: 'NavTabs',
  props: {
    items: {
      type: Array as PropType<NavItem[]>,
      default: emptyArrayFunc,
    },
    activeKey: {
      type: String,
      default: '',
    },
    collapsed: {
      type: Boolean,
      default: false,
    },
    toggle: {
      type: Boolean,
      default: false,
    }
  },
  emits: [
    'select',
    'toggle',
  ],
  setup(props: NavTabsProps, {emit}) {
    const {t} = useI18n();

    const onSelect = (index: string) => {
      emit('select', index);
    };

    const onToggle = () => {
      emit('toggle');
    };

    const getClassName = (item: NavItem): string => {
      const cls = [];
      if (item.emphasis) cls.push('emphasis');
      if (item.id) cls.push(item.id);
      return cls.join(' ');
    };

    return {
      onSelect,
      onToggle,
      getClassName,
      t,
    };
  },
});
</script>
<style lang="scss" scoped>
.nav-tabs {
  display: flex;
  border-bottom: 1px solid #e6e6e6;

  .toggle {
    flex: 0 0 40px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    border-right: 1px solid #e6e6e6;
  }

  .el-menu {
    flex: 1 0 auto;
    display: flex;
    height: var(--cl-nav-tabs-height);
    border-bottom: none;

    .el-menu-item {
      height: var(--cl-nav-tabs-height);
      line-height: var(--cl-nav-tabs-height);

      &:hover {
        color: var(--cl-primary-color);
        background: inherit;
      }

      &:focus {
        background: inherit;
      }

      &.emphasis {
        color: var(--cl-info-color);
        border-bottom: none;
      }
    }
  }

  .extra {
    background: transparent;
    display: flex;
    align-items: center;
    height: var(--cl-nav-tabs-height);
  }
}
</style>

<style scoped>
.nav-tabs >>> .el-menu--horizontal {
  /*border-bottom: none;*/
}
</style>
