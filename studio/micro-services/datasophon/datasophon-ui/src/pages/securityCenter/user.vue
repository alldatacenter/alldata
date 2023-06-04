<!--
 * @Author: mjzhu
 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2022-10-27 16:41:30
 * @FilePath: \ddh-ui\src\pages\securityCenter\user.vue
-->

<template>
  <div class="user-list">
    <a-card class="mgb16 card-shadow">
      <a-row type="flex" align="middle">
        <a-col :span="22">
          <a-input placeholder="请输入用户名" class="w252 mgr12" @change="(value) => getVal(value, 'username')" allowClear />
          <a-button class type="primary" icon="search" @click="onSearch"></a-button>
        </a-col>
        <a-col :span="2" style="text-align: right">
          <a-button style="margin-left: 10px;" type="primary" @click="createUser({})">添加用户</a-button>
        </a-col>
      </a-row>
    </a-card>
    <a-card class="card-shadow">  
      <div class="table-info steps-body">
        <a-table @change="tableChange" :columns="columns" :loading="loading" :dataSource="dataSource" rowKey="id" :pagination="pagination"></a-table>
      </div>
    </a-card>  
  </div>
</template>

<script>
import AddUser from "./commponents/addUser.vue";
import DelectUser from "./commponents/delectUser.vue";
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
        // {
        //   title: "角色",
        //   key: "createUser",
        //   dataIndex: "createUser",
        // },
        {
          title: "邮箱",
          key: "email",
          dataIndex: "email",
        },
        { title: "电话", key: "phone", dataIndex: "phone" },
        { title: "创建时间", key: "createTime", dataIndex: "createTime" },
        {
          title: "操作",
          key: "action",
          width: 140,
          customRender: (text, row, index) => {
            return (
              this.username == "admin" ? (
                <span class="flex-container">
                  <a
                    class="btn-opt"
                    onClick={() => this.createUser(row)}
                  >
                    编辑
                  </a>
                  <a-divider type="vertical" />
                  { row.userType !==1?  <a class="btn-opt" onClick={() => this.delectUser(row)}>
                    删除
                  </a> :<a class="btn-opt" style="color: #bbb">
                    删除
                  </a>}
                 
                </span>
              ) :
                row.username == this.username ? (
                  <span class="flex-container">
                    <a
                      class="btn-opt"
                      onClick={() => this.createUser(row)}
                    >
                    编辑
                    </a>
                    <a-divider type="vertical" />
                    <a class="btn-opt" onClick={() => this.delectUser(row)}>
                    删除
                    </a>
                  </span>
                ) :  (
                  <span class="flex-container">
                    <a
                      class="btn-opt" style="color: #bbb"
                    //onClick={() => this.createUser(row)}
                    >
                    编辑
                    </a>
                    <a-divider type="vertical" />
                    <a class="btn-opt" style="color: #bbb">
                    删除
                    </a>
                  </span>
                )
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
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.getUserList();
    },
    getVal(val, filed) {
      this.params[`${filed}`] = val.target.value;
    },
    //   查询
    onSearch() {
      this.pagination.current = 1;
      this.getUserList();
    },
    createUser(obj) {
      const self = this;
      let width = 520;
      let title = JSON.stringify(obj) === "{}" ? "添加用户" : "编辑用户";
      let content = (
        <AddUser detail={obj} callBack={() => self.getUserList()} />
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
    delectUser(obj) {
      const self = this;
      let width = 400;
      let content = (
        <DelectUser
          sysTypeTxt="用户"
          detail={obj}
          callBack={() => self.getUserList()}
        />
      );
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
    getUserList() {
      this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        username: this.params.username || "",
      };
      this.username =  this.user.username
      this.$axiosPost(global.API.getUserList, params).then((res) => {
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
  // background: #f5f7f8;
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
