<template>
  <div class="search">
    <Card>
      <Form
        ref="searchForm"
        :model="searchForm"
        inline
        :label-width="75"
        class="search-form mb_10"
      >
        <Form-item label="优惠券名称" prop="couponName">
          <Input
            type="text"
            v-model="searchForm.couponName"
            placeholder="请输入优惠券名称"
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
          ></DatePicker>
        </Form-item>
        <Button
          @click="handleSearch"
          type="primary"
          icon="ios-search"
          class="search-btn"
          >搜索</Button
        >
      </Form>
      <Row class="operation padding-row" v-if="getType !== 'ACTIVITY'">
        <Button @click="add" type="primary">添加优惠券</Button>
        <Button @click="delAll">批量关闭</Button>
      </Row>
      <Table
        v-if="refreshTable"
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        class="mt_10"
        @on-selection-change="changeSelect"
      >
        <template slot-scope="{ row }" slot="action">
          <Button
            v-if="
              row.promotionStatus === 'CLOSE' || row.promotionStatus === 'NEW'
            "
            type="info"
            size="small"
            @click="see(row)"
            >编辑
          </Button>
          <Button
            v-else
            type="default"
            size="small"
            @click="see(row, 'onlyView')"
            >查看
          </Button>
          <Button
            class="ml_5"
            v-if="
              row.promotionStatus === 'START' || row.promotionStatus === 'NEW'
            "
            type="error"
            size="small"
            @click="close(row)"
            >关闭
          </Button>
          <Button
            class="ml_5"
            v-if="
              row.promotionStatus === 'CLOSE' || row.promotionStatus === 'END'
            "
            type="error"
            size="small"
            @click="remove(row)"
            >删除
          </Button>
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
import {
  getPlatformCouponList,
  updatePlatformCouponStatus,
  deletePlatformCoupon,
} from "@/api/promotion";
import {
  promotionsStatusRender,
  promotionsScopeTypeRender,
} from "@/utils/promotions";

