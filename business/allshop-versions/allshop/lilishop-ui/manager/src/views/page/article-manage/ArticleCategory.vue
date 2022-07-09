<template>
  <div>
    <Card>
      <div class="operation mb_10">
        <Button @click="addParent" type="primary" icon="md-add"
          >添加一级分类</Button
        >
      </div>
      <tree-table
        ref="treeTable"
        size="default"
        :loading="loading"
        :data="tableData"
        :columns="columns"
        :border="true"
        :show-index="false"
        :is-fold="true"
        :expand-type="false"
        primary-key="id"
      >
        <template slot="action" slot-scope="scope">
          <Button
            type="info"
            @click="edit(scope.row)"
            size="small"
            style="margin-right: 5px"
            >编辑
          </Button>

          <Button
            type="error"
            @click="remove(scope.row)"
            size="small"
            style="margin-right: 5px"
            >删除
          </Button>
          <Button
            v-show="scope.row.level != 1"
            type="success"
            @click="addChildren(scope.row)"
            size="small"
            style="margin-right: 5px"
            >添加子分类
          </Button>
        </template>
      </tree-table>
    </Card>
    <Modal
      :title="modalTitle"
      v-model="modalVisible"
      :mask-closable="false"
      :width="500"
    >
      <Form
        ref="form"
        :model="formAdd"
        :label-width="100"
        :rules="formValidate"
      >
        <div v-if="showParent">
          <FormItem label="上级分类" prop="parentId">
            {{ parentTitle }}
            <Input
              v-model="formAdd.parentId"
              clearable
              style="width: 100%; display: none"
            />
          </FormItem>
        </div>
        <FormItem label="分类名称" prop="articleCategoryName">
          <Input
            v-model="formAdd.articleCategoryName"
            clearable
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="排序值" prop="sort">
          <InputNumber v-model="formAdd.sort"></InputNumber>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="Submit"
          >提交</Button
        >
      </div>
    </Modal>
  </div>
</template>
<script>
import {
  saveArticleCategory,
  getArticleCategory,
  delArticleCategory,
  updateArticleCategory,
} from "@/api/pages";
import TreeTable from "@/views/my-components/tree-table/Table/Table";
import uploadPicInput from "@/views/my-components/lili/upload-pic-input";
import { regular } from "@/utils";
export default {
  name: "lili-components",
  components: {
    TreeTable,
    uploadPicInput,
  },
  data() {
    return {
      submitLoading:false,
      loading: false, // 加载状态
      expandLevel: 1, // 展开的层级
      modalType: 0, // 添加或编辑标识
      modalVisible: false, // 添加或编辑显示
      modalTitle: "", // 添加或编辑标题
      showParent: false, // 是否展示上级菜单
      parentTitle: "", // 父级菜单名称
      formAdd: {
        // 添加或编辑表单对象初始化数据
        parentId: "",
        sort: 1,
        level: 0,
        articleCategoryName: "",
      },
      // 表单验证规则
      formValidate: {
        articleCategoryName: [regular.REQUIRED],
        sort: [regular.REQUIRED,regular.INTEGER],
      },
      columns: [
        {
          title: "分类名称",
          key: "articleCategoryName",
          witt: "100px",
        },
        {
          title: "排序",
          key: "sort",
          width: "100px",
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          headerAlign: "center",
          width: "400px",
          type: "template",
          template: "action",
        },
      ],
      tableData: [], // 表格数据
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getAllList();
    },
    // 添加子分类
    addChildren(v) {
      this.modalType = 0;
      this.modalTitle = "添加子分类";
      this.formAdd.articleCategoryName = "";
      this.parentTitle = v.articleCategoryName;
      this.formAdd.level = eval(v.level + "+1");
      this.showParent = true;
      delete this.formAdd.id;
      this.formAdd.parentId = v.id;
      this.modalVisible = true;
    },
    // 编辑分类
    edit(v) {
      this.modalType = 1;
      this.modalTitle = "编辑";
      this.formAdd.id = v.id;
      this.formAdd.articleCategoryName = v.articleCategoryName;
      this.formAdd.level = v.level;
      this.formAdd.parentId = v.parentId;
      this.formAdd.sort = v.sort;
      this.showParent = false;
      this.modalVisible = true;
    },
    // 添加一级分类
    addParent() {
      this.modalType = 0;
      this.modalTitle = "添加一级分类";
      this.parentTitle = "顶级分类";
      this.showParent = true;
      this.$refs.form.resetFields();
      delete this.formAdd.id;
      this.formAdd.parentId = 0;
      this.modalVisible = true;
    },
    // 提交
    Submit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          if (this.modalType === 0) {
            // 添加 避免编辑后传入id等数据 记得删除
            delete this.formAdd.id;
            saveArticleCategory(this.formAdd).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("添加成功");

                this.formAdd = {
                  // 添加或编辑表单对象初始化数据
                  parentId: "",
                  sort: 1,
                  level: 0,
                  articleCategoryName: "",
                };
              } else {
                // this.$Message.error(res.message);
              }
              this.getAllList();
              this.modalVisible = false;
            });
          } else {
            // 编辑
            updateArticleCategory(this.formAdd, this.formAdd.id).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("修改成功");
              } else {
                // this.$Message.error(res.message);
              }
              this.getAllList();
              this.modalVisible = false;
              this.$refs.form.resetFields();
            });
          }
        }
      });
    },
    // 删除分类
    remove(v) {
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除 " + v.articleCategoryName + " ?",
        loading: true,
        onOk: () => {
          // 删除
          delArticleCategory(v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("操作成功");
              this.getAllList();
            }
          });
        },
      });
    },
    // 获取分类数据
    getAllList(newval) {
      this.loading = true;
      getArticleCategory().then((res) => {
        this.loading = false;
        if (res.success) {
          // 仅展开指定级数 默认后台已展开所有
          let expandLevel = this.expandLevel;
          res.result.forEach(function (e) {
            if (expandLevel == 1) {
              if (e.level == 0) {
                e.expand = false;
              }
              if (e.children && e.children.length > 0) {
                e.children.forEach(function (c) {
                  if (c.level == 1) {
                    c.expand = false;
                  }
                  if (c.children && c.children.length > 0) {
                    c.children.forEach(function (b) {
                      if (b.level == 2) {
                        b.expand = false;
                      }
                    });
                  }
                });
              }
            } else if (expandLevel == 2) {
              if (e.level == 0) {
                e.expand = true;
              }
              if (e.children && e.children.length > 0) {
                e.children.forEach(function (c) {
                  if (c.level == 1) {
                    c.expand = false;
                  }
                  if (c.children && c.children.length > 0) {
                    c.children.forEach(function (b) {
                      if (b.level == 2) {
                        b.expand = false;
                      }
                    });
                  }
                });
              }
            } else if (expandLevel == 3) {
              if (e.level == 0) {
                e.expand = true;
              }
              if (e.children && e.children.length > 0) {
                e.children.forEach(function (c) {
                  if (c.level == 1) {
                    c.expand = true;
                  }
                  if (c.children && c.children.length > 0) {
                    c.children.forEach(function (b) {
                      if (b.level == 2) {
                        b.expand = false;
                      }
                    });
                  }
                });
              }
            }
          });
          this.tableData = res.result;
        }
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
.article {
  font-size: 16px;
  font-weight: 400;
  margin: 12px 0;
}
</style>
