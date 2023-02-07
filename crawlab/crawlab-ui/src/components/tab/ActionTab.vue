<template>
  <el-tooltip :content="t('components.tab.newTab')">
    <cl-tab
        v-track="{code: 'click_tabs_view_add_tab'}"
        :icon="icon"
        :show-close="false"
        :show-title="false"
        class="action-tab"
        @click="onClick"
    />
  </el-tooltip>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';
import Icon from '@/components/icon/Icon.vue';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'ActionTab',
  props: {
    icon: {
      type: [String, Array] as PropType<Icon>,
    },
    title: {
      type: String,
    }
  },
  emits: [
    'click',
  ],
  setup(props: ActionTabProps, {emit}) {
    const {t} = useI18n();

    const onClick = () => {
      emit('click');
    };

    return {
      onClick,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.action-tab {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

<style scoped>
.action-tab >>> .icon {
  color: #909399;
}
</style>
