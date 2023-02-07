<template>
  <cl-nav-action-group class="task-detail-actions-data">
    <cl-nav-action-fa-icon
      :icon="['fa', 'database']"
      :tooltip="t('components.task.actions.data.tooltip.dataActions')"
    />
    <cl-nav-action-item>
      <el-tooltip :content="t('components.task.actions.data.tooltip.displayAllFields')">
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
      v-export="{
        target,
        conditions,
      }"
    >
      <cl-fa-icon-button
        :icon="['fa', 'download']"
        :tooltip="t('components.task.actions.data.tooltip.export')"
        type="primary"
        id="export-btn"
        class-name="export-btn"
      />
    </cl-nav-action-item>
    <cl-nav-action-item>
      <cl-fa-icon-button
        :icon="['fa', 'lightbulb']"
        :tooltip="t('components.task.actions.data.tooltip.inferDataFieldsTypes')"
        type="primary"
        class-name="infer-data-fields-types-btn"
        @click="onClickInferDataFieldsTypes"
      />
    </cl-nav-action-item>
  </cl-nav-action-group>
</template>

<script lang="ts" setup>
import {computed, defineComponent, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {ExportTypeCsv} from '@/constants/export';
import {useStore} from 'vuex';
import useSpider from '@/components/spider/spider';
import useRequest from '@/services/request';
import {FILTER_OP_EQUAL} from '@/constants';
import {inferDataFieldTypes} from "@/utils/dataFields";
import {ElMessage, ElMessageBox} from "element-plus";

const {
  get,
} = useRequest();

// i18n
const {t} = useI18n();

// store
const ns = 'task';
const nsDc = 'dataCollection';
const store = useStore();
const {
  task: taskState,
  dataCollection: dataCollectionState,
} = store.state as RootStoreState;

const {
  allDict: allSpiderDict,
} = useSpider(store);

// spider
const spider = computed(() => allSpiderDict.value.get(taskState.form.spider_id as string));

// spider collection name
const colName = ref<string>();
watch(() => spider.value, async () => {
  if (!spider.value) return;
  const res = await get(`/spiders/${spider.value._id}`);
  colName.value = (res.data as Spider)?.col_name;
});

// target
const target = () => colName.value;

// conditions
const conditions = () => [{key: '_tid', op: FILTER_OP_EQUAL, value: taskState.form._id}];

// display all fields
const displayAllFields = ref<boolean>(taskState.dataDisplayAllFields);
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

</script>

<style scoped>
.task-detail-actions-data >>> .display-all-fields {
  margin-right: 10px;
}
</style>
