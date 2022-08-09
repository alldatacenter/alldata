<template>
  <div>
    <Card>
      <Form @keydown.enter.native="handleSearch" ref="searchForm" :model="searchForm" inline :label-width="70"
        class="search-form">
        <Form-item label="商品名称" prop="goodsName">
          <Input type="text" v-model="searchForm.goodsName" placeholder="请输入商品名称" clearable style="width: 200px" />
        </Form-item>
        <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>
      </Form>
      <Row class="operation" style="margin:10px 0;">
        <Button @click="delAll" type="primary">批量下架</Button>
      </Row>
      <Table :loading="loading" border :columns="columns" :data="data" ref="table" sortable="custom"
        @on-selection-change="changeSelect">
        <template slot="goodsName" slot-scope="{row}">
          <div>
            <div class="div-zoom">
              <a @click="linkTo(row.goodsId,row.skuId)">{{row.goodsName}}</a>
            </div>
            <Poptip trigger="hover" title="扫码在手机中查看" transfer>
              <div slot="content">
                <vue-qr :text="wapLinkTo(row.goodsId,row.skuId)" :margin="0" colorDark="#000" colorLight="#fff"
                  :size="150"></vue-qr>
              </div>
              <img src="../../assets/qrcode.svg" class="hover-pointer" width="20" height="20" alt="">
            </Poptip>
          </div>
        </template>
      </Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage"
          @on-page-size-change="changePageSize" :page-size-opts="[10,20,50]" size="small" show-total show-elevator
          show-sizer></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
import { getDistributionGoods, delDistributionGoods } from "@/api/distribution";
import vueQr from "vue-qr";
export default {
  components: {
    "vue-qr": vueQr,
  },
  name: "distributionGoods",
  data() {
    return {
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
      },
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      columns: [
        // 表头
        {
          type: "selection",
          width: 60,
          align: "center",
          fixed: "left",
        },
        {
          title: "商品图片",
          fixed: "left",
          key: "thumbnail",
          width: 120,
          align: "center",
          render: (h, params) => {
            return h("img", {
              attrs: {
                src: params.row.thumbnail || '',
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
          slot: "goodsName",
          minWidth: 200,
          tooltip: true,
        },
        {
          title: "商品价格",
          key: "price",
          minWidth: 100,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.price, "￥")
            );
          },
        },
        {
          title: "库存",
          key: "quantity",
          minWidth: 80,
        },
        {
          title: "添加时间",
          key: "createTime",
          minWidth: 100,
        },
        {
          title: "店铺名称",
          key: "storeName",
          minWidth: 100,
          tooltip: true,
        },
        {
          title: "佣金金额",
          key: "commission",
          minWidth: 100,
          sortable: false,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.commission, "￥")
            );
          },
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          fixed: "right",
          minWidth: 100,
          render: (h, params) => {
            return h("div", [
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
                "下架"
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
    // 初始化数据
    init() {
      this.getDataList();
    },
    // 分页 改变页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
      this.clearSelectAll();
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
    // 清除选中状态
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },
    // 选中后赋值
    changeSelect(e) {
      this.selectList = e;
      this.selectCount = e.length;
    },
    // 获取列表数据
    getDataList() {
      this.loading = true;
      getDistributionGoods(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },
    // 下架商品
    remove(v) {
      this.$Modal.confirm({
        title: "确认下架",
        content: "您确认要下架么?",
        loading: true,
        onOk: () => {
          // 下架
          delDistributionGoods(v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("下架成功");
              this.getDataList();
            }
          });
        },
      });
    },
    // 批量下架
    delAll() {
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要下架的数据");
        return;
      }
      this.$Modal.confirm({
        title: "确认下架",
        content: "您确认要下架所选的 " + this.selectCount + " 条数据?",
        loading: true,
        onOk: () => {
          let ids = [];
          this.selectList.forEach((item) => {
            ids.push(item.id);
          });
          // 批量下架
          delDistributionGoods(ids.toString()).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("下架成功");
              this.clearSelectAll();
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


