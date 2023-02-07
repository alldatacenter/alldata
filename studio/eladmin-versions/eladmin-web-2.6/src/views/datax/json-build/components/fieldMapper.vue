<template>
  <div class="app-container">
    <el-form label-position="left" label-width="80px" :model="readerForm">
      <el-form-item label="源端字段">
        <el-checkbox
          v-model="readerForm.lcheckAll"
          :indeterminate="readerForm.isIndeterminate"
          @change="lHandleCheckAllChange"
        >全选</el-checkbox>
        <div style="margin: 15px 0;" />
        <el-checkbox-group v-model="readerForm.lcolumns" @change="lHandleCheckedChange">
          <el-checkbox v-for="c in fromColumnsList" :key="c" :label="c">{{ c }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="目标字段">
        <el-checkbox
          v-model="readerForm.rcheckAll"
          :indeterminate="readerForm.isIndeterminate"
          @change="rHandleCheckAllChange"
        >全选</el-checkbox>
        <div style="margin: 20px 0;" />
        <el-checkbox-group v-model="readerForm.rcolumns" @change="rHandleCheckedChange">
          <el-checkbox v-for="c in toColumnsList" :key="c" :label="c">{{ c }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'FieldMapper',
  data() {
    return {
      mapperJson: {},
      fromColumnsList: [],
      toColumnsList: [],
      readerForm: {
        lcolumns: [],
        rcolumns: [],
        lcheckAll: false,
        rcheckAll: false,
        isIndeterminate: true
      }
    }
  },
  mounted() {
  },
  methods: {
    lHandleCheckAllChange(val) {
      this.readerForm.lcolumns = val ? this.fromColumnsList : []
      this.readerForm.isIndeterminate = false
    },
    rHandleCheckAllChange(val) {
      this.readerForm.rcolumns = val ? this.toColumnsList : []
      this.readerForm.isIndeterminate = false
    },
    lHandleCheckedChange(value) {
      const checkedCount = value.length
      this.readerForm.checkAll = checkedCount === this.fromColumnsList.length
      this.readerForm.isIndeterminate = checkedCount > 0 && checkedCount < this.fromColumnsList.length
    },
    rHandleCheckedChange(value) {
      const checkedCount = value.length
      this.readerForm.checkAll = checkedCount === this.toColumnsList.length
      this.readerForm.isIndeterminate = checkedCount > 0 && checkedCount < this.toColumnsList.length
    },
    getLColumns() {
      return this.readerForm.lcolumns
    },
    getRColumns() {
      return this.readerForm.rcolumns
    }
  }
}
</script>
