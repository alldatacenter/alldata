<template>
  <div class="search">
    <Card>
      <Tabs value="RETURN_MONEY" @on-click="handleClickType">
        <TabPane label="退款" name="RETURN_MONEY">
          <Row class="operation" style="margin-bottom: 10px">
            <Button @click="add"  type="primary" >添加</Button>
          </Row>
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
        </TabPane>
        <TabPane label="取消" name="CANCEL">
          <Row class="operation" style="margin-bottom: 10px">
            <Button @click="add" type="primary" icon="md-add">添加</Button>
          </Row>
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
        </TabPane>
        <TabPane label="退货" name="RETURN_GOODS">
          <Row class="operation" style="margin-bottom: 10px">
            <Button @click="add" type="primary" icon="md-add">添加</Button>
          </Row>
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
        </TabPane>
        <TabPane label="投诉" name="COMPLAIN">
          <Row class="operation" style="margin-bottom: 10px">
            <Button @click="add" type="primary" icon="md-add">添加</Button>
            <Button @click="getDataList" icon="md-refresh">刷新</Button>
          </Row>
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
        </TabPane>
      </Tabs>
    </Card>
    <Modal
      :title="modalTitle"
      v-model="modalVisible"
      :mask-closable="false"
      :width="500"
    >
      <Form ref="form" :model="form" :label-width="100" :rules="formValidate">
        <FormItem label="售后原因" prop="reason">
          <Input v-model="form.reason" maxlength="20" clearable style="width: 100%"/>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="handleSubmit"
        >提交
        </Button
        >
      </div>
    </Modal>
  </div>
</template>
<script>
  import * as API_Order from "@/api/order";

  export default {
    data() {
      return {
        modalVisible: false,//添加售后原因弹出框
        modalTitle: "", //添加售后原因弹出框标题
        loading: true, // 表单加载状态
        submitLoading: false, // 添加或编辑提交状态
        form: {
          reason: ""
        },//添加编辑表单
        formValidate: {
          reason: [
            {
              required: true,
              message: "请输入售后原因",
              trigger: "blur",
            },
          ],
        },
        searchForm: {
          // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
          sort: "createTime", // 默认排序字段
          order: "desc", // 默认排序方式
          serviceType: "RETURN_MONEY"
        },
        columns: [
          {
            title: "创建人",
            key: "createBy",
            minWidth: 120,
          },
          {
            title: "原因",
            key: "reason",
            minWidth: 400,
          },
          {
            title: "时间",
            key: "createTime",
            minWidth: 100,
          },
          {
            title: "操作",
            key: "action",
            align: "center",
            width: 200,
            render: (h, params) => {
              return h("div", [
                h(
                  "Button",
                  {
                    props: {
                      type: "info",
                      size: "small",
                    },
                    style: {
                      marginRight: "5px"
                    },
                    on: {
                      click: () => {
                        this.edit(params.row);
                      }
                    }
                  },
                  "编辑"
                ),
                h(
                  "Button",
                  {
                    props: {
                      type: "error",
                      size: "small",
                    },
                    on: {
                      click: () => {
                        this.remove(params.row);
                      }
                    }
                  },
                  "删除"
                )
              ]);
            },
          },
        ],
        data: [], // 表单数据
        total: 0,//条数
      };
    },

    methods: {
      // 分页 修改页码
      changePage(v) {
        this.searchForm.pageNumber = v;
        this.getDataList();
        this.clearSelectAll();
      },
      // 分页 修改页数
      changePageSize(v) {
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = v;
        this.getDataList();
      },
      //切换tab
      handleClickType(v) {
        this.searchForm.pageNumber = 1 // 当前页数
        this.searchForm.pageSize = 10 // 页面大小
        //退款
        if (v == "RETURN_MONEY") {
          this.searchForm.serviceType = "RETURN_MONEY"
        }
        //退货
        if (v == "RETURN_GOODS") {
          this.searchForm.serviceType = "RETURN_GOODS"
        }
        //取消
        if (v == "CANCEL") {
          this.searchForm.serviceType = "CANCEL"
        }
        //取消
        if (v == "COMPLAIN") {
          this.searchForm.serviceType = "COMPLAIN"
        }
        this.getDataList();
      },
      //获取售后原因数据
      getDataList() {
        this.loading = true;
        API_Order.getAfterSaleReasonPage(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.data = res.result.records;
            this.total = res.result.total
          }
        });
        this.loading = false;
      },
      //添加售后原因
      add() {
        this.form.reason = ""
        this.modalVisible = true
        this.modalTitle = "添加售后原因"
      },
      //修改售后原因
      edit(v) {
   
        this.form.reason = v.reason
        this.form.id = v.id
  
        this.modalVisible = true
        this.modalTitle = "修改售后原因"
      },
      //提交表单
      handleSubmit() {
        this.form.serviceType = this.searchForm.serviceType
        this.$refs.form.validate((valid) => {
          if (valid) {
            this.submitLoading = true;
            if (this.modalTitle == '添加售后原因') {
              // 添加
              delete this.form.id;
              API_Order.addAfterSaleReason(this.form).then((res) => {
                this.submitLoading = false;
                if (res.success) {
                  this.$Message.success("添加成功");
                  this.getDataList();
                  this.modalVisible = false;
                }
              });
            } else {
              // 编辑
              API_Order.editAfterSaleReason(this.form.id, this.form).then((res) => {
                this.submitLoading = false;
                if (res.success) {
                  this.$Message.success("修改成功");
                  this.getDataList();
                  this.modalVisible = false;
                }
              });
            }
          }
        });
      },
      //删除售后原因
      remove(v) {
        this.$Modal.confirm({
          title: "确认删除",
          // 记得确认修改此处
          content: "确认要删除此售后原因?",
          loading: true,
          onOk: () => {
            // 删除
            API_Order.delAfterSaleReason(v.id).then((res) => {
              this.$Modal.remove();
              if (res.success) {
                this.$Message.success("售后原因已删除");
                this.getDataList();
              }
            });
          },
        });
      }

    },
    mounted() {
      this.getDataList();
    },
  };
</script>
