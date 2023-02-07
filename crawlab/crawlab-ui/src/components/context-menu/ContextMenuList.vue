<template>
  <ul class="context-menu-list">
    <li
      v-for="(item, $index) in items"
      :key="$index"
      :class="['context-menu-item', item.className].join(' ')"
      @click="onClick(item)"
    >
      <span class="prefix">
        <template v-if="item.icon">
          <font-awesome-icon v-if="Array.isArray(item.icon)" :icon="item.icon"/>
          <cl-atom-material-icon v-else-if="typeof item.icon === 'string'" :is-dir="false" :name="item.icon"/>
        </template>
      </span>
      <span class="title">
        {{ item.title }}
      </span>
    </li>
  </ul>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue';

export default defineComponent({
  name: 'ContextMenuList',
  props: {
    items: {
      type: Array as PropType<ContextMenuItem[]>,
      default: () => {
        return [];
      },
    },
  },
  setup(props, {emit}) {
    const onClick = (item: ContextMenuItem) => {
      if (!item.action) return;
      item.action();
      emit('hide');
    };

    return {
      onClick,
    };
  },
});
</script>

<style lang="scss" scoped>
.context-menu-list {
  list-style: none;
  margin: 0;
  padding: 0;
  min-width: auto;

  .context-menu-item {
    height: var(--cl-context-menu-item-height);
    max-width: var(--cl-context-menu-item-max-width);
    display: flex;
    align-items: center;
    margin: 0;
    padding: 10px;
    cursor: pointer;

    &:hover {
      background-color: var(--cl-primary-plain-color);
    }

    .title {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .prefix {
      width: 24px;
      display: flex;
      align-items: center;
    }
  }
}
</style>
