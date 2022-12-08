<template>
  <div class="search">
    <Card>
      <Form
        @keydown.enter.native="handleSearch"
        ref="searchForm"
        :model="searchForm"
        inline
        :label-width="70"
        class="search-form"
      >
        <Form-item label="规格名称" prop="specName">
          <Input
            type="text"
            v-model="searchForm.specName"
            placeholder="请输入规格名称"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Button @click="handleSearch" type="primary" class="search-btn">搜索</Button>
      </Form>
      <Row class="operation padding-row">
        <Button @click="add" type="primary">添加</Button>
        <Button @click="delAll">批量删除</Button>
      </Row>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        sortable="custom"
        @on-sort-change="changeSort"
        @on-selection-change="changeSelect"
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
    <Modal
      :title="modalTitle"
      v-model="modalVisible"
      :mask-closable="false"
      :width="500"
    >
      <Form ref="form" :model="form" :label-width="100"
            :rules="formValidate">
        <FormItem label="规格名称" prop="specName">
          <Input v-model="form.specName" maxlength="30" clearable style="width: 100%"/>
        </FormItem>
        <FormItem label="规格值" prop="specValue">
          <Select
            v-model="form.specValue"
            placeholder="输入后回车添加"
            multiple
            filterable
            allow-create
            :popper-append-to-body="false"
            popper-class="spec-values-popper"
            style="width: 100%; text-align: left; margin-right: 10px"
          >
            <Option v-for="item in specValue" :value="item" :label="item" :key="item">
            </Option>
          </Select>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="saveSpec">提交</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import {
  getSpecListData,
  insertSpec,
  updateSpec,
  delSpec
} from "@/api/goods";

import {regular} from "@/utils";
export default {
  name: "spec",
  components: {},
  data() {
    return {
      loading: true, // 表单加载状态
      modalType: 0, // 添加或编辑标识
      modalVisible: false, // 添加或编辑显示
      modalTitle: "", // 添加或编辑标题
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "asc", // 默认排序方式
      },
      // 表单验证规则
      formValidate: {
        specName: [
          regular.REQUIRED,
          // regular.VARCHAR20
        ],
        specValue: [
          regular.REQUIRED,
          regular.VARCHAR255
        ],
      },
      form: {
        // 添加或编辑表单对象初始化数据
        specName: "",
        specValue: "",
      },
      /** 编辑规格值 */
      specValue: [],
      submitLoading: false, // 添加或编辑提交状态
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      columns: [
        // 表头
        {
          type: "selection",
          width: 60,
          align: "center",
        },
        {
          title: "规格名称",
          key: "specName",
          width: 200,
        },
        {
          title: "规格值",
          key: "specValue",
          minWidth: 250,
          tooltip: true
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          fixed: "right",
          width: 250,
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
                      this.edit(params.row);
                    },
                  },
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
    //初始化，获取数据
    init() {
      this.getDataList();
    },
    //修改分页
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
      this.clearSelectAll();
    },
    //修改页面大小
    changePageSize(v) {
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    //搜索参数
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    //重置搜索参数
    handleReset() {
      this.$refs.searchForm.resetFields();
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      // 重新加载数据
      this.getDataList();
    },
    //更改排序
    changeSort(e) {
      this.searchForm.sort = e.key;
      this.searchForm.order = e.order;
      if (e.order === "normal") {
        this.searchForm.order = "";
      }
      this.getDataList();
    },
    //清除已选择
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },
    //修改已选择
    changeSelect(e) {
      this.selectList = e;
      this.selectCount = e.length;
    },
    //获取数据
    getDataList() {
      this.loading = true;
      // 带多条件搜索参数获取表单数据 请自行修改接口
      getSpecListData(this.searchForm).then((res) => {
        this.loading = false;
        this.data = res.records;
        this.total = res.total;
      });
      this.loading = false;
    },
    //新增规格
    saveSpec() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          if (this.modalType === 0) {
            // 添加 避免编辑后传入id等数据
            delete this.form.id;
            insertSpec(this.form).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("操作成功");
                this.getDataList();
                this.modalVisible = false;
              }
            });
          } else {
            // 编辑
            updateSpec(this.form.id, this.form).then((res) => {
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
    //弹出添加框
    add() {
      this.modalType = 0;
      this.modalTitle = "添加";
      this.$refs.form.resetFields();
      this.specValue = '';
      delete this.form.id;
      this.modalVisible = true;
    },
    //弹出编辑框
    edit(v) {
      this.modalType = 1;
      this.modalTitle = "编辑";
      // 转换null为""
      for (let attr in v) {
        if (v[attr] === null) {
          v[attr] = "";
        }
      }
      let localVal = v.specValue;

      this.form.specName = v.specName;
      this.form.id = v.id;
      this.form.specValue = v.specValue;

      if (localVal && localVal.indexOf("," > 0)) {
        this.form.specValue = localVal.split(",")
        this.specValue = this.form.specValue
        this.$set(this, 'specValue', this.form.specValue)
      } else {
        this.specValue = [];
      }
      this.modalVisible = true;
    },
    // 删除规格
    remove(v) {
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除 " + v.specName + " ?",
        loading: true,
        onOk: () => {
          delSpec(v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getDataList();
            }
          });
        },
      });
    },
    // 批量删除
    delAll() {
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要删除的数据");
        return;
      }
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除所选的 " + this.selectCount + " 条数据?",
        loading: true,
        onOk: () => {
          let ids = "";
          this.selectList.forEach(function (e) {
            ids += e.id + ",";
          });
          ids = ids.substring(0, ids.length - 1);
          delSpec(ids).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除成功");
              this.clearSelectAll();
              this.searchForm.pageNumber = 1
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
