<template>
  <div class="pintuan-goods">
    <Card>
      <Table style="margin: 10px 0" border :columns="columns" :data="data"></Table>

      <Row class="operation" v-if="status == 'manager'">
        <Button type="primary" @click="openSkuList">选择商品</Button>
        <Button @click="delAll">批量删除</Button>
        <Button @click="getDataList" icon="md-refresh">刷新</Button>
        <Button type="dashed" @click="openTip = !openTip">{{
          openTip ? "关闭提示" : "开启提示"
        }}</Button>
      </Row>
      <Row v-show="openTip" v-if="status == 'manager'">
        <Alert show-icon>
          已选择 <span>{{ selectCount }}</span> 项
          <a class="select-clear" @click="clearSelectAll">清空</a>
        </Alert>
      </Row>
      <h3 class="act-goods">活动商品</h3>
      <Table
        class="mt_10"
        :loading="loading"
        border
        :columns="goodsColumns"
        :data="goodsData"
        ref="table"
        @on-selection-change="changeSelect"
      >
        <template slot-scope="{ row, index }" slot="price">
          <Input
            v-model="row.price"
            :disabled="status === 'view'"
            @input="goodsData[index].price = row.price"
          />
        </template>
        <template slot-scope="{ index }" slot="action">
          <Button
            type="error"
            size="small"
            ghost
            v-if="status === 'manager'"
            @click="delGoods(index)"
            >删除</Button
          >
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
      <Row class="operation">
        <Button @click="closeCurrentPage">返回</Button>
        <Button
          v-if="status == 'manager'"
          type="primary"
          :loading="submitLoading"
          @click="save"
          >保存</Button
        >
      </Row>
    </Card>

    <sku-select
      ref="skuSelect"
      :goodsData="goodsData"
      @selectedGoodsData="selectedGoodsData"
    ></sku-select>
  </div>
</template>
<script>
import { getPintuanGoodsList, getPintuanDetail, editPintuan } from "@/api/promotion.js";
import skuSelect from "@/views/lili-dialog";
export default {
  components: {
    skuSelect,
  },
  data() {
    return {
      openTip: true, // 显示提示
      loading: false, // 表单加载状态
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
      },

      submitLoading: false, // 添加或编辑提交状态
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      data: [], // 表单数据
      total: 0, // 表单数据总数
      status: this.$route.query.status, // 查看还是修改
      columns: [
        // 活动详情表头
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
          title: "状态",
          key: "promotionStatus",
          minWidth: 100,
          render: (h, params) => {
            let text = "未知",
              color = "";
            if (params.row.promotionStatus == "NEW") {
              text = "未开始";
              color = "default";
            } else if (params.row.promotionStatus == "START") {
              text = "已开始";
              color = "green";
            } else if (params.row.promotionStatus == "END") {
              text = "已结束";
              color = "blue";
            } else if (params.row.promotionStatus == "CLOSE") {
              text = "已关闭";
              color = "red";
            }
            return h("div", [h("Tag", { props: { color: color } }, text)]);
          },
        },
      ],
      goodsColumns: [
        // 活动商品表头
        { type: "selection", width: 60, align: "center" },
        {
          title: "商品名称",
          key: "goodsName",
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
          slot: "price",
          minWidth: 50,
        },
        {
          title: "操作",
          slot: "action",
          minWidth: 50,
          align: "center",
        },
      ],
      goodsData: [], // 商品列表
    };
  },
  methods: {
    // 关闭当前页面
    closeCurrentPage() {
      this.$store.commit("removeTag", "pintuan-goods");
      localStorage.storeOpenedList = JSON.stringify(
        this.$store.state.app.storeOpenedList
      );
      this.$router.go(-1);
    },
    // 保存商品
    save() {
      if (this.goodsData.length == 0) {
        this.$Modal.warning({ title: "提示", content: "请选择活动商品" });
        return;
      }
      for (let i = 0; i < this.goodsData.length; i++) {
        let data = this.goodsData[i];
        if (!data.price) {
          this.$Modal.warning({
            title: "提示",
            content: `请填写【${data.goodsName}】的价格`,
          });
          return;
        }
      }
      this.goodsData.forEach((item) => {
        item.promotionId = this.data[0].id;
        item.startTime = this.data[0].startTime;
        item.endTime = this.data[0].endTime;
      });
      this.data[0].promotionGoodsList = this.goodsData;
      this.submitLoading = true;
      editPintuan(this.data[0]).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success("修改拼团商品成功");
          this.closeCurrentPage();
        }
      });
    },
    init() {
      // 初始化数据
      this.getDataList();
      this.getPintuanMsg();
    },

    changePage(v) {
      // 分页 改变页数
      this.searchForm.pageNumber = v;
      this.getDataList();
      this.clearSelectAll();
    },

    changePageSize(v) {
      // 分页 改变每页数
      this.searchForm.pageSize = v;
      this.getDataList();
    },

    handleSearch() {
      // 搜索
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      this.getDataList();
    },

    handleReset() {
      // 重置
      this.searchForm.pageNumber = 1;
      this.searchForm.promotionName = "";
      this.selectDate = null;
      this.getDataList();
    },

    clearSelectAll() {
      // 清空所有已选项
      this.$refs.table.selectAll(false);
    },
    changeSelect(e) {
      // 获取选择数据
      this.selectList = e;
      this.selectCount = e.length;
    },

    getDataList() {
      // 获取商品列表
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
    getPintuanMsg() {
      // 获取拼团详情
      getPintuanDetail(this.$route.query.id).then((res) => {
        if (res.success) this.data.push(res.result);
      });
    },
    delGoods(index) {
      // 删除商品
      this.goodsData.splice(index, 1);
    },
    delAll() {
      // 批量删除商品
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要删除的数据");
        return;
      }
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除所选的 " + this.selectCount + " 条数据?",
        onOk: () => {
          let ids = [];
          this.selectList.forEach(function (e) {
            ids.push(e.skuId);
          });
          this.goodsData = this.goodsData.filter((item) => {
            return !ids.includes(item.skuId);
          });
        },
      });
    },
    selectedGoodsData(item) {
      // 选择商品
      console.log(item);
      let list = [];
      item.forEach((e) => {
        list.push({
          goodsName: e.goodsName,
          price: e.price,
          originalPrice: e.price,
          quantity: e.quantity,
          storeId: e.storeId,
          sellerName: e.sellerName,
          thumbnail: e.thumbnail,
          skuId: e.id,
          categoryPath: e.categoryPath,
          goodsId: e.goodsId,
          goodsType: e.goodsType,
        });
      });
      this.goodsData = list;
    },
    openSkuList() {
      // 显示商品选择器
      this.$refs.skuSelect.open("goods");
      let data = JSON.parse(JSON.stringify(this.goodsData));
      data.forEach((e) => {
        e.id = e.skuId;
      });
      this.$refs.skuSelect.goodsData = data;
    },
  },
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
.operation {
  margin-bottom: 10px;
}
.act-goods {
  margin: 20px 0;
  font-size: 15px;
  &::before {
    content: "|";
    color: $theme_color;
    display: inline-block;
    font-weight: bold;
    font-size: 16px;
    margin-right: 5px;
  }
}
</style>
