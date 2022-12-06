<template>
  <div class="app-container">
    <FieldMapper ref="mapper" />
  </div>
</template>
<script>
import FieldMapper from './components/fieldMapper'
export default {
  name: 'Mapper',
  components: { FieldMapper },
  methods: {
    sendColumns(fromColumnsList, toColumnsList) {
      this.$refs.mapper.fromColumnsList = fromColumnsList
      this.$refs.mapper.toColumnsList = toColumnsList
    },
    getLColumns() {
      return this.$refs.mapper.getLColumns()
    },
    getRColumns() {
      return this.$refs.mapper.getRColumns()
    }
  }
}
</script>
