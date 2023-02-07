<template>
  <el-form inline label-width="100px">
    <el-form-item :label="t('components.export.type')">
      <el-select v-model="exportType" @change="onExportTypeChange">
        <el-option :value="ExportTypeCsv" :label="t('components.export.types.csv')"/>
        <el-option :value="ExportTypeJson" :label="t('components.export.types.json')"/>
      </el-select>
    </el-form-item>
  </el-form>
</template>

<script lang="ts">
import {defineComponent, onBeforeMount, PropType, ref} from 'vue';
import {ExportTypeCsv, ExportTypeJson} from '@/constants/export';
import {translate} from '@/utils';
import {ElForm, ElFormItem, ElOption, ElSelect} from 'element-plus';

// i18n
const t = translate;

export default defineComponent({
  name: 'ExportForm',
  components: {
    ElForm,
    ElFormItem,
    ElSelect,
    ElOption,
  },
  props: {
    defaultType: {
      type: String as PropType<ExportType>,
      default: ExportTypeCsv,
    },
    target: {
      type: String,
    },
  },
  emits: [
    'export-type-change',
  ],
  setup(props: ExportFormProps, {emit}) {
    const exportType = ref<ExportType>();

    const onExportTypeChange = (value: string) => {
      emit('export-type-change', value);
    };

    onBeforeMount(() => {
      exportType.value = props.defaultType;
    });

    return {
      exportType,
      ExportTypeCsv,
      ExportTypeJson,
      onExportTypeChange,
      t,
    };
  }
});
</script>

<style lang="scss" scoped>
.el-form {
  .el-form-item {
    width: 100%;

    .el-select {
      width: 100%;
    }
  }
}
</style>
