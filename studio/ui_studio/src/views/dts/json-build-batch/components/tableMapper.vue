<template>
  <div class="app-container">
    <el-form label-position="left" label-width="80px" :model="readerForm">
      <el-form-item label="源端表">
        <el-checkbox
          v-model="readerForm.lcheckAll"
          :indeterminate="readerForm.isIndeterminate"
          @change="lHandleCheckAllChange"
        >全选</el-checkbox>
        <div style="margin: 15px 0;" />
        <el-checkbox-group v-model="readerForm.ltables" @change="lHandleCheckedChange">
          <el-checkbox v-for="c in fromTablesList" :key="c" :label="c">{{ c }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="目标表">
        <el-checkbox
          v-model="readerForm.rcheckAll"
          :indeterminate="readerForm.isIndeterminate"
          @change="rHandleCheckAllChange"
        >全选</el-checkbox>
        <div style="margin: 20px 0;" />
        <el-checkbox-group v-model="readerForm.rtables" @change="rHandleCheckedChange">
          <el-checkbox v-for="c in toTablesList" :key="c" :label="c">{{ c }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'TableMapper',
  data() {
    return {
      mapperJson: {},
      fromTablesList: [],
      toTablesList: [],
      readerForm: {
        ltables: [],
        rtables: [],
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
      this.readerForm.ltables = val ? this.fromTablesList : []
      this.readerForm.isIndeterminate = false
    },
    rHandleCheckAllChange(val) {
      this.readerForm.rtables = val ? this.toTablesList : []
      this.readerForm.isIndeterminate = false
    },
    lHandleCheckedChange(value) {
      const checkedCount = value.length
      this.readerForm.checkAll = checkedCount === this.fromTablesList.length
      this.readerForm.isIndeterminate = checkedCount > 0 && checkedCount < this.fromTablesList.length
    },
    rHandleCheckedChange(value) {
      const checkedCount = value.length
      this.readerForm.checkAll = checkedCount === this.toTablesList.length
      this.readerForm.isIndeterminate = checkedCount > 0 && checkedCount < this.toTablesList.length
    },
    getLTables() {
      return this.readerForm.ltables
    },
    getRTables() {
      return this.readerForm.rtables
    }
  }
}
</script>
