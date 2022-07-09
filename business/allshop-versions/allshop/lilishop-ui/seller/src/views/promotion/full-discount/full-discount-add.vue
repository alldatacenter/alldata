<template>
  <div>
    <Card>
      <Form ref="form" :model="form" :label-width="120" :rules="formRule">
        <div class="base-info-item">
          <h4>基本信息</h4>
          <div class="form-item-view">
            <FormItem label="活动名称" prop="promotionName">
              <Input
                type="text"
                v-model="form.promotionName"
                :disabled="form.promotionStatus != 'NEW'"
                placeholder="活动名称"
                clearable
                style="width: 280px"
              />
            </FormItem>
            <FormItem label="活动时间" prop="rangeTime">
              <DatePicker
                type="datetimerange"
                v-model="form.rangeTime"
                :disabled="form.promotionStatus != 'NEW'"
                format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择"
                :options="options"
                style="width: 280px"
              >
              </DatePicker>
            </FormItem>
            <FormItem label="活动描述" prop="description">
              <Input
                v-model="form.description"
                :disabled="form.promotionStatus != 'NEW'"
                type="textarea"
                :rows="4"
                clearable
                style="width: 280px"
              />
            </FormItem>
          </div>

          <h4>优惠设置</h4>
          <div class="form-item-view">
            <FormItem label="优惠门槛" prop="fullMoney">
              <Input
                type="text"
                v-model="form.fullMoney"
                :disabled="form.promotionStatus != 'NEW'"
                placeholder="优惠门槛"
                clearable
                style="width: 280px"
              />
              <span class="describe">消费达到当前金额可以参与优惠</span>
            </FormItem>
            <FormItem label="优惠方式">
              <RadioGroup
                type="button"
                button-style="solid"
                v-model="form.discountType"
              >
                <Radio
                  :disabled="form.promotionStatus != 'NEW'"
                  label="fullMinusFlag"
                  >减现金</Radio
                >
                <Radio
                  :disabled="form.promotionStatus != 'NEW'"
                  label="fullRateFlag"
                  >打折</Radio
                >
              </RadioGroup>
            </FormItem>
            <FormItem
              v-if="form.discountType == 'fullMinusFlag'"
              label="优惠金额"
              prop="fullMinus"
            >
              <Input
                :disabled="form.promotionStatus != 'NEW'"
                type="text"
                v-model="form.fullMinus"
                placeholder="优惠金额"
                clearable
                style="width: 280px"
              />
            </FormItem>
            <FormItem
              v-if="form.discountType == 'fullRateFlag'"
              label="优惠折扣"
              prop="fullRate"
            >
              <Input
                :disabled="form.promotionStatus != 'NEW'"
                type="text"
                v-model="form.fullRate"
                placeholder="优惠折扣"
                clearable
                style="width: 280px"
              />
              <span class="describe">优惠折扣为0-10之间数字，可有一位小数</span>
            </FormItem>
            <FormItem label="额外赠送">
              <Checkbox
                :disabled="form.promotionStatus != 'NEW'"
                v-model="form.freeFreightFlag"
                >免邮费</Checkbox
              >
              <Checkbox
                :disabled="form.promotionStatus != 'NEW'"
                v-model="form.couponFlag"
                >送优惠券</Checkbox
              >
              <Checkbox
                :disabled="form.promotionStatus != 'NEW'"
                v-model="form.giftFlag"
                >送赠品</Checkbox
              >
              <Checkbox
                :disabled="form.promotionStatus != 'NEW'"
                v-if="
                  Cookies.get('userInfoSeller') &&
                  JSON.parse(Cookies.get('userInfoSeller')).selfOperated
                "
                v-model="form.pointFlag"
                >送积分</Checkbox
              >
            </FormItem>
            <FormItem v-if="form.couponFlag" label="赠送优惠券" prop="couponId">
              <Select
                v-model="form.couponId"
                :disabled="form.promotionStatus != 'NEW'"
                filterable
                :remote-method="getCouponList"
                placeholder="输入优惠券名称搜索"
                :loading="couponLoading"
                style="width: 280px"
              >
                <Option
                  v-for="item in couponList"
                  :value="item.id"
                  :key="item.id"
                  >{{ item.couponName }}</Option
                >
              </Select>
            </FormItem>
            <FormItem v-if="form.giftFlag" label="赠品" prop="giftId">
              <Select
                :disabled="form.promotionStatus != 'NEW'"
                v-model="form.giftId"
                filterable
                :remote-method="getGiftList"
                placeholder="输入赠品名称搜索"
                :loading="giftLoading"
                style="width: 280px"
              >
                <Option
                  v-for="item in giftList"
                  :value="item.id"
                  :key="item.id"
                  >
                  {{ item.goodsName }}
                  </Option
                >
              </Select>
            </FormItem>
            <FormItem v-if="form.pointFlag" label="赠积分" prop="point">
              <InputNumber
                :min="0"
                :disabled="form.promotionStatus != 'NEW'"
                v-model="form.point"
                type="number"
                style="width: 280px"
              />
            </FormItem>
            <FormItem label="使用范围" prop="scopeType">
              <RadioGroup
                type="button"
                button-style="solid"
                v-model="form.scopeType"
              >
                <Radio :disabled="form.promotionStatus != 'NEW'" label="ALL"
                  >全品类</Radio
                >
                <Radio
                  :disabled="form.promotionStatus != 'NEW'"
                  label="PORTION_GOODS"
                  >指定商品</Radio
                >
              </RadioGroup>
            </FormItem>

            <FormItem
              style="width: 100%"
              v-if="form.scopeType == 'PORTION_GOODS'"
            >
              <div
                style="display: flex; margin-bottom: 10px"
                v-if="form.promotionStatus == 'NEW'"
              >
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
                :columns="columns"
                :data="form.promotionGoodsList"
                @on-selection-change="changeSelect"
              >
                <template slot-scope="{ row }" slot="QRCode">
                  <img
                    :src="row.QRCode || '../../../assets/lili.png'"
                    width="50px"
                    height="50px"
                    alt=""
                  />
                </template>
                <template slot-scope="{ index }" slot="action">
                  <Button
                    type="error"
                    :disabled="form.promotionStatus != 'NEW' && !!id"
                    size="small"
                    ghost
                    @click="delGoods(index)"
                    >删除</Button
                  >
                </template>
              </Table>
            </FormItem>

            <div>
              <Button type="text" @click="closeCurrentPage">返回</Button>
              <Button
                type="primary"
                :disabled="form.promotionStatus != 'NEW' && !!id"
                :loading="submitLoading"
                @click="handleSubmit"
                >提交</Button
              >
            </div>
          </div>
        </div>
      </Form>
    </Card>
    <sku-select
      ref="skuSelect"
      @selectedGoodsData="selectedGoodsData"
    ></sku-select>
  </div>
