
<template>
  <div class="layout">
    <div class="view-list">
      <div class="view-item">
        <div class="-item-tilte">{{res.list[0].title}}</div>
        <div class="-item-image">
          <img :src="res.list[0].img" alt />
        </div>
      </div>
      <div class="view-item">
        <div class="-item-tilte">{{res.list[1].title}}</div>
        <div class="-item-image">
          <img :src="res.list[1].img" alt />
        </div>
      </div>
    </div>
    <div class="view-list">
      <div class="view-item">
        <div class="-item-tilte">{{res.list[2].title}}</div>
        <div class="-item-image">
          <img :src="res.list[2].img" alt />
        </div>
      </div>
      <div class="view-item">
        <div class="-item-tilte">{{res.list[3].title}}</div>
        <div class="-item-image">
          <img :src="res.list[3].img" alt />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  title: "文字图片模板",
  props: ["res"]
};
</script>
<style lang="scss" scoped>
@import "./tpl.scss";
.layout {
  display: flex;
  background: #e8e8e8;
  align-items: center;
  justify-content: center;
  background-size: cover;
  padding: 0;
}
.-item-image{

     padding: 10px ;
     >img{

         width: 100%;
     }
}
.-item-tilte {
  background: #ff9f28;
  height: 30px;
  color: #fff;
  font-size: 14px;
  text-align: center;
  line-height: 30px;
}

.view-list {
  width: 48%;
  margin: 0 auto;
  display: flex;
  background: #fff;
   border-top-left-radius: 0.8em;
    border-top-right-radius: 0.8em;
    border: 1px solid #ededed;

  > .view-item {
    width: 50%;
  }
  > .view-item:nth-of-type(1) {
    > .-item-tilte {
      border-top-left-radius: 0.8em;
    }
  }
  > .view-item:nth-of-type(2) {
    > .-item-tilte {
      border-top-right-radius: 0.8em;
    }
  }
}
</style>
