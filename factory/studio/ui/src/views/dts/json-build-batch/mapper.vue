<template>
  <div class="app-container">
    <TableMapper ref="mapper" />
  </div>
</template>
<script>
import TableMapper from './components/tableMapper'
export default {
  name: 'Mapper',
  components: { TableMapper },
  methods: {
    sendTables(fromTablesList, toTablesList) {
      this.$refs.mapper.fromTablesList = fromTablesList
      this.$refs.mapper.toTablesList = toTablesList
    },
    getLTables() {
      return this.$refs.mapper.getLTables()
    },
    getRTables() {
      return this.$refs.mapper.getRTables()
    }
  }
}
</script>
