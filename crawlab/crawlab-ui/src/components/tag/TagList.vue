<template>
  <cl-tag
    v-for="tag in tags"
    :key="tag._id"
    :clickable="clickable"
    :tag="tag"
    @click="onClick"
  />
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';
import {emptyArrayFunc} from '@/utils/func';

export default defineComponent({
  name: 'TagList',
  props: {
    tags: {
      type: Array as PropType<Tag[]>,
      default: emptyArrayFunc,
    },
    clickable: {
      type: Boolean,
      default: true,
    },
  },
  emits: [
    'click'
  ],
  setup(props: TagListProps, {emit}) {
    const onClick = (tag: Tag) => {
      emit('click', tag);
    };

    return {
      onClick,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
