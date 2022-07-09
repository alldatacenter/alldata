<template>
  <div class="wrapper">
    <Card>
      <Form ref="searchForm" :model="params" inline :label-width="100" class="search-form">
        <Form-item label="商品名称">
          <Input type="text" v-model="params.name" placeholder="请输入商品名称" clearable style="width: 200px" />
        </Form-item>

        <Button @click="getLiveGoodsMethods('clear')" type="primary" class="search-btn" icon="ios-search">搜索</Button>
      </Form>
      <h4 v-if="!reviewed">
        由于直播商品需经过小程序直播平台的审核，你需要在此先提审商品，为了不影响直播间选取商品，请提前1天提审商品；
      </h4>

      <div>
        <Tabs v-model="params.auditStatus">
          <TabPane v-for="(item,index) in liveTabWay" :key="index" :label="item.label" :name="item.type+''">
          </TabPane>
        </Tabs>
      </div>

      <Button v-if="!reviewed" type="primary" style="margin-bottom:10px;" @click="addNewLiveGoods" icon="md-add">选择商品</Button>
      <Button type="primary" v-if="params.auditStatus == 0" ghost style="margin:0 0 10px 10px" @click="getLiveGoodsMethods('clear')">更新状态</Button>
      <div style="position:relative">
        <Spin size="large" fix v-if="tableLoading">
        </Spin>
        <Table class="mt_10" disabled-hover :columns="liveGoodsColumns" :data="liveGoodsData">

          <template slot-scope="{ row }" slot="goodsName">
            <div class="flex-goods">
              <img class="thumbnail" :src="row.thumbnail || row.goodsImage">
              {{ row.goodsName || row.name }}
            </div>
          </template>
          <template slot-scope="{ row ,index }" class="price" slot="price">
            <!-- 如果为新增商品显示 -->

            <RadioGroup v-if="params.auditStatus == 99" @on-change="changeRadio(row,'priceType')" v-model="row.priceType">
              <div class="price-item">
                <Radio :label="1">一口价:</Radio>
                <InputNumber :min="0.1" v-if="liveGoodsData[index].priceType == 1" style="width:100px" v-model="liveGoodsData[index].price"></InputNumber>
              </div>
              <div class="price-item">
                <Radio :label="2">区间价:</Radio> <span v-if="liveGoodsData[index].priceType == 2">
                  <InputNumber :min="0.1" style="width:100px" v-model="liveGoodsData[index].price" />至
                  <InputNumber :min="0.1" style="width:100px" v-model="liveGoodsData[index].price2" />
                </span>
              </div>
              <div class="price-item">
                <Radio :label="3">折扣价:</Radio> <span v-if="liveGoodsData[index].priceType == 3">原价<InputNumber :min="0.1" style="width:100px" v-model="liveGoodsData[index].price"></InputNumber>现价
                  <InputNumber :min="0.1" style="width:100px" v-model="liveGoodsData[index].price2" />
                </span>
              </div>
            </RadioGroup>
            <div v-else>
              <div v-if="row.priceType == 1">{{row.price | unitPrice('￥')}}</div>
              <div v-if="row.priceType == 2">{{row.price | unitPrice('￥')}}至{{row.price2 | unitPrice('￥')}}</div>
              <div v-if="row.priceType == 3">{{row.price | unitPrice('￥')}}<span class="original-price">{{row.price2 | unitPrice('￥')}}</span></div>
            </div>

          </template>

          <template slot-scope="{ row,index }" slot="action">
            <Button v-if="params.auditStatus == 99" type="primary" @click="()=>{liveGoodsData.splice(index,1)}">删除</Button>
            <Button v-if="params.auditStatus != 99 && !reviewed" ghost type="primary" @click="()=>{$router.push({path:'/goods-operation-edit',query:{id:row.goodsId}})}">查看</Button>
            <Button v-if="reviewed" :type="row.___selected ? 'primary' : 'default'" @click="selectedLiveGoods(row,index)">{{row.___selected ? '已':''}}选择</Button>
          </template>
        </Table>
        <div class="flex">
          <Page size="small" :total="goodsTotal" @on-change="changePageNumber" class="pageration" @on-page-size-change="changePageSize" :page-size="params.pageSize" show-total show-elevator
            show-sizer>
          </Page>

        </div>
      </div>
    </Card>
    <sku-select ref="skuSelect" @selectedGoodsData="selectedGoodsData"></sku-select>
    <div v-if="params.auditStatus == 99" class="submit">
      <Button type="primary" :loading="saveGoodsLoading" @click="saveLiveGoods">保存商品</Button>
    </div>
  </div>
</template>

