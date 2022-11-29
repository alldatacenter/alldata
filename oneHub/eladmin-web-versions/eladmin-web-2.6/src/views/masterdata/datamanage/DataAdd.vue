<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button v-hasPerm="['masterdata:data:add']" size="mini" icon="el-icon-plus" round :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{ loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" label-width="80px">
        <el-form-item
          v-for="(item, index) in columnList"
          :key="item.id"
          :label="item.columnComment"
          :prop="item.columnName"
          :rules="[{ required: item.isRequired, message: item.columnComment + '不能为空' }]"
        >
          <template v-if="item.htmlType === 'input'">
            <el-input
              v-model.trim="form[item.columnName]"
              :placeholder="'请输入' + item.columnComment"
              :maxlength="parseInt(item.columnLength)"
            />
          </template>
          <template v-if="item.htmlType === 'textarea'">
            <el-input
              type="textarea"
              v-model.trim="form[item.columnName]"
              :placeholder="'请输入' + item.columnComment"
              :maxlength="parseInt(item.columnLength)"
            />
          </template>
          <template v-if="item.htmlType === 'number'">
            <el-input-number
              v-model.trim="form[item.columnName]"
              :controls="false"
              :precision="parseInt(item.columnScale)"
            ></el-input-number>
          </template>
          <template v-if="item.htmlType === 'select'">
            <template v-if="item.isBindDict === '1' && item.dictList && item.dictList.length > 0">
              <el-select v-model.trim="form[item.columnName]" placeholder="请选择">
                <el-option
                  v-for="item in item.dictList"
                  :key="item.id"
                  :label="item.label"
                  :value="item.value">
                </el-option>
              </el-select>
            </template>
          </template>
          <template v-if="item.htmlType === 'radio'">
            <template v-if="item.isBindDict === '1' && item.dictList && item.dictList.length > 0">
              <el-radio-group v-model.trim="form[item.columnName]">
                <el-radio
                  v-for="item in item.dictList"
                  :key="item.id"
                  :label="item.value"
                >{{ item.label }}</el-radio>
              </el-radio-group>
            </template>
          </template>
          <template v-if="item.htmlType === 'checkbox'">
            <template v-if="item.isBindDict === '1' && item.dictList && item.dictList.length > 0">
              <el-checkbox-group v-model.trim="form[item.columnName]">
                <el-checkbox
                  v-for="item in item.dictList"
                  :key="item.id"
                  :label="item.value"
                >{{ item.label }}</el-checkbox>
              </el-checkbox-group>
            </template>
          </template>
          <template v-if="item.htmlType === 'datetime'">
            <template v-if="item.columnType === 'date'">
              <el-date-picker
                v-model.trim="form[item.columnName]"
                format="yyyy-MM-dd"
                value-format="yyyy-MM-dd"
                type="date"
                placeholder="选择日期"
              ></el-date-picker>
            </template>
            <template v-if="item.columnType === 'time'">
              <el-time-picker
                v-model="form[item.columnName]"
                format="HH:mm:ss"
                value-format="HH:mm:ss"
                placeholder="选择时间点">
              </el-time-picker>
            </template>
            <template v-if="item.columnType === 'year'">
              <el-date-picker
                v-model.trim="form[item.columnName]"
                format="yyyy"
                value-format="yyyy"
                type="year"
                placeholder="选择年份"
              ></el-date-picker>
            </template>
            <template v-if="item.columnType === 'datetime'">
              <el-date-picker
                v-model.trim="form[item.columnName]"
                format="yyyy-MM-dd HH:mm:ss"
                value-format="yyyy-MM-dd HH:mm:ss"
                type="datetime"
                placeholder="选择日期时间"
              ></el-date-picker>
            </template>
          </template>
        </el-form-item>
      </el-form>
    </div>
  </el-card>
</template>

<script>
import { getFormParam } from '@/api/masterdata/datamodel'
import { addData } from '@/api/masterdata/datamanage'

export default {
  name: 'DataAdd',
  props: {
    data: {
      type: Object,
      default: function() {
        return {}
      }
    }
  },
  data() {
    return {
      title: '主数据新增',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      // 保存按钮
      loadingOptions: {
        loading: false,
        loadingText: '保存',
        isDisabled: false
      },
      // 表单参数
      form: {},
      tableName: '',
      columnList: []
    }
  },
  created() {
    console.log('data:' + this.data)
    getFormParam(this.data.modelId).then(response => {
      if (response.success) {
        const { data } = response
        this.tableName = data.tableName
        this.columnList = data.columnList.filter(item => item.isInsert === '1')
        // checkbox特殊处理
        this.columnList.filter(item => item.htmlType === 'checkbox').forEach((item, index, arr) => {
          this.$set(this.form, item.columnName, [])
        })
      }
    })
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          const data = JSON.parse(JSON.stringify(this.form))
          // checkbox特殊处理
          this.columnList.filter(item => item.htmlType === 'checkbox').forEach((item, index, arr) => {
            this.$set(data, item.columnName, data[item.columnName].join(','))
          })
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          addData({ tableName: this.tableName, datas: data }).then(response => {
            if (response.success) {
              this.$message.success('保存成功')
              setTimeout(() => {
                // 2秒后跳转列表页
                this.$emit('showCard', this.showOptions)
              }, 2000)
            } else {
              this.$message.error('保存失败')
              this.loadingOptions.loading = false
              this.loadingOptions.loadingText = '保存'
              this.loadingOptions.isDisabled = false
            }
          }).catch(() => {
            this.loadingOptions.loading = false
            this.loadingOptions.loadingText = '保存'
            this.loadingOptions.isDisabled = false
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
  height: calc(100vh - 230px);
  overflow-y: auto;
}
</style>
