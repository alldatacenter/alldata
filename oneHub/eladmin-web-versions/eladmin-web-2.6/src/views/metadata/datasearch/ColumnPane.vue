<template>
  <ul class="list-group">
    <li v-for="(item, index) in data" :key="index" class="list-group-item">
      <span class="list-group-item-heading">{{item.columnName}}({{item.columnComment}})</span>
      <p class="list-group-item-text">数据源: {{item.sourceName}} 数据表: {{item.tableName}}({{item.tableComment}})</p>
    </li>
  </ul>
</template>

<script>
export default {
  name: 'ColumnPane',
  props: {
    data: {
      type: Array,
      default: function() {
        return []
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.list-group {
  margin-bottom: 20px;
  padding-left: 0;
  .list-group-item {
    position: relative;
    display: block;
    padding: 10px 15px;
    margin-bottom: -1px;
    background-color: #fff;
    border: 1px solid #ddd;
    &:first-child {
      border-top-left-radius: 4px;
      border-top-right-radius: 4px;
    }
    &:last-child {
      margin-bottom: 0;
      border-bottom-left-radius: 4px;
      border-bottom-right-radius: 4px;
    }
    .list-group-item-heading {
      margin-top: 0;
      margin-bottom: 5px;
    }
    .list-group-item-text {
      margin-bottom: 0;
      line-height: 1.3;
      font-size: 14px;
    }
  }
}
</style>
