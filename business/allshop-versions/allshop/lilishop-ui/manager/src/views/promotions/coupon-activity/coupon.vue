<template>
  <div class="search">
    <Card>
      <Row class="operation padding-row">
        <Button @click="add" type="primary">添加活动</Button>
      </Row>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        sortable="custom"
      >
        <template slot-scope="{ row }" slot="action">
          <Button type="info" size="small" style="margin-right: 10px" @click="info(row)">
            查看
          </Button>
          <Button
            v-if="
              (!checked && row.promotionStatus === 'START') ||
              row.promotionStatus === 'NEW'
            "
            type="error"
            size="small"
            style="margin-right: 10px"
            @click="remove(row)"
            >关闭
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
import { getCouponActivityList, closeActivity } from "@/api/promotion";
import { promotionsStatusRender } from "@/utils/promotions";

export default {
  name: "coupon-activity",
  components: {},
  data() {
    return {
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
      },
      form: {
        // 添加或编辑表单对象初始化数据
        promotionName: "",
      },
      // 表单验证规则
      formValidate: {
        promotionName: [{ required: true, message: "不能为空", trigger: "blur" }],
      },
      submitLoading: false, // 添加或编辑提交状态
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      columns: [
        // 表头
        {
          title: "活动名称",
          key: "promotionName",
          minWidth: 120,
        },
        {
          title: "活动类型",
          key: "couponActivityType",
          minWidth: 120,
          render: (h, params) => {
            if (params.row.couponActivityType === "REGISTERED") {
              return h("div", ["注册赠券"]);
            } else {
              return h("div", ["精确发券"]);
            }
          },
        },
        {
          title: "活动范围",
          key: "activityScope",
          minWidth: 120,
          render: (h, params) => {
            let text = "未知";
            if (params.row.activityScope === "DESIGNATED") {
              text = "指定会员";
            } else {
              text = "全部会员";
            }
            return h("div", [text]);
          },
        },
        {
          title: "活动时间",
          minWidth: 150,
          render: (h, params) => {
            if (params.row.startTime && params.row.endTime) {
              return h("div", {
                domProps: {
                  innerHTML: params.row.startTime + "<br/>" + params.row.endTime,
                },
              });
            }
          },
        },
        {
          title: "状态",
          minWidth: 80,
          key: "promotionStatus",
          fixed: "right",
          render: (h, params) => {
            return promotionsStatusRender(h, params);
          },
        },
        {
          title: "操作",
          slot: "action",
          align: "center",
          fixed: "right",
          minWidth: 100,
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
    };
  },
  props: {
    // 是否为选中模式
    checked: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    //获取数据 初始化
    init() {
      this.getDataList();
    },
    //增加券活动
    add() {
      this.$router.push({ name: "add-coupon-activity" });
    },
    //查看详情
    info(v) {
      this.$router.push({ name: "coupon-activity-info", query: { id: v.id } });
    },
    // 分页 修改页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    // 分页 修改页数
    changePageSize(v) {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    //搜索活动
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    //数据获取
    getDataList() {
      this.loading = true;
      if (this.selectDate && this.selectDate[0] && this.selectDate[1]) {
        this.searchForm.startTime = this.selectDate[0].getTime();
        this.searchForm.endTime = this.selectDate[1].getTime();
      } else {
        this.searchForm.startTime = null;
        this.searchForm.endTime = null;
      }
      getCouponActivityList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
      this.loading = false;
    },
    //跳转编辑
    edit(v) {
      this.$router.push({ name: "edit-platform-coupon", query: { id: v.id } });
    },
    //停止活动
    remove(v) {
      this.$Modal.confirm({
        title: "确认关闭",
        content: "确认要关闭此优惠券活动么?关闭活动只能重新创建",
        loading: true,
        onOk: () => {
          // 删除
          closeActivity(v.id)
            .then((res) => {
              if (res.success) {
                this.$Message.success("优惠券活动已关闭");
                this.getDataList();
                this.$Modal.remove();
              }
            })
            .catch(() => {});
        },
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
