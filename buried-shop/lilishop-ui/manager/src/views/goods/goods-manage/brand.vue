<template>
  <div class="search">
    <Card>
      <Form ref="searchForm" @keydown.enter.native="handleSearch" :model="searchForm" inline :label-width="70"
            class="search-form">
        <Form-item label="品牌名称">
          <Input type="text" v-model="searchForm.name" placeholder="请输入品牌名称" clearable style="width: 200px"/>
        </Form-item>
        <Button @click="handleSearch" type="primary">搜索</Button>
      </Form>
      <Row class="operation padding-row">
        <Button @click="add" type="primary">添加</Button>
      </Row>
      <Table :loading="loading" border :columns="columns" :data="data" ref="table"></Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage"
              @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]" size="small"
              show-total show-elevator show-sizer></Page>
      </Row>
    </Card>
    <Modal :title="modalTitle" v-model="modalVisible" :mask-closable="false" :width="500">
      <Form ref="form" :model="form" :label-width="100" :rules="formValidate">
        <FormItem label="品牌名称" prop="name">
          <Input v-model="form.name" clearable style="width: 100%"/>
        </FormItem>
        <FormItem label="品牌图标" prop="logo">
          <upload-pic-input v-model="form.logo" style="width: 100%"></upload-pic-input>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="handleSubmit">提交</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import {
  getManagerBrandPage,
  addBrand,
  updateBrand,
  disableBrand,
  delBrand,
} from "@/api/goods";
import uploadPicInput from "@/views/my-components/lili/upload-pic-input";

import {regular} from "@/utils";

export default {
  name: "brand",
  components: {
    uploadPicInput
  },
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
        sort: "create_time", // 默认排序字段
        order: "desc", // 默认排序方式
      },
      form: {
        // 添加或编辑表单对象初始化数据
        name: "",
        logo: "",
        deleteFlag: "",
      },
      // 表单验证规则
      formValidate: {
        name: [
          regular.REQUIRED,
          regular.VARCHAR20
        ],
        logo: [
          regular.REQUIRED,
          regular.URL200
        ],
      },
      submitLoading: false, // 添加或编辑提交状态
      columns: [
        {
          title: "品牌名称",
          key: "name",
          width: 200,
          resizable: true,
          sortable: false,
        },
        {
          title: "品牌图标",
          key: "logo",
          align: "left",
          render: (h, params) => {
            return h("img", {
              attrs: {
                src: params.row.logo || '',
                alt: "加载图片失败",
              },
              style: {
                cursor: "pointer",
                width: "80px",
                height: "60px",
                margin: "10px 0",
                "object-fit": "contain",
              },
            });
          },
        },
        {
          title: "状态",
          key: "deleteFlag",
          align: "left",
          render: (h, params) => {
            if (params.row.deleteFlag == 0) {
              return h("Tag", {props: {color: "green",},}, "启用");
            } else if (params.row.deleteFlag == 1) {
              return h("Tag", {props: {color: "volcano",},}, "禁用");
            }
          },
          filters: [
            {
              label: "启用",
              value: 0,
            },
            {
              label: "禁用",
              value: 1,
            },
          ],
          filterMultiple: false,
          filterMethod(value, row) {
            if (value == 0) {
              return row.deleteFlag == 0;
            } else if (value == 1) {
              return row.deleteFlag == 1;
            }
          },
        },
        {
          title: "操作",
          key: "action",
          width: 180,
          align: "center",
          fixed: "right",
          render: (h, params) => {
            let enableOrDisable = "";
            if (params.row.deleteFlag == 0) {
              enableOrDisable = h(
                "Button",
                {
                  props: {
                    size: "small",
                    type: "error",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.disable(params.row);
                    },
                  },
                },
                "禁用"
              );
            } else {
              enableOrDisable = h(
                "Button",
                {
                  props: {
                    type: "success",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.enable(params.row);
                    },
                  },
                },
                "启用"
              );
            }
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
              enableOrDisable,

              h(
                "Button",
                {
                  props: {
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.delBrand(params.row.id);
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
    // 删除品牌
    async delBrand(id) {
      let res = await delBrand(id);

      if (res.success) {
        this.$Message.success("品牌删除成功!");
        this.getDataList();
      }
    },
    // 初始化数据
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
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    // 获取数据
    getDataList() {
      this.loading = true;
      getManagerBrandPage(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
    },
    // 提交表单
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          if (this.modalType === 0) {
            // 添加 避免编辑后传入id等数据 记得删除
            delete this.form.id;
            addBrand(this.form).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("操作成功");
                this.getDataList();
                this.modalVisible = false;
              }
            });
          } else {
            // 编辑
            updateBrand(this.form).then((res) => {
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
    // 添加
    add() {
      this.modalType = 0;
      this.modalTitle = "添加";
      this.$refs.form.resetFields();
      delete this.form.id;
      this.modalVisible = true;
    },
    // 编辑
    edit(v) {
      this.modalType = 1;
      this.modalTitle = "编辑";
      this.$refs.form.resetFields();
      // 转换null为""
      for (let attr in v) {
        if (v[attr] === null) {
          v[attr] = "";
        }
      }
      let str = JSON.stringify(v);
      let data = JSON.parse(str);
      this.form = data;
      this.modalVisible = true;
    },
    // 启用品牌
    enable(v) {
      this.$Modal.confirm({
        title: "确认启用",
        content: "您确认要启用品牌 " + v.name + " ?",
        loading: true,
        onOk: () => {
          disableBrand(v.id, {disable: false}).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getDataList();
            }
          });
        },
      });
    },
    // 禁用
    disable(v) {
      this.$Modal.confirm({
        title: "确认禁用",
        content: "您确认要禁用品牌 " + v.name + " ?",
        loading: true,
        onOk: () => {
          disableBrand(v.id, {disable: true}).then((res) => {
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
