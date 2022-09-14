<template>
  <div class="tableInfo">
    <Form ref="formValidate" :model="tableInfo">
      <Row>
        <Col span="12">
          <FormItem>
            <div slot="label" style="margin-left: 34px;"><span style="color: red">*</span>表名</div>
            <Input v-model="tableInfo.tableName" style="width: 300px"/>
          </FormItem>
        </Col>
        <Col span="12">
          <FormItem label="别名:" :label-width="labelWidth">
            <Input type="text" v-model="tableInfo.alias" style="width: 300px"></Input>
          </FormItem>
        </Col>
      </Row>
      <Row>
        <Col span="12">
          <FormItem label="标签:" :label-width="labelWidth">
            <Input type="text" v-model="tableInfo.tags" style="width: 300px" placeholder="多个标签请用逗号隔开"></Input>
          </FormItem>
        </Col>
        <Col span="12">
          <FormItem>
            <div slot="label" style="margin-left: 34px;"><span style="color: red">*</span>作用域</div>
            <Select v-model="tableInfo.scope" style="width:300px">
              <Option v-for="item in scopeList" :value="item.value" :key="item.value">{{ item.label }}</Option>
            </Select>
          </FormItem>
        </Col>
      </Row>
      <Row>
        <Col span="12">
          <FormItem>
            <div slot="label" style="margin-left: 34px;"><span style="color: red">*</span>所属分层</div>
            <Select v-model="tableInfo.layer" style="width:300px">
              <Option v-for="item in layerList" :value="item.value" :key="item.value">{{ item.label }}</Option>
            </Select>
          </FormItem>
        </Col>
        <Col span="12">
          <FormItem label="描述:" :label-width="labelWidth">
            <Input v-model="tableInfo.description" type="textarea" :autosize="{minRows: 2,maxRows: 5}" style="width: 300px"></Input>
          </FormItem>
        </Col>
      </Row>
    </Form>
  </div>
</template>

<script>
export default {
  props: {
    formData: {
      type: Object,
    }
  },
  data(){
    return{
      labelWidth: 80,
      tableInfo: {
        tableName: '',
        alias: '',
        tags: '',
        description: '',
        id: ''
      },
      scopeList: [
        {
          value: '任务级',
          label: '任务级'
        },
        {
          value: '工程级',
          label: '工程级'
        }
      ],
      layerList: [
        {
          value: 'ODS',
          label: 'ODS'
        },
        {
          value: '维表',
          label: '维表'
        },
        {
          value: 'DWD',
          label: 'DWD'
        },
        {
          value: 'DWS',
          label: 'DWS'
        }
      ]
    }
  },
  watch: {
    formData: {
      handler() {
        if (this.formData) {
          this.tableInfo = this.formData
        }
      },
    },
    tableInfo: {
      handler(n) {
        this.$emit('update:formData', n) // 更新
      },
      deep: true
    }
  },
  methods: {
  }
}
</script>

<style lang="scss">
.tableInfo{
  padding: 20px 60px;
}
</style>
