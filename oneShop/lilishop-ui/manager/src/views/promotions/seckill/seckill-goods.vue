<template>
  <div class="seckill-goods">
    <Card>
      <Table border :columns="columns" :data="data">
        <template slot-scope="{ row }" slot="applyEndTime">
          {{ unixDate(row.applyEndTime) }}
        </template>
        <template slot-scope="{ row }" slot="hours">
          <Tag v-for="item in unixHours(row.hours)" :key="item">{{ item }}</Tag>
        </template>
      </Table>
      <Table
        :loading="loading"
        border
        class="operation"
        :columns="goodsColumns"
        :data="goodsList"
        ref="table"
      >
        <template slot-scope="{ row }" slot="originalPrice">
          <div>{{ row.originalPrice | unitPrice("￥") }}</div>
        </template>

        <template slot-scope="{ row }" slot="quantity">
          <div>{{ row.quantity }}</div>
        </template>

        <template slot-scope="{ row }" slot="price">
          <div>{{ row.price | unitPrice("￥") }}</div>
        </template>

        <template slot-scope="{ row }" slot="time">
          <Tag>{{ row.timeLine + ":00" }}</Tag>
        </template>
        <template slot-scope="{ row }" slot="QRCode">
          <img
            v-if="row.QRCode"
            :src="row.QRCode || '../../../assets/lili.png'"
            width="50px"
            height="50px"
            alt=""
          />
        </template>
        <template slot-scope="{ row, index }" slot="action">
          <Button type="error" size="small" @click="delGoods(index, row)">删除 </Button>
        </template>
      </Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page
          :current="searchForm.pageNumber + 1"
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
import {
  seckillGoodsList,
  seckillDetail,
  auditApplySeckill,
  delSeckillGoods,
} from "@/api/promotion.js";

export default {
  data() {
    return {
      promotionStatus: "", // 活动状态
      showModal: false, // modal显隐
      openTip: true, // 显示提示
      loading: false, // 表单加载状态
      submitLoading: false, // 加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 0, // 当前页数
        pageSize: 10, // 页面大小
      },
      total: 0, // 总数
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      data: [], // 表单数据
      columns: [
        // 表头
        {
          title: "活动名称",
          key: "promotionName",
          minWidth: 120,
        },
        {
          title: "活动开始时间",
          key: "startTime",
        },
        {
          title: "报名截止时间",
          slot: "applyEndTime",
        },
        {
          title: "时间场次",
          slot: "hours",
        },
        {
          title: "活动状态",
          key: "promotionStatus",
          minWidth: 80,
          sortable: false,
          render: (h, params) => {
            if (params.row.promotionStatus == "NEW") {
              return h("div", [
                h("Badge", {
                  props: {
                    status: "error",
                    text: "新建",
                  },
                }),
              ]);
            } else if (params.row.promotionStatus == "START") {
              return h("div", [
                h("Badge", {
                  props: {
                    status: "success",
                    text: "开始",
                  },
                }),
              ]);
            } else if (params.row.promotionStatus == "END") {
              return h("div", [
                h("Badge", {
                  props: {
                    status: "error",
                    text: "结束",
                  },
                }),
              ]);
            } else if (params.row.promotionStatus == "CLOSE") {
              return h("div", [
                h("Badge", {
                  props: {
                    status: "error",
                    text: "废弃",
                  },
                }),
              ]);
            }
          },
        },
      ],
      goodsColumns: [
        // 商品表单
        {
          title: "商品名称",
          key: "goodsName",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "商品价格",
          slot: "originalPrice",
          width: 110,
        },
        {
          title: "库存",
          slot: "quantity",
          minWidth: 30,
          width: 90,
        },
        {
          title: "活动价格",
          slot: "price",
          width: 100,
        },
        {
          title: "商家名称",
          key: "storeName",
          minWidth: 100,
          tooltip: true,
        },
        {
          title: "活动场次",
          width: 100,
          slot: "time",
        },
        // {
        //   title: "状态",
        //   slot: "promotionApplyStatus",
        //   width: 90,
        // },
        {
          title: "操作",
          slot: "action",
          width: 150,
          align: "center",
        },
      ],
      goodsList: [], // 商品列表
      params: {
        // 请求参数
        seckillId: this.$route.query.id,
        applyStatus: "PASS",
        failReason: "",
        ids: "",
      },
      rules: {
        // 验证规则
        failReason: [{ required: true, message: "请输入拒绝原因" }],
      },
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getSeckillMsg();
    },
    // 分页 改变页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
      this.clearSelectAll();
    },
    // 分页 改变页数
    changePageSize(v) {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 清除选中状态
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },

    getDataList() {
      // 获取商品详情
      this.loading = true;
      this.searchForm.seckillId = this.$route.query.id;
      seckillGoodsList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success && res.result) {
          this.goodsList = res.result.records;
          this.total = res.result.total;
        }
      });
    },

    getSeckillMsg() {
      // 获取活动详情
      seckillDetail(this.$route.query.id).then((res) => {
        if (res.success && res.result) {
          this.data = [];
          this.data.push(res.result);
          this.promotionStatus = res.result.promotionStatus;
          this.getDataList();
        }
      });
    },
    delGoods(index, row) {
      // 删除商品
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除该商品吗?删除后不可恢复",
        onOk: () => {
          const params = {
            seckillId: row.seckillId,
            id: row.id,
          };
          delSeckillGoods(params).then((res) => {
            if (res.success) {
              this.goodsList.splice(index, 1);
              this.$Message.success("删除成功！");
            }
          });
        },
      });
    },
    unixDate(time) {
      // 处理报名截止时间
      return this.$options.filters.unixToDate(new Date(time) / 1000);
    },
    unixHours(item) {
      // 处理小时场次
      let hourArr = item.split(",");
      for (let i = 0; i < hourArr.length; i++) {
        hourArr[i] += ":00";
      }
      return hourArr;
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
.operation {
  margin: 10px 0;
}

.reason {
  cursor: pointer;
  color: #2d8cf0;
  font-size: 12px;
}
</style>
