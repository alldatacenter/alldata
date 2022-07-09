<template>
  <div class="wrapper">
    <!-- 一级分类 -->
    <div class="list">
      <div class="list-item" :class="{active:parentIndex === cateIndex}" @click="handleClickChild(cate,cateIndex)" v-for="(cate,cateIndex) in categoryList" :key="cateIndex">
        {{cate.name}}
      </div>
    </div>
    <!-- 二级分类 -->
    <div class="list">
      <div class="list-item" :class="{active:secondIndex === secondI}" @click="handleClickSecondChild(second,secondI)" v-if="secondLevel.length != 0" v-for="(second,secondI) in secondLevel"
        :key="secondI">
        {{second.name}}
      </div>
    </div>
    <!--三级分类 -->
    <div class="list">
      <div class="list-item" :class="{active:thirdIndex === thirdI}" @click="handleClickthirdChild(third,thirdI)" v-if="thirdLevel.length != 0" v-for="(third,thirdI) in thirdLevel" :key="thirdI">
        {{third.name}}
      </div>
    </div>
  </div>

</template>
<script>
export default {
  data() {
    return {
      parentIndex: '', // 分类父级下标
      secondIndex: '', // 分类二级下标
      thirdIndex: '', // 分类三级下标
      categoryList: [], // 分类列表一级数据
      secondLevel: [], // 分类列表二级数据
      thirdLevel: [], // 分类列表三级数据
      selectedData: "", // 已选分类数据
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    // 点击一级
    handleClickChild(item, index) {
      this.parentIndex = index;
      this.secondLevel = item.children;
      item.___type = "category";
      item.allId = item.id;

      this.secondIndex = '';
      this.thirdIndex = '';
      this.thirdLevel = []
      this.$emit("selected", [item]);
      // 点击第一级的时候默认显示第二级第一个
      // this.handleClickSecondChild(item.children, 0);
    },
    // 点击二级
    handleClickSecondChild(second, index) {
      second.___type = "category";
      second.allId = `${second.parentId},${second.id}`

      this.secondIndex = index;
      this.thirdLevel = second.children;
      this.thirdIndex = '';
      this.$emit("selected", [second]);
      // this.handleClickthirdChild(second.children[0], 0);
    },
    // 点击三级
    handleClickthirdChild(item, index) {
      item.___type = "category";
      item.allId = `${this.categoryList[this.parentIndex].id},${item.parentId},${item.id}`
      this.$emit("selected", [item]);
      this.thirdIndex = index;
    },
    init() {
      let category = JSON.parse(localStorage.getItem('category'))
      if (category) {
        category.forEach((item) => {
          item.___type = "category";
        });
        this.categoryList = category;
        // this.handleClickChild(category[0], 0);
      } else {
        setTimeout(() => {
          category = JSON.parse(localStorage.getItem('category'))
          category.forEach((item) => {
            item.___type = "category";
          });
          this.categoryList = category;
          // this.handleClickChild(category[0], 0);
        },3000)
      }
      
    },
  },
};
</script>
<style lang="scss" scoped>
.list {
  width: 30%;
  margin: 0 1.5%;
  height: 400px;
  overflow: auto;
  > .list-item {
    padding: 10px;
    transition: 0.35s;
    cursor: pointer;
  }
  .list-item:hover {
    background: #ededed;
  }
}
.active {
  background: #ededed;
}
.wrapper {
  overflow: hidden;
}
</style>
