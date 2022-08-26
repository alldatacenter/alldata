<template>
  <div class="seckill">
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
            icon="ios-search"
            class="search-btn"
            >搜索</Button
          >
        </Form>
      </Row>
      <Tabs value="list" class="mt_10" @on-click="clickTabPane">
        <TabPane label="秒杀活动列表" name="list">
          <Table
            :loading="loading"
            border
            :columns="columns"
            :data="data"
            ref="table"
            class="mt_10"
          >
            <template slot-scope="{ row }" slot="action">
              <Button
                type="info"
                size="small"
                class="mr_5"
                v-if="row.promotionStatus === 'CLOSE' || row.promotionStatus === 'NEW'"
                @click="edit(row)"
                >编辑</Button
              >

              <Button type="info" size="small" class="mr_5" v-else @click="manage(row)"
                >查看</Button
              >

              <Button
                type="success"
                size="small"
                class="mr_5"
                v-if="row.promotionStatus == 'NEW'"
                @click="manage(row)"
                >管理</Button
              >

              <Button
                type="error"
                size="small"
                v-if="row.promotionStatus == 'START' || row.promotionStatus == 'NEW'"
                class="mr_5"
                @click="off(row)"
                >关闭</Button
              >
              &nbsp;
              <Button
                type="error"
                size="small"
                v-if="row.promotionStatus == 'CLOSE' || row.promotionStatus == 'END'"
                ghost
                @click="expire(row)"
                >删除</Button
              >
            </template>
          </Table>

          <Row type="flex" justify="end" class="mt_10">
            <Page
              style="margin: 20px 0"
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
        </TabPane>
        <TabPane label="秒杀活动设置" name="setup">
          <setupSeckill v-if="setupFlag"></setupSeckill>
        </TabPane>
      </Tabs>
    </Card>
  </div>
</template>

<script>
import { getSeckillList, delSeckill, updateSeckillStatus } from "@/api/promotion";
import setupSeckill from "@/views/promotions/seckill/seckill-setup";
import { promotionsStatusRender } from "@/utils/promotions";

export default {
  name: "seckill",
  components: {
    setupSeckill,
  },
  data() {
    return {
      selectDate: [],
      loading: true, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime",
        order: "desc", // 默认排序方式
      },
      setupFlag: false, //默认不请求设置
      columns: [
        // 表单
        {
          title: "活动名称",
          key: "promotionName",
          minWidth: 130,
          tooltip: true,
        },

        {
          title: "开始时间",
          key: "startTime",
          width: 180,
        },
        {
          title: "申请截止时间",
          key: "applyEndTime",
          width: 180,
        },

        {
          title: "活动状态",
          key: "promotionStatus",
          width: 100,
          render: (h, params) => {
            return promotionsStatusRender(h, params);
          },
        },

        {
          title: "申请规则",
          key: "seckillRule",
          minWidth: 120,
          tooltip: true,
        },
        {
          title: "操作",
          slot: "action",
          align: "center",
          width: 250,
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
    };
  },
  methods: {
    // tab栏点击切换列表和设置
    clickTabPane(name) {
      if (name == "setup") {
        this.setupFlag = true;
      } else {
        this.setupFlag = false;
      }
    },
    // 初始化信息
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
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 搜索
    handleSearch() {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },
    edit(v) {
      // 编辑
      this.$router.push({ name: "manager-seckill-add", query: { id: v.id } });
    },
    manage(v) {
      // 管理
      this.$router.push({ name: "seckill-goods", query: { id: v.id } });
    },

    off(v) {
      // 下架
      this.$Modal.confirm({
        title: "提示",
        content: "您确定要下架该活动吗？",
        onOk: () => {
          updateSeckillStatus(v.id).then((res) => {
            if (res.success) {
              this.$Message.success("下架成功");
              this.getDataList();
            }
          });
        },
      });
    },
    expire(v) {
      // 作废
      this.$Modal.confirm({
        title: "提示",
        content: "您确定要作废该活动吗？",
        onOk: () => {
          delSeckill(v.id).then((res) => {
            if (res.success) {
              this.$Message.success("作废成功");
              this.getDataList();
            }
          });
        },
      });
    },
    // 获取数据集合
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
      getSeckillList(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss">
.mr_5 {
  margin: 0 5px;
}
</style>
