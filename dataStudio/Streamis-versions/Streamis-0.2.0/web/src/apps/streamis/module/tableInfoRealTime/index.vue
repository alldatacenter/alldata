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
          <FormItem label="可用用户:" :label-width="labelWidth">
            <Input v-model="tableInfo.grantUser" style="width: 300px" placeholder="多个用户请用逗号隔开"/>
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
          <FormItem label="描述:" :label-width="labelWidth">
            <Input v-model="tableInfo.description" type="textarea" :autosize="{minRows: 2,maxRows: 5}" style="width: 300px"></Input>
          </FormItem>
        </Col>
      </Row>
      <Row>
        <Col span="18">
        </Col>
        <Col span="6">
          <FormItem>
            <Button type="primary" @click="saveDataSource" size="large" style="width: 90px">保存</Button>
            <Button @click="cancelDataSource" style="margin-left: 8px; width: 90px" size="large">取消</Button>
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
        scope: '',
        grantUser: '',
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
        this.tableInfo = this.formData
      },
    },
    tableInfo: {
      handler(n) {
        let a = JSON.stringify(this.formData)
        let b = JSON.stringify(n)
        this.$emit('change', a !== b , n)
        this.$emit('update:formData', n) // 更新
      },
      deep: true
    }
  },
  methods: {
    saveDataSource(){
      this.$emit('saveDataSource')
    },
    cancelDataSource(){
      this.$emit('cancelDataSource')
    }
  }
}
</script>

<style lang="scss">
.tableInfo{
  padding: 20px 60px;
}
</style>
