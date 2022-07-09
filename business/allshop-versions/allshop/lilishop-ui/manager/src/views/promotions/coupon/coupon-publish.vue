<template>
  <div>
    <Card>
      <Form ref="form" :model="form" :label-width="120" :rules="formRule">
        <div class="base-info-item">
          <h4>基本信息</h4>
          <div class="form-item-view">
            <FormItem label="活动名称" prop="promotionName">
              <Input
                :disabled="disabled"
                type="text"
                v-model="form.promotionName"
                placeholder="活动名称"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="优惠券名称" prop="couponName">
              <Input
                :disabled="disabled"
                type="text"
                v-model="form.couponName"
                placeholder="优惠券名称"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="优惠券类型" prop="couponType">
              <Select :disabled="disabled" v-model="form.couponType" style="width: 260px">
                <Option value="DISCOUNT">打折</Option>
                <Option value="PRICE">减免现金</Option>
              </Select>
            </FormItem>
            <FormItem
              label="折扣"
              prop="couponDiscount"
              v-if="form.couponType == 'DISCOUNT'"
            >
              <Input
                :disabled="disabled"
                type="number"
                v-model="form.couponDiscount"
                placeholder="折扣"
                clearable
                style="width: 260px"
              />
              <span class="describe">请输入0-10之间数字，可以输入一位小数</span>
            </FormItem>
            <FormItem label="面额" prop="price" v-if="form.couponType == 'PRICE'">
              <Input
                :disabled="disabled"
                type="text"
                v-model="form.price"
                placeholder="面额"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="活动类型" prop="getType">
              <Select :disabled="disabled" v-model="form.getType" style="width: 260px">
                <Option value="FREE">免费领取</Option>
                <Option value="ACTIVITY">活动赠送</Option>
              </Select>
            </FormItem>

            <FormItem label="店铺承担比例" prop="storeCommission">
              <Input
                :disabled="disabled"
                v-model="form.storeCommission"
                placeholder="店铺承担比例"
                style="width: 260px"
              >
                <span slot="append">%</span>
              </Input>
              <span class="describe">店铺承担比例，输入0-100之间数值</span>
            </FormItem>
            <FormItem label="发放数量" prop="publishNum" v-if="form.getType === 'FREE'">
              <Input
                :disabled="disabled"
                v-model="form.publishNum"
                placeholder="发放数量"
                style="width: 260px"
              />
              <div class="tips">如果发放数量为0时,则代表不限制发放数量</div>
            </FormItem>
            <FormItem
              label="领取数量限制"
              prop="couponLimitNum"
              v-if="form.getType === 'FREE'"
            >
              <Input
                :disabled="disabled"
                v-model="form.couponLimitNum"
                placeholder="领取限制"
                clearable
                style="width: 260px"
              />
              <div class="tips">如果领取数量为0时,则代表不限制领取数量</div>
            </FormItem>
            <FormItem label="范围描述" prop="description">
              <Input
                :disabled="disabled"
                v-model="form.description"
                type="textarea"
                :rows="4"
                maxlength="50"
                show-word-limit
                clearable
                style="width: 260px"
              />
            </FormItem>
          </div>
          <h4>使用限制</h4>
          <div class="form-item-view">
            <FormItem label="消费门槛" prop="consumeThreshold">
              <Input
                :disabled="disabled"
                type="text"
                v-model="form.consumeThreshold"
                placeholder="消费门槛"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="有效期" prop="rangeTime">
              <div v-if="form.getType == 'ACTIVITY'">
                <RadioGroup v-model="rangeTimeType">
                  <Radio :disabled="disabled" :label="1">起止时间</Radio>
                  <Radio :disabled="disabled" :label="0">固定时间</Radio>
                </RadioGroup>
              </div>
              <div v-if="rangeTimeType == 1">
                <DatePicker
                  :disabled="disabled"
                  type="datetimerange"
                  v-model="form.rangeTime"
                  format="yyyy-MM-dd HH:mm:ss"
                  placeholder="请选择"
                  :options="options"
                  style="width: 260px"
                >
                </DatePicker>
              </div>
              <div class="effectiveDays" v-if="rangeTimeType == 0">
                领取当天开始
                <InputNumber
                  v-model="form.effectiveDays"
                  :min="1"
                  style="width: 100px"
                  :max="365"
                />
                天内有效(1-365间的整数)
              </div>
            </FormItem>

            <FormItem label="使用范围" prop="scopeType">
              <RadioGroup type="button" button-style="solid" v-model="form.scopeType">
                <Radio :disabled="disabled" label="ALL">全品类</Radio>
                <Radio :disabled="disabled" label="PORTION_GOODS">指定商品</Radio>
                <Radio :disabled="disabled" label="PORTION_GOODS_CATEGORY"
                  >部分商品分类</Radio
                >
              </RadioGroup>
            </FormItem>

            <FormItem style="width: 100%" v-if="form.scopeType == 'PORTION_GOODS'">
              <div style="display: flex; margin-bottom: 10px">
                <Button :disabled="disabled" type="primary" @click="openSkuList"
                  >选择商品</Button
                >
                <Button
                  :disabled="disabled"
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
              </Table>
            </FormItem>

            <FormItem v-if="form.scopeType == 'PORTION_GOODS_CATEGORY'">
              <Cascader
                :disabled="disabled"
                :data="goodsCategoryList"
                style="width: 260px"
                v-model="form.scopeIdGoods"
              ></Cascader>
            </FormItem>
            <div>
              <Button :disabled="disabled" type="text" @click="closeCurrentPage"
                >返回</Button
              >
              <Button
                :disabled="disabled"
                type="primary"
                :loading="submitLoading"
                @click="handleSubmit"
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
import {
  savePlatformCoupon,
  getPlatformCoupon,
  editPlatformCoupon,
} from "@/api/promotion";
import { getCategoryTree } from "@/api/goods";
import { regular } from "@/utils";
import skuSelect from "@/views/lili-dialog";

