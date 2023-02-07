<template>
  <cl-form
    v-if="form"
    ref="formRef"
    :model="form"
    :selective="isSelectiveForm"
  >
    <!--Row-->
    <cl-form-item
      :span="2"
      :label="t('components.node.form.name')"
      not-editable
      prop="name"
      required
    >
      <el-input
        v-locate="'name'"
        v-model="form.name"
        :disabled="isFormItemDisabled('name')"
        :placeholder="t('components.node.form.name')"
      />
    </cl-form-item>
    <cl-form-item
      v-if="readonly"
      :span="2"
      :label="t('components.node.form.key')"
      not-editable
      prop="key"
    >
      <el-input
        v-locate="'key'"
        :model-value="form.key"
        disabled
      />
    </cl-form-item>
    <!--./Row-->

    <!--Row-->
    <!--TODO: implement tags later-->
    <cl-form-item
      v-if="false"
      :span="2"
      :label="t('components.node.form.tags')"
      prop="tags"
    >
      <cl-tag-input v-locate="'tags'" v-model="form.tags" :disabled="isFormItemDisabled('tags')"/>
    </cl-form-item>
    <!--./Row-->

    <!--Row-->
    <cl-form-item
      :span="2"
      :label="t('components.node.form.type')"
      not-editable
      prop="type"
    >
      <cl-node-type
        v-locate="'type'"
        :is-master="form.is_master"
      />
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('components.node.form.ip')"
      prop="ip"
    >
      <el-input
        v-locate="'ip'"
        v-model="form.ip"
        :disabled="isFormItemDisabled('ip')"
        :placeholder="t('components.node.form.ip')"
      />
    </cl-form-item>
    <!--./Row-->

    <!--Row-->
    <cl-form-item
      :span="2"
      :label="t('components.node.form.mac')"
      prop="mac"
    >
      <el-input
        v-locate="'mac'"
        v-model="form.mac"
        :disabled="isFormItemDisabled('mac')"
        :placeholder="t('components.node.form.mac')"
      />
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('components.node.form.hostname')"
      prop="hostname"
    >
      <el-input
        v-locate="'hostname'"
        v-model="form.hostname"
        :disabled="isFormItemDisabled('hostname')"
        :placeholder="t('components.node.form.hostname')"
      />
    </cl-form-item>
    <!--./Row-->

    <!--Row-->
    <cl-form-item
      :span="2"
      :label="t('components.node.form.enabled')"
      prop="enabled"
    >
      <cl-switch
        v-locate="'enabled'"
        v-model="form.enabled"
        :disabled="isFormItemDisabled('enabled')"
        @change="onEnabledChange"
      />
    </cl-form-item>
    <cl-form-item
      :span="2"
      :label="t('components.node.form.max_runners')"
      prop="max_runners"
    >
      <el-input-number
        v-locate="'max_runners'"
        v-model="form.max_runners"
        :disabled="isFormItemDisabled('max_runners')"
        :min="0"
        :placeholder="t('components.node.form.max_runners')"
      />
    </cl-form-item>
    <!--./Row-->

    <!--Row-->
    <cl-form-item
      :span="4"
      :label="t('components.node.form.description')"
      prop="description"
    >
      <el-input
        v-locate="'description'"
        v-model="form.description"
        :disabled="isFormItemDisabled('description')"
        :placeholder="t('components.node.form.description')"
        type="textarea"
      />
    </cl-form-item>
  </cl-form>
  <!--./Row-->
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useNode from '@/components/node/node';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'NodeForm',
  props: {
    readonly: {
      type: Boolean,
    }
  },
  setup(props, {emit}) {
    // i18n
    const {t} = useI18n();

    // store
    const store = useStore();

    const onEnabledChange = (value: boolean) => {
      sendEvent(value ? 'click_node_form_enable' : 'click_node_form_disable');
    };

    return {
      ...useNode(store),
      onEnabledChange,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
