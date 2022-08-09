<template>
  <div>
    <Card>
      <Form ref="form" :model="form" :label-width="120" :rules="formRule">
        <div class="base-info-item">
          <div class="form-item-view">
            <h4>商品信息</h4>
            <FormItem label="商品名称">
              <div>{{ form.goodsName }}</div>
            </FormItem>
            <FormItem label="SKU编码">
              <div>{{ form.skuId }}</div>
            </FormItem>
            <FormItem label="店铺名称">
              <div>{{ form.goodsSku.storeName }}</div>
            </FormItem>
            <FormItem label="商品价格">
              <div>{{ form.goodsSku.price | unitPrice('￥') }}</div>
            </FormItem>
            <FormItem label="商品库存">
              <div>{{ form.goodsSku.quantity }}</div>
            </FormItem>
            <FormItem label="结算价格" prop="settlementPrice">
              <Input
                :disabled="onlyView"
                type="number"
                v-model="form.settlementPrice"
                placeholder="请填写结算价格"
                clearable
                style="width: 260px"
              />
            </FormItem>
           
            <FormItem label="最低可砍" prop="lowestPrice">
              <Input
                :disabled="onlyView"
                type="number"
                v-model="form.lowestPrice"
                placeholder="请填写最低可砍金额"
                clearable
                style="width: 260px"
              />
            </FormItem>
            <FormItem label="最高可砍" prop="highestPrice">
              <Input
                :disabled="onlyView"
                type="number"
                v-model="form.highestPrice"
                placeholder="请填写最高可砍金额"
                clearable
                style="width: 260px"
              />
            </FormItem>

            <FormItem label="活动库存" prop="stock">
              <Input
                :disabled="onlyView"
                type="number"
                v-model="form.stock"
                placeholder="请填写活动库存"
                clearable
                style="width: 260px"
              />
            </FormItem>

            <FormItem label="活动时间" prop="rangeTime">
              <DatePicker
                :disabled="onlyView"
                type="datetimerange"
                v-model="form.rangeTime"
                format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择"
                :options="options"
                style="width: 300px"
              >
              </DatePicker>
            </FormItem>
            <div>
              <Button type="text" @click="closeCurrentPage">返回</Button>
              <Button type="primary" v-if="!onlyView" :loading="submitLoading" @click="handleSubmit">提交</Button>
            </div>
          </div>
        </div>
      </Form>
    </Card>
    
  </div>
</template>

