<template>
  <div>
    <Card>
      <Form ref="searchForm" :model="searchForm" inline :label-width="100" class="search-form">

        <Form-item label="直播状态" prop="promotionStatus">
          <Select v-model="searchForm.status" placeholder="请选择" clearable style="width: 200px">
            <Option value="NEW">未开始</Option>
            <Option value="START">直播中</Option>
            <Option value="END">已结束</Option>

          </Select>
        </Form-item>

        <Button @click="handleSearch" type="primary" class="search-btn" icon="ios-search">搜索</Button>
      </Form>
      <div class="btns">
        <Button @click="createLive()" type="primary">创建直播</Button>
      </div>
      <Tabs v-model="searchForm.status">
        <!-- 标签栏 -->
        <TabPane v-for="(item,index) in tabs" :key="index" :name="item.status" :label="item.title">

        </TabPane>

      </Tabs>
      <Table :columns="liveColumns" :data="liveData"></Table>
      <Row type="flex" justify="end" class="mt_10">
        <Page :current="searchForm.pageNumber" :total="total" :page-size="searchForm.pageSize" @on-change="changePageNumber" @on-page-size-change="changePageSize" :page-size-opts="[10, 20, 50]"
          size="small" show-total show-elevator show-sizer></Page>
      </Row>

    </Card>
  </div>
</template>

<script>
import { getLiveList } from "@/api/promotion.js";
export default {
  data() {
    return {
      // 查询数据的总数
      total: 0,
      // 查询的form
      searchForm: {
        pageSize: 10,
        pageNumber: 1,
        status: "NEW",
      },
      // 直播tab选项栏
      tabs: [
        {
          title: "直播中",
          status: "START",
        },
        {
          title: "未开始",
          status: "NEW",
        },
        {
          title: "已结束",
          status: "END",
        },
      ],
      liveColumns: [
        {
          title: "直播标题",
          key: "name",
        },
        {
          title: "主播昵称",
          key: "anchorName",
        },
        {
          title: "直播开始时间",
          key: "createTime",
          render: (h, params) => {
            return h(
              "span",

              this.$options.filters.unixToDate(params.row.startTime)
            );
          },
        },
        {
          title: "直播结束时间",
          key: "endTime",
          render: (h, params) => {
            return h(
              "span",

              this.$options.filters.unixToDate(params.row.endTime)
            );
          },
        },

        {
          title: "直播状态",
          render: (h, params) => {
            return h(
              "span",
              params.row.status == "NEW"
                ? "未开始"
                : params.row.status == "START"
                ? "直播中"
                : "已结束"
            );
          },
        },
        {
          title: "操作",
          key: "action",
          render: (h, params) => {
            return h(
              "div",
              {
                style: {
                  display: "flex",
                },
              },
              [
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
                        this.getLiveDetail(params.row);
                      },
                    },
                  },
                  "查看/添加商品"
                ),
              ]
            );
          },
        },
      ], //table中显示的title
      liveData: [], //table中显示的直播数据
    };
  },
  watch: {
    "searchForm.status": {
      handler() {
        this.liveData = [];
        this.getStoreLives();
      },
      deep: true,
    },
  },
  mounted() {
    this.getStoreLives();
  },
  methods: {
    /**
     * 搜索直播间状态
     */
    handleSearch() {
      this.getStoreLives();
    },

    /**
     * 页面数据大小分页回调
     */
    changePageSize(val) {
      console.log(val)
      this.searchForm.pageSize = val;
      this.getStoreLives();
    },
    /**
     * 分页回调
     */
    changePageNumber(val) {
      console.log(val)
      this.searchForm.pageNumber = val;
      this.getStoreLives();
    },

    /**
     * 获取店铺直播间列表
     */
    async getStoreLives() {
      let result = await getLiveList(this.searchForm);
      if (result.success) {
        this.liveData = result.result.records;
        this.total = result.result.total;
      }
    },

    /**
     * 获取直播间详情
     */
    getLiveDetail(val) {
      this.$router.push({
        path: "/add-live",
        query: { ...val, liveStatus: this.searchForm.status },
      });
    },
    /**
     * 创建直播
     */
    createLive() {
      this.$router.push({ path: "/add-live" });
    },

  },
};
</script>

<style lang="scss" scoped>
@import "@/styles/table-common.scss";
.btns {
  margin-bottom: 10px;
  margin-top: 10px;
}
.page {
  margin-top: 20px;
}
</style>
