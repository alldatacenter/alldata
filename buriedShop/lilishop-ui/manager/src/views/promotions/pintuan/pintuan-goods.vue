<template>
  <div class="pintuan-goods">
    <Card>
      <h4>活动详情</h4>
      <Table style="margin: 10px 0" border :columns="columns" :data="data"></Table>

      <h4>商品信息</h4>
      <Table
        :loading="loading"
        border
        class="operation"
        :columns="goodsColumns"
        :data="goodsData"
        ref="table"
        sortable="custom"
      >
        <template slot-scope="{ row }" slot="goodsName">
          <div>
            <a class="mr_10" @click="linkTo(row.goodsId, row.skuId)">{{
              row.goodsName
            }}</a>
            <Poptip trigger="hover" title="扫码在手机中查看" transfer>
              <div slot="content">
                <vue-qr
                  :text="wapLinkTo(row.goodsId, row.skuId)"
                  :margin="0"
                  colorDark="#000"
                  colorLight="#fff"
                  :size="150"
                ></vue-qr>
              </div>
              <img
                src="../../../assets/qrcode.svg"
                style="vertical-align: middle"
                class="hover-pointer"
                width="20"
                height="20"
                alt=""
              />
            </Poptip>
          </div>
        </template>
      </Table>
      <Row type="flex" justify="end" class="page operation">
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
import { getPintuanGoodsList, getPintuanDetail } from "@/api/promotion.js";
import vueQr from "vue-qr";
import { promotionsStatusRender } from "@/utils/promotions";

export default {
  components: {
    "vue-qr": vueQr,
  },
  data() {
    return {
      loading: false, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
      },

      data: [], // 表单数据
      total: 0, // 表单数据总数
      columns: [
        {
          title: "活动名称",
          key: "promotionName",
          minWidth: 120,
        },
        {
          title: "活动开始时间",
          key: "startTime",
          minWidth: 120,
        },
        {
          title: "活动结束时间",
          key: "endTime",
          minWidth: 120,
        },
        {
          title: "成团人数",
          key: "requiredNum",
        },
        {
          title: "限购数量",
          key: "limitNum",
        },
        {
          title: "状态",
          key: "promotionStatus",
          minWidth: 100,
          render: (h, params) => {
            return promotionsStatusRender(h, params);
          },
        },
      ],
      goodsColumns: [
        {
          title: "商品名称",
          slot: "goodsName",
          minWidth: 120,
        },

        {
          title: "库存",
          key: "quantity",
          minWidth: 40,
        },

        {
          title: "拼团价格",
          key: "price",
          minWidth: 50,
          render: (h, params) => {
            return h("div", this.$options.filters.unitPrice(params.row.price, "￥"));
          },
        },
      ],
      goodsData: [], // 商品数据
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getDataList();
      this.getPintuanMsg();
    },
    // 分页 改变页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    // 分页 改变页数
    changePageSize(v) {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 获取拼团商品列表
    getDataList() {
      this.loading = true;
      this.searchForm.pintuanId = this.$route.query.id;

      getPintuanGoodsList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.goodsData = res.result.records;
          this.total = res.result.total;
        }
      });
    },
    // 获取拼团详情
    getPintuanMsg() {
      getPintuanDetail(this.$route.query.id).then((res) => {
        if (res.success) this.data.push(res.result);
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
h4 {
  margin: 20px 0;
  padding: 0 10px;
  font-weight: bold;
  color: #333;
  font-size: 14px;
  text-align: left;
  border-left: 3px solid red;
}
</style>
