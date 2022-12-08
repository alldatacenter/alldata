<template>
  <div class="full-cut">
    <Card>
      <Row>
        <Form
          ref="searchForm"
          :model="searchForm"
          inline
          :label-width="70"
          class="search-form"
        >
          <Form-item label="活动名称" prop="promotionName">
            <Input
              type="text"
              v-model="searchForm.promotionName"
              placeholder="请输入活动名称"
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
            class="search-btn"
            icon="ios-search"
            >搜索</Button
          >
        </Form>
      </Row>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        class="mt_10"
        sortable="custom"
      >
        <template slot-scope="{ row }" slot="applyEndTime">
          {{ unixDate(row.applyEndTime) }}
        </template>
        <template slot-scope="{ row }" slot="promotionType">
          {{ row.fullMinusFlag ? "满减" : "满折" }}
        </template>
        <template slot-scope="{ row }" slot="hours">
          <Tag v-for="item in unixHours(row.hours)" :key="item">{{ item }}</Tag>
        </template>
        <template slot-scope="{ row }" slot="action">
          <div>
            <Button type="info" size="small" @click="view(row)">查看</Button>
            <Button
              type="error"
              v-if="row.promotionStatus === 'NEW' || row.promotionStatus === 'START'"
              style="margin-left: 5px"
              size="small"
              @click="openOrClose(row)"
              >关闭</Button
            >
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
import { getFullDiscountList, updateFullDiscount } from "@/api/promotion.js";
import { promotionsStatusRender } from "@/utils/promotions";

export default {
  data() {
    return {
      selectDate: [],
      total: 0,
      loading: false, // 表单加载状态
      searchForm: {
        // 请求参数
        pageNumber: 1,
        pageSize: 10,
        sort: "createTime",
        order: "desc",
      },
      columns: [
        // 表头
        {
          title: "活动名称",
          key: "promotionName",
          minWidth: 120,
        },
        {
          title: "开始时间",
          key: "startTime",
          width: 170,
        },
        {
          title: "结束时间",
          key: "endTime",
          width: 170,
        },
        {
          title: "店铺名称",
          key: "storeName",
          minWidth: 60,
        },
        {
          title: "活动类型",
          slot: "promotionType",
          minWidth: 60,
        },
        {
          title: "活动状态",
          key: "promotionStatus",
          minWidth: 60,
          render: (h, params) => {
            return promotionsStatusRender(h, params);
          },
        },
        {
          title: "操作",
          slot: "action",
          align: "center",
          width: 140,
        },
      ],
      data: [], // 列表数据
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getDataList();
    },
    // 开启或关闭活动
    openOrClose(row) {
      let name = "开启";
      if (row.promotionStatus === "START") {
        name = "关闭";
        this.$Modal.confirm({
          title: "提示",
          // 记得确认修改此处
          content: `确认${name}此活动吗?需要一定时间才能生效，请耐心等待`,
          loading: true,
          onOk: () => {
            // 删除
            updateFullDiscount(row.id).then((res) => {
              this.$Modal.remove();
              if (res.success) {
                this.$Message.success(`${name}成功`);
                this.getDataList();
              }
            });
          },
        });
      }
    },
    changePage(v) {
      // 改变页数
      this.searchForm.pageNumber = v;
      this.getDataList();
    },
    changePageSize(v) {
      // 改变页码
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
    // 获取列表数据
    getDataList() {
      this.loading = true;
      if (this.selectDate && this.selectDate[0] && this.selectDate[1]) {
        this.searchForm.startTime = this.selectDate[0].getTime();
        this.searchForm.endTime = this.selectDate[1].getTime();
      } else {
        this.searchForm.startTime = null;
        this.searchForm.endTime = null;
      }
      getFullDiscountList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          console.log(res)
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
    },
    view(row) {
      // 查看
      this.$router.push({ name: "full-discount-detail", query: { id: row.id } });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
