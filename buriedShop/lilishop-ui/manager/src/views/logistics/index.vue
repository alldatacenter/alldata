<template>
  <div class="search">
    <Card>
      <Row class="operation padding-row">
        <Button @click="add" type="primary">添加</Button>
      </Row>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
      >
        <!-- 页面展示 -->
        <template slot="disableSlot" slot-scope="{row}">
          <i-switch size="large" :value="row.switch" @on-change="changeSwitch(row)">
            <span slot="open">开启</span>
            <span slot="close">禁用</span>
          </i-switch>
        </template>
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
    <Modal
      :title="modalTitle"
      v-model="modalVisible"
      :mask-closable="false"
      :width="500"
    >
      <Form ref="form" :model="form" :label-width="120" :rules="formValidate">
        <FormItem label="物流公司名称" prop="name">
          <Input v-model="form.name" clearable style="width: 100%"/>
        </FormItem>
        <FormItem label="物流公司代码" prop="code">
          <Input v-model="form.code" clearable style="width: 100%"/>
        </FormItem>
        <FormItem label="支持电子面单">
          <i-switch v-model="form.standBy" size="large">
            <span slot="OPEN">开</span>
            <span slot="CLOSE">关</span>
          </i-switch>
        </FormItem>
        <FormItem label="电子面单表单">
          <Input v-model="form.formItems" clearable style="width: 100%"/>
        </FormItem>
        <FormItem label="禁用状态" prop="disabled">
          <i-switch v-model="form.disabled" size="large">
            <span slot="OPEN">开启</span>
            <span slot="CLOSE">禁用</span>
          </i-switch>
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
  import {
    getLogisticsPage,
    updateLogistics,
    addLogistics,
    delLogistics,
  } from "@/api/logistics";

  export default {
    name: "logistics",
    data() {
      return {
        loading: true, // 表单加载状态
        modalVisible: false, // 添加或编辑显示
        modalTitle: "", // 添加或编辑标题
        searchForm: {
          // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 20, // 页面大小
          sort: "createTime", // 默认排序字段
          order: "desc", // 默认排序方式
          name: "",
        },
        form: {
          // 添加或编辑表单对象初始化数据
          name: "",
          disabled:"OPEN"
        },
        // 表单验证规则
        formValidate: {
          name: [
            {
              required: true,
              message: "请输入物流公司名称",
              trigger: "blur",
            },
          ],
        },
        submitLoading: false, // 添加或编辑提交状态
        columns: [
          {
            title: "物流公司名称",
            key: "name",
            minWidth: 120,
            sortable: false,
          },
          {
            title: "物流公司编码",
            key: "code",
            minWidth: 120,
            sortable: false,
          },
          {
            title: "状态",
            key: "disabled",
            width: 150,
            slot: "disableSlot",
          },
          {
            title: "创建时间",
            key: "createTime",
            width: 180,
            sortable: false,
          },
          {
            title: "操作",
            key: "action",
            align: "center",
            width: 150,
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
                      marginRight: "5px",
                    },
                    on: {
                      click: () => {
                        this.detail(params.row);
                      },
                    },
                  },
                  "修改"
                ),
                h(
                  "Button",
                  {
                    props: {
                      type: "error",
                      size: "small",
                    },
                    style: {
                      marginRight: "5px",
                    },
                    on: {
                      click: () => {
                        this.remove(params.row);
                      },
                    },
                  },
                  "删除"
                ),
              ]);
            },
          },
        ],
        data: [], // 表单数据
        total: 0, // 表单数据总数
      };
    },
    methods: {
      // 初始化
      init() {
        this.getDataList();
      },
      // 分页 改变页码
      changePage(v) {
        this.searchForm.pageNumber = v;
        this.getDataList();
      },
      // 分页 改变页数
      changePageSize(v) {
        this.searchForm.pageSize = v;
        this.getDataList();
      },
      // 获取列表
      getDataList() {
        this.loading = true;

        getLogisticsPage(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            const data = res.result.records;
            data.forEach(e => {
              e.switch = e.disabled === 'OPEN' ? true : false
            });
            this.data = data;
            this.total = res.result.total;
          }
        });
        this.total = this.data.length;
        this.loading = false;
      },
      // switch 切换状态
      changeSwitch (v) {
        this.form.name = v.name;
        this.form.code = v.code;
        this.form.standBy = v.standBy;
        this.form.formItems = v.formItems;
        this.form.disabled = v.disabled === 'CLOSE' ? 'OPEN' : 'CLOSE';
        updateLogistics(v.id, this.form).then((res) => {
          if (res.success) {
            this.$Message.success("操作成功");
            this.getDataList();
          }
        });
      },
      // 确认提交
      handleSubmit() {
        this.$refs.form.validate((valid) => {
          if (valid) {
            this.submitLoading = true;
            this.form.disabled = this.form.disabled == true ? "OPEN" : "CLOSE"
            if (this.modalTitle == "添加") {
              // 添加 避免编辑后传入id等数据 记得删除
              delete this.form.id;

              this.form.disabled
                ? (this.form.disabled = "OPEN")
                : (this.form.disabled = "CLOSE");
              addLogistics(this.form).then((res) => {
                this.submitLoading = false;
                if (res.success) {
                  this.$Message.success("操作成功");
                  this.getDataList();
                  this.modalVisible = false;
                }
              });
            } else {
              // 编辑
              updateLogistics(this.id, this.form).then((res) => {
                this.submitLoading = false;
                if (res.success) {
                  this.$Message.success("操作成功");
                  this.getDataList();
                  this.modalVisible = false;
                }
              });
            }
          }
        });
      },
      // 添加信息
      add() {
        this.modalTitle = "添加";
        this.form = {};
        this.$refs.form.resetFields();

        this.modalVisible = true;
      },
      // 编辑
      detail(v) {
        this.id = v.id;
        this.modalTitle = "修改";
        this.modalVisible = true;

        this.form.name = v.name;
        this.form.code = v.code;
        this.form.standBy = v.standBy;
        this.form.formItems = v.formItems;
        this.form.disabled = v.disabled;
        this.form.disabled == "OPEN"
          ? (this.form.disabled = true)
          : (this.form.disabled = false);
      },
      // 删除物流公司
      remove(v) {
        this.$Modal.confirm({
          title: "确认删除",
          // 记得确认修改此处
          content: "您确认要删除 " + v.name + " ?",
          loading: true,
          onOk: () => {
            // 删除
            delLogistics(v.id).then((res) => {
              this.$Modal.remove();
              if (res.success) {
                this.$Message.success("操作成功");
                this.getDataList();
              }
            });
          },
        });
      },
    },
    mounted() {
      this.init();
    },
  };
</script>
