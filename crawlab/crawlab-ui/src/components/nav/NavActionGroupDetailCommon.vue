<template>
  <cl-nav-action-group>
    <cl-nav-action-item v-if="showBackButton">
      <cl-nav-action-button
        v-auth="ACTION_BACK"
        :disabled="disabled"
        :icon="['fa', 'undo']"
        button-type="label"
        :label="t('components.nav.actions.back')"
        type="primary"
        id="back-btn"
        class-name="back-btn"
        @click="() => $emit('back')"
      />
    </cl-nav-action-item>
    <cl-nav-action-item v-if="showSaveButton">
      <cl-nav-action-button
        v-auth="ACTION_SAVE"
        :disabled="disabled"
        :icon="['fa', 'save']"
        button-type="label"
        :label="t('components.nav.actions.save')"
        type="success"
        id="save-btn"
        class-name="save-btn"
        @click="() => $emit('save')"
      />
    </cl-nav-action-item>
  </cl-nav-action-group>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useI18n} from 'vue-i18n';
import {ACTION_BACK, ACTION_SAVE} from '@/constants';

export default defineComponent({
  name: 'NavActionGroupDetailCommon',
  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    showBackButton: {
      type: Boolean,
      default: true
    },
    showSaveButton: {
      type: Boolean,
      default: true
    },
  },
  emits: [
    'back',
  ],
  setup() {
    const {t} = useI18n();

    return {
      t,
      ACTION_BACK,
      ACTION_SAVE,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
