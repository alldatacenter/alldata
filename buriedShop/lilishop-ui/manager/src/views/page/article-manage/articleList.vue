<template>
  <div class="wrapper">
    <Row>
      <Col style="height: 100%" span="4">
        <Card class="article-category mr_10">
          <Tree :data="treeData" @on-select-change="handleCateChange"></Tree>
        </Card>
      </Col>
      <Col span="20">
        <Card class="article-detail">
          <Row @keydown.enter.native="handleSearch">
            <Form
              ref="searchForm"
              :model="searchForm"
              inline
              :label-width="70"
              style="width: 100%"
              class="search-form"
            >
              <Form-item label="文章标题" prop="title">
                <Input
                  type="text"
                  v-model="searchForm.title"
                  placeholder="请输入文章标题"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Button
                @click="handleSearch"
                type="primary"
                icon="ios-search"
                class="search-btn"
                >搜索</Button
              >
            </Form>
          </Row>
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
            <template slot="openStatusSlot" slot-scope="scope">
              <div></div>
              <i-switch
                size="large"
                v-model="scope.row.openStatus"
                @on-change="changeSwitch(scope.row)"
              >
                <span slot="open">展示</span>
                <span slot="close">隐藏</span>
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
            >
            </Page>
          </Row>
        </Card>
      </Col>
    </Row>
    <template v-if="!selected">
      <Modal
        :title="modalTitle"
        v-model="modalVisible"
        :mask-closable="false"
        :width="1100"
      >
        <Form ref="form" :model="form" :label-width="100">
          <FormItem label="文章标题" prop="title">
            <Input v-model="form.title" clearable style="width: 40%" />
          </FormItem>
          <FormItem label="文章分类" prop="categoryId">
            <Select
              v-model="treeValue"
              placeholder="请选择"
              clearable
              style="width: 180px"
            >
              <Option v-if="treeValue" :value="treeValue" style="display: none"
                >{{ treeValue }}
              </Option>
              <Tree
                :data="treeDataDefault"
                @on-select-change="handleCheckChange"
              ></Tree>
            </Select>
          </FormItem>
          <FormItem label="文章排序" prop="sort">
            <Input
              type="number"
              v-model="form.sort"
              clearable
              style="width: 10%"
            />
          </FormItem>
          <FormItem class="form-item-view-el" label="文章内容" prop="content">
            <editor
              ref="editor"
              openXss
              v-model="form.content"
              :init="{ ...initEditor,height:'800px' }"
            ></editor>
          </FormItem>
          <FormItem label="是否展示" prop="openStatus">
            <i-switch size="large" v-model="form.openStatus">
              <span slot="open">展示</span>
              <span slot="close">隐藏</span>
            </i-switch>
          </FormItem>
        </Form>
        <div slot="footer">
          <Button type="text" @click="modalVisible = false">取消</Button>
          <Button type="primary" :loading="submitLoading" @click="handleSubmit"
            >提交</Button
          >
        </div>
      </Modal>
    </template>
  </div>
</template>

