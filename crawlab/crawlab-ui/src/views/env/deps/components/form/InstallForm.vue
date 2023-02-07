<template>
  <cl-dialog
    :title="t('common.actions.install')"
    :visible="visible"
    width="640px"
    :confirm-loading="loading"
    @confirm="onConfirm"
    @close="onClose"
  >
    <cl-form>
      <cl-form-item :span="4" :label="t('views.env.deps.dependency.form.name')">
        <cl-tag
          v-for="n in names"
          :key="n"
          class="dep-name"
          type="primary"
          :label="n"
          size="small"
        />
      </cl-form-item>
      <cl-form-item :span="4" :label="t('views.env.deps.dependency.form.mode')">
        <el-select v-model="mode">
          <el-option value="all" :label="t('views.env.deps.dependency.form.allNodes')"/>
          <el-option value="selected-nodes" :label="t('views.env.deps.dependency.form.selectedNodes')"/>
        </el-select>
      </cl-form-item>
      <cl-form-item :label="t('views.env.deps.dependency.form.upgrade')">
        <cl-switch v-model="upgrade"/>
      </cl-form-item>
      <cl-form-item v-if="mode === 'selected-nodes'" :span="4" :label="t('views.env.deps.dependency.form.selectedNodes')">
        <el-select v-model="nodeIds" multiple :placeholder="t('views.env.deps.dependency.form.selectedNodes')">
          <el-option v-for="n in nodes" :key="n.key" :value="n._id" :label="n.name"/>
        </el-select>
      </cl-form-item>
    </cl-form>
  </cl-dialog>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import {translate} from '@/utils';

const t = translate;

export default defineComponent({
  name: 'InstallForm',
  props: {
    visible: {
      type: Boolean,
    },
    names: {
      type: Array,
      default: () => {
        return [];
      },
    },
    nodes: {
      type: Array,
      default: () => {
        return [];
      }
    },
    loading: {
      type: Boolean,
    },
  },
  emits: [
    'confirm',
    'close',
  ],
  setup(props, {emit}) {
    const mode = ref('all');
    const upgrade = ref(true);
    const nodeIds = ref([]);

    const reset = () => {
      mode.value = 'all';
      nodeIds.value = [];
    };

    const onConfirm = () => {
      emit('confirm', {
        mode: mode.value,
        upgrade: upgrade.value,
        nodeIds: nodeIds.value,
      });
      reset();
    };

    const onClose = () => {
      emit('close');
      reset();
    };

    return {
      mode,
      upgrade,
      nodeIds,
      onConfirm,
      onClose,
      t,
    };
  },
});
</script>

<style scoped>
.dep-name {
  margin-right: 10px;
}
</style>
