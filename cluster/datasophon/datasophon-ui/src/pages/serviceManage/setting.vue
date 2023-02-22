<!--
 * @Author: mjzhu
 * @describe: 服务配置
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-10-27 11:01:48
 * @FilePath: \ddh-ui\src\pages\serviceManage\setting.vue
-->
<template>
  <div class="service-setting steps">
    <div class="flex-bewteen-container" style="flex-direction:row-reverse;">
      <div class="w180" style="margin-right:23px;">
        版本：
        <a-select placeholder="请选择" :value="currentVersion" @change="changeVersion" style="width:180px">
          <a-select-option v-for="(child, childIndex) in verSionList" :key="childIndex" :value="child">{{child}}</a-select-option>
        </a-select>
      </div>
    </div>
    <div class="flex-bewteen-container" style="align-items: baseline; margin-top:10px;">
      <a-spin :spinning="false" class=" w180  setting" style="display: grid;height:300px;">
       <!-- <a-radio-group :default-value="currentId"  @change="changeCasting" style="margin-left:1px;" >
         <a-radio-button :value="item.id" v-for="(item, childIndex) in GroupList" :key="childIndex" :style="radioStyle" >
          {{item.roleGroupName}}
          </a-radio-button>
       </a-radio-group> -->
        <div  v-for="(item, childIndex) in GroupList" :key="childIndex" @click="handlerClick(item,childIndex)" :class="[currentId==item.id ? 'active':'','system']">
          <div :class="[currentId==item.id ? 'active':'','system']">
            {{item.roleGroupName}}
                <!-- <a-icon  type="sync" class="menu-sub-icon" @click="textCompare" /> -->
            <a-popover trigger="hover" placement="rightTop" class="popover-index" overlayClassName="popover-index" :content="()=> getMoreMenu(item)">
                <a-icon type="more" class="fr" />
              </a-popover>
          </div>
        </div>
      </a-spin>
      <a-spin :spinning="loading" class="steps-body" style="position: relative; flex:1; margin:0 20px">
      <CommonTemplate :ref="'CommonTemplateRef'" :class="['']" :steps4Data="steps4Data" :templateData="templateData" />
      <div class="footer">
        <a-button class="mgr10" type="primary" @click="handleSubmit">保存</a-button>
      </div>
     </a-spin>
  
    </div>

  </div>
</template>
<script>
import CommonTemplate from "@/components/commonTemplate/index";
import { mapActions, mapState } from "vuex";
import RenameGroup from "./renameGroup.vue";

