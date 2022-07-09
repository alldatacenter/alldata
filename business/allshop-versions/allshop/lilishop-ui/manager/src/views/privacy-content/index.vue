<template>
  <div class="wrapper">
    <Row>
      <Col span="24">
      <Card class="article-detail">
        <Table :loading="loading" border :columns="columns" :data="data" ref="table">
        </Table>
      </Card>
      </Col>
    </Row>
    <template v-if="!selected">
      <Modal :title="modalTitle" v-model="modalVisible" :mask-closable="false" :width="1100">
        <Form ref="form" :model="form.article" :label-width="100">
          <FormItem label="文章标题" prop="title">
            <Input v-model="form.article.title" clearable style="width: 40%" />
          </FormItem>
          <FormItem label="文章分类" prop="categoryId">
            <Select v-model="treeValue" placeholder="请选择" clearable style="width: 180px">
              <Option :value="treeValue" style="display: none">{{
                      treeValue
                    }}
              </Option>
              <Tree :data="treeDataDefault" @on-select-change="handleCheckChange"></Tree>
            </Select>
          </FormItem>
          <FormItem label="文章排序" prop="sort">
            <Input type="number" v-model="form.article.sort" clearable style="width: 10%" />
          </FormItem>
          <FormItem class="form-item-view-el" label="文章内容" prop="content">
            <editor openXss v-model="form.article.content"></editor>
          </FormItem>
          <FormItem label="是否展示" prop="openStatus">
            <i-switch size="large" v-model="form.article.openStatus"  >
              <span slot="open">展示</span>
              <span slot="close">隐藏</span>
            </i-switch>
          </FormItem>
        </Form>
        <div slot="footer">
          <Button type="text" @click="modalVisible = false">取消</Button>
          <Button type="primary" :loading="submitLoading" @click="handleSubmit">提交</Button>
        </div>
      </Modal>
    </template>
  </div>
</template>

<script>
import {
  getArticleCategory,
  updatePrivacy,
  getPrivacy,
} from "@/api/pages";
import editor from "@/views/my-components/lili/editor";

export default {
  name: "privacy",
  components: {
    editor,
  },
  props: {
    selected: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      loading: false, // 表单加载状态
      modalVisible: false, // 添加或编辑显示
      treeDataDefault: [],
      list: [], // 列表
      treeValue: "", // 选择的分类
      //树结构
      treeData: [],
      submitLoading: false, // 添加或编辑提交状态
      modalTitle:'',
      currindex:'',
      form: {
        type:'',
        article:{
          openStatus: false,
          title: '',
          categoryId: '',
          sort: 1,
          content: '',
          type:''
        },
        id:''
      },
      columns: [
        // 表头
        {
          title:"协议名称",
          key: "name",
          width: 150,
        },
        // 表头
        {
          title:"协议类型",
          key: "type",
          width: 150,
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
              )
            ]);
          },
        },
      ],
      data: [
        {
          name:'店铺入驻协议',
          type:'STORE_REGISTER'
        },
        {
          name:'用户协议',
          type:'USER_AGREEMENT'
        },
        {
          name:'证照信息',
          type:'LICENSE_INFORMATION'
        },
        {
          name:'关于我们',
          type:'ABOUT'
        },
        {
          name:'隐私策略',
          type:'PRIVACY_POLICY'
        },
      ], // 表单数据
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getAllList(0);
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
      this.form.article.categoryId = value;
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
    // 编辑文章modal
    edit(data) {
      this.modalType = 1;
      this.modalTitle = "编辑协议";
      this.form.article = {
        content:''
      };
      this.$refs.form.resetFields();
      this.loading = true;
      getPrivacy(data.type).then((res) => {
        this.loading = false;
        if(res.result){
          this.modalVisible = true;
          this.form.article.categoryId = res.result.categoryId;
          console.log(this.treeDataDefault);
          this.form.id = res.result.id;
          this.form.article.content =res.result.content;
          this.form.article.title = res.result.title;
          this.form.article.sort = res.result.sort;
          this.form.article.openStatus = res.result.openStatus;
          this.form.article.type = res.result.type;
          this.form.type =  res.result.type;
        }
      });
      this.loading = false;
    },
    // 添加文章
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          // 编辑
          updatePrivacy(this.form.id,this.form.type,this.form.article).then((res) => {
            this.submitLoading = false;
            if (res.success) {
              this.$Message.success("操作成功");
              this.modalVisible = false;
            }
          });
        }
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