export default {
  name: "coupon",
  data() {
    return {
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "create_time", // 默认排序字段
        order: "desc", // 默认排序方式
        getType: "", // 默认排序方式
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
          title: "活动名称",
          key: "promotionName",
          minWidth: 100,
          fixed: "left",
        },
        {
          title: "优惠券名称",
          key: "couponName",
          minWidth: 100,
          tooltip: true,
        },
        {
          title: "面额/折扣",
          key: "price",
          width: 100,
          render: (h, params) => {
            if (params.row.price) {
              return h(
                "div",
                this.$options.filters.unitPrice(params.row.price, "￥")
              );
            } else {
              return h("div", params.row.couponDiscount + "折");
            }
          },
        },

        {
          title: "已领取数量/总数量",
          key: "publishNum",
          width: 130,
          render: (h, params) => {
            return h(
              "div",
              params.row.receivedNum +
                "/" +
                (params.row.publishNum === 0 ? "不限制" : params.row.publishNum)
            );
          },
        },

        {
          title: "已被使用的数量/已领取数量",
          key: "publishNum",
          render: (h, params) => {
            return h("div", params.row.usedNum + "/" + params.row.receivedNum);
          },
        },
        {
          title: "优惠券类型",
          key: "couponType",
          width: 120,
          render: (h, params) => {
            let text = "";
            if (params.row.couponType === "DISCOUNT") {
              return h("Tag", { props: { color: "blue" } }, "打折");
            } else if (params.row.couponType === "PRICE") {
              return h("Tag", { props: { color: "geekblue" } }, "减免现金");
            } else {
              return h("Tag", { props: { color: "purple" } }, "未知");
            }
          },
        },
        {
          title: "品类描述",
          key: "scopeType",
          width: 120,
          render: (h, params) => {
            return promotionsScopeTypeRender(h, params);
          },
        },
        {
          title: "活动时间",
          width: 150,
          render: (h, params) => {
            if (
              params?.row?.getType === "ACTIVITY" &&
              params?.row?.rangeDayType === "DYNAMICTIME"
            ) {
              return h("div", "长期有效");
            } else if (params?.row?.startTime && params?.row?.endTime) {
              return h("div", {
                domProps: {
                  innerHTML:
                    params.row.startTime + "<br/>" + params.row.endTime,
                },
              });
            }
          },
        },
        {
          title: "状态",
          width: 100,
          key: "promotionStatus",
          fixed: "right",
          render: (h, params) => {
            return promotionsStatusRender(h, params);
          },
          minWidth: 70,
        },
        {
          title: "操作",
          slot: "action",
          align: "center",
          fixed: "right",
          width: 130,
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
      refreshTable: true, // 修改选中状态后刷新表格
      selectDate: [], //选中的信息
    };
  },
  props: {
    //优惠券类型 查询参数
    getType: {
      type: String,
      default: "",
    },
    promotionStatus: {
      type: String,
      default: "",
    },
    //已选择优惠券
    selectedList: {
      type: Array,
      default: () => {
        return [];
      },
    },
  },
  watch: {
    $route(to, from) {
      if (to.fullPath == "/promotions/manager-coupon") {
        this.init();
      }
    },
    // 选中优惠券 父级传值
    selectedList: {
      handler(val) {
        // 判断是否是父级回调给自己已选择优惠券
        if (val.length) {
          this.selectList = val;
          this.data.forEach((item) => {
            item._checked = false;
            if (this.selectList.length) {
              this.selectList.forEach((child) => {
                if (item.id == child.id) {
                  item._checked = true;
                }
              });
            }
          });
        } else {
          this.data.forEach((item) => {
            item._checked = false;
          });
        }
        this.refreshTable = false;
        this.$nextTick(() => {
          this.refreshTable = true;
        });
      },
      deep: true,
    },
  },
  methods: {
    check() {
      // 选中的优惠券
      this.$emit("selected", this.selectList);
    },
    // 初始化数据
    init() {
      this.getDataList();
    },
    add() {
      // 跳转添加页面
      this.$router.push({ name: "add-platform-coupon" });
    },
    info(v) {
      // 查看优惠券
      this.$router.push({ name: "platform-coupon-info", query: { id: v.id } });
    },
    changePage(v) {
      // 改变页码
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    changePageSize(v) {
      // 改变页数
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    handleSearch() {
      // 搜索
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    clearSelectAll() {
      // 清除选中状态
      this.$refs.table.selectAll(false);
    },
    /**
     * 选择优惠券
     */
    changeSelect(e) {
      this.selectList = e;
      this.selectCount = e.length;
      if (this.getType === "ACTIVITY") this.check();
    },
    getDataList() {
      // 获取数据
      this.loading = true;
      if (this.selectDate && this.selectDate[0] && this.selectDate[1]) {
        this.searchForm.startTime = this.selectDate[0].getTime();
        this.searchForm.endTime = this.selectDate[1].getTime();
      } else {
        this.searchForm.startTime = null;
        this.searchForm.endTime = null;
      }
      getPlatformCouponList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          console.log(res);
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.total = this.data.length;
      this.loading = false;
    },

    see(v, only) {
      // 跳转编辑页面
      let data;
      only ? (data = { onlyView: true, id: v.id }) : (data = { id: v.id });
      this.$router.push({ name: "edit-platform-coupon", query: data });
    },
    close(v) {
      // 下架优惠券
      this.$Modal.confirm({
        title: "确认关闭",
        // 记得确认修改此处
        content: "确认要关闭此优惠券么?",
        loading: true,
        onOk: () => {
          // 删除
          updatePlatformCouponStatus({
            couponIds: v.id,
            effectiveDays: 0,
          })
            .then((res) => {
              this.$Modal.remove();
              if (res.success) {
                this.$Message.success("优惠券已关闭");
                this.getDataList();
              }
            })
            .catch(() => {
              this.$Modal;
            });
        },
      });
    },
    remove(v) {
      // 下架优惠券
      this.$Modal.confirm({
        title: "确认删除",
        // 记得确认修改此处
        content: "确认要删除此优惠券么?",
        loading: true,
        onOk: () => {
          // 删除
          deletePlatformCoupon(v.id)
            .then((res) => {
              this.$Modal.remove();
              if (res.success) {
                this.$Message.success("优惠券已删除");
                this.getDataList();
              }
            })
            .catch(() => {
              this.$Modal;
            });
        },
      });
    },
    delAll() {
      // 批量下架
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要下架的优惠券");
        return;
      }
      this.$Modal.confirm({
        title: "确认下架",
        content: "您确认要下架所选的 " + this.selectCount + " 条数据?",
        loading: true,
        onOk: () => {
          let ids = [];
          this.selectList.forEach(function (e) {
            ids.push(e.id);
          });
          let params = {
            couponIds: ids.toString(),
            promotionStatus: "CLOSE",
          };
          // 批量删除
          updatePlatformCouponStatus(params).then((res) => {
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
    //如果作为组件方式，传入了类型值，则搜索参数附加类型
    if (this.getType) {
      this.searchForm.getType = this.getType;
      this.columns.pop();
    }
    if (this.promotionStatus) {
      this.searchForm.promotionStatus = this.promotionStatus;
      this.columns.pop();
    }
    this.init();
  },
};
</script>
