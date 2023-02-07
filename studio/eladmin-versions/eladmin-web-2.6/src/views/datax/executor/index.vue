<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="el-icon-edit" @click="handleCreate">
        添加
      </el-button>
    </div>
    <el-table
      v-loading="listLoading"
      :data="list"
      element-loading-text="Loading"
      border
      fit
      highlight-current-row
    >
      <el-table-column label="排序" width="50" align="center">
        <template slot-scope="scope">{{ scope.row.order }}</template>
      </el-table-column>
      <el-table-column label="名称" width="120" align="center" :show-overflow-tooltip="true">
        <template slot-scope="scope">{{ scope.row.title }}</template>
      </el-table-column>
      <el-table-column label="分组标识" width="200" align="center">
        <template slot-scope="scope">{{ scope.row.appName }}</template>
      </el-table-column>
      <el-table-column label="注册方式" width="110" align="center" :show-overflow-tooltip="true">
        <template slot-scope="scope"> {{ addressTypes.find(t => t.value === scope.row.addressType).label }}</template>
      </el-table-column>
      <el-table-column label="在线机器" align="center" :show-overflow-tooltip="true">
        <template slot-scope="scope">{{ scope.row.addressList }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
        <template slot-scope="{row}">
          <el-button type="primary" size="mini" @click="handleUpdate(row)">
            编辑
          </el-button>
          <el-button v-if="row.status!='deleted'" size="mini" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="listQuery.current" :limit.sync="listQuery.size" @pagination="fetchData" />

    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="80px" style="width: 400px; margin-left:50px;">
        <el-form-item label="AppName" prop="appName">
          <el-input v-model="temp.appName" placeholder="AppName" />
        </el-form-item>
        <el-form-item label="名称" prop="title">
          <el-input v-model="temp.title" placeholder="请输入执行器名称" />
        </el-form-item>
        <el-form-item label="排序" prop="prop">
          <el-input v-model="temp.order" placeholder="执行器序号" />
        </el-form-item>
        <el-form-item label="注册方式" prop="addressType">
          <el-radio-group v-model="temp.addressType">
            <el-radio :label="0">自动注册</el-radio>
            <el-radio :label="1">手动录入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="机器地址" prop="addressList">
          <el-input v-model="temp.addressList" :disabled="dialogStatus!=='create'" placeholder="多个以逗号分隔" />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="dialogStatus==='create'?createData():updateData()">
          确定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as executor from '@/api/dts/datax-executor'
import waves from '@/directive/waves' // waves directive
import Pagination from '@/components/Pagination' // secondary package based on el-pagination

export default {
  name: 'Executor',
  components: { Pagination },
  directives: { waves },
  filters: {
    statusFilter(status) {
      const statusMap = {
        published: 'success',
        draft: 'gray',
        deleted: 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      list: null,
      listLoading: true,
      total: 0,
      listQuery: {
        current: 1,
        size: 10,
        name: undefined,
        jobGroup: undefined
      },
      editJsonVisible: false,
      pluginData: [],
      dialogFormVisible: false,
      dialogStatus: '',
      textMap: {
        update: 'Edit',
        create: 'Create'
      },
      rules: {
        appName: [{ required: true, message: 'appName is required', trigger: 'blur' }],
        title: [{ required: true, message: 'title is required', trigger: 'blur' }],
        order: [{ required: true, message: 'title is required', trigger: 'blur' }],
        addressType: [{ required: true, message: 'title is required', trigger: 'change' }]
      },
      temp: {
        id: undefined,
        appName: undefined,
        title: undefined,
        order: undefined,
        addressType: undefined,
        addressList: undefined
      },
      addressTypes: [
        { value: 0, label: '自动注册' },
        { value: 1, label: '手动录入' }
      ]
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.listLoading = true
      executor.getList().then(response => {
        const { content } = response
        this.list = content
        this.listLoading = false
      })
    },
    resetTemp() {
      this.temp = {
        id: undefined,
        appName: undefined,
        title: undefined,
        order: undefined,
        addressType: undefined,
        addressList: undefined
      }
    },
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    createData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          executor.created(this.temp).then(() => {
            this.fetchData()
            this.dialogFormVisible = false
            this.$notify({
              title: 'Success',
              message: 'Created Successfully',
              type: 'success',
              duration: 2000
            })
          })
        }
      })
    },
    handleUpdate(row) {
      this.temp = Object.assign({}, row) // copy obj
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    updateData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          const tempData = Object.assign({}, this.temp)
          tempData.configJson = this.configJson
          executor.updated(tempData).then(() => {
            this.fetchData()
            this.dialogFormVisible = false
            this.$notify({
              title: 'Success',
              message: 'Update Successfully',
              type: 'success',
              duration: 2000
            })
          })
        }
      })
    },
    handleDelete(row) {
      executor.deleted(row.id).then(response => {
        this.fetchData()
        this.$notify({
          title: 'Success',
          message: 'Delete Successfully',
          type: 'success',
          duration: 2000
        })
      })
      // const index = this.list.indexOf(row)
    },
    handleFetchPv(id) {
      executor.fetch(id).then(response => {
        this.pluginData = response
        this.dialogPvVisible = true
      })
    }
  }
}
</script>
