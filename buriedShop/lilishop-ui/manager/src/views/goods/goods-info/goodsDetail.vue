<template>
  <div>
    <Form :label-width="120">
      <Card>
        <div class="base-info-item">
          <h4>基本信息</h4>
          <div class="form-item-view">
            <FormItem label="商品分类">
              <span v-for="(item, index) in goods.categoryName" :key="index">
                {{ item }}
                <i v-if="index !== goods.categoryName.length - 1">&gt;</i>
              </span>
            </FormItem>
            <FormItem label="商品名称">
              {{ goods.goodsName }}
            </FormItem>

            <FormItem label="商品卖点">
              {{ goods.sellingPoint }}
            </FormItem>
          </div>
          <h4>商品交易信息</h4>
          <div class="form-item-view">
            <FormItem label="计量单位"> {{ goods.goodsUnit }}</FormItem>
            <FormItem label="销售模式">
              {{ goods.salesModel === "RETAIL" ? "零售型" : "批发型" }}
            </FormItem>
            <FormItem label="销售规则" v-if="goods.salesModel !== 'RETAIL'">
              <Table
                border
                :columns="wholesalePreviewColumns"
                :data="wholesaleData"
              >
              </Table>
            </FormItem>
          </div>
          <h4>商品规格及图片</h4>
          <div class="form-item-view">
            <FormItem label="商品编号"> {{ goods.id }}</FormItem>
            <FormItem label="商品价格">
              ¥{{ goods.price | unitPrice }}
            </FormItem>
            <FormItem label="商品图片">
              <div
                class="demo-upload-list"
                v-for="(item, __index) in goods.goodsGalleryList"
                :key="__index"
              >
                <img :src="item" />
                <div class="demo-upload-list-cover">
                  <Icon
                    type="ios-eye-outline"
                    @click.native="handleViewGoodsPicture(item)"
                  ></Icon>
                </div>
                <Modal title="View Image" v-model="goodsPictureVisible">
                  <img
                    :src="previewGoodsPicture"
                    v-if="goodsPictureVisible"
                    style="width: 100%"
                  />
                </Modal>
              </div>
            </FormItem>
            <FormItem label="商品规格">
              <Table :columns="skuColumn" :data="skuData">
                <template slot="showImage" slot-scope="scope">
                  <div style="margin-top: 5px; height: 80px; display: flex">
                    <div>
                      <img
                        :src="scope.row.image"
                        style="height: 60px; margin-top: 1px; width: 60px"
                      />
                    </div>
                  </div>
                </template>
                <template slot-scope="{ row }" slot="wholePrice0">
                  <Input
                    v-if="wholesaleData[0]"
                    clearable
                    disabled
                    v-model="wholesaleData[0].price"
                  >
                    <span slot="append">元</span>
                  </Input>
                </template>
                <template slot-scope="{ row }" slot="wholePrice1">
                  <Input
                    v-if="wholesaleData[1]"
                    clearable
                    disabled
                    v-model="wholesaleData[1].price"
                  >
                    <span slot="append">元</span>
                  </Input>
                </template>
                <template slot-scope="{ row }" slot="wholePrice2">
                  <Input
                    v-if="wholesaleData[2]"
                    clearable
                    disabled
                    v-model="wholesaleData[2].price"
                  >
                    <span slot="append">元</span>
                  </Input>
                </template>
              </Table>
            </FormItem>
          </div>
          <h4>商品详情描述</h4>
          <div class="form-item-view">
            <FormItem label="商品描述">
              <div v-html="goods.intro"></div>
            </FormItem>
            <FormItem label="移动端描述">
              <div v-html="goods.mobileIntro"></div>
            </FormItem>
          </div>
        </div>
      </Card>
    </Form>
  </div>
</template>
<script>
import { getGoodsDetail } from "@/api/goods";
export default {
  name: "goodsDetail",
  data() {
    return {
      goods: {}, // 商品信息
      previewGoodsPicture: "", // 预览图片
      goodsPictureVisible: false, // 预览图片模态框
      wholesalePreviewColumns: [
        {
          title: "销售规则",
          width: 300,
          render: (h, params) => {
            let guide =
              "当商品购买数量 ≥" +
              params.row.num +
              " 时，售价为 ￥" +
              params.row.price +
              " /" +
              this.goods.goodsUnit;
            return h("div", guide);
          },
        },
      ],
      wholesaleData: [],
      skuColumn: [
        // 规格表头
        {
          title: "规格",
          key: "specs",
        },
        {
          title: "编号",
          key: "sn",
        },
        {
          title: "重量(kg)",
          key: "weight",
        },
      ],
      skuData: [], // sku数据
    };
  },
  mounted() {
    this.initGoods(this.$route.query.id);
  },
  methods: {
    // 初始化数据，获取商品详情
    initGoods(id) {
      getGoodsDetail(id).then((res) => {
        this.goods = res.result;
        let that = this;
        res.result.skuList.forEach(function (sku, index, array) {
          that.skuData.push({
            specs: sku.goodsName,
            sn: sku.sn,
            weight: sku.weight,
            cost: that.$options.filters.unitPrice(sku.cost, "¥"),
            price: that.$options.filters.unitPrice(sku.price, "¥"),
            image: sku.thumbnail,
          });
        });
        if (res.result.salesModel === "WHOLESALE" && res.result.wholesaleList) {
          res.result.wholesaleList.forEach((item, index) => {
            this.skuColumn.push({
              title: "购买量 ≥ " + item.num,
              slot: "wholePrice" + index,
            });
          });
        } else {
          this.skuColumn.push(
            {
              title: "成本",
              key: "cost",
            },
            {
              title: "价格",
              key: "price",
            }
          );
        }
        this.skuColumn.push({
          title: "图片",
          slot: "showImage",
        });
        this.wholesaleData = res.result.wholesaleList;
      });
    },
    // 预览商品图片
    handleViewGoodsPicture(url) {
      this.previewGoodsPicture = url;
      this.goodsPictureVisible = true;
    },
  },
};
</script>

<style lang="scss" soped>
/*平铺*/
div.base-info-item {
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

  .form-item-view {
    padding-left: 80px;
  }
}

.demo-upload-list {
  display: inline-block;
  width: 60px;
  height: 60px;
  text-align: center;
  line-height: 60px;
  border: 1px solid transparent;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  position: relative;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.2);
  margin-right: 4px;
}
.demo-upload-list img {
  width: 100%;
  height: 100%;
}
.demo-upload-list-cover {
  display: none;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
}
.demo-upload-list:hover .demo-upload-list-cover {
  display: block;
}
.demo-upload-list-cover i {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  margin: 0 2px;
}
.ivu-table table {
  width: 100% !important;
}
</style>
