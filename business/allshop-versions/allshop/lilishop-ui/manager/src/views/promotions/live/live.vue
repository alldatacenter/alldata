<template>
  <div>
    <Card>
      <Tabs v-model="searchForm.status">
        <!-- 标签栏 -->
        <TabPane
          v-for="(item, index) in tabs"
          :key="index"
          :name="item.status"
          :label="item.title"
        >
        </TabPane>
      </Tabs>
      <Table :columns="liveColumns" :data="liveData"></Table>
      <Row type="flex" style="margin: 20px" justify="end" class="mt_10">
        <Page
          :current="searchForm.pageNumber"
          :total="total"
          :page-size="searchForm.pageSize"
          @on-change="changePageNumber"
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
import { getLiveList, whetherStar } from "@/api/promotion.js";
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
            return h("span", this.$options.filters.unixToDate(params.row.startTime));
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
          title: "是否推荐",
          align: "center",
          render: (h, params) => {
            return h("div", [
              h(
                "i-switch",
                {
                  // 数据库0 enabled,1 disabled
                  props: {
                    type: "primary",
                    size: "large",
                    value: params.row.recommend == true,
                  },
                  on: {
                    "on-change": () => {
                      this.star(params.row, params.index);
                    },
                  },
                },
                [
                  h("span", {
                    slot: "open",
                    domProps: {
                      innerHTML: "是",
                    },
                  }),
                  h("span", {
                    slot: "close",
                    domProps: {
                      innerHTML: "否",
                    },
                  }),
                ]
              ),
            ]);
          },
        },

        {
          title: "直播状态",
          render: (h, params) => {
            if (params.row.status == "NEW") {
              return h("div", [h("tag", { props: { color: "blue" } }, "未开始")]);
            } else if (params.row.status == "START") {
              return h("div", [h("tag", { props: { color: "green" } }, "直播中")]);
            } else {
              return h("div", [h("tag", { props: { color: "volcano" } }, "已结束")]);
            }
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
                  "查看"
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
    // 直播状态
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
     * 是否推荐
     */
    async star(val, index) {
      let switched;
      if (this.liveData[index].recommend) {
        this.$set(this.liveData[index], "recommend", false);
        switched = false;
      } else {
        this.$set(this.liveData[index], "recommend", true);
        switched = true;
      }

      await whetherStar({ id: val.id, recommend: switched });

      this.getStoreLives();
    },

    /**
     * 页面数据大小分页回调
     */
    changePageSize(val) {
      this.searchForm.pageSize = val;
      this.getStoreLives();
    },
    /**
     * 分页回调
     */
    changePageNumber(val) {
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
        path: "/live-detail",
        query: { ...val, liveStatus: this.searchForm.status },
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.btns {
  margin-bottom: 10px;
  margin-top: 10px;
}
.page {
  margin-top: 20px;
}
</style>