export default {
  components: { CommonTemplate },
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      loading: false,
      templateData: [],
      verSionList: [],
      GroupList:[],
      currentId:undefined,
      currentVersion: undefined,
      clusterId: Number(localStorage.getItem("clusterId") || -1),
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
      value:0
    };
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
  },
  methods: {
    ...mapActions("steps", ["setCommandType", "setCommandIds"]),
    getMoreMenu(props) {
      let arr = [
        { name: "重命名", key: "rename" },
        { name: "删除", key: "del" },
      ];
      // if (props.meta.obj.needRestart) arr.splice(2, 0, { name: "重启", key: "restart" })
      return arr.map((item, index) => {
        return (
          <div key={index}>
            <a
              class="more-menu-btn"
              style="border-width:0px;min-width:100px;color: #333;"
              onClick={() => this.batchOpt(item, props)}
            >
              {item.name}
            </a>
          </div>
        );
      });
    },
    changeName (params) {
      this.GroupList.forEach(item => {
        if(item.id === params.roleGroupId) {
          item.roleGroupName = params.roleGroupName
        }
      })
    },
    renameCharacter(props) {
      const self = this;
      let width = 520;
      let title =  "重命名";
      let content = (
        <RenameGroup grouopObj={props} callBack={(params) => self.changeName(params)} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    batchOpt (item, props) {
      if (item.key === 'rename') {
        this.renameCharacter(props)
        return false
      }
      this.$confirm({
        width: 450,
        title: () => {
          return (
            <div style="font-size: 22px;">
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content: (
          <div style="margin-top:20px">
            <div style="padding:0 65px;font-size: 16px;color: #555555;">
              {'确认删除吗？'}
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.confirmDel(item, props)}
              >
                确定
              </a-button>
              <a-button
                style="margin-right:10px;"
                onClick={() => this.$destroyAll()}
              >
                取消
              </a-button>
            </div>
          </div>
        ),
        icon: () => {
          return <div />;
        },
        closable: true,
      });
    },
    confirmDel(item, props) {
      let params = {
        roleGroupId: props.id
      };
      this.$axiosPost(global.API.delGroup, params).then((res) => {
        this.$destroyAll();
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.GroupList = this.GroupList.filter(item => item.id !== props.id)
          if (this.GroupList.length > 0 && this.currentId===props.id) {
            this.currentId = this.GroupList[0].id;
          }
        }
      });
    },
    handlerClick(item,childIndex){
      console.log(item);
      this.currentId = item.id  
      this.getConfigVersion()
    },
    handlearrayWithData(a) {
      let obj = {};
      let arr = [];
      for (var k in a) {
        if (k.includes("arrayWith")) {
          let key = "";
          if (k.includes("arrayWithKey")) {
            key = k.split("arrayWithKey")[0];
            arr.push(key);
          }
          if (k.includes("arrayWithVal")) {
            key = k.split("arrayWithVal")[0];
            arr.push(key);
          }
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      for (var f in obj) {
        let keys = [];
        let vals = [];
        for (var i in a) {
          if (i.includes(f)) {
            if (i.includes("arrayWithKey")) {
              keys.push(i);
            }
            if (i.includes("arrayWithVal")) {
              vals.push(i);
            }
          }
        }
        keys.map((item, index) => {
          obj[f].push({
            [`${a[item]}`]: a[vals[index]],
          });
        });
      }
      return obj;
    },
    handleMultipleData(a) {
      let obj = {};
      let arr = [];
      for (var k in a) {
        if (k.includes("multiple")) {
          let key = k.split("multiple")[0];
          arr.push(key);
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      // obj{ a: , b: }
      for (var f in obj) {
        let vals = [];
        for (var i in a) {
          if (i.includes(f)) {
            if (i.includes("multiple")) {
              vals.push(i);
            }
          }
        }
        vals.map((item, index) => {
          obj[f].push(a[vals[index]]);
        });
      }
      return obj;
    },
    // 单个标签页的保存
    handleSubmit() {
      const self = this
      this.$refs[`CommonTemplateRef`].form.validateFields(
        async (err, values) => {
          if (!err) {
            let param = _.cloneDeep(this.templateData);
            const arrayWithData = this.handlearrayWithData(values);
            const multipleData = this.handleMultipleData(values);
            const formData = { ...values, ...arrayWithData, ...multipleData };
            console.log(formData, "formDataformData");
            for (var name in formData) {
              param.forEach((item) => {
                if (item.name === name) {
                  item.value = formData[name];
                }
              });
            }
            param.forEach((item) => {
              item.name = item.name.replaceAll("!", ".");
            });
            let filterParam = param.filter(
              (item) => !(!item.required && item.hidden)
            );
            console.log(arrayWithData, "arrayWithData", filterParam);
            let serviceName = ''
            const serviceId = this.$route.params.serviceId || ''
            const menuData = JSON.parse(localStorage.getItem('menuData')) || []
            const arr = menuData.filter(item => item.path === 'service-manage')
            if (arr.length > 0) {
              arr[0].children.map(item => {
                if (item.meta.params.serviceId == serviceId) serviceName = item.name
              })
            }
            // 处理表单数据 将相同的key处理成数组
            let saveParam = {
              clusterId: this.clusterId,
              serviceName,
              serviceConfig: JSON.stringify(filterParam),
              roleGroupId:this.currentId
            };
            // // 等待网络请求结束
            let res = await this.$axiosPost(
              global.API.saveServiceConfig,
              saveParam
            );
            if (res.code === 200) {
              this.$message.success("保存成功");
              // this.getConfigVersion()
              // this.getServiceRoleType()
            } else {
              // this.$message.error(res.msg || "保存失败");
            }
          }
        }
      );
    },
    changeVersion(val) {
      this.currentVersion = val;
      this.getServiceConfigOption();
    },
    changeCasting(val){
      console.log(val.target.value);
      this.currentId = val.target.value
      this.getConfigVersion()
    },
  
    //获取角色组
    getServiceRoleType() {
      this.loading = true;
      const params={
        serviceInstanceId: this.$route.params.serviceId,
      }
      this.$axiosPost(global.API.getRoleGroupList, params).then((res) => {
        if (res.code !== 200) return  //this.$message.error('获取角色组列表失败')
        this.GroupList = res.data
        if (this.GroupList.length > 0) {
          this.currentId = this.GroupList[0].id;
          //this.getServiceConfigOption( true);
        }
        this.getConfigVersion()
      })
    },
    // 获取服务版本
    getConfigVersion() {
      this.loading = true;
      const params = {
        serviceInstanceId: this.$route.params.serviceId,
        roleGroupId: JSON.stringify(this.currentId)||'',
      };
      this.$axiosPost(global.API.getConfigVersion, params).then((res) => {
        if (res.code === 200) {
          this.verSionList = res.data;
          if (this.verSionList.length > 0) {
            this.currentVersion = this.verSionList[0];
            this.getServiceConfigOption( true);
          }
        }
      });
    },
    getServiceConfigOption(loading) {
      if (!loading) this.loading = true;
      const self = this;
      const params = {
        serviceInstanceId: this.$route.params.serviceId,
        page: 1,
        pageSize: 10000,
        "version":this.currentVersion||'',
        "roleGroupId": JSON.stringify(this.currentId)||'',
      };
      this.$axiosPost(global.API.getConfigInfo, params).then((res) => {
        if (res.code === 200) {
          self.templateData = self.handlerTemplate(res.data);
          self.loading = false;
        }
      });
    },
    handlerTemplate(data) {
      data.forEach((item) => {
        item.name = item.name.replaceAll(".", "!");
      });
      return data;
    },
  },
  created() {},
  mounted() {
    this.getServiceRoleType()
    // setTimeout(()=>{
    //   this.getConfigVersion()
    // },1000)
  },
};
</script>
<style lang="less" scoped>
.service-setting {
  /deep/ .ant-spin-container {
    position: relative;
  }
  .setting{
     overflow-y: auto;
     font-size: 12px;
     padding-left: 20px;
     color: #000;
     .active{
      color: #fff !important;
      background-color: #2872e0;
      &.ant-form-item{
        color: #fff;
      }
     }
     .system{
        padding: 4px 0 ;
        text-align: center;
        cursor: pointer;
        font-size: 14px;
        .fr {
          float: right;
          position: relative;
          top: 4px;
          right: 4px;
          visibility: hidden;
        }
        &:hover {
          .fr {
            visibility: visible;
          }
        }
     }
      &::-webkit-scrollbar {
      width: 3px;
      height: 1px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 3px;
      background: @primary-color;
    }

    &::-webkit-scrollbar-track {
      -webkit-box-shadow: inset 0 0 1px rgba(0, 0, 0, 0);
      border-radius: 3px;
      background: @primary-3;
    }
  }
  .steps-body {
   max-height: calc(100vh - 240px);
   height: calc(100vh - 240px);

    border: 1px solid #e5e6e8;
    margin: 10px 0;
    padding: 20px 6% 0;
    .footer {
      // margin: 0 32px 0 auto;
      // margin: 0 32px 0 0;
      // margin: 0 auto;
      height: 64px;
      display: flex;
      justify-content: center;
      align-items: center;
      button {
        width: 86px;
      }
      /deep/
        .ant-btn.ant-btn-loading:not(.ant-btn-circle):not(.ant-btn-circle-outline):not(.ant-btn-icon-only) {
        padding-left: 20px;
      }
    }
  }
}
</style> 