<template>
  <div class="wrapper">
    <Card>
      <Form ref="form" :model="form" :rules="formRule">
        <div class="base-info-item">
          <h4>添加积分商品</h4>
          <div class="form-item-view">
            <FormItem astyle="width: 100%">
              <div style="display: flex; margin-bottom: 10px">
                <Button type="primary" @click="openSkuList">选择商品</Button>
                <Button
                  type="error"
                  ghost
                  style="margin-left: 10px"
                  @click="delSelectGoods"
                  >批量删除</Button
                >
              </div>
              <Table
                border
                v-if="showTable"
                :columns="columns"
                :data="promotionGoodsList"
                @on-selection-change="changeSelect"
              >
                <template slot-scope="{ row }" slot="skuId">
                  <div>{{ row.skuId }}</div>
                </template>

                <template slot-scope="{ index }" slot="settlementPrice">
                  <Input
                    type="number"
                    v-model="promotionGoodsList[index].settlementPrice"
                  />
                </template>

                <template slot-scope="{ row, index }" slot="pointsGoodsCategory">
                  <Select
                    v-model="promotionGoodsList[index].pointsGoodsCategoryId"
                    :transfer="true"
                    :label-in-value="true"
                    @on-change="
                      (val) => {
                        changeCategory(val, index);
                      }
                    "
                  >
                    <Option
                      v-for="item in categoryList"
                      :value="item.id"
                      :key="item.id"
                      >{{ item.name }}</Option
                    >
                  </Select>
                </template>
                <template slot-scope="{ index }" slot="activeStock">
                  <Input type="number" v-model="promotionGoodsList[index].activeStock" />
                </template>
                <template slot-scope="{ index }" slot="points">
                  <Input type="number" v-model="promotionGoodsList[index].points" />
                </template>
              </Table>
            </FormItem>

            <FormItem label="兑换时间" prop="time">
              <DatePicker
                type="datetime"
                v-model="form.startTime"
                format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择"
                :options="options"
                clearable
                style="width: 200px"
              >
              </DatePicker>
              -
              <DatePicker
                type="datetime"
                v-model="form.endTime"
                format="yyyy-MM-dd HH:mm:ss"
                :options="options"
                placeholder="请选择"
                clearable
                style="width: 200px"
              >
              </DatePicker>
            </FormItem>
            <div>
              <Button type="text" @click="closeCurrentPage">返回</Button>
              <Button type="primary" :loading="submitLoading" @click="handleSubmit"
                >提交</Button
              >
            </div>
          </div>
        </div>
      </Form>
    </Card>
    <sku-select ref="skuSelect" @selectedGoodsData="selectedGoodsData"></sku-select>
  </div>
</template>