<script>
import skuSelect from "@/views/lili-dialog"; //选择商品组件
import { addLiveStoreGoods, getLiveGoods } from "@/api/promotion.js";
export default {
  components: {
    skuSelect,
  },
  data() {
    return {
      goodsTotal: 0, //商品总数
      saveGoodsLoading: false, //保存商品加载
      tableLoading: false, //表格是否加载
      params: {
        pageNumber: 1,
        pageSize: 10,
        auditStatus: 2, //商品状态
      },
      // 商品审核状态
      liveTabWay: [
        {
          label: "待提审",
          type: 0,
        },
        {
          label: "已审核",
          type: 2,
        },

        {
          label: "审核中",
          type: 1,
        },

        {
          label: "审核未通过",
          type: 3,
        },
      ],

      // 商品表格columns
      liveGoodsColumns: [
        {
          title: "商品",
          slot: "goodsName",
        },
        {
          title: "价格",
          slot: "price",
        },
        {
          title: "库存",
          key: "quantity",
          width: 100,
        },

        {
          title: "操作",
          slot: "action",
          width: 100,
        },
      ],
      // 表格商品详情
      liveGoodsData: [],
      // 已选商品
      selectedGoods: [],
    };
  },
  props: {
    // 是否是已审核，此处为组件模式时使用。去除添加等功能 只保留查询以及新增选择回调数据
    reviewed: {
      type: Boolean,
      default: false,
    },
    // 初始化信息，此处为组件模式时使用。父级将数据传输到此方法上
    init: {
      type: null,
      default: "",
    },
  },
  watch: {
    //此处为组件模式时使用 监听此处为开启则需要删除tab上面的数据只显示已审核
    reviewed: {
      handler(val) {
        if (val) {
          this.liveTabWay = this.liveTabWay.filter((item) => {
            return item.label == "已审核";
          });
        }
      },
      immediate: true,
    },
    //此处为组件模式时使用 监听父级给传值
    init: {
      handler(val) {
        if (val) {
          this.$nextTick(() => {
            // 将当前父级返回的数据和当前数据进行匹配
            this.selectedGoods = val;
            this.liveGoodsData.forEach((item, index) => {
              val.forEach((callback) => {
                if (item.id == callback.id) {
                  this.$set(this.liveGoodsData[index], "___selected", true);
                  // this.selectedGoods.push(item);
                }
              });
            });
          });
        }
      },
      immediate: true,
    },
    // 监听如果次数变化说明用户再点击tab
    "params.auditStatus": {
      handler(val) {
        this.liveGoodsData = [];
        if (val != 99) {
          this.params.pageNumber = 1;
          this.getLiveGoodsMethods();
        }
      },
      immediate: true,
    },
  },
  methods: {
    /**
     * 回调参数补充
     */
    selectedLiveGoods(val, index) {
      this.$emit("selectedGoods", val);
    },
    /**
     * 解决radio数据不回显问题
     */
    changeRadio(val) {
      this.$set(this.liveGoodsData[val._index], "priceType", val.priceType);
    },
    /**
     * 页面数据大小分页回调
     */
    changePageSize(val) {
      this.params.pageSize = val;
      this.getLiveGoodsMethods("clear");
    },
    /**
     * 分页回调
     */
    changePageNumber(val) {
      this.params.pageNumber = val;
      this.getLiveGoodsMethods("clear");
    },
    /**
     * 清除新增的tab
     */
    clearNewLiveTab() {
      this.liveTabWay.map((item, index) => {
        return item.type == 99 && this.liveTabWay.splice(index, 1);
      });
    },

    /**
     * 查询商品
     */
    async getLiveGoodsMethods(type) {
      this.tableLoading = true;

      let result = await getLiveGoods(this.params);
      if (result.success) {
        // 将表格数据清除
        if (type == "clear") {
          this.liveGoodsData = [];
        }
        this.liveGoodsData.push(...result.result.records);
        this.goodsTotal = result.result.total;
      }
      this.tableLoading = false;
    },

    /**
     * 保存直播商品
     */
    async saveLiveGoods() {
      this.saveGoodsLoading = true;
      let submit = this.liveGoodsData.map((element) => {
        console.log(element);
        return {
          goodsId: element.goodsId, //商品id
          goodsImage: element.small, //商品图片  最大为 300 * 300
          name: element.goodsName, //商品昵称
          price: parseInt(element.price), //商品价格
          quantity: element.quantity, //库存
          price2: element.price2 ? parseInt(element.price2) : "", //商品价格
          priceType: element.priceType, // priceType  Number  是  价格类型，1：一口价（只需要传入price，price2不传） 2：价格区间（price字段为左边界，price2字段为右边界，price和price2必传） 3：显示折扣价（price字段为原价，price2字段为现价， price和price2必传）
          skuId: element.id,
          url: `pages/product/goods?id=${element.id}&goodsId=${element.goodsId}`, //小程序地址
        };
      });

      let result = await addLiveStoreGoods(submit);
      if (result.success) {
        this.$Message.success({
          content: `添加成功！`,
        });

        this.params.auditStatus = 0;
      }
      this.saveGoodsLoading = false;
    },

    /**
     * 商品选择器回调的商品信息
     */
    selectedGoodsData(goods) {
      goods.map((item) => {
        return (item.priceType = 1);
      });

      this.liveGoodsData.push(...goods);
    },

    /**
     * 新增商品
     */
    addNewLiveGoods() {
      this.clearNewLiveTab();
      this.liveTabWay.push({
        type: 99,
        label: "新增商品",
      });
      this.$set(this, "liveGoodsData", []);
      this.params.auditStatus = 99;
      this.$refs.skuSelect.open("goods");
      this.$refs.skuSelect.goodsData = JSON.parse(
        JSON.stringify(this.liveGoodsData)
      );
    },
  },
};
</script>

<style lang="scss" scoped>
@import "@/styles/table-common.scss";
.search-form {
  margin-bottom: 10px;
}
.flex {
  display: flex;
  justify-content: flex-end;
  margin: 20px 0;
}
.wrapper {
  position: relative;
}
.thumbnail {
  width: 100px;
  height: 100px;
  border-radius: 0.4em;
}
.flex-goods {
  margin: 10px;
  display: flex;

  align-items: center;
  > img {
    margin-right: 10px;
  }
}
.price-item {
  margin: 15px 5px;
  > * {
    margin: 5px;
  }
}
.submit {
  box-shadow: 3px 5px 12px rgba(0, 0, 0, 0.1);
  height: 60px;
  background: #fff;
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
.original-price {
  margin-left: 10px;
  color: #999;
  text-decoration: line-through;
}

h4 {
  margin-bottom: 10px;
  padding: 0 10px;
  border: 1px solid #ddd;
  background-color: #f8f8f8;
  color: #333;
  font-size: 12px;
  line-height: 40px;
  text-align: left;
}
</style>
