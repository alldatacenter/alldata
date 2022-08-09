<template>
  <div class="not-enough" ref="obtain" id="demo">
    <Affix :offset-top="62" @on-change="change">
      <ul class="nav-bar" v-show="topSearchShow">
        <li
          v-for="(item, index) in conData.options.navList"
          :class="currentIndex === index ? 'curr' : ''"
          @click="changeCurr(index)"
          :key="index"
        >
          <p @click="gotoDemo">{{ item.title }}</p>
          <p @click="gotoDemo">{{ item.desc }}</p>
        </li>
      </ul>
    </Affix>
    <div class="content" v-if="showContent">
      <div
        v-for="(item, index) in conData.options.list[currentIndex]"
        :key="index"
        class="hover-pointer"
        @click="linkTo(item.url)"
      >
        <img :src="item.img" width="210" height="210" :alt="item.name" />
        <p>{{ item.name }}</p>
        <p>
          <span>{{ Number(item.price) | unitPrice("￥") }}</span>
        </p>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  mounted() {
    window.addEventListener('scroll',this.handleScrollx,true)
  },
  props: {
    data: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      screenHeight:document.body.clientHeight,
      scrollHieght:0,
      topSearchShow: true, //展示 导航条
      topIndex: 0, // 当前滚动条位置 （取整）
      scrollTops: 0,
      open:'',
      currentIndex: 0, // 当前分类下标
      conData: this.data, // 装修数据
      showContent: true, // 是否展示内容
    };
  },
  watch: {
    data: function (val) {
      this.conData = val;
    },
    conData: function (val) {
      this.$emit("content", val);
    },
  },
  methods: {
    handleScrollx(){
      // console.log('滚动高度',window.pageYOffset) // 获取滚动条的高度
      // console.log(this.$refs.obtain.getBoundingClientRect().top) //获取到距离顶部的距离
      this.scrollHieght = Number(window.pageYOffset);//获取到距离顶部的距离
      this.scrollTops = Number(this.$refs.obtain.getBoundingClientRect().top); // 获取到距离顶部的距离
         this.topSearchShow = true; // 展示图钉
          if(this.scrollTops < -660){ // 超过隐藏
            this.topSearchShow = false;
      }
    },
     toguid(path,id){
      var path =path;
      var Id = id;
      localStorage.setItem('toId',Id);
      this.$router.push(path);
    },
    change(status){ //获取是否获取到图钉
      this.open = status
    },
    gotoDemo(){ // 跳转到demo的位置 
    if(this.open){ // 获取到图钉之后在跳转当前位置
       document.querySelector("#demo").scrollIntoView(true);
    }
      //scrollIntoView()可以在所有的HTML元素上调用，通过滚动浏览器窗口或某个容器元素
    },
    changeCurr(index) {
      // 选择分类
      this.currentIndex = index;
    },
  },
};
</script>
<style lang="scss" scoped>
.nav-bar {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-bottom: 10px;
  background-color: #f8f8f8;
  height: 60px;
  align-items: center;
  position: relative;
  position: sticky;
  position: -webkit-sticky;
  top: 0;
  li {
    padding: 0 30px;
    text-align: center;
    p:nth-child(1) {
      font-size: 16px;
      border-radius: 50px;
      padding: 0 7px;
    }

    p:nth-child(2) {
      font-size: 14px;
      color: #999;
    }

    &:hover {
      p {
        color: $theme_color;
      }
      cursor: pointer;
    }
    border-right: 1px solid #eee;
  }
  li:last-of-type {
    border: none;
  }

  .curr {
    p:nth-child(1) {
      background-color: $theme_color;

      color: #fff;
    }
    p:nth-child(2) {
      color: $theme_color;
    }
  }
}

.content {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  > div {
    padding: 10px;
    box-sizing: border-box;
    border: 1px solid #eee;
    margin-bottom: 10px;

    &:hover {
      border-color: $theme_color;
      color: $theme_color;
    }

    p:nth-of-type(1) {
      overflow: hidden;
      width: 210px;
      white-space: nowrap;
      text-overflow: ellipsis;
      margin: 10px 0 5px 0;
    }
    p:nth-of-type(2) {
      color: $theme_color;
      font-size: 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      span:nth-child(2) {
        text-decoration: line-through;
        font-size: 12px;
        color: #999;
      }
    }
  }

}
</style>
