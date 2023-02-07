<template>
  <div class="git-file-status">
    <span class="file-name" :style="fileNameStyle">
      {{ fileStatus.name }}
    </span>
    <span class="file-path">
      {{ fileStatus.path }}
    </span>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';
import {emptyObjectFunc} from '@/utils/func';

export default defineComponent({
  name: 'GitFileStatus',
  props: {
    fileStatus: {
      type: Object as PropType<GitChange>,
      default: emptyObjectFunc,
    },
  },
  setup(props: GitFileProps, {emit}) {
    const fileNameStyle = computed<Partial<CSSStyleDeclaration>>(() => {
      const {fileStatus} = props;
      switch (fileStatus?.worktree) {
        case '?':
          return {color: 'var(--cl-danger-color)'}
        case 'M':
          return {color: 'var(--cl-primary-color)'}
        case 'A':
          return {color: 'var(--cl-success-color)'}
        case 'D':
          return {color: 'var(--cl-info-color)'}
        case 'R':
          return {color: 'var(--cl-primary-color)'}
        case 'C':
          return {color: 'var(--cl-primary-color)'}
        case 'U':
          return {color: 'var(--cl-danger-color)'}
        default:
          return {};
      }
    });

    return {
      fileNameStyle,
    };
  },
});
</script>

<style scoped lang="scss">
.git-file-status {
  .file-name {
  }

  .file-path {
    margin-left: 10px;
    font-size: 11px;
    color: var(--cl-info-medium-light-color);
  }
}
</style>
