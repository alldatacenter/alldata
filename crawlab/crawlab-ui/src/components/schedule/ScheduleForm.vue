<template>
  <cl-form
    v-if="form"
    ref="formRef"
    :model="form"
    :rules="formRules"
    :selective="isSelectiveForm"
    class="schedule-form"
  >
    <!-- Row -->
    <cl-form-item
      :span="2"
      :label="t('components.schedule.form.name')"
      prop="name"
      required
    >
      <el-input
        v-locate="'name'"
        v-model="form.name"
        :disabled="isFormItemDisabled('name')"
        :placeholder="t('components.schedule.form.name')"
      />
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('components.schedule.form.spider')"
      prop="spider_id"
      required
    >
      <el-select
        v-locate="'spider_id'"
        v-model="form.spider_id"
        :disabled="isFormItemDisabled('spider_id')"
        filterable
      >
        <el-option
          v-for="op in allSpiderSelectOptions"
          :key="op.value"
          :label="op.label"
          :value="op.value"
        />
      </el-select>
    </cl-form-item>
    <!-- ./Row -->

    <!-- Row -->
    <cl-form-item
      :span="2"
      :label="t('components.schedule.form.cron')"
      prop="cron"
      required
    >
      <el-input
        v-locate="'cron'"
        v-model="form.cron"
        :disabled="isFormItemDisabled('cron')"
        :placeholder="t('components.schedule.form.cron')"
      />
    </cl-form-item>
    <cl-form-item
      :not-editable="isSelectiveForm"
      :span="2"
      :label="t('components.schedule.form.cronInfo')"
    >
      <div class="nav-btn">
        <cl-schedule-cron :cron="form.cron" icon-only/>
      </div>
    </cl-form-item>
    <!-- ./Row -->

    <!-- Row -->
    <cl-form-item
      :span="2"
      :label="t('components.schedule.form.command')"
      prop="cmd"
    >
      <cl-input-with-button
        v-locate="'cmd'"
        v-model="form.cmd"
        :button-icon="['fa', 'edit']"
        :disabled="isFormItemDisabled('cmd')"
        :button-label="t('common.actions.edit')"
        :placeholder="t('components.schedule.form.command')"
      />
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('components.schedule.form.param')"
      prop="param"
    >
      <cl-input-with-button
        v-locate="'param'"
        v-model="form.param"
        :button-icon="['fa', 'edit']"
        :disabled="isFormItemDisabled('param')"
        :button-label="t('common.actions.edit')"
        :placeholder="t('components.schedule.form.param')"
      />
    </cl-form-item>
    <!-- ./Row -->

    <!-- Row -->
    <cl-form-item
      :span="2"
      :label="t('components.schedule.form.defaultMode')"
      prop="mode"
    >
      <el-select
        v-locate="'mode'"
        v-model="form.mode"
        :disabled="isFormItemDisabled('mode')"
      >
        <el-option
          v-for="op in modeOptions"
          :key="op.value"
          :label="op.label"
          :value="op.value"
        />
      </el-select>
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('components.schedule.form.enabled')"
      prop="enabled"
      required
    >
      <cl-switch
        v-locate="'enabled'"
        v-model="form.enabled" @change="onEnabledChange"
      />
    </cl-form-item>
    <!-- ./Row -->

    <cl-form-item
      v-if="form.mode === TASK_MODE_SELECTED_NODE_TAGS"
      :span="4"
      :label="t('components.schedule.form.selectedTags')"
      prop="node_tags"
      required
    >
      <cl-check-tag-group
        v-locate="'node_tags'"
        v-model="form.node_tags"
        :disabled="isFormItemDisabled('node_tags')"
        :options="allNodeTags"
      />
    </cl-form-item>

    <cl-form-item
      v-if="[TASK_MODE_SELECTED_NODES, TASK_MODE_SELECTED_NODE_TAGS].includes(form.mode)"
      :span="4"
      :label="t('components.schedule.form.selectedNodes')"
      required
    >
      <cl-check-tag-group
        v-locate="'node_ids'"
        v-model="form.node_ids"
        :disabled="form.mode === TASK_MODE_SELECTED_NODE_TAGS && isFormItemDisabled('node_ids')"
        :options="allNodeSelectOptions"
      />
    </cl-form-item>

    <!-- Row -->
    <cl-form-item
      :span="4"
      :label="t('components.schedule.form.description')"
      prop="description"
    >
      <el-input
        v-locate="'description'"
        v-model="form.description"
        :disabled="isFormItemDisabled('description')"
        :placeholder="t('components.schedule.form.description')"
        type="textarea"
      />
    </cl-form-item>
    <!-- ./Row -->
  </cl-form>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useSchedule from '@/components/schedule/schedule';
import useSpider from '@/components/spider/spider';
import {TASK_MODE_SELECTED_NODE_TAGS, TASK_MODE_SELECTED_NODES} from '@/constants/task';
import useNode from '@/components/node/node';
import {ElMessage} from 'element-plus';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'ScheduleForm',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'schedule';
    const store = useStore();

    // use node
    const {
      allListSelectOptions: allNodeSelectOptions,
      allTags: allNodeTags,
    } = useNode(store);

    // use spider
    const {
      allListSelectOptions: allSpiderSelectOptions,
    } = useSpider(store);

    // use schedule
    const {
      form,
    } = useSchedule(store);

    // on enabled change
    const onEnabledChange = async (value: boolean) => {
      if (value) {
        await store.dispatch(`${ns}/enable`, form.value._id);
        ElMessage.success(t('components.schedule.message.success.enable'));
      } else {
        await store.dispatch(`${ns}/disable`, form.value._id);
        ElMessage.success(t('components.schedule.message.success.disable'));
      }

      value ? sendEvent('click_schedule_form_enable') : sendEvent('click_schedule_form_disable');

      await store.dispatch(`${ns}/getList`);
    };

    return {
      ...useSchedule(store),

      allSpiderSelectOptions,
      allNodeSelectOptions,
      allNodeTags,
      TASK_MODE_SELECTED_NODES,
      TASK_MODE_SELECTED_NODE_TAGS,
      onEnabledChange,
      t,
    };
  },
});
</script>

<style scoped>
</style>
