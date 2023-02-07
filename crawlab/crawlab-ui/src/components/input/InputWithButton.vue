<template>
  <div :class="['input-with-button', className].join(' ')">
    <!-- Input -->
    <el-input
      v-model="internalValue"
      :placeholder="placeholder"
      :size="size"
      :id="id"
      class="input"
      :disabled="disabled"
      @input="onInput"
      @blur="onBlur"
      @focus="onFocus"
      @keyup.enter="onBlur"
    />
    <!-- ./Input -->

    <!-- cl-button -->
    <cl-button
      v-if="buttonLabel"
      disabled
      :tooltip="t('common.status.currentlyUnavailable')"
      :size="size"
      :type="buttonType"
      no-margin
      class-name="button"
      @click="onClick"
    >
      <cl-icon v-if="buttonIcon" :icon="buttonIcon"/>
      {{ buttonLabel }}
    </cl-button>
    <template v-else-if="buttonIcon">
      <cl-fa-icon-button
        v-if="isFaIcon"
        :disabled="disabled"
        :icon="buttonIcon"
        :size="size"
        :type="buttonType"
        class-name="button"
        @click="onClick"
      />
      <cl-icon-button
        v-else
        :disabled="disabled"
        :icon="buttonIcon"
        :size="size"
        :type="buttonType"
        class-name="button"
        @click="onClick"
      />
    </template>
    <!-- ./cl-button -->
  </div>
</template>

<script lang="ts">
import {defineComponent, onMounted, PropType, ref, watch} from 'vue';
import useIcon from '@/components/icon/icon';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'InputWithButton',
  props: {
    modelValue: {
      type: String,
    },
    placeholder: {
      type: String,
    },
    size: {
      type: String as PropType<BasicSize>,
      default: 'default',
    },
    buttonType: {
      type: String as PropType<BasicType>,
      default: 'primary',
    },
    buttonLabel: {
      type: String,
      default: 'Click',
    },
    buttonIcon: {
      type: [String, Array] as PropType<string | string[]>,
    },
    disabled: {
      type: Boolean,
      default: true,
    },
    id: {
      type: String,
    },
    className: {
      type: String,
    },
  },
  emits: [
    'update:model-value',
    'input',
    'click',
    'blur',
    'focus',
    'keyup.enter',
  ],
  setup(props: InputWithButtonProps, {emit}) {
    // i18n
    const {t} = useI18n();

    const internalValue = ref<string>();

    const {
      isFaIcon: _isFaIcon,
    } = useIcon();

    const isFaIcon = () => {
      const {buttonIcon} = props;
      if (!buttonIcon) return false;
      return _isFaIcon(buttonIcon);
    };

    watch(() => props.modelValue, () => {
      internalValue.value = props.modelValue;
    });

    const onInput = (value: string) => {
      emit('update:model-value', value);
      emit('input', value);
    };

    const onClick = () => {
      emit('click');
    };

    const onBlur = () => {
      emit('blur');
    };

    const onFocus = () => {
      emit('focus');
    };

    const onKeyUpEnter = () => {
      emit('keyup.enter');
    };

    onMounted(() => {
      const {modelValue} = props;
      internalValue.value = modelValue;
    });

    return {
      internalValue,
      isFaIcon,
      onClick,
      onInput,
      onBlur,
      onFocus,
      onKeyUpEnter,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.input-with-button {
  display: flex;
  align-items: center;
}
</style>

<style scoped>
.input-with-button >>> .input.el-input .el-input__inner {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  /*border-right-color: transparent;*/
}

.input-with-button >>> .button-wrapper .el-button {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.input-with-button >>> .button-wrapper {
  height: 32px;
}
</style>