</template>

<script>
import {
  getShopCouponList,
  getFullDiscountById,
  newFullDiscount,
  editFullDiscount,
} from "@/api/promotion";
import { getGoodsSkuListDataSeller } from "@/api/goods";
import { regular } from "@/utils";
import skuSelect from "@/views/lili-dialog";
import Cookies from "js-cookie";
export default {
  name: "full-discount-add",
  components: {
    skuSelect,
  },
  data() {
    const checkPrice = (rule, value, callback) => {
      if (!value && value !== 0) {
        return callback(new Error("面额不能为空"));
      } else if (!regular.money.test(value)) {
        callback(new Error("请输入正整数或者两位小数"));
      } else if (parseFloat(value) > 99999999) {
        callback(new Error("面额设置超过上限值"));
      } else {
        callback();
      }
    };
    const checkWeight = (rule, value, callback) => {
      if (!value && typeof value !== "number") {
        callback(new Error("优惠门槛不能为空"));
      } else if (!regular.money.test(value)) {
        callback(new Error("请输入正整数或者两位小数"));
      } else if (parseFloat(value) > 99999999) {
        callback(new Error("优惠门槛设置超过上限值"));
      } else {
        callback();
      }
    };
    return {
      Cookies,
      form: {
        // 活动表单
        discountType: "fullMinusFlag",
        scopeType: "ALL",
        promotionGoodsList: [],
        promotionStatus: "NEW",
      },
      id: this.$route.query.id, // 活动id
      submitLoading: false, // 添加或编辑提交状态
      selectedGoods: [], // 已选商品列表，便于删除
      formRule: {
        // 验证规则
        promotionName: [{ required: true, message: "活动名称不能为空" }],
        rangeTime: [{ required: true, message: "请选择活动时间" }],
        description: [{ required: true, message: "请填写活动描述" }],
        price: [
          { required: true, message: "请输入面额" },
          { validator: checkPrice },
        ],
        consumptionLimit: [{ required: true, validator: checkWeight }],
        fullMoney: [{ required: true, validator: checkWeight }],
        fullMinus: [
          { required: true, message: "请填写优惠金额" },
          { pattern: regular.money, message: "请输入正确金额" },
        ],
        fullRate: [
          { required: true, message: "请填写优惠折扣" },
          {
            pattern: regular.discount,
            message: "请输入0-10的数字,可有一位小数",
          },
        ],
        couponId: [{ required: true, message: "请选择优惠券" }],
        giftId: [{ required: true, message: "请选择赠品" }],
        point: [{ required: true, message: "请填写积分" }],
      },
      couponList: [], // 店铺优惠券列表
      giftList: [], // 赠品列表
      giftLoading: false, // 请求赠品状态
      columns: [
        // 表头
        {
          type: "selection",
          width: 60,
          align: "center",
        },
        {
          title: "商品名称",
          key: "goodsName",
          minWidth: 120,
        },
        {
          title: "商品价格",
          key: "price",
          minWidth: 40,
          render: (h, params) => {
            return h(
              "div",
              this.$options.filters.unitPrice(params.row.price, "￥")
            );
          },
        },
        {
          title: "库存",
          key: "quantity",
          minWidth: 40,
        },
        {
          title: "操作",
          slot: "action",
          minWidth: 50,
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
    if (this.id) {
      this.getDetail();
    }
    await this.getCouponList();
    await this.getGiftList();
  },
  methods: {
    // 关闭当前页面
    closeCurrentPage() {
      this.$store.commit("removeTag", "full-cut-detail");
      localStorage.storeOpenedList = JSON.stringify(
        this.$store.state.app.storeOpenedList
      );
      this.$router.go(-1);
    },
    openSkuList() {
      // 显示商品选择器
      this.$refs.skuSelect.open("goods");
      let data = JSON.parse(JSON.stringify(this.form.promotionGoodsList));
      data.forEach((e) => {
        e.id = e.skuId;
      });
      this.$refs.skuSelect.goodsData = data;
    },
    getDetail() {
      // 获取活动详情
      getFullDiscountById(this.id).then((res) => {
        console.log(res);
        let data = res.result;
        if (data.scopeType === "ALL") {
          data.promotionGoodsList = [];
        }
        if (data.fullMinusFlag) {
          data.discountType = "fullMinusFlag";
          delete data.fullMinusFlag;
        } else {
          data.discountType = "fullRateFlag";
          delete data.fullRateFlag;
        }
        data.rangeTime = [];
        data.rangeTime.push(new Date(data.startTime), new Date(data.endTime));

        this.form = data;
      });
    },
    /** 保存 */
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          const params = JSON.parse(JSON.stringify(this.form));
          const strat = this.$options.filters.unixToDate(
            this.form.rangeTime[0] / 1000
          );
          const end = this.$options.filters.unixToDate(
            this.form.rangeTime[1] / 1000
          );
          params.startTime = strat;
          params.endTime = end;

          if (
            params.scopeType == "PORTION_GOODS" &&
            (!params.promotionGoodsList ||
              params.promotionGoodsList.length == 0)
          ) {
            this.$Modal.warning({ title: "提示", content: "请选择指定商品" });
            return;
          }
          if (params.scopeType == "ALL") {
            delete params.promotionGoodsList;
            params.number = -1;
          } else {
            params.number = 1;
            params.promotionGoodsList.forEach((e) => {
              e.startTime = params.stratTime;
              e.endTime = params.endTime;
            });
          }
          if (params.discountType == "fullMinusFlag") {
            params.fullMinusFlag = true;
          } else {
            params.fullRateFlag = true;
          }
          delete params.rangeTime;
          this.submitLoading = true;
          if (!this.id) {
            // 添加 避免编辑后传入id等数据 记得删除
            delete params.id;
            newFullDiscount(params).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("添加活动成功");
                this.closeCurrentPage();
              }
            });
          } else {
            // 编辑
            delete params.updateTime;

            editFullDiscount(params).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("编辑活动成功");
                this.closeCurrentPage();
              }
            });
          }
        }
      });
    },

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
            ids.push(e.id);
          });
          this.form.promotionGoodsList = this.form.promotionGoodsList.filter(
            (item) => {
              return !ids.includes(item.id);
            }
          );
        },
      });
    },
    delGoods(index) {
      // 删除商品
      this.form.promotionGoodsList.splice(index, 1);
    },
    selectedGoodsData(item) {
      // 回显已选商品
      let list = [];
      item.forEach((e) => {
        list.push({
          goodsName: e.goodsName,
          price: e.price,
          quantity: e.quantity,
          storeId: e.storeId,
          storeName: e.storeName,
          thumbnail: e.thumbnail,
          skuId: e.id,
        });
      });
      this.form.promotionGoodsList = list;
    },
    getCouponList(query) {
      // 优惠券列表
      let params = {
        pageSize: 10,
        pageNumber: 0,
        getType: "ACTIVITY",
        couponName: query,
        promotionStatus: "START",
      };
      this.couponLoading = true;
      getShopCouponList(params).then((res) => {
        this.couponLoading = false;
        if (res.success) {
          this.couponList = res.result.records;
        }
      });
    },
    getGiftList(query) {
      // 赠品列表
      let params = {
        pageSize: 10,
        pageNumber: 1,
        id: query === this.form.giftId ? this.form.giftId : null,
        goodsName: query === this.form.giftId ? null : query,
        marketEnable: "UPPER",
        authFlag: "PASS"
      };
      this.giftLoading = true;
      getGoodsSkuListDataSeller(params).then((res) => {
        this.giftLoading = false;
        if (res.success) {
          this.giftList = res.result.records;
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
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
.ivu-form-item {
  margin-bottom: 24px !important;
}
</style>
