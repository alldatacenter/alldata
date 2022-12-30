<template>
  <div>
    <Card>
      <Form ref="form" :model="form" :label-width="120">
        <div class="base-info-item">
          <h4>基本信息</h4>
          <div class="form-item-view">
            <FormItem label="活动名称" prop="promotionName">
              <Input
                type="text"
                v-model="form.promotionName"
                disabled
                placeholder="活动名称"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="活动时间" prop="rangeTime">
              <DatePicker
                type="datetimerange"
                v-model="form.rangeTime"
                disabled
                format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择"
                :options="options"
                style="width: 320px"
              >
              </DatePicker>
            </FormItem>
            <FormItem label="活动描述" prop="description">
              <Input
                v-model="form.description"
                disabled
                type="textarea"
                :rows="4"
                clearable
                style="width: 260px"
              />
            </FormItem>
          </div>

          <h4>优惠设置</h4>
          <div class="form-item-view">
            <FormItem label="优惠门槛" prop="fullMoney">
              <Input
                type="text"
                v-model="form.fullMoney"
                disabled
                placeholder="优惠门槛"
                clearable
                style="width: 260px"
              />
              <span class="describe">消费达到当前金额可以参与优惠</span>
            </FormItem>
            <FormItem label="赠送优惠券">
              <RadioGroup
                type="button"
                button-style="solid"
                v-model="form.discountType"
              >
                <Radio label="fullMinusFlag" disabled>减现金</Radio>
                <Radio label="fullRateFlag" disabled>打折</Radio>
              </RadioGroup>
            </FormItem>
            <FormItem
              v-if="form.discountType == 'fullMinusFlag'"
              label="优惠金额"
              prop="fullMinus"
            >
              <Input
                type="text"
                disabled
                v-model="form.fullMinus"
                placeholder="优惠金额"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem
              v-if="form.discountType == 'fullRateFlag'"
              label="优惠折扣"
              prop="fullRate"
            >
              <Input
                type="text"
                v-model="form.fullRate"
                placeholder="优惠折扣"
                disabled
                clearable
                style="width: 260px"
              />
              <span class="describe">优惠折扣为0-10之间数字，可有一位小数</span>
            </FormItem>
            <FormItem label="额外赠送">
              <Checkbox v-model="form.freeFreightFlag" disabled>免邮费</Checkbox
              >&nbsp;
              <Checkbox v-model="form.couponFlag" disabled>送优惠券</Checkbox
              >&nbsp;
              <Checkbox v-model="form.giftFlag" disabled>送赠品</Checkbox>&nbsp;
              <Checkbox v-model="form.pointFlag" disabled>送积分</Checkbox>
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
                v-model="form.giftId"
                filterable
                :remote-method="getGiftList"
                placeholder="输入赠品名称搜索"
                disabled
                :loading="giftLoading"
                style="width: 260px"
              >
                <Option
                  v-for="item in giftList"
                  :value="item.id"
                  :key="item.id"
                  >{{ item.goodsName }}</Option
                >
              </Select>
            </FormItem>
            <FormItem v-if="form.pointFlag" label="赠积分" prop="point">
              <Input
                v-model="form.point"
                type="number"
                disabled
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="使用范围" prop="scopeType">
              <RadioGroup
                type="button"
                button-style="solid"
                v-model="form.scopeType"
              >
                <Radio label="ALL" disabled>全品类</Radio>
                <Radio label="PORTION_GOODS" disabled>指定商品</Radio>
              </RadioGroup>
            </FormItem>

            <FormItem
              style="width: 100%"
              v-if="form.scopeType == 'PORTION_GOODS'"
            >
              <Table border :columns="columns" :data="form.promotionGoodsList">
                <template slot-scope="{ row }" slot="goodsName">
                  <div>
                    <a class="mr_10" @click="linkTo(row.goodsId, row.skuId)">{{
                      row.goodsName
                    }}</a>
                    <Poptip trigger="hover" title="扫码在手机中查看" transfer>
                      <div slot="content">
                        <vue-qr
                          :text="wapLinkTo(row.goodsId, row.skuId)"
                          :margin="0"
                          colorDark="#000"
                          colorLight="#fff"
                          :size="150"
                        ></vue-qr>
                      </div>
                      <img
                        src="../../../assets/qrcode.svg"
                        style="vertical-align: middle"
                        class="hover-pointer"
                        width="20"
                        height="20"
                        alt=""
                      />
                    </Poptip>
                  </div>
                </template>
              </Table>
            </FormItem>

            <div>
              <Button
                @click="$router.push({ name: 'promotions/full-discount' })"
                >返回</Button
              >
            </div>
          </div>
        </div>
      </Form>
    </Card>
  </div>
</template>

<script>
import { getPlatformCouponList, getFullDiscountById } from "@/api/promotion";
import { getGoodsSkuData } from "@/api/goods";
import vueQr from "vue-qr";
export default {
  name: "add-full-discount",
  components: {
    "vue-qr": vueQr,
  },
  data() {
    return {
      form: {
        // 表单
        discountType: "fullMinusFlag",
        scopeType: "ALL",
        promotionGoodsList: [],
      },
      id: this.$route.query.id, // 活动id
      couponList: [], // 优惠券列表
      giftList: [], // 赠品列表
      giftLoading: false, // 赠品加载状态
      columns: [
        {
          type: "selection",
          width: 60,
          align: "center",
        },
        {
          title: "商品名称",
          slot: "goodsName",
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
    this.getCouponList();
    this.getGiftList();
  },
  methods: {
    getDetail() {
      // 获取活动详情
      getFullDiscountById(this.id).then((res) => {
        let data = res.result;
        if (!data.scopeType === "ALL") {
          data.promotionGoodsList = [];
        }
        if (data.fullMinusFlag) {
          data.discountType = "fullMinusFlag";
          delete data.fullMinusFlag;
        } else {
          data.discountType = "fullMinusFlag";
          delete data.fullRateFlag;
        }
        data.rangeTime = [];
        data.rangeTime.push(new Date(data.startTime), new Date(data.endTime));

        this.form = data;
      });
    },
    getCouponList(query) {
      // 优惠券列表
      let params = {
        pageSize: 10,
        pageNumber: 0,
        getType: "ACTIVITY",
        storeId: "",
        couponName: query,
        promotionStatus: "START",
      };
      this.couponLoading = true;
      getPlatformCouponList(params).then((res) => {
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
      getGoodsSkuData(params).then((res) => {
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
</style>
