
<template>
  <div class="search">
    <Card>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
      ></Table>
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
    <!-- 修改模态框 -->
    <Modal v-model="formValidate" title="详细信息" width="500">
      <Form
        ref="formValidate"
        :model="form"
        :label-width="80"
      >
        <FormItem label="用户名" prop="userName">
          <span> {{form.userName}}</span>
        </FormItem>
        <FormItem label="手机号码" prop="mobile">
          <span> {{form.mobile}}</span>
        </FormItem>
        <FormItem label="类型" prop="type">
          <span v-if="form.type == 'FUNCTION'">功能建议</span>
          <span v-if="form.type == 'OPTIMIZE'">优化反馈</span>
          <span v-if="form.type == 'OTHER'">其他意见</span>
        </FormItem>
        <FormItem label="反馈内容" prop="context">
          <Input style="width: 85%" v-model="form.context" type="textarea" disabled :autosize="{minRows: 3,maxRows: 5}"
          ></Input>
        </FormItem>
        <FormItem label="相关材料" prop="images">
          <div v-if="form.images == null">
            暂无
          </div>
          <div v-else>
            <span v-for="(item, index) in this.form.images.split(',')" :key="index">
              <Avatar shape="square" icon="ios-person" size="large" style="width: 80px;height: 90px;margin-right: 5px" :src="item"
              />
            </span>
          </div>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="formValidate = false">取消</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
  import {
    getMemberFeedback,
    getMemberFeedbackDetail
  } from "@/api/other";

  export default {
    name: "feedback",
    data() {
      return {
        loading: true, // 加载状态
        form: {}, // 表单数据
        searchForm: { // 请求参数
          pageNumber: 1,
          pageSize: 10,
        },
        formValidate: false, // modal显隐
        columns: [ // 表头
          {
            title: "会员名称",
            key: "userName",
            minWidth: 120,
            tooltip: true

          },
          {
            title: "手机号码",
            key: "mobile",
            minWidth: 120,
            tooltip: true

          },
          {
            title: "反馈内容",
            key: "context",
            minWidth: 380,
            tooltip: true
          },
          {
            title: "类型",
            key: "type",
            minWidth: 120,
            render: (h, params) => {
              if (params.row.type == "FUNCTION") {
                return h('div', [h('span', {}, "功能建议")]);
              } else if (params.row.type == "OPTIMIZE") {
                return h('div', [h('span', {}, "优化反馈")]);
              } else if (params.row.type == "OTHER") {
                return h('div', [h('span', {}, "其他意见")]);
              } else {
                return h('div', [h('span', {}, "未知意见")]);
              }
            }
          },
          {
            title: "创建时间",
            key: "createTime",
            width: 170,
            sortable: true,
          },
          {
            title: "操作",
            key: "action",
            align: "center",
            width: 130,
            render: (h, params) => {
              return h("div", [
                h(
                  "Button",
                  {
                    props: {
                      size: "small",
                      type: "info"
                    },
                    style: {
                      marginRight: "5px",
                    },
                    on: {
                      click: () => {
                        this.detail(params.row);
                      },
                    },
                  },
                  "查看"
                )
              ]);
            },
          },
        ],
        data: [], // 表格数据
        total: 0 // 数据总数

      };
    },
    methods: {
      // 初始化数据
      init() {
        this.getFeedback();
      },
      // 分页 修改页码
      changePage(v) {
        this.searchForm.pageNumber = v;
        this.getFeedback();
      },
      // 分页 修改页数
      changePageSize(v) {
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = v;
        this.getFeedback();
      },
      /**
       * 查询意见反馈
       */
      getFeedback() {
        this.loading = true;
        getMemberFeedback(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.data = res.result.records;
            this.total = res.result.total;
          }
        });
      },
      /**
       * 投诉建议详细
       */
      detail(v) {
        getMemberFeedbackDetail(v.id).then((res) => {
          this.loading = false;
          if (res.success) {
            this.$set(this, "form", res.result);
            this.formValidate = true
          }
        });
      }
    },
    mounted() {
      this.init();
    },
  };
</script>
