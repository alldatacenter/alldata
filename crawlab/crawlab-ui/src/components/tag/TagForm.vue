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
        :label="t('components.tag.form.name')"
        not-editable
        prop="name"
        required
    >
      <el-input
          v-model="form.name"
          :disabled="isFormItemDisabled('name')"
          :placeholder="t('components.tag.form.name')"
      />
    </cl-form-item>
    <cl-form-item
        :span="2"
        :label="t('components.tag.form.color')"
        prop="color"
        required
    >
      <cl-color-picker
          v-model="form.color"
          :predefine="predefinedColors"
          class="color-picker"
          show-alpha
      />
    </cl-form-item>
    <cl-form-item
        :span="4"
        :label="t('components.tag.form.description')"
        prop="description"
    >
      <el-input
          v-model="form.description"
          :disabled="isFormItemDisabled('description')"
          :placeholder="t('components.tag.form.description')"
          type="textarea"
      />
    </cl-form-item>
  </cl-form>
</template>

<script lang="ts">
import {defineComponent, readonly} from 'vue';
import {useStore} from 'vuex';
import useTag from '@/components/tag/tag';
import {getPredefinedColors} from '@/utils/color';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'TagForm',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const store = useStore();

    // predefined colors
    const predefinedColors = readonly<string[]>(getPredefinedColors());

    return {
      ...useTag(store),
      predefinedColors,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
