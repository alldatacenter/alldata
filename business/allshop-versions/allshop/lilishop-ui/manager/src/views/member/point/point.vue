<template>
  <div class="search">
    <Card style="padding:0 10px 10px 0">
      <Form
        @keydown.enter.native="handleSearch"
        ref="searchForm"
        :model="searchForm"
        inline
        style="margin-top:10px"
        :label-width="70"
        class="search-form"
      >
        <Form-item label="会员名称" prop="username">
          <Input
            type="text"
            v-model="searchForm.memberName"
            placeholder="请输入会员名称"
            clearable
            style="width: 200px"
          />
          </Form-item>
        <Button @click="handleSearch" class="search-btn" type="primary" icon="ios-search">搜索</Button >
      </Form>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        class="mt_10"
      >
      </Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page
          :current="searchForm.pageNumber"
          :total="total"
          :page-size="searchForm.pageSize"
          @on-change="changePage"
          @on-page-size-change="changePageSize"
          :page-size-opts="[10, 20, 50]"
          size="small"
          show-total
          show-elevator
          show-sizer
        ></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
  import region from "@/views/lili-components/region";
  import * as API_Member from "@/api/member.js";
  import ossManage from "@/views/sys/oss-manage/ossManage";

  export default {
    // 积分历史页面
    name: "point",
    components: {
      region,
      ossManage,
    },
    data() {
      return {
        loading: true, // 表单加载状态
        searchForm: { // 请求参数
          pageNumber: 1,
          pageSize: 10,
        },
        columns: [
          {
            title: "会员名称",
            key: "memberName",
            minWidth: 120,
            tooltip: true
          },
          {
            title: "操作内容",
            key: "content",
            minWidth: 180,
            tooltip: true
          },

          {
            title: "之前积分",
            key: "beforePoint",
            width: 110,
          },

          {
            title: "变动积分",
            key: "variablePoint",
            width: 110,
            render: (h, params) => {
              if (params.row.pointType == 'INCREASE') {
                return h('div', [
                  h('span', {
                    style: {
                      color: 'green'
                    }
                  }, "+" + params.row.variablePoint),
                ]);
              } else {
                return h('div', [
                  h('span', {
                    style: {
                      color: 'red'
                    }
                  }, '-' + params.row.variablePoint),
                ]);
              }

            }
          },
          {
            title: "当前积分",
            key: "point",
            width: 110,
          },
          {
            title: "操作时间",
            key: "createTime",
            width: 170
          },

        ],
        data: [], // 表单数据
        total: 0, // 表单数据总数
      };
    },
    methods: {
      // 回调给父级
      callback(val) {
        this.$emit("callback", val);
      },
      // 初始化数据
      init() {
        this.getData();
      },
      // 分页 改变页码
      changePage(v) {
        this.searchForm.pageNumber = v;
        this.getData();
      },
      // 分页 改变页数
      changePageSize(v) {
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = v;
        this.getData();
      },
      // 搜索
      handleSearch() {
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = 10;
        this.getData();
      },
      //查新积分列表
      getData() {
        this.loading = true;
        API_Member.getHistoryPointData(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.data = res.result.records;
            this.total = res.result.total;
          }
        });
        this.loading = false;
      },
    },
    mounted() {
      this.init();
    },
  };
</script>
<style lang="scss" scoped>
  .face {
    width: 60px;
    height: 60px;
    border-radius: 50%;
  }
</style>