<script>
  import {
    getKanJiaActivityGoodsById,
    editKanJiaActivityGoods,
  } from "@/api/promotion";
  import {regular} from "@/utils";


  export default {
    name: "editKanjiaActivityGoods",
   
    watch: {},
    data() {
      const checkSettlementPrice = (rule, value, callback) => {
        if (!value && value !== 0) {
          return callback(new Error("结算金额不能为空"));
        } else if (!regular.money.test(value)) {
          callback(new Error("请输入正整数或者两位小数"));
        } else if (parseFloat(value) > 99999999) {
          callback(new Error("结算金额设置超过上限值"));
        } else {
          callback();
        }
      };
      const checkPurchasePrice = (rule, value, callback) => {
        if (!value && value !== 0) {
          return callback(new Error("最低购买金额不能为空"));
        } else if (!regular.money.test(value)) {
          callback(new Error("请输入正整数或者两位小数"));
        } else if (parseFloat(value) > 99999999) {
          callback(new Error("最低购买金额设置超过上限值"));
        } else {
          callback();
        }
      };
      const checkLowestPrice = (rule, value, callback) => {
        if (!value && value !== 0) {
          return callback(new Error("最低可砍金额不能为空"));
        } else if (!regular.money.test(value)) {
          callback(new Error("请输入正整数或者两位小数"));
        } else if (parseFloat(value) > 99999999) {
          callback(new Error("最低可砍金额设置超过上限值"));
        } else {
          callback();
        }
      };
      const checkHighestPrice = (rule, value, callback) => {
        if (!value && value !== 0) {
          return callback(new Error("最高可砍金额不能为空"));
        } else if (!regular.money.test(value)) {
          callback(new Error("请输入正整数或者两位小数"));
        } else if (parseFloat(value) > 99999999) {
          callback(new Error("最高可砍金额设置超过上限值"));
        } else {
          callback();
        }
      };
      return {
        modalType: 0, // 是否编辑
        form: {
          purchasePrice:0,
          goodsSku: {},
        },
        id: this.$route.query.id, // 砍价活动id
        onlyView:this.$route.query.onlyView, // 是否为只读
        submitLoading: false, // 添加或编辑提交状态
        formRule: {
          settlementPrice: [
            {required: true, message: "请输入结算金额"},
            {validator: checkSettlementPrice},
          ],
         
          lowestPrice: [
            {required: true, message: "请输入最低可砍金额"},
            {validator: checkLowestPrice},
          ],
          highestPrice: [
            {required: true, message: "请输入最高可砍金额"},
            {validator: checkHighestPrice},
          ],
          rangeTime: [{required: true, message: "请选择活动时间"}],
          stock: [{required: true, message: "请输入活动库存"}],
        },
        options: {
          disabledDate(date) {
            return date && date.valueOf() < Date.now() - 86400000;
          },
        },
      };
    },
    async mounted() {
      // 如果id不为空则查询信息
      if (this.id) {
        this.getKanJiaActivityGoods();
        this.modalType = 1;
      }
    },
    methods: {
      getKanJiaActivityGoods() {
        getKanJiaActivityGoodsById(this.id).then((res) => {
          this.form = res.result;
          this.form.rangeTime = [];
          this.form.rangeTime.push(new Date(this.form.startTime), new Date(this.form.endTime));
        });
      },

      /** 保存砍价活动 */
      handleSubmit() {

        this.$refs.form.validate((valid) => {
          if (valid) {
            const params = JSON.parse(JSON.stringify(this.form));
            if (this.form.rangeTime[0] === '' || this.form.rangeTime[0] === '') {
              this.$Message.error("请选择活动时间");
              return
            }
            params.startTime = this.$options.filters.unixToDate(
              this.form.rangeTime[0] / 1000
            );
            params.endTime = this.$options.filters.unixToDate(
              this.form.rangeTime[1] / 1000
            );
            delete params.rangeTime
            console.warn(params)
            //校验库存参数
            if (params.stock <= 0 || params.stock > params.goodsSku.quantity) {
              this.$Message.error("活动库存不能为0且不能超过商品库存");
              return
            }
          
            // 结算价格金额格式校验
            if (!regular.money.test(params.settlementPrice)) {
              this.$Message.error("结算价格金额格式不正确");
              return
            }
            // 结算价格金额格式校验
            if (params.settlementPrice < 0 || params.settlementPrice > params.price) {
              this.$Message.error("结算价格金额不能为0且不能超过商品价格");
              return
            }
            //最高砍价校验
            if (!regular.money.test(params.highestPrice)) {
              this.$Message.error("最高可砍金额格式错误");
              return
            }
            if (params.highestPrice <= 0 || params.highestPrice > params.price) {
              this.$Message.error("最高可砍金额不能为0且不能超过商品价格");
              return
            }

            //最低砍价校验
            if (!regular.money.test(params.lowestPrice)) {
              this.$Message.error("最低可砍金额格式错误");
              return
            }
            if (params.lowestPrice <= 0 || params.lowestPrice > params.price) {
              this.$Message.error("最低可砍金额不能为0");
              return
            }
            //校验最高最低砍价金额
            if (params.lowestPrice > params.highestPrice) {
              this.$Message.error("最低砍价金额不能大于最高砍价金额");
              return
            }

            this.submitLoading = true;
            // 添加 避免编辑后传入id等数据 记得删除
            editKanJiaActivityGoods(params).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("砍价活动修改成功");
                this.closeCurrentPage();
              }
            });
          }
        });
      },
      // 关闭当前页面
      closeCurrentPage() {
        this.$store.commit("removeTag", "add-kan-jia-goods");
        localStorage.pageOpenedList = JSON.stringify(
          this.$store.state.app.pageOpenedList
        );
        this.$router.go(-1);
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

