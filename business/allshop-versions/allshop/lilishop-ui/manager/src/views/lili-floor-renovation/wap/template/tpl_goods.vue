<template>
  <div class="layout">
    <div class="goods-cell-title">
      <div
        class="goods-item-title"
        :class="{ selected: selected.index == index }"
        @click="handleClickTitle(title, index)"
        v-for="(title, index) in res.list[0].titleWay"
        :key="index"
      >
        <h4>{{ title.title }}</h4>
        <div>{{ title.desc }}</div>
      </div>
    </div>
    <div class="goods-list">
      <div
        v-if="
          item.___index != undefined
            ? selected.index == item.___index
            : selected.val == item.type
        "
        class="goods-item"
        v-for="(item, item_index) in res.list[0].listWay"
        :key="item_index"
      >
        <div class="goods-img">
          <Icon
            size="20"
            color="#e1251b"
            @click="closeGoods(item, item_index)"
            class="goods-icon"
            type="ios-close-circle"
          />
          <img :src="item.img" alt />
        </div>
        <div class="goods-desc">
          <div class="goods-title">
            {{ item.title }}
          </div>
          <div class="goods-bottom">
            <div class="goods-price">￥{{ item.price | unitPrice }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      selected: {
        // 已选数据
        index: 0,
      },
    };
  },
  props: ["res"],
  watch: {
    res: {
      handler(val) {
        // 监听父级的值 如果有值将值赋给selected
        if (val) {
          this.selected.val = this.res.list[0].listWay[0].type;
        }
      },
      immediate: true,
    },
  },
  methods: {
    // 删除商品
    closeGoods(val, index) {
      this.res.list[0].listWay.splice(index, 1);
    },
    // 切换商品列表
    handleClickTitle(val, index) {
      this.selected.index = index;
      this.selected.val = val.title;
    },
  },
};
</script>
<style lang="scss" scoped>
.layout {
  padding: 8px 0;
  background: #e8e8e8;
}
.selected {
  > h4 {
    color: #000 !important;
  }
  > div {
    font-weight: bold;
    color: $theme_color !important;
  }
}
.goods-cell-title {
  padding: 10px;
  transition: 0.35s;
  display: flex;
  cursor: pointer;
  > .goods-item-title {
    flex: 1;
    text-align: center;
    > h4 {
      font-size: 16px;
      color: #666;
    }
    > div {
      color: #999;
      font-size: 12px;
    }
  }
}
.goods-list {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
}
.goods-item {
  width: 50%;
  margin-bottom: 10px;
  border-radius: 0.4em;
  overflow: hidden;
}
.goods-img {
  position: relative;
  margin: 0 auto;
  width: 158px;
  height: 150px;
  border-top-left-radius: 0.4em;
  border-top-right-radius: 0.4em;
  overflow: hidden;
  > img {
    width: 100%;
    height: 100%;
  }
}
.goods-desc {
  border-bottom-left-radius: 0.4em;
  border-bottom-right-radius: 0.4em;
  width: 158px;
  background: #fff;
  padding: 4px;
  margin: 0 auto;
  > .goods-title {
    font-size: 12px;
    height: 38px;
    display: -webkit-box;
    font-weight: 500;
    -webkit-box-orient: vertical;

    -webkit-line-clamp: 2;

    overflow: hidden;
  }
  > .goods-bottom {
    display: flex;
    > .goods-price {
      line-height: 2;
      color: $theme_color;
    }
  }
}
.goods-icon {
  right: 5px;
  top: 5px;
  position: absolute;
}
</style>
