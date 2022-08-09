<template>
  <div class="add-eval">
    <div class="title">
      <card _Title="评价详情" :_Size="16"></card>
      <p>
        <span class="color999">创建人：</span
        ><span>{{ orderGoods.createBy | secrecyMobile }}</span>
        <span class="color999 ml_20">{{ orderGoods.createTime }}</span>
      </p>
    </div>
    <!-- 物流评分、服务评分 -->
    <div class="delivery-rate">
      <div class="fontsize_16">物流服务评价：</div>
      <div class="color999">
        <span>物流评价：<Rate disabled :value="Number(orderGoods.deliveryScore)" /></span>
        <span>服务评价：<Rate disabled :value="Number(orderGoods.serviceScore)" /></span>
        <span
          >服务评价：<Rate disabled :value="Number(orderGoods.descriptionScore)"
        /></span>
      </div>
    </div>
    <!-- 添加订单评价  左侧商品详情  右侧评价框 -->
    <ul class="goods-eval">
      <li>
        <div class="goods-con">
          <img
            :src="orderGoods.goodsImage"
            class="hover-pointer"
            alt=""
            width="100"
            @click="goGoodsDetail(orderGoods.skuId, orderGoods.goodsId)"
          />
          <p
            class="hover-pointer color999"
            @click="goGoodsDetail(orderGoods.skuId, orderGoods.goodsId)"
          >
            {{ orderGoods.goodsName }}
          </p>
        </div>

        <div class="eval-con">
          <div>
            <span class="color999">商品评价：</span>
            <RadioGroup
              style="margin-bottom: 5px; color: #999"
              v-model="orderGoods.grade"
              type="button"
              button-style="solid"
            >
              <Radio label="GOOD" disabled>好评</Radio>
              <Radio label="MODERATE" disabled>中评</Radio>
              <Radio label="WORSE" disabled>差评</Radio>
            </RadioGroup>
            <Input
              type="textarea"
              maxlength="500"
              readonly
              show-word-limit
              :rows="4"
              v-model="orderGoods.content"
            />
          </div>
          <div>
            <span class="color999">商家回复：</span>
            <span>{{ orderGoods.reply }}</span>
          </div>
          <div style="display: flex; align-items: center">
            <template v-if="orderGoods.images">
              <div
                class="demo-upload-list"
                v-for="(img, index) in orderGoods.images.split(',')"
                :key="index"
              >
                <img :src="img" />
                <div class="demo-upload-list-cover">
                  <Icon type="ios-eye-outline" @click.native="handleView(img)"></Icon>
                </div>
              </div>
            </template>
          </div>
        </div>
      </li>
    </ul>
    <Modal title="View Image" v-model="visible">
      <img :src="previewImage" v-if="visible" style="width: 100%" />
    </Modal>
  </div>
</template>
<script>
import { evaluationDetail } from "@/api/member.js";
export default {
  data() {
    return {
      order: {}, // 订单详情
      orderGoods: {}, // 订单商品
      visible: false, // 图片预览
      previewImage: "", // 预览图片链接
      loading: false, // 加载状态
    };
  },
  methods: {
    getDetail() {
      // 获取评价详情
      evaluationDetail(this.$route.query.id).then((res) => {
        if (res.success) this.orderGoods = res.result;
      });
    },
    goGoodsDetail(skuId, goodsId) {
      // 跳转商品详情
      let routerUrl = this.$router.resolve({
        path: "/goodsDetail",
        query: { skuId, goodsId },
      });
      window.open(routerUrl.href, "_blank");
    },
    handleView(name) {
      // 预览图片
      this.previewImage = name;
      this.visible = true;
    },
  },
  mounted() {
    this.getDetail();
  },
};
</script>
<style lang="scss" scoped>
.delivery-rate {
  display: flex;
  align-items: center;
  margin-top: 20px;
  height: 50px;
  border-bottom: 1px solid #eee;
  > div:nth-child(1) {
    width: 120px;
    font-weight: bold;
  }
}
.goods-eval li {
  display: flex;
  border-bottom: 1px solid #eee;

  .goods-con {
    width: 30%;
    padding: 20px;
    text-align: center;
    p {
      word-wrap: wrap;
      &:hover {
        color: $theme_color;
      }
    }
  }
  .eval-con {
    width: 70%;
    padding: 20px;
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
  margin-top: 10px;
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
.icon-upload {
  width: 58px;
  height: 58px;
  line-height: 58px;
  text-align: center;
  display: inline-block;
  border: 1px dashed #999;
  border-radius: 4px;
  margin-top: 10px;
  &:hover {
    cursor: pointer;
    border-color: $theme_color;
  }
}
</style>
