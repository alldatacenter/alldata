<template>
  <div class="search">
    <Card>
      <Row>
        <Form
          ref="searchForm"
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
          <Form-item label="活动状态" prop="promotionStatus">
            <Select
              v-model="searchForm.promotionStatus"
              placeholder="请选择"
              clearable
              style="width: 200px"
            >
              <Option value="NEW">未开始</Option>
              <Option value="START">已开始/上架</Option>
              <Option value="END">已结束/下架</Option>
              <Option value="CLOSE">紧急关闭/作废</Option>
            </Select>
          </Form-item>
          <Form-item label="活动时间">
            <DatePicker
              v-model="selectDate"
              type="daterange"
              clearable
              placeholder="选择起始时间"
              style="width: 200px"
            >
            </DatePicker>
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
        <Button @click="add" type="primary">添加砍价</Button>
      </Row>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        sortable="custom"
        @on-sort-change="changeSort"
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
        <template slot-scope="{ row }" slot="startTime">
          <div>{{ row.startTime }}</div>
          <div>{{ row.endTime }}</div>
        </template>
        <template slot-scope="{ row }" slot="quantity">
          <div>{{ row.stock }}</div>
        </template>
        <template slot-scope="{ row }" slot="action">
          <Button
            v-if="row.promotionStatus === 'CLOSE' || row.promotionStatus === 'NEW'"
            type="info"
            size="small"
            style="margin-right: 10px"
            @click="edit(row)"
            >编辑
          </Button>
          <Button
            v-else
            size="small"
            style="margin-right: 10px"
            @click="edit(row, 'onlyView')"
            >查看
          </Button>
          <Button
            type="error"
            size="small"
            style="margin-right: 10px"
            @click="delAll(row)"
            >删除
          </Button>
        </template>
      </Table>
      <Row type="flex" justify="end" class="page">
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
import { getKanJiaGoodsList, delKanJiaGoods } from "@/api/promotion";
import { promotionsStatusRender } from "@/utils/promotions";
import vueQr from "vue-qr";

export default {
  name: "coupon",
  components: {
    "vue-qr": vueQr,
  },

  data() {
    return {
      selectDate: [], //选中的数据
      loading: true, // 表单加载状态
      modalType: 0, // 添加或编辑标识
      modalVisible: false, // 添加或编辑显示
      modalTitle: "", // 添加或编辑标题
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
        goodsName: "",
      },
      form: {
        // 添加或编辑表单对象初始化数据
      },
      // 表单验证规则
      formValidate: {},
      submitLoading: false, // 添加或编辑提交状态
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      columns: [
        {
          title: "商品名称",
          slot: "goodsName",
          minWidth: 150,
          tooltip: true,
        },
        {
          title: "库存数量",
          slot: "quantity",
          width: 100,
        },
        {
          title: "剩余活动库存",
          key: "stock",
          width: 110,
        },
        {
          title: "每人最低砍",
          key: "lowestPrice",
          minWidth: 100,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.lowestPrice, "￥")
            );
          },
        },
        {
          title: "每人最高砍",
          key: "highestPrice",
          minWidth: 100,
          tooltip: true,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.highestPrice, "￥")
            );
          },
        },
        {
          title: "结算价格",
          key: "settlementPrice",
          minWidth: 100,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.settlementPrice, "￥")
            );
          },
        },
        {
          title: "活动开始时间",
          slot: "startTime",
          minWidth: 150,
        },
        {
          title: "状态",
          key: "promotionStatus",
          render: (h, params) => {
            return promotionsStatusRender(h, params);
          },
          width: 100,
        },
        {
          title: "操作",
          slot: "action",
          align: "center",
          fixed: "right",
          width: 150,
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
      selectCoupon: [], //本级选中的优惠券
    };
  },
  props: {},
  watch: {
    $route(to, from) {
      if (to.fullPath == "/promotions/manager-coupon") {
        this.init();
      }
    },
  },
  methods: {
    check() {
      // this.selectCoupon.push(this.selectList)
      this.$emit("selected", this.selectCoupon);
    },
    init() {
      this.getDataList();
    },
    // 添加砍价活动跳转
    add() {
      this.$router.push({ name: "add-kanJia-activity-goods" });
    },

    info(v) {
      this.$router.push({ name: "platform-coupon-info", query: { id: v.id } });
    },
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
      // this.clearSelectAll();
    },
    changePageSize(v) {
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    handleSearch() {
      this.searchForm.pageNumber = 0;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    changeSort(e) {
      this.searchForm.sort = e.key;
      this.searchForm.order = e.order;
      if (e.order === "normal") {
        this.searchForm.order = "";
      }
      this.getDataList();
    },
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },

    getDataList() {
      this.loading = true;
      if (this.selectDate && this.selectDate[0] && this.selectDate[1]) {
        this.searchForm.startTime = this.selectDate[0].getTime();
        this.searchForm.endTime = this.selectDate[1].getTime();
      } else {
        this.searchForm.startTime = null;
        this.searchForm.endTime = null;
      }
      // 带多条件搜索参数获取表单数据 请自行修改接口
      getKanJiaGoodsList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },
    edit(v, type) {
      let data = {
        id: v.id,
      };
      type ? (data.onlyView = true) : "";
      this.$router.push({
        name: "edit-kanJia-activity-goods",
        query: data,
      });
    },
    delAll(row) {
      this.$Modal.confirm({
        title: "确认删除",
        content: "确认需要删除此砍价商品",
        loading: true,
        onOk: () => {
          let ids = [];
          this.selectList.forEach(function (e) {
            ids.push(e.id);
          });
          // 批量删除
          delKanJiaGoods(row.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除成功");
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
<style lang="scss">
@import "@/styles/table-common.scss";
</style>
