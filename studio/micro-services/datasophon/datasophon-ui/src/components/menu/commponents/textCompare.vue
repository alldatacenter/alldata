<template>
  <div class="steps8">
    <div style="display:flex; justify-content: space-between;" >
       <div class=" w180" style="display: grid;"> 
          <a-radio-group :default-value="currentList"  @change="changeCasting" style="margin-left:10px;"  >
            <a-radio-button :value="item.id" v-for="(item, childIndex) in GroupList" :key="childIndex" :style="radioStyle"  button-style="solid">
              {{item.roleGroupName}}
            </a-radio-button>
          </a-radio-group>
        </div>
      <div class="diff_list"> 
        <code-diff v-show="!loading"  class="center" :old-string="oldStr" :new-string="newStr"  :context="context" outputFormat="side-by-side" />
      </div>
    </div>
    <div class="ant-modal-confirm-btns-new">
      <a-button
        style="margin-right: 10px"
        type="primary"
        @click.stop="formCancel"
        >重启过时服务</a-button
      >
    </div>
  </div>
 
</template>
<script>
import CodeDiff from 'vue-code-diff'
export default {
  name:'textCompare',
  props: {
    serviceId: {
      type: Object,
      default: function () {
        return {};
      },
    },
    callBack:Function
  },
  components: {CodeDiff},
  data() {
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 5 },
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 19 },
      },
      radioStyle: {
        display: 'block',
        height: '30px',
        lineHeight: '30px',
        marginTop:'5px' ,
      },
      form: this.$form.createForm(this),
      value1: "",
      loading: true ,
      currentList:undefined,
      GroupList:[],  //列表
      textConfig:{},
      modifiedVal:[],
      origionText:[],
      oldStr: 'old code',
      newStr: 'new code',
      context: 1000000 //不同地方上下间隔多少行不隐藏
    };
  },
  watch: {
  },
  methods: {
    mapTotag() {},
    formCancel() {
      const params={
        "roleGroupId": this.currentList,
      }
      console.log(params);
      this.$axiosPost(global.API.restartObsoleteService, params).then((res) => {
        if (res.code !== 200) return  
        this.$message.success("重启服务成功");
        this.$destroyAll();
      })
    },
    changeCasting(val){
      console.log(val);
      this.currentList = val.target.value
      this.handleSubmit();

    },
    handleSubmit() {
      const _this = this
      const params = {
        "serviceInstanceId":this.serviceId.id,
        "roleGroupId": this.currentList,
      }
      console.log(params);
      this.$axiosPost(global.API.configVersionCompare, params).then((res) => {  
        this.loading = false;
        if (res.code !== 200) return
        // _this.callBack();
        console.log(res);
        this.textConfig = res.data
        this.origionText = res.data.oldConfig
        this.modifiedVal = res.data.newConfig
        this.oldStr = JSON.stringify(this.origionText, null, 4);
        this.newStr = JSON.stringify(this.modifiedVal, null, 4);
      }).catch((err) => {});
      
      
    },
    getServiceRoleType() {
      const params={
        serviceInstanceId :this.serviceId.id
      }
      //this.loading = true;
      //角色组列表
      this.$axiosPost(global.API.getRoleGroupList, params).then((res) => {
        if (res.code !== 200) return  
        this.GroupList = res.data
        if (this.GroupList.length > 0) {
          this.currentList = this.GroupList[0].id;
          this.handleSubmit()
        }
        
      })
    }
  },
  mounted() {
    this.getServiceRoleType()
  },
};
</script>
<style lang="less" scoped>
.diff_list {
  flex:1;
  max-height: 600px;
  overflow-y: auto;
  overflow-x: hidden;
  // overflow-x: auto;
  margin-left: 10px;
}


 /deep/ .d2h-code-line-ctn  .hljs{
    display: inline;
    
   }
/deep/ .d2h-code-side-line {
  height: 18px;
}
// /deep/ .d2h-wrapper>.d2h-code-side-line,
// .d2h-wrapper .d2h-code-line {
//   height: 18px;
// }
// /deep/ .d2h-code-line-prefix,
// .d2h-code-linenumber,
// .d2h-code-side-linenumber,
// .d2h-emptyplaceholder {
//   height: 18px;
// }

// ::v-deep.d2h-code-side-linenumber {
// 	  position: relative;
// 	}
</style>
