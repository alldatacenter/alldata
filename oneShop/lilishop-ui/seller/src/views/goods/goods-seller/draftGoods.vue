<template>
  <div class="search">
    <Card>
      <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form mb_10"
        @keydown.enter.native="handleSearch">
        <Form-item label="商品名称" prop="goodsName">
          <Input type="text" v-model="searchForm.goodsName" placeholder="请输入商品名称" clearable style="width: 200px" />
        </Form-item>
        <Form-item label="商品编号" prop="id">
          <Input
            type="text"
            v-model="searchForm.id"
            placeholder="商品编号"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item style="margin-left: -35px" class="br">
          <Button @click="handleSearch" type="primary" icon="ios-search"
            >搜索</Button
          >
          <Button @click="handleReset">重置</Button>
        </Form-item>
      </Form>
      <Table :loading="loading" border :columns="columns" :data="data" ref="table" class="mt_10"></Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage"
          @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]" size="small" show-total show-elevator
          show-sizer></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
import { getDraftGoodsListData, deleteDraftGoods } from "@/api/goods";
export default {
  name: "goods",
  components: {},
  data() {
    return {
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "create_time", // 默认排序字段
        order: "desc", // 默认排序方式
        saveType: "TEMPLATE",
      },
      columns: [
        // 表头
        {
          title: "编号",
          key: "id",
          minWidth: 120,
        },
        {
          title: "商品原图",
          key: "original",
          width: 120,
          align: "center",
          render: (h, params) => {
            return h("img", {
              attrs: {
                src: params.row.original,
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
          title: "商品名称",
          key: "goodsName",
          minWidth: 120,
        },
        {
          title: "商品价格",
          key: "price",
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.price, "￥")
            );
          },
        },
        {
          title: "创建时间",
          key: "createTime",
          minWidth: 120,
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
                    type: "success",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.editGoods(params.row);
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
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.removeDraft(params.row.id);
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
    init() {
      // 初始化数据
      this.getDataList();
    },
    // 编辑模板
    editGoods(v) {
      this.$router.push({
        name: "goods-template-operation-edit",
        query: { draftId: v.id },
      });
    },
    // 删除模板
    removeDraft(id) {
      let showType = "模版";
      this.$Modal.confirm({
        title: "确认审核",
        content: "您确认要删除id为 " + id + " 的" + showType + "吗?",
        loading: true,
        onOk: () => {
          deleteDraftGoods(id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除成功");
              this.getDataList();
            }
          });
        },
      });
    },
    // 改变页数
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    // 改变页码
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
    // 重置
    handleReset() {
      this.$refs.searchForm.resetFields();
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      // 重新加载数据
      this.getDataList();
    },
    // 获取列表数据
    getDataList() {
      this.loading = true;
      // 带多条件搜索参数获取表单数据
      getDraftGoodsListData(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
    },
  },
  mounted() {
    this.init();
  },
  watch: {
    $route(to, from) {
      this.init();
    },
  },
};
</script>
<style lang="scss" scoped>
</style>