<script>
import {
  getArticleCategory,
  saveArticle,
  getArticle,
  delArticle,
  updateArticle,
  seeArticle,
  updateArticleStatus,
} from "@/api/pages";
import Editor from "@tinymce/tinymce-vue";
import { initEditor } from "@/views/lili-components/editor/config";
export default {
  name: "article",
  components: {
    editor: Editor,
  },
  props: {
    selected: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      initEditor: initEditor,
      selectedIndex: 99999, // 已选下标
      loading: true, // 表单加载状态
      modalType: 0, // 添加或编辑标识
      modalVisible: false, // 添加或编辑显示
      modalTitle: "", // 添加或编辑标题
      treeDataDefault: [],
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
        categoryId: "",
      },
      searchTreeValue: "", // 切换
      form: {
        // 添加或编辑表单对象初始化数据
        openStatus: false,
        title: "",
        categoryId: "",
        sort: 1,
        content: "",
        id: "",
      },
      list: [], // 列表
      treeValue: "", // 选择的分类
      //树结构
      treeData: [],
      submitLoading: false, // 添加或编辑提交状态
      columns: [
        // 表头
        {
          title: "分类名称",
          key: "articleCategoryName",
          width: 150,
        },
        {
          title: "文章标题",
          key: "title",
          minWidth: 200,
          tooltip: true,
        },
        {
          title: "是否显示",
          key: "openStatus",
          width: 100,
          slot: "openStatusSlot",
        },
        {
          title: "排序",
          key: "sort",
          width: 100,
        },
        {
          title: "操作",
          key: "action",
          align: "center",

          width: 230,
          render: (h, params) => {
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    size: "small",
                    type:
                      this.selectedIndex == params.index
                        ? "primary"
                        : "default",
                  },
                  style: {
                    marginRight: "5px",
                    display: this.selected ? "" : "none",
                  },
                  on: {
                    click: () => {
                      this.selectedIndex = params.index;
                      this.$emit("callbacked", params.row);
                    },
                  },
                },
                this.selectedIndex == params.index ? "已选" : "选择"
              ),
              h(
                "Button",
                {
                  props: {
                    size: "small",
                    type: "info",
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
  watch: {
    "searchForm.categoryId": {
      handler() {
        this.handleSearch();
      },
      deep: true,
    },
    "searchForm.title": {
      handler() {
        this.handleSearch();
      },
      deep: true,
    },
  },
  methods: {
    // 初始化数据
    init() {
      this.getDataList();
      this.getAllList(0);
    },
    // 选择分类回调
    handleCateChange(data) {
      let { value, title } = data[0];
      this.list.push({
        value,
        title,
      });
      this.searchForm.categoryId = value;
      this.searchTreeValue = title;
    },
    //是否展示文章
    changeSwitch(v) {
      let params = {
        status: v.openStatus,
      };
      updateArticleStatus(v.id, params).then((res) => {
        this.submitLoading = false;
        if (res.success) {
        }
      });
    },
    // 文章分类的选择事件
    handleCheckChange(data) {
      let value = "";
      let title = "";
      this.list = [];
      data.forEach((item, index) => {
        value += `${item.value},`;
        title += `${item.title},`;
      });
      value = value.substring(0, value.length - 1);
      title = title.substring(0, title.length - 1);
      this.list.push({
        value,
        title,
      });
      this.form.categoryId = value;
      this.treeValue = title;
    },
    // 改变页数
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    // 改变页码
    changePageSize(v) {
      this.selected.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 搜索列表
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    // 获取全部文章分类
    getAllList(parent_id) {
      this.loading = true;
      getArticleCategory(parent_id).then((res) => {
        this.loading = false;
        if (res.success) {
          this.treeData = this.getTree(res.result);
          this.treeDataDefault = this.getTree(res.result);
          this.treeData.unshift({
            title: "全部",
            level: 0,
            children: [],
            id: "0",
            categoryId: 0,
          });
        }
      });
    },
    // 文章分类格式化方法
    getTree(tree = []) {
      let arr = [];
      if (!!tree && tree.length !== 0) {
        tree.forEach((item) => {
          let obj = {};
          obj.title = item.articleCategoryName;
          obj.value = item.id;
          obj.attr = item.articleCategoryName; // 其他你想要添加的属性
          obj.expand = false;
          obj.selected = false;
          obj.children = this.getTree(item.children); // 递归调用
          arr.push(obj);
        });
      }
      return arr;
    },
    // 获取文章列表
    getDataList(val) {
      if (val) this.form = {};
      this.loading = true;
      getArticle(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.total = res.result.total;
          //为了在是否展示一列展示开关 需要改一下数据类型，最终提交再次更改
          this.data = [];
          if (res.result.records.length > 0) {
            this.data = res.result.records;
          }
        }
      });
      this.total = this.data?.length;
      this.loading = false;
    },
    // 添加文章
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          if (this.modalType === 0) {
            // 添加 避免编辑后传入id等数据 记得删除
            delete this.form.id;
            saveArticle(this.form).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("操作成功");
                this.getDataList();
                this.modalVisible = false;
              }
            });
          } else {
            // 编辑
            updateArticle(this.form).then((res) => {
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
    // 添加文章modal
    add() {
      this.modalType = 0;
      this.modalTitle = "添加文章";
      this.treeValue = "";

      this.form = {
        sort: 1,
        content: "",
      };
      this.$refs.form.resetFields();
      delete this.form.id;
      this.modalVisible = true;
    },
    // 编辑文章modal
    edit(data) {
      this.modalType = 1;
      this.modalTitle = "编辑文章";
      this.treeValue = "";
      this.form = {
        content: "",
      };
      this.$refs.form.resetFields();

      seeArticle(data.id).then((res) => {
        if (res.result) {
          this.modalVisible = true;
          this.form.categoryId = res.result.categoryId;
          this.treeValue = data.articleCategoryName;
          this.form.id = data.id;
          this.form.content = res.result.content;
          this.form.title = res.result.title;
          this.form.sort = res.result.sort;
          this.form.openStatus = res.result.openStatus;
        }
      });
    },
    // 删除文章
    remove(v) {
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除么?",
        loading: true,
        onOk: () => {
          // 删除
          delArticle(v.id).then((res) => {
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