<script>
import { addPointsGoods, getPointsGoodsCategoryList } from "@/api/promotion";
import { regular } from "@/utils";
import skuSelect from "@/views/lili-dialog";
export default {
  name: "addPoinsGoods",
  components: {
    skuSelect,
  },
  data() {
    const isLtEndDate = (rule, value, callback) => {
      if (new Date(value).getTime() > new Date(this.form.endTime).getTime()) {
        callback(new Error());
      } else {
        callback();
      }
    };
    const isGtStartDate = (rule, value, callback) => {
      if (new Date(value).getTime() < new Date(this.form.startTime).getTime()) {
        callback(new Error());
      } else {
        callback();
      }
    };
    return {
      form: {
        promotionGoodsList: [], // 活动商品列表
      },
      showTable: true,
      promotionGoodsList: [], // 活动商品列表
      categoryList: [], // 分类列表
      submitLoading: false, // 添加或编辑提交状态
      selectedGoods: [], // 已选商品列表，便于删除
      formRule: {
        startTime: [
          {
            required: true,
            type: "date",
            message: "请选择开始时间",
          },
          {
            trigger: "change",
            message: "开始时间要小于结束时间",
            validator: isLtEndDate,
          },
        ],
        endTime: [
          {
            required: true,
            type: "date",
            message: "请选择结束时间",
          },
          {
            trigger: "change",
            message: "结束时间要大于开始时间",
            validator: isGtStartDate,
          },
        ],
        discount: [
          { required: true, message: "请输入折扣" },
          {
            pattern: regular.discount,
            message: "请输入0-10的数字,可有一位小数",
          },
        ],
        sellerCommission: [
          { required: true, message: "请输入店铺承担比例" },
          { pattern: regular.rate, message: "请输入0-100的正整数" },
        ],
        publishNum: [
          { required: true, message: "请输入发放数量" },
          { pattern: regular.Integer, message: "请输入正整数" },
        ],
        couponLimitNum: [
          { required: true, message: "请输入领取限制" },
          { pattern: regular.Integer, message: "请输入正整数" },
        ],
        description: [{ required: true, message: "请输入范围描述" }],
      },
      columns: [
        {
          type: "selection",
          width: 60,
          align: "center",
        },
        {
          title: "商品名称",
          key: "goodsName",
          minWidth: 120,
          render: (h, params) => {
            return h("div", params.row.goodsSku.goodsName);
          },
        },
        {
          title: "SKU编码",
          slot: "skuId",
          minWidth: 120,
        },
        {
          title: "店铺名称",
          key: "storeName",
          minWidth: 60,
          render: (h, params) => {
            return h("div", params.row.goodsSku.storeName);
          },
        },
        {
          title: "商品价格",
          key: "price",
          minWidth: 40,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.goodsSku.price, "￥")
            );
          },
        },
        {
          title: "库存",
          key: "quantity",
          minWidth: 20,
          render: (h, params) => {
            return h("div", params.row.goodsSku.quantity);
          },
        },
        {
          title: "结算价格",
          slot: "settlementPrice",
          minWidth: 40,
        },
        {
          title: "分类",
          slot: "pointsGoodsCategory",
          minWidth: 60,
        },
        {
          title: "活动库存",
          slot: "activeStock",
          minWidth: 40,
        },
        {
          title: "兑换积分",
          slot: "points",
          minWidth: 40,
        },
        {
          title: "操作",
          key: "action",
          minWidth: 50,
          align: "center",
          render: (h, params) => {
            return h(
              "Button",
              {
                props: {
                  size: "small",
                  type: "error",
                  ghost: true,
                },
                on: {
                  click: () => {
                    this.delGoods(params.index);
                  },
                },
              },
              "删除"
            );
          },
        },
      ],
      options: {
        disabledDate(date) {
          return date && date.valueOf() < Date.now() - 86400000;
        },
      },
    };
  },
  async mounted() {
    await this.getCategory();
  },
  methods: {
    // 获取商品分类
    async getCategory() {
      let res = await getPointsGoodsCategoryList();
      this.categoryList = res.result.records;
    },
    /** 保存积分商品 */
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          let params = this.promotionGoodsList;
          const start = this.$options.filters.unixToDate(this.form.startTime / 1000);
          const end = this.$options.filters.unixToDate(this.form.endTime / 1000);

          if (!params || params.length == 0) {
            this.$Modal.warning({ title: "提示", content: "请选择指定商品" });
            return;
          }

          this.submitLoading = true;
          params = params.map((j) => {
            j.startTime = start;
            j.endTime = end;
            return j;
          });
          addPointsGoods(params).then((res) => {
            this.submitLoading = false;
            if (res.success) {
              this.$Message.success("积分商品创建成功");
              this.closeCurrentPage();
            }
          });
        }
      });
    },
    // 关闭当前页面
    closeCurrentPage() {
      this.$store.commit("removeTag", "add-points-goods");
      localStorage.pageOpenedList = JSON.stringify(this.$store.state.app.pageOpenedList);
      this.$router.go(-1);
    },
    // 选择分类
    changeCategory(val, index) {
      this.promotionGoodsList[index].pointsGoodsCategoryName = val.label;
    },
    // 变更选中状态
    changeSelect(e) {
      // 已选商品批量选择
      this.selectedGoods = e;
    },
    delSelectGoods() {
      // 多选删除商品
      if (this.selectedGoods.length <= 0) {
        this.$Message.warning("您还未选择要删除的数据");
        return;
      }
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除所选商品吗?",
        onOk: () => {
          let ids = [];
          this.selectedGoods.forEach(function (e) {
            ids.push(e.skuId);
          });
          this.promotionGoodsList = this.promotionGoodsList.filter((item) => {
            return !ids.includes(item.skuId);
          });
        },
      });
    },
    delGoods(index) {
      // 删除商品
      this.promotionGoodsList.splice(index, 1);
    },
    openSkuList() {
      // 显示商品选择器
      this.$refs.skuSelect.open("goods");
      let data = JSON.parse(JSON.stringify(this.promotionGoodsList));
      data.forEach((e) => {
        e.id = e.skuId;
      });
      this.$refs.skuSelect.goodsData = data;
    },
    selectedGoodsData(item) {
      // 回显已选商品
      let list = [];
      item.forEach((e) => {
        const obj = {
          settlementPrice: e.settlementPrice || 0,
          pointsGoodsCategoryId: e.pointsGoodsCategoryId || 0,
          pointsGoodsCategoryName: e.pointsGoodsCategoryName || "",
          activeStock: e.activeStock || 0,
          points: e.points || 0,
          skuId: e.id,
          goodsId: e.goodsId,
          originalPrice: e.price || 0,
          thumbnail: e.thumbnail || "",
          goodsName: e.goodsName || "",
          goodsSku: e,
        };
        list.push(obj);
      });
      this.promotionGoodsList = list;
    },
  },
};
</script>

<style lang="scss" scpoed>
h4 {
  margin-bottom: 10px;
  padding: 0 10px;
  border: 1px solid #ddd;
  background-color: #f8f8f8;
  font-weight: bold;
  color: #333;
  font-size: 14px;
  line-height: 40px;
  text-align: left;
}
.describe {
  font-size: 12px;
  margin-left: 10px;
  color: #999;
}
.wrapper {
  min-height: 800px;
}
</style>
