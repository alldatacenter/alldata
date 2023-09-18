<!--
 * @Author: mjzhu
 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2022-07-27 16:15:06
 * @FilePath: \ddh-ui\src\pages\securityCenter\user.vue
-->

<template>

  <a-tabs @change="onSearch">
    <a-tab-pane :key="item.key" :tab="item.title"  v-for="item in [{title:'用户列表',key:'user'},{title:'用户组列表',key:'userGroup'}]">
      <div class="user-list">
        <a-card class="mgb16 card-shadow">
          <a-row type="flex" align="middle">
            <a-col :span="22">
              <a-input placeholder="请输入用户名" v-if="item.key == 'user'" class="w252 mgr12" @change="(value) => getVal(value, 'username')" allowClear />
              <a-input placeholder="请输入用户组名"  v-if="item.key == 'userGroup'" class="w252 mgr12" @change="(value) => getVal(value, 'groupName')" allowClear />
              <a-button class type="primary" icon="search" @click="onSearch(item.key)"></a-button>
            </a-col>
            <a-col :span="2" style="text-align: right">
              <a-button style="margin-left: 10px;" type="primary" @click="createUser({},item.key)">{{item.key == "user" ?'添加用户':'添加用户组'}}</a-button>
            </a-col>
          </a-row>
        </a-card>
        <a-card class="card-shadow">  
          <div class="table-info steps-body">
            <a-table @change="(pagination)=>{this.tableChange(pagination,item.key)}" :columns="item.key == 'user' ?columns : groupColumns" :loading="loading" :dataSource="dataSource" rowKey="id" :pagination="pagination"></a-table>
          </div>
        </a-card>  
      </div>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import AddUser from "./commponents/addUser.vue";
import DelectUser from "./commponents/delectUser.vue";
import AddUserGroup from "./commponents/addUserGroup.vue";
import DelectUserGroup from "./commponents/delectUserGroup.vue";
import { mapGetters, mapState, mapMutations } from "vuex";

export default {
  name: "USER",
  data() {
    return {
      params: {},
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      username:'',
      dataSource: [],
      loading: false,
      groupColumns:[
        {
          title: "序号",
          key: "index",
          width: 70,
          customRender: (text, row, index) => {
            return (
              <span>
                {parseInt(
                  this.pagination.current === 1
                    ? index + 1
                    : index +
                        1 +
                        this.pagination.pageSize * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        { title: "组名", key: "groupName", dataIndex: "groupName" },
        {
          title: "用户",
          key: "clusterUsers",
          dataIndex: "clusterUsers",
        },
        {
          title: "操作",
          key: "action",
          width: 300,
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                  <a class="btn-opt" onClick={() => this.delectUser(row,'userGroup')}>
                    删除
                  </a> 
                </span>
            );
          },
        },
      ],
      columns: [
        {
          title: "序号",
          key: "index",
          width: 70,
          customRender: (text, row, index) => {
            return (
              <span>
                {parseInt(
                  this.pagination.current === 1
                    ? index + 1
                    : index +
                        1 +
                        this.pagination.pageSize * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        { title: "用户名", key: "username", dataIndex: "username" },
        {
          title: "主用户组",
          key: "mainGroup",
          dataIndex: "mainGroup",
        },
        {
          title: "附属用户组",
          key: "otherGroups",
          dataIndex: "otherGroups",
        },
        {
          title: "操作",
          key: "action",
          width: 300,
          customRender: (text, row, index) => {
            return (
              <span class="flex-container">
                  <a class="btn-opt" onClick={() => this.delectUser(row)}>
                    删除
                  </a>
                </span>
            );
          },
        },
      ],
    };
  },
  computed: {
    ...mapGetters("account", ["user"]),
  },
  methods: {
    tableChange(pagination,key) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.getUserList(key == 'userGroup' ? key : null);
    },
    getVal(val, filed) {
      this.params[`${filed}`] = val.target.value;
    },
    //   查询
    onSearch(key) {
      this.pagination.current = 1;
      this.getUserList(key == 'userGroup' ? key : null);
    },
    createUser(obj,key) {
      const self = this;
      let width = 520;
      let title = JSON.stringify(obj) === "{}" ? "添加用户" : "编辑用户";
      let content = (
        <AddUser detail={obj} callBack={() => self.getUserList()} />
      );
      if(key == 'userGroup'){
        title = JSON.stringify(obj) === "{}" ? "添加用户组" : "编辑用户组";
        content = <AddUserGroup detail={obj} callBack={() => self.getUserList('userGroup')} />
      }
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
    delectUser(obj,key) {
      const self = this;
      let width = 400;
      let content = (
        <DelectUser
          sysTypeTxt="用户"
          detail={obj}
          callBack={() => self.getUserList()}
        />
      );
      if(key == 'userGroup'){
        content = <DelectUserGroup detail={obj} callBack={() => self.getUserList(key == 'userGroup' ? key : null)} />
      }
      this.$confirm({
        width: width,
        title: () => {
          return (
            <div>
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    getUserList(key) {
      this.loading = true;
      let params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
      };
      if(key){
        params.groupName =  this.params.groupName?this.params.groupName : ''
      }else{
        params.username =  this.params.username?this.params.username:''
      }
      this.$axiosPost(key?global.API.getTenantGroup:global.API.getTenant, params).then((res) => {
        this.loading = false;
        console.log(res);
        this.dataSource = res.data;
        this.pagination.total = res.total;
      });
    },
  },
  mounted() {
    this.getUserList();
  },
};
</script>

<style lang="less" scoped>
.user-list {
  background: #f5f7f8;
  .btn-opt {
    border-radius: 1px;
    font-size: 12px;
    color: #0264c8;
    letter-spacing: 0;
    font-weight: 400;
    margin: 0 5px;
  }
}
</style>
