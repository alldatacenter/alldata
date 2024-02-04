<template>
  <div class="app-container">
    <TableReader ref="tablereader" @selectDataSource="showDataSource" />
  </div>
</template>

<script>
import TableReader from './reader/tableReader'
export default {
  name: 'Reader',
  components: { TableReader },
  data() {
    return {
      dataSource: ''
    }
  },
  methods: {
    getData() {
      return this.$refs.tablereader.getData()
    },
    showDataSource(data) {
      this.dataSource = data
      this.getData()
    }
  }
}
</script>
