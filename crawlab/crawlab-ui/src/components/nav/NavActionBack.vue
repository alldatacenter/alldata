<template>
  <cl-nav-action-group>
    <cl-nav-action-button
        :button-type="buttonType"
        :disabled="disabled"
        :icon="icon"
        :label="t(label)"
        :tooltip="tooltip"
        type="primary"
        @click="() => $emit('click')"
    />
  </cl-nav-action-group>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'NavActionBack',
  props: {
    buttonType: {
      type: String as PropType<ButtonType>,
      default: 'label',
    },
    label: {
      type: String,
      default: 'components.nav.actions.back'
    },
    tooltip: {
      type: String,
    },
    icon: {
      type: [String, Array] as PropType<Icon>,
      default: () => {
        return ['fa', 'undo'];
      }
    },
    type: {
      type: String as PropType<BasicType>,
      default: 'primary'
    },
    size: {
      type: String as PropType<BasicSize>,
      default: 'default'
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: [
    'click',
  ],
  setup() {
    const {t} = useI18n();

    return {
      t,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
