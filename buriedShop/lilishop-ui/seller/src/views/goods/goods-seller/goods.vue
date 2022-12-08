<template>
  <div class="search">
    <Card>
      <Row @keydown.enter.native="handleSearch">
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
          <Form-item label="状态" prop="status">
            <Select
              v-model="searchForm.marketEnable"
              placeholder="请选择"
              clearable
              style="width: 200px"
            >
              <Option value="DOWN">下架</Option>
              <Option value="UPPER">上架</Option>
            </Select>
          </Form-item>
          <Form-item label="销售模式" prop="status">
            <Select
              v-model="searchForm.salesModel"
              placeholder="请选择"
              clearable
              style="width: 200px"
            >
              <Option value="RETAIL">零售</Option>
              <Option value="WHOLESALE">批发</Option>
            </Select>
          </Form-item>
          <Form-item label="商品类型" prop="status">
            <Select
              v-model="searchForm.goodsType"
              placeholder="请选择"
              clearable
              style="width: 200px"
            >
              <Option value="PHYSICAL_GOODS">实物商品</Option>
              <Option value="VIRTUAL_GOODS">虚拟商品</Option>
            </Select>
          </Form-item>
          <Form-item label="商品编号" prop="sn">
            <Input
              type="text"
              v-model="searchForm.id"
              placeholder="商品编号"
              clearable
              style="width: 200px"
            />
          </Form-item>
          <Button @click="handleSearch" type="primary" class="search-btn">搜索</Button>
          <Button @click="handleReset" class="search-btn">重置</Button>
        </Form>
      </Row>
      <Row class="operation padding-row">
        <Button @click="addGoods" type="primary">添加商品</Button>
        <Dropdown @on-click="handleDropdown">
          <Button type="default">
            批量操作
            <Icon type="ios-arrow-down"></Icon>
          </Button>
          <DropdownMenu slot="list">
            <DropdownItem name="uppers">批量上架</DropdownItem>
            <DropdownItem name="lowers">批量下架</DropdownItem>
            <DropdownItem name="deleteAll">批量删除</DropdownItem>
            <DropdownItem name="batchShipTemplate">批量设置物流模板</DropdownItem>
          </DropdownMenu>
        </Dropdown>
      </Row>

      <Table
        class="mt_10"
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        @on-selection-change="changeSelect"
      >
        <!-- 商品栏目格式化 -->
        <template slot="goodsSlot" slot-scope="{ row }">
          <div style="margin-top: 5px; height: 90px; display: flex">
            <div style="">
              <img
                :src="row.original"
                style="height: 80px; margin-top: 3px; width: 70px"
              />
            </div>

            <div style="margin-left: 13px">
              <div class="div-zoom">
                <a @click="linkTo(row.id, row.skuId)">{{ row.goodsName }}</a>
              </div>
              <Poptip trigger="hover" title="扫码在手机中查看" transfer>
                <div slot="content">
                  <!-- <vueQr>123</vueQr> -->
                  <vue-qr
                    :text="wapLinkTo(row.id, row.skuId)"
                    :margin="0"
                    colorDark="#000"
                    colorLight="#fff"
                    :size="150"
                  ></vue-qr>
                </div>
                <img
                  src="../../../assets/qrcode.svg"
                  class="hover-pointer"
                  width="20"
                  height="20"
                  alt=""
                />
              </Poptip>
            </div>
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

    <Modal
      title="更新库存"
      v-model="updateStockModalVisible"
      :mask-closable="false"
      :width="500"
    >
      <Tabs value="updateStock">
        <TabPane label="手动规格更新" name="updateStock">
          <Table
            class="mt_10"
            :columns="updateStockColumns"
            :data="stockList"
            border
          ></Table>
        </TabPane>
        <TabPane label="批量规格更新" name="stockAll">
          <Input type="number" v-model="stockAllUpdate" placeholder="统一规格修改" />
        </TabPane>
      </Tabs>

      <div slot="footer">
        <Button type="text" @click="updateStockModalVisible = false">取消</Button>
        <Button type="primary" @click="updateStock">更新</Button>
      </div>
    </Modal>

    <!-- 批量设置物流模板 -->
    <Modal
      title="批量设置物流模板"
      v-model="shipTemplateModal"
      :mask-closable="false"
      :width="500"
    >
      <Form ref="shipTemplateForm" :model="shipTemplateForm" :label-width="120">
        <FormItem class="form-item-view-el" label="物流模板" prop="templateId">
          <Select v-model="shipTemplateForm.templateId" style="width: 200px">
            <Option v-for="item in logisticsTemplate" :value="item.id" :key="item.id"
              >{{ item.name }}
            </Option>
          </Select>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="shipTemplateModal = false">取消</Button>
        <Button type="primary" @click="saveShipTemplate">更新</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import {
  getGoodsListDataSeller,
  getGoodsSkuListDataSeller,
  updateGoodsSkuStocks,
  upGoods,
  lowGoods,
  deleteGoods,
  batchShipTemplate,
} from "@/api/goods";
import * as API_Shop from "@/api/shops";

