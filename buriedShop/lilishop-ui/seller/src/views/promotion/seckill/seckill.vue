<template>
  <div class="seckill">
    <Card>
      <Form ref="searchForm" :model="searchForm" inline :label-width="70" class="search-form">
        <Form-item label="活动名称" prop="goodsName">
          <Input type="text" v-model="searchForm.promotionName" placeholder="请输入活动名称" clearable style="width: 200px" />
        </Form-item>
        <Form-item label="活动状态" prop="promotionStatus">
          <Select v-model="searchForm.promotionStatus" placeholder="请选择" clearable style="width: 200px">
            <Option value="NEW">未开始</Option>
            <Option value="START">已开始/上架</Option>
            <Option value="END">已结束/下架</Option>
            <Option value="CLOSE">紧急关闭/作废</Option>
          </Select>
        </Form-item>
        <Form-item label="活动时间">
          <DatePicker v-model="selectDate" type="daterange" clearable placeholder="选择起始时间" style="width: 200px"></DatePicker>
        </Form-item>
        <Button @click="handleSearch" type="primary" class="search-btn">搜索</Button>
        <Button @click="handleReset" class="ml_10">重置</Button>
      </Form>

      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        class="mt_10"
      >
        <template slot-scope="{ row }" slot="applyEndTime">
          {{ unixDate(row.applyEndTime) }}
        </template>
        <template slot-scope="{ row }" slot="hours">
          <Tag v-for="item in unixHours(row.hours)" :key="item">{{
            item
          }}</Tag>
        </template>
        <template slot-scope="{ row }" slot="action">
          <Button
            v-if="row.promotionStatus === 'NEW'"
            type="primary"
            size="small"
            @click="manage(row)"
            >管理</Button
          >
          <Button
            v-else
            type="info"
            size="small"
            @click="manage(row)"
            >查看</Button
          >
        </template>
      </Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePage" @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]"
          size="small" show-total show-elevator show-sizer></Page>
      </Row>
    </Card>
  </div>
</template>

<script>
import { seckillList } from "@/api/promotion";
export default {
  name: "seckill",
  components: {},
  data() {
    return {
      selectDate:[],
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "startTime",
        order: "desc", // 默认排序方式
      },
      columns: [
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
          title: "状态",
          key: "promotionStatus",
          width: 100,
          render: (h, params) => {
            let text = "未知",
              color = "default";
            if (params.row.promotionStatus == "NEW") {
              text = "未开始";
              color = "geekblue";
            } else if (params.row.promotionStatus == "START") {
              text = "已开始";
              color = "green";
            } else if (params.row.promotionStatus == "END") {
              text = "已结束";
              color = "volcano";
            } else if (params.row.promotionStatus == "CLOSE") {
              text = "已关闭";
              color = "red";
            }
            return h("div", [
              h(
                "Tag",
                {
                  props: {
                    color: color,
                  },
                },
                text
              ),
            ]);
          },
        },
        {
          title: "操作",
          slot: "action",
          align: "center",
          width: 100,
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
    // 重置
    handleReset() {
      this.searchForm = {};
      this.selectDate = "";
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    // 管理
    manage(row) {
      this.$router.push({ name: "seckill-goods", query: { id: row.id } });
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
      // 带多条件搜索参数获取表单数据
      seckillList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
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
  // 页面缓存处理，从该页面离开时，修改KeepAlive为false，保证进入该页面是刷新
  beforeRouteLeave(to, from, next) {
    from.meta.keepAlive = false
    next()
  }
};
</script>
<style lang="scss"  scoped>

</style>
