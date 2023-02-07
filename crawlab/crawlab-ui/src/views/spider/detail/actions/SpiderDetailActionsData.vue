<template>
  <cl-nav-action-group class="spider-detail-actions-data">
    <cl-nav-action-fa-icon
      :icon="['fa', 'database']"
      :tooltip="t('components.spider.actions.data.tooltip.dataActions')"
    />
    <cl-nav-action-item>
      <el-tooltip :content="t('components.spider.actions.data.tooltip.displayAllFields')">
        <cl-switch
          class="display-all-fields"
          :active-icon="['fa', 'eye']"
          :inactive-icon="['fa', 'eye']"
          inline-prompt
          v-model="displayAllFields"
          @change="onDisplayAllFieldsChange"
        />
      </el-tooltip>
    </cl-nav-action-item>
    <cl-nav-action-item
      v-export="colName"
    >
      <cl-fa-icon-button
        :icon="['fa', 'download']"
        :tooltip="t('components.spider.actions.data.tooltip.export')"
        type="primary"
        id="export-btn"
        class-name="export-btn"
      />
    </cl-nav-action-item>
    <cl-nav-action-item>
      <cl-fa-icon-button
        :icon="['fa', 'lightbulb']"
        :tooltip="t('components.spider.actions.data.tooltip.inferDataFieldsTypes')"
        type="primary"
        class-name="infer-data-fields-types-btn"
        @click="onClickInferDataFieldsTypes"
      />
    </cl-nav-action-item>
    <cl-nav-action-item>
      <el-tooltip
        :content="dedupEnabled ? t('components.spider.actions.data.tooltip.dedup.enabled') : t('components.spider.actions.data.tooltip.dedup.disabled')"
      >
        <cl-switch
          class="dedup"
          :active-icon="['fa', 'filter']"
          :inactive-icon="['fa', 'filter']"
          inline-prompt
          v-model="dedupEnabled"
          @change="onDedupEnabledChange"
        />
      </el-tooltip>
    </cl-nav-action-item>
    <cl-nav-action-item v-if="dedupEnabled">
      <cl-fa-icon-button
        :icon="['fa', 'list']"
        :tooltip="t('components.spider.actions.data.tooltip.dedup.fields')"
        type="primary"
        class-name="infer-data-fields-types-btn"
        @click="onClickDedupFields"
      />
    </cl-nav-action-item>
    <cl-nav-action-item v-if="dedupEnabled">
      <el-tooltip :content="t('components.result.dedup.labels.dedupType')">
        <el-select
          class="dedup-type"
          v-model="dedupType"
          @change="onDedupTypeChange"
        >
          <el-option :value="DEDUP_TYPE_IGNORE" :label="t('components.result.dedup.types.ignore')"/>
          <el-option :value="DEDUP_TYPE_OVERWRITE" :label="t('components.result.dedup.types.overwrite')"/>
        </el-select>
      </el-tooltip>
    </cl-nav-action-item>
  </cl-nav-action-group>
</template>

<script lang="ts" setup>
import {ref, watch} from 'vue';
import {ExportTypeCsv} from '@/constants/export';
import {useStore} from 'vuex';
import {translate} from "@/utils";
import {inferDataFieldTypes} from "@/utils/dataFields";
import {ElMessage, ElMessageBox} from "element-plus";
import {DEDUP_TYPE_IGNORE, DEDUP_TYPE_OVERWRITE} from "@/constants/dedup";

const t = translate;

// store
const ns = 'spider';
const nsDc = 'dataCollection';
const store = useStore();
const {
  spider: spiderState,
  dataCollection: dataCollectionState,
} = store.state as RootStoreState;

// spider col name
const colName = () => spiderState.form.col_name as string;

// export type
const exportType = ref<ExportType>(ExportTypeCsv);

// display all fields
const displayAllFields = ref<boolean>(spiderState.dataDisplayAllFields);
const onDisplayAllFieldsChange = (val: boolean) => {
  store.commit(`${ns}/setDataDisplayAllFields`, val);
};

const inferFields = async () => {
  let fields = store.getters[`${nsDc}/resultFields`] as DataField[];
  const data = dataCollectionState.resultTableData as Result[];
  fields = inferDataFieldTypes(fields, data);
  const form = {
    ...dataCollectionState.form,
    fields,
  };
  store.commit(`${nsDc}/setForm`, form);
  await store.dispatch(`${nsDc}/updateById`, {
    id: form._id,
    form,
  });
  await store.dispatch(`${nsDc}/getById`, form._id);
};

const onClickInferDataFieldsTypes = async () => {
  await ElMessageBox.confirm(t('common.messageBox.confirm.proceed'), t('common.actions.inferDataFieldsTypes'), {type: 'warning'});
  await inferFields();
  await ElMessage.success(t('common.message.success.action'));
};

watch(() => JSON.stringify(dataCollectionState.resultTableData), async () => {
  if (!dataCollectionState.form?.fields?.length && dataCollectionState.resultTableData?.length) {
    await inferFields();
  }
});

const dedupEnabled = ref<boolean>(dataCollectionState.form?.dedup?.enabled as boolean);
watch(() => dataCollectionState.form?.dedup?.enabled, async (val) => {
  dedupEnabled.value = val as boolean;
});
const onDedupEnabledChange = async (val: boolean) => {
  store.commit(`${nsDc}/setForm`, {
    ...dataCollectionState.form,
    dedup: {
      ...dataCollectionState.form?.dedup,
      enabled: val,
    },
  });
  await store.dispatch(`${nsDc}/updateById`, {
    id: dataCollectionState.form._id,
    form: dataCollectionState.form,
  });
  await ElMessage.success(val ? t('common.message.success.enabled') : t('common.message.success.disabled'));
};

const dedupType = ref<string>(dataCollectionState.form?.dedup?.type as string || DEDUP_TYPE_IGNORE);
watch(() => dataCollectionState.form?.dedup?.type, async (val) => {
  dedupType.value = val || DEDUP_TYPE_IGNORE;
});
const onDedupTypeChange = async (val: string) => {
  store.commit(`${nsDc}/setForm`, {
    ...dataCollectionState.form,
    dedup: {
      ...dataCollectionState.form?.dedup,
      type: val,
    },
  });
  await store.dispatch(`${nsDc}/updateById`, {
    id: dataCollectionState.form._id,
    form: dataCollectionState.form,
  });
  await ElMessage.success(t('common.message.success.update'));
};

const onClickDedupFields = () => {
  store.commit(`${nsDc}/setDedupFieldsDialogVisible`, true);
};
</script>

<style scoped>
.spider-detail-actions-data >>> .display-all-fields,
.spider-detail-actions-data >>> .dedup {
  margin-right: 10px;
}

.spider-detail-actions-data >>> .dedup-type {
  width: 120px;
}
</style>