export default {
  name: "goods",
  data() {
    return {
      id: "", //要操作的id
      loading: true, // 表单加载状态
      shipTemplateForm: {}, // 物流模板
      shipTemplateModal: false, // 物流模板是否显示
      logisticsTemplate: [], // 物流列表
      updateStockModalVisible: false, // 更新库存模态框显隐
      stockAllUpdate: undefined, // 更新库存数量
      searchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "create_time", // 默认排序字段
        order: "desc", // 默认排序方式
      },
      stockList: [], // 库存列表
      form: {
        // 添加或编辑表单对象初始化数据
        goodsName: "",
        sn: "",
        marketEnable: "",
        price: "",
        sellerName: "",
      },
      updateStockColumns: [
        {
          title: "sku规格",
          key: "sn",
          minWidth: 120,
          render: (h, params) => {
            return h("div", {}, params.row.simpleSpecs);
          },
        },
        {
          title: "审核状态",
          key: "authFlag",
          width: 130,
          render: (h, params) => {
            if (params.row.authFlag == "TOBEAUDITED") {
              return h("Tag", { props: { color: "blue" } }, "待审核");
            } else if (params.row.authFlag == "PASS") {
              return h("Tag", { props: { color: "green" } }, "通过");
            } else if (params.row.authFlag == "REFUSE") {
              return h("Tag", { props: { color: "red" } }, "审核拒绝");
            }
          },
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          width: 200,
          render: (h, params) => {
            let vm = this;
            return h("InputNumber", {
              props: {
                value: params.row.quantity,
              },
              on: {
                "on-change": (event) => {
                  vm.stockList[params.index].quantity = event;
                },
              },
            });
          },
        },
      ],
      // 表单验证规则
      formValidate: {},
      submitLoading: false, // 添加或编辑提交状态
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      columns: [
        {
          type: "selection",
          width: 60,
          align: "center",
        },
        {
          title: "商品编号",
          key: "id",
          width: 180,
          tooltip: true,
        },
        {
          title: "商品名称",
          key: "goodsName",
          minWidth: 200,
          slot: "goodsSlot",
        },
        {
          title: "销售模式",
          key: "salesModel",
          width: 100,
          render: (h, params) => {
            if (params.row.salesModel === "RETAIL") {
              return h("Tag", { props: { color: "orange" } }, "零售");
            } else if (params.row.salesModel === "WHOLESALE") {
              return h("Tag", { props: { color: "magenta" } }, "批发");
            } else {
              return h("Tag", { props: { color: "volcano" } }, "其他类型");
            }
          },
        },
        {
          title: "商品类型",
          key: "goodsType",
          width: 130,
          render: (h, params) => {
            if (params.row.goodsType === "PHYSICAL_GOODS") {
              return h("Tag", { props: { color: "geekblue" } }, "实物商品");
            } else if (params.row.goodsType === "VIRTUAL_GOODS") {
              return h("Tag", { props: { color: "purple" } }, "虚拟商品");
            } else {
              return h("Tag", { props: { color: "cyan" } }, "电子卡券");
            }
          },
        },
        {
          title: "商品价格",
          key: "price",
          width: 130,
          render: (h, params) => {
            return h("div", this.$options.filters.unitPrice(params.row.price, "￥"));
          },
        },
        {
          title: "库存",
          key: "quantity",
          width: 120,
          render: (h, params) => {
            if (params.row.quantity) {
              return h("div", params.row.quantity);
            } else {
              return h("div", 0);
            }
          },
        },
        {
          title: "审核状态",
          key: "authFlag",
          width: 120,
          render: (h, params) => {
            if (params.row.authFlag == "PASS") {
              return h("Tag", { props: { color: "green" } }, "通过");
            } else if (params.row.authFlag == "TOBEAUDITED") {
              return h("Tag", { props: { color: "volcano" } }, "待审核");
            } else if (params.row.authFlag == "REFUSE") {
              return h("Tag", { props: { color: "red" } }, "审核拒绝");
            }
          },
        },
        {
          title: "上架状态",
          key: "marketEnable",
          width: 130,
          sortable: false,
          render: (h, params) => {
            if (params.row.marketEnable == "DOWN") {
              return h("Tag", { props: { color: "red" } }, "下架");
            } else if (params.row.marketEnable == "UPPER") {
              return h("Tag", { props: { color: "green" } }, "上架");
            }
          },
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          fixed: "right",
          width: 200,
          render: (h, params) => {
            let enableOrDisable = "";
            let showEditStock = "";
            if (params.row.marketEnable == "DOWN") {
              enableOrDisable = h(
                "Button",
                {
                  props: {
                    size: "small",
                    type: "success",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.upper(params.row);
                    },
                  },
                },
                "上架"
              );
            } else {
              showEditStock = h(
                "Button",
                {
                  props: {
                    type: "default",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.getStockDetail(params.row.id);
                    },
                  },
                },
                "库存"
              );
              enableOrDisable = h(
                "Button",
                {
                  props: {
                    type: "warning",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.lower(params.row);
                    },
                  },
                },
                "下架"
              );
            }
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    type: "info",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.editGoods(params.row);
                    },
                  },
                },
                "编辑"
              ),
              showEditStock,
              enableOrDisable,
            ]);
          },
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
    };
  },
  methods: {
    init() {
      // 初始化数据
      this.getDataList();
    },
    // 添加商品
    addGoods() {
      this.$router.push({ name: "goods-operation" });
    },
    // 编辑商品
    editGoods(v) {
      this.$router.push({ name: "goods-operation-edit", query: { id: v.id } });
    },

    //批量操作
    handleDropdown(v) {
      //批量上架
      if (v == "uppers") {
        this.uppers();
      }
      //批量下架
      if (v == "lowers") {
        this.lowers();
      }
      //批量删除商品
      if (v == "deleteAll") {
        this.deleteAll();
      }
      //批量设置物流模板
      if (v == "batchShipTemplate") {
        this.batchShipTemplate();
      }
    },
    // 获取库存详情
    getStockDetail(id) {
      getGoodsSkuListDataSeller({ goodsId: id, pageSize: 1000 }).then((res) => {
        if (res.success) {
          this.updateStockModalVisible = true;
          this.stockAllUpdate = undefined;
          this.stockList = res.result.records;
        }
      });
    },
    // 更新库存
    updateStock() {
      let updateStockList = this.stockList.map((i) => {
        let j = { skuId: i.id, quantity: i.quantity };
        if (this.stockAllUpdate) {
          j.quantity = this.stockAllUpdate;
        }
        return j;
      });
      updateGoodsSkuStocks(updateStockList).then((res) => {
        if (res.success) {
          this.updateStockModalVisible = false;
          this.$Message.success("更新库存成功");
          this.getDataList();
        }
      });
    },
    // 改变页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
      this.clearSelectAll();
    },
    // 改变页数
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
    // 重置搜索条件
    handleReset() {
      this.searchForm = {};
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = 10;
      // 重新加载数据
      this.getDataList();
    },
    // 清除多选
    clearSelectAll() {
      this.$refs.table.selectAll(false);
    },
    // 添加选中项
    changeSelect(e) {
      this.selectList = e;
      this.selectCount = e.length;
    },
    //保存物流模板信息
    saveShipTemplate() {
      this.$Modal.confirm({
        title: "确认设置物流模板",
        content: "您确认要设置所选的 " + this.selectCount + " 个商品的物流模板?",
        loading: true,
        onOk: () => {
          let ids = [];
          this.selectList.forEach(function (e) {
            ids.push(e.id);
          });
          // 批量设置物流模板
          batchShipTemplate(this.shipTemplateForm).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("物流模板设置成功");
              this.clearSelectAll();
              this.getDataList();
            }
            this.shipTemplateModal = false;
          });
        },
      });
    },
    //批量设置物流模板
    batchShipTemplate() {
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要设置物流模板的商品");
        return;
      }
      this.getShipTempList();
      let data = [];
      this.selectList.forEach(function (e) {
        data.push(e.id);
      });
      this.shipTemplateForm.goodsId = data;
      this.shipTemplateModal = true;
    },
    // 获取商品列表数据
    getDataList() {
      this.loading = true;
      // 带多条件搜索参数获取表单数据
      getGoodsListDataSeller(this.searchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.data = res.result.records;
          this.total = res.result.total;
        }
      });
    },
    // 获取物流模板
    getShipTempList() {
      API_Shop.getShipTemplate().then((res) => {
        if (res.success) {
          this.logisticsTemplate = res.result;
        }
      });
    },
    //下架商品
    lower(v) {
      this.$Modal.confirm({
        title: "确认下架",
        content: "您确认要下架 " + v.goodsName + " ?",
        loading: true,
        onOk: () => {
          let params = {
            goodsId: v.id,
          };
          lowGoods(params).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("下架成功");
              this.getDataList();
            }
          });
        },
      });
    },
    //批量下架
    lowers() {
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要下架的商品");
        return;
      }
      this.$Modal.confirm({
        title: "确认下架",
        content: "您确认要下架所选的 " + this.selectCount + " 个商品?",
        loading: true,
        onOk: () => {
          let ids = [];
          this.selectList.forEach(function (e) {
            ids.push(e.id);
          });
          let params = {
            goodsId: ids.toString(),
          };
          // 批量上架
          lowGoods(params).then((res) => {
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
    //批量删除商品
    deleteAll() {
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要删除的商品");
        return;
      }
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除所选的 " + this.selectCount + " 个商品?",
        loading: true,
        onOk: () => {
          let ids = [];
          this.selectList.forEach(function (e) {
            ids.push(e.id);
          });
          let params = {
            goodsId: ids.toString(),
          };
          // 批量删除
          deleteGoods(params).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除成功");
              this.clearSelectAll();
              this.getDataList();
            }
          });
        },
      });
    },
    //批量上架
    uppers(v) {
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要上架的商品");
        return;
      }
      this.$Modal.confirm({
        title: "确认上架",
        content: "您确认要上架所选的 " + this.selectCount + " 个商品?",
        loading: true,
        onOk: () => {
          let ids = [];
          this.selectList.forEach(function (e) {
            ids.push(e.id);
          });
          let params = {
            goodsId: ids.toString(),
          };
          // 批量上架
          upGoods(params).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("上架成功");
              this.clearSelectAll();
              this.getDataList();
            }
          });
        },
      });
    },
    upper(v) {
      this.$Modal.confirm({
        title: "确认上架",
        content: "您确认要上架 " + v.goodsName + " ?",
        loading: true,
        onOk: () => {
          let params = {
            goodsId: v.id,
          };
          upGoods(params).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("上架成功");
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
  mounted() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
@import "@/styles/table-common.scss";
</style>
