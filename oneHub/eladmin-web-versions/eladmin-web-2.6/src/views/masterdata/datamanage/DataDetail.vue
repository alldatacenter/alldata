<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" label-width="80px" disabled>
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
import { getData } from '@/api/masterdata/datamanage'

export default {
  name: 'DataDetail',
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
      title: '主数据详情',
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      // 表单参数
      form: {},
      tableName: '',
      columnList: []
    }
  },
  created() {
    console.log('data:' + this.data)
    this.getFormData()
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    async getFormData() {
      const data = await getFormParam(this.data.modelId).then(response => {
        if (response.success) {
          return response.data
        }
      })
      this.tableName = data.tableName
      const columns = data.columnList.filter(item => item.isDetail === '1')
      const formData = await getData({ id: this.data.id, tableName: this.tableName }).then(response => {
        if (response.success) {
          return response.data
        }
      })
      // checkbox特殊处理
      columns.filter(item => item.htmlType === 'checkbox').forEach((item, index, arr) => {
        this.$set(formData, item.columnName, formData[item.columnName].split(',') || [])
      })
      this.columnList = columns
      this.form = formData
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
