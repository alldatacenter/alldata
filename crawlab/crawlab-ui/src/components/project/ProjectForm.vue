<template>
  <cl-form
    v-if="form"
    ref="formRef"
    :model="form"
    :rules="formRules"
    :selective="isSelectiveForm"
  >
    <cl-form-item
      :span="2"
      :offset="2"
      :label="t('components.project.form.name')"
      not-editable
      prop="name"
      required
    >
      <el-input
        v-locate="'name'"
        v-model="form.name"
        :disabled="isFormItemDisabled('name')"
        :placeholder="t('components.project.form.name')"
      />
    </cl-form-item>
    <cl-form-item
      v-if="false"
      :span="2"
      :label="t('components.project.form.tags')"
      prop="tags"
    >
      <cl-tag-input v-model="form.tags" :disabled="isFormItemDisabled('tags')"/>
    </cl-form-item>
    <cl-form-item
      :span="4"
      :label="t('components.project.form.description')"
      prop="description"
    >
      <el-input
        v-locate="'description'"
        v-model="form.description"
        :disabled="isFormItemDisabled('description')"
        :placeholder="t('components.project.form.description')"
        type="textarea"
      />
    </cl-form-item>
  </cl-form>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useProject from '@/components/project/project';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'ProjectForm',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const store = useStore();

    return {
      ...useProject(store),
      t,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
