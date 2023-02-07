<template>
  <cl-form
      :key="JSON.stringify(form)"
      :model="form"
  >
    <cl-form-item :span="4" prop="key" :label="t('settings.form.key')">
      <el-input v-model="internalForm.key" disabled/>
    </cl-form-item>
    <cl-form-item :span="4" prop="name" :label="t('settings.form.name')">
      <el-input v-model="internalForm.name" disabled/>
    </cl-form-item>
    <cl-form-item :span="4" prop="description" :label="t('settings.form.description')">
      <el-input :model-value="t(internalForm.description)" type="textarea" disabled/>
    </cl-form-item>
    <cl-form-item :span="4" prop="cmd" :label="t('settings.form.command')">
      <el-input v-model="internalForm.cmd" :placeholder="t('settings.form.command')" @change="onChange"/>
    </cl-form-item>
    <cl-form-item :span="4" prop="proxy" :label="t('settings.form.proxy')">
      <el-input v-model="internalForm.proxy" :placeholder="t('settings.form.proxy')" @change="onChange"/>
    </cl-form-item>
  </cl-form>
</template>

<script lang="ts">
import {defineComponent, onBeforeMount, ref, watch} from 'vue';

const pluginName = 'dependency';
const t = (path) => window['_tp'](pluginName, path);

export default defineComponent({
  name: 'DependencySettingForm',
  props: {
    form: {
      type: Object,
      default: () => {
      }
    }
  },
  emits: [
    'change',
  ],
  setup(props, {emit}) {
    const internalForm = ref({});

    const onChange = () => {
      emit('change', internalForm.value);
    };

    watch(() => props.form, () => {
      internalForm.value = {...props.form};
    });

    onBeforeMount(() => {
      internalForm.value = {...props.form};
    });

    return {
      internalForm,
      onChange,
      t,
    };
  },
});
</script>

<style scoped>

</style>
