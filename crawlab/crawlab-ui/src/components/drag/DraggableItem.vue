<template>
  <div
    :class="classes"
    :draggable="true"
    class="draggable-item"
    @dragstart="$emit('d-start', item)"
    @dragend="$emit('d-end', item)"
    @dragenter="$emit('d-enter', item)"
    @dragleave="$emit('d-leave', item)"
  >
    <cl-draggable-item-content :item="item"/>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, PropType} from 'vue';

export default defineComponent({
  name: 'DraggableItem',
  props: {
    item: {
      type: Object as PropType<DraggableItemData>,
      required: true,
    },
    dragging: {
      type: Boolean,
      default: false,
    }
  },
  emits: [
    'd-start',
    'd-end',
    'd-enter',
    'd-leave',
  ],
  setup(props: DraggableItemProps) {
    const dragging = computed(() => {
      const {item} = props;
      return item.dragging;
    });

    const classes = computed(() => {
      const cls = [];
      if (dragging.value) cls.push('dragging');
      return cls;
    });

    return {
      classes,
    };
  },
});
</script>

<style lang="scss" scoped>
.draggable-item {
  position: relative;

  &.dragging {
    visibility: hidden;
  }

  &.dragging * {
    pointer-events: none;
  }
}
</style>
