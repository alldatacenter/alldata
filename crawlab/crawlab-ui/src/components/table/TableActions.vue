<template>
  <div class="table-actions">
    <slot name="prefix"></slot>
    <!--    <cl-fa-icon-button-->
    <!--        v-if="showButton(ACTION_ADD)"-->
    <!--        :icon="['fa', 'plus']"-->
    <!--        class="action-btn"-->
    <!--        tooltip="Add"-->
    <!--        type="success"-->
    <!--        :disabled="selection.length === 0"-->
    <!--        @click="onAdd"-->
    <!--    />-->
    <cl-fa-icon-button
      v-if="showButton(ACTION_EDIT)"
      :disabled="selection.length === 0"
      :icon="['fa', 'edit']"
      id="edit-btn"
      class="action-btn edit-btn"
      size="small"
      :tooltip="t('components.table.actions.editSelected')"
      type="warning"
      @click="onEdit"
    />
    <cl-fa-icon-button
      v-if="showButton(ACTION_DELETE)"
      :disabled="selection.length === 0"
      :icon="['fa', 'trash-alt']"
      id="delete-btn"
      class="action-btn delete-btn"
      size="small"
      :tooltip="t('components.table.actions.deleteSelected')"
      type="danger"
      @click="onDelete"
    />
    <div
      v-export="{
        target,
        conditions,
      }"
    >
      <cl-fa-icon-button
        v-if="showButton(TABLE_ACTION_EXPORT)"
        :icon="['fa', 'file-export']"
        id="export-btn"
        class="action-btn export-btn"
        size="small"
        :tooltip="`${t('components.table.actions.export')}`"
        type="primary"
        @click="onExport"
      />
    </div>
    <cl-fa-icon-button
      v-if="showButton(TABLE_ACTION_CUSTOMIZE_COLUMNS)"
      :icon="['fa', 'arrows-alt']"
      id="customize-columns-btn"
      class="action-btn customize-columns-btn"
      size="small"
      :tooltip="t('components.table.actions.customizeColumns')"
      type="primary"
      @click="onCustomizeColumns"
    />
    <slot name="suffix"></slot>
  </div>
</template>

<script lang="ts">
import {defineComponent, inject, PropType} from 'vue';
import {ACTION_ADD, ACTION_DELETE, ACTION_EDIT,} from '@/constants/action';
import {TABLE_ACTION_CUSTOMIZE_COLUMNS, TABLE_ACTION_EXPORT,} from '@/constants/table';
import {useI18n} from 'vue-i18n';
import {useStore} from 'vuex';
import {useRoute} from 'vue-router';
import {getPrimaryPath} from '@/utils';

export default defineComponent({
  name: 'TableActions',
  emits: [
    'edit',
    'delete',
    'export',
    'customize-columns'
  ],
  props: {
    selection: {
      type: Array as PropType<TableData>,
      required: false,
      default: () => {
        return [];
      },
    },
    visibleButtons: {
      type: Array as PropType<BuiltInTableActionButtonName[]>,
      required: false,
      default: () => {
        return [];
      }
    }
  },
  setup(props: TableActionsProps, {emit}) {
    // i18n
    const {t} = useI18n();

    // store
    const store = useStore();

    // route
    const route = useRoute();

    // const onAdd = () => {
    //   emit('click-add');
    // };

    const onEdit = () => {
      emit('edit');
    };

    const onDelete = async () => {
      emit('delete');
    };

    const onExport = () => {
      emit('export');
    };

    const onCustomizeColumns = () => {
      emit('customize-columns');
    };

    const showButton = (name: string): boolean => {
      const {visibleButtons} = props;
      if (visibleButtons && visibleButtons.length > 0 && !visibleButtons.includes(name)) {
        return false;
      }
      const currentRoutePath = route.path;
      const {actionVisibleFn} = (store.state as RootStoreState).layout;
      if (!actionVisibleFn) return true;
      return actionVisibleFn(currentRoutePath, name);
    };

    // export target
    const target = () => {
      const primaryPath = getPrimaryPath(route.path);
      return primaryPath.replace(/^\//, '');
    };

    // store context
    const storeContext = inject<ListStoreContext<BaseModel>>('store-context');

    // export conditions
    const conditions = () => {
      const state = storeContext?.state;
      return state?.tableListFilter || [];
    };

    return {
      ACTION_ADD,
      ACTION_EDIT,
      ACTION_DELETE,
      TABLE_ACTION_EXPORT,
      TABLE_ACTION_CUSTOMIZE_COLUMNS,
      // onAdd,
      onEdit,
      onDelete,
      onExport,
      onCustomizeColumns,
      showButton,
      target,
      conditions,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.table-actions {
  display: inline-flex;

  .nav-action-button {
    display: inline-flex;
  }
}
</style>