export default {
  name: "edit-platform-coupon",
  components: {
    skuSelect,
  },
  watch: {
    "form.getType": {
      handler(val) {
        if (val == "FREE") {
          this.rangeTimeType = 1;
        }
      },
      deep: true,
    },
    $route(e) {
      // 监听路由，参数变化调取接口
      this.id = e.query.id;
      if (this.id) {
        this.getCoupon();
      } else {
        this.$refs.form.resetFields();
      }
    },
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
        callback(new Error("消费门槛不能为空"));
      } else if (!regular.money.test(value)) {
        callback(new Error("请输入正整数或者两位小数"));
      } else if (parseFloat(value) > 99999999) {
        callback(new Error("消费门槛设置超过上限值"));
      } else {
        callback();
      }
    };
    return {
      disabled: this.$route.query.onlyView,
      rangeTimeType: 1, // 当前时间类型
      modalType: 0, // 是否编辑
      form: {
        /** 店铺承担比例 */
        storeCommission: 0,
        /** 发行数量 */
        publishNum: 0,
        /** 运费承担者 */
        scopeType: "ALL",
        /** 限领数量 */
        couponLimitNum: 1,
        /** 活动类型 */
        couponType: "PRICE",
        /** 优惠券名称 */
        couponName: "",
        promotionName: "",
        getType: "FREE",
        promotionGoodsList: [],
        scopeIdGoods: [],
        rangeDayType: "",
      },
      id: this.$route.query.id, // 优惠券id
      submitLoading: false, // 添加或编辑提交状态
      selectedGoods: [], // 已选商品列表，便于删除
      goodsCategoryList: [], // 商品分类列表
      formRule: {
        promotionName: [{ required: true, message: "活动名称不能为空" }],
        couponName: [{ required: true, message: "优惠券名称不能为空" }],
        price: [{ required: true, message: "请输入面额" }, { validator: checkPrice }],
        rangeTime: [{ required: true, message: "请选择优惠券有效期" }],
        consumeThreshold: [
          { required: true, message: "请输入消费门槛" },
          { validator: checkWeight },
        ],
        couponDiscount: [
          { required: true, message: "请输入折扣" },
          {
            pattern: regular.discount,
            message: "请输入0-10的数字,可有一位小数",
          },
        ],
        storeCommission: [
          { required: true, message: "请输入店铺承担比例" },
          { pattern: regular.rate, message: "请输入0-100的正整数" },
        ],
        publishNum: [
          { required: true, message: "请输入发放数量" },
          { pattern: regular.Integer, message: "请输入正整数" },
        ],
        couponLimitNum: [
          { required: true, message: "领取限制不能为空" },
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
        },
        {
          title: "商品价格",
          key: "price",
          minWidth: 40,
          render: (h, params) => {
            return h("div", this.$options.filters.unitPrice(params.row.price, "￥"));
          },
        },
        {
          title: "库存",
          key: "quantity",
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
    await this.getCagetoryList();
    // 如果id不为空则查询信息
    if (this.id) {
      this.getCoupon();
      this.modalType = 1;
    }
  },
  methods: {
    // 获取优惠券数据
    getCoupon() {
      getPlatformCoupon(this.id).then((res) => {
        let data = res.result;
        if (!data.promotionGoodsList) data.promotionGoodsList = [];
        this.rangeTimeType = data.rangeDayType === "DYNAMICTIME" ? 0 : 1;
        if (data.scopeType == "PORTION_GOODS_CATEGORY") {
          let prevCascader = data.scopeId.split(",");
          function next(params, prev) {
            for (let i = 0; i < params.length; i++) {
              const item = params[i];
              if (item.children) {
                next(item.children, [...prev, item]);
              } else {
                if (prevCascader.includes(item.id)) {
                  prevCascader = prevCascader.map((key) => {
                    if (key === item.id) {
                      let result = prev.map((item) => item.id);

                      return [...result, item.id];
                    } else {
                      return key;
                    }
                  });
                } else {
                  i === params.length - 1 && (prev = []);
                }
              }
            }
          }

          next(this.goodsCategoryList, []);
          data.scopeIdGoods = prevCascader;
        }
        data.rangeTime = [];
        if (data.startTime && data.endTime) {
          data.rangeTime.push(new Date(data.startTime), new Date(data.endTime));
        }
        this.form = data;
      });
    },
    /** 保存平台优惠券 */
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          const params = JSON.parse(JSON.stringify(this.form));
          // 判断当前活动类型
          params.getType != "ACTIVITY" ? delete params.effectiveDays : "";

          //判断当前时间类型
          if (this.rangeTimeType == 1) {
            params.rangeDayType = "FIXEDTIME";
            params.startTime = this.$options.filters.unixToDate(
              this.form.rangeTime[0] / 1000
            );
            params.endTime = this.$options.filters.unixToDate(
              this.form.rangeTime[1] / 1000
            );
            delete params.effectiveDays;
          } else {
            params.rangeDayType = "DYNAMICTIME";
            delete params.rangeTime;
          }

          let scopeId = [];

          if (
            params.scopeType == "PORTION_GOODS" &&
            (!params.promotionGoodsList || params.promotionGoodsList.length == 0)
          ) {
            this.$Modal.warning({ title: "提示", content: "请选择指定商品" });
            return;
          }

          if (
            params.scopeType == "PORTION_GOODS_CATEGORY" &&
            (!params.scopeIdGoods || params.scopeIdGoods.length == 0)
          ) {
            this.$Modal.warning({ title: "提示", content: "请选择商品分类" });
            return;
          }

          if (params.scopeType == "PORTION_GOODS") {
            //指定商品
            params.promotionGoodsList.forEach((item) => {
              scopeId.push(item.skuId);
            });
            params.scopeId = scopeId.toString();
          } else if (params.scopeType == "ALL") {
            delete params.promotionGoodsList;
          } else if (params.scopeType == "PORTION_GOODS_CATEGORY") {
            //部分商品分类
            scopeId = this.filterCategoryId(params.scopeIdGoods, []);
            params.scopeId = scopeId.toString();
            delete params.promotionGoodsList;
          }
          delete params.scopeIdGoods;

          this.submitLoading = true;
          if (this.modalType === 0) {
            // 添加 避免编辑后传入id等数据 记得删除
            delete params.id;

            savePlatformCoupon(params).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("优惠券发送成功");
                this.closeCurrentPage();
              }
            });
          } else {
            // 编辑
            delete params.consumeLimit;
            delete params.updateTime;

            editPlatformCoupon(params).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("优惠券修改成功");
                this.closeCurrentPage();
              }
            });
          }
        }
      });
    },
    // 关闭当前页面
    closeCurrentPage() {
      this.$store.commit("removeTag", "add-platform-coupon");
      localStorage.pageOpenedList = JSON.stringify(this.$store.state.app.pageOpenedList);
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
          this.form.promotionGoodsList = this.form.promotionGoodsList.filter((item) => {
            return !ids.includes(item.id);
          });
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
          originalPrice: e.price,
          quantity: e.quantity,
          storeId: e.storeId,
          storeName: e.storeName,
          skuId: e.id,
          categoryPath: e.categoryPath,
          thumbnail: e.small,
          goodsType: e.goodsType,
          goodsId: e.goodsId,
          originPrice: e.price,
        });
      });
      this.form.promotionGoodsList = list;
    },
    async getCagetoryList() {
      // 获取全部商品分类
      let data = await getCategoryTree();
      this.goodsCategoryList = data.result;
      // 过滤出可显示的值
      this.goodsCategoryList = this.goodsCategoryList.map((item) => {
        if (item.children) {
          item.children = item.children.map((child) => {
            if (child.children) {
              child.children = child.children.map((son) => {
                return {
                  value: son.id,
                  label: son.name,
                };
              });
              return {
                value: child.id,
                label: child.name,
                children: child.children,
              };
            } else {
              return {
                value: child.id,
                label: child.name,
              };
            }
          });
        }
        return { value: item.id, label: item.name, children: item.children };
      });
    },
    filterCategoryId(list, idArr) {
      // 递归获取分类id
      list.forEach((e) => {
        if (e instanceof Array) {
          this.filterCategoryId(e, idArr);
        } else {
          if (!idArr.includes(e)) idArr.push(e);
        }
      });
      return idArr;
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

.effectiveDays {
  font-size: 12px;
  color: #999;

  > * {
    margin: 0 4px;
  }
}
.tips {
  font-size: 12px;
  color: #999;
}
</style>
