<template>
  <cl-button @click="onClick">{{value}}</cl-button>
  <cl-button type="danger" @click="onClick">{{value}}</cl-button>
  <cl-fa-icon-button :icon="['fa', 'plus']" circle/>
  <cl-form>
    <cl-form-item label="Button Text">
      <el-input v-model="value"/>
    </cl-form-item>
  </cl-form>
  <cl-table
      :columns="tableColumns"
      :data="tableData"
  />
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';

export default defineComponent({
  name: 'App',
  setup(props, {emit}) {
    const value = ref('Button');

    const tableColumns = [
      {
        key: 'key',
        label: 'Key',
      },
      {
        key: 'value',
        label: 'Value',
      },
    ];

    const tableData = [
      {key: 'a', value: 1},
      {key: 'b', value: 2},
      {key: 'c', value: 3},
    ];

    const onClick = () => {
      console.log(`clicked ${value.value}`)
    };

    return {
      value,
      tableColumns,
      tableData,
      onClick,
    };
  },
});
</script>
