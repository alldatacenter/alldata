<template>
  <div class="search">
    <Card>
      <Form
        ref="searchForm"
        @keydown.enter.native="handleSearch"
        :model="searchForm"
        inline
        :label-width="70"
        class="search-form"
      >
        <Form-item label="商品名称" prop="goodsName">
          <Input
            type="text"
            v-model="searchForm.goodsName"
            placeholder="请输入商品名称"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Form-item label="商品编号" prop="id">
          <Input
            type="text"
            v-model="searchForm.id"
            placeholder="请输入商品编号"
            clearable
            style="width: 200px"
          />
        </Form-item>
        <Button @click="handleSearch" class="search-btn" type="primary" icon="ios-search"
          >搜索</Button
        >
      </Form>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        class="mt_10"
      >
        <!-- 商品栏目格式化 -->
        <template slot="goodsSlot" slot-scope="scope">
          <div style="margin-top: 5px; height: 80px; display: flex">
            <div style="">
              <img
                :src="scope.row.original"
                style="height: 60px; margin-top: 3px; width: 60px"
              />
            </div>

            <div style="margin-left: 13px">
              <div class="div-zoom">
                <a>{{ scope.row.goodsName }}</a>
              </div>
            </div>
          </div>
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
  </div>
</template>

<script>
import { authGoods, getAuthGoodsListData } from "@/api/goods";

export default {
  name: "goods",
  components: {},
  data() {
    return {
      id: "", //要操作的id
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "create_time", // 默认排序字段
        order: "desc", // 默认排序方式
      },
      goodsAuditForm: {
        // 商品编辑表单
        auth_flag: 1,
      },
      columns: [
        {
          title: "商品名称",
          key: "goodsName",
          minWidth: 180,
          slot: "goodsSlot",
          tooltip: true,
        },
        {
          title: "商品编号",
          key: "id",
          minWidth: 100,
          tooltip: true,
        },
        {
          title: "价格",
          key: "price",
          minWidth: 130,
          render: (h, params) => {
            return h("div", this.$options.filters.unitPrice(params.row.price, "￥"));
          },
        },
        {
          title: "审核状态",
          key: "authFlag",
          minWidth: 130,
          render: (h, params) => {
            if (params.row.authFlag == "TOBEAUDITED") {
              return h("div", [
                h("Badge", {
                  props: {
                    status: "error",
                    text: "待审核",
                  },
                }),
              ]);
            } else if (params.row.authFlag == "PASS") {
              return h("div", [
                h("Badge", {
                  props: {
                    status: "success",
                    text: "审核通过",
                  },
                }),
              ]);
            } else if (params.row.authFlag == "REFUSE") {
              return h("div", [
                h("Badge", {
                  props: {
                    status: "error",
                    text: "审核拒绝",
                  },
                }),
              ]);
            }
          },
        },

        {
          title: "店铺名称",
          key: "storeName",
          minWidth: 100,
          tooltip: true,
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          fixed: "right",
          width: 200,
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
                      this.examine(params.row, 1);
                    },
                  },
                },
                "通过"
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
                      this.examine(params.row, 2);
                    },
                  },
                },
                "拒绝"
              ),
              h(
                "Button",
                {
                  props: {
                    size: "small",
                  },
                  on: {
                    click: () => {
                      this.showDetail(params.row);
                    },
                  },
                },
                "查看"
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
    changePage(v) {
      // 改变页码
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    changePageSize(v) {
      // 改变每页数量
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    handleSearch() {
      // 搜索
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    getDataList() {
      // 获取列表数据
      this.loading = true;
      // 带多条件搜索参数获取表单数据
      this.searchForm.authFlag = 0;
      getAuthGoodsListData(this.searchForm).then((res) => {
        this.loading = false;
        if (res.records) {
          this.data = res.records;
          this.total = res.total;
        }
      });
    },
    examine(v, authFlag) {
      // 审核商品
      let examine = "通过";
      this.goodsAuditForm.authFlag = "PASS";
      if (authFlag != 1) {
        examine = "拒绝";
        this.goodsAuditForm.authFlag = "REFUSE";
      }
      this.$Modal.confirm({
        title: "确认审核",
        content: "您确认要审核" + examine + " " + v.goodsName + " ?",
        loading: true,
        onOk: () => {
          authGoods(v.id, this.goodsAuditForm).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("审核成功");
              this.getDataList();
            }
          });
        },
      });
    },
    //查看商品详情
    showDetail(v) {
      let id = v.id;
      this.$router.push({
        name: "goods-detail",
        query: { id: id },
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
