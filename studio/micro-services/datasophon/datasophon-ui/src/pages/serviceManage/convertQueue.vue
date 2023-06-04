
<template>
  <div style="padding-top: 20px">
    <a-form :label-col="labelCol" :wrapper-col="wrapperCol" :form="form" class="p0-32-10-32 form-content">
      <a-form-item label="队列名称" >
        <a-input :disabled="type =='show'"
            v-decorator="['queueName', {rules: [{ required: true, message: '队列名称不能为空' }],initialValue:this.pageData.queueName || ''}]"
            placeholder="请输入队列名称"
          />
      </a-form-item>
      <a-form-item label="资源占比" >
        <a-slider id="test" :disabled="type =='show'" 
        v-decorator="['capacity', {rules: [{ required: true, message: '数据源不能为空' }],initialValue:Number(this.pageData.capacity || '0')}]"/>
      </a-form-item>
      <a-form-item label="关联标签" >
        <a-select showSearch allowClear :disabled="type =='show'"
          v-decorator="['nodeLabel', {rules: [{ required: true, message: '关联标签不能为空' }],
          initialValue:this.pageData.nodeLabel}]" placeholder="请选择关联标签"  >
          <a-select-option v-for="list in cateList" :key="list.nodeLabel" :value="list.nodeLabel"  :title="list.nodeLabel">  {{list.nodeLabel}} </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="允许提交用户" >
        <a-select showSearch allowClear :disabled="type =='show'" mode="multiple"
          v-decorator="['aclUsers', {rules: [{ required: true, message: '允许提交用户不能为空' }],
          initialValue:this.pageData.aclUsers?this.pageData.aclUsers.split(','):[]}]" placeholder="请选择允许提交用户"  >
          <a-select-option v-for="list in userList" :key="list.username" :value="list.username"  :title="list.username">  {{list.username}} </a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
    
    <div class="ant-modal-confirm-btns-new">
      <a-button  :disabled="type =='show'"
        style="margin-right: 10px"
        type="primary"
        @click.stop="handleSubmit"
        :loading="loading"
        >确认</a-button
      >
      <a-button @click.stop="formCancel">取消</a-button>
    </div>
  </div>
</template>
<script>
export default {
  name: "convertQueue",
  props: {
    obj:{
      type:Object,
      default: function () {
        return {};
      },
    },
    type:{
      type:String,
      default: function () {
        return '';
      },
    },
    callBack:Function
  },
  data() {
    console.log(this.obj,'this.obj')
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 5 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 19 },
      },
      form: this.$form.createForm(this),
      loading: false,
      cateList: [], //类型
      userList:[],  //列表
      pageData:this.obj.data || {},
      clusterId: Number(localStorage.getItem("clusterId") || -1),
    };
  },
  watch: {},
  methods: {
    formCancel() {
      this.$destroyAll();
    },
    getUserList() {
      this.$axiosPost(global.API.getTenant, {
        pageSize:10000,
        username:'',
        page:1
      }).then((res) => {
        this.userList = res.data || [];
      });
    },
    getLabelList() {
      this.$axiosPost(global.API.getLabelList, {
        clusterId: this.clusterId,
      }).then((res) => {
        if (res.code === 200) {
          this.cateList = res.data;
        }
      });
    },
    handleSubmit(e) {
      const _this = this;
      e.preventDefault();
      this.form.validateFields((err, values) => {
        if (!err) {
          const params = {
              "queueName":values.queueName,
              "clusterId":_this.clusterId,
              "capacity":values.capacity,
              "nodeLabel":values.nodeLabel,
              "aclUsers":values.aclUsers.join(','),
          }
          if(_this.pageData.id){
            params['id'] = _this.pageData.id
            params['parent'] = _this.pageData.parent
          } else {
            params['parent'] = _this.obj.parent
          }
          this.loading = true;
          this.$axiosJsonPost('/ddh/cluster/queue/capacity/'+this.type, params).then((res) => {  
            this.loading = false;
            if (res.code !== 200) return
            this.$message.success('修改成功')
            this.$destroyAll();
            _this.callBack(params);
          }).catch((err) => {});
        }
      });
    },
  },
  created(){
    this.getUserList();
    this.getLabelList();
  },
  mounted() {
  },
};
</script>
<style lang="less" scoped>
</style>
