<template>
  <div class="empty">
    <img
      class="empty-img"
      :style="{ width: _Size + 'px' }"
      src="../../assets/images/empty.png"
      alt=""
    />
    <p v-if="_Title">{{ _Title }}</p>
  </div>
</template>

<script>
export default {
  name: 'Main',
  props: {
    _Title: { // 描述内容
      type: null,
      default: '暂无更多'
    },
    _Size: { // 图片大小
      type: Number,
      default: 150
    }
  }
};
</script>

<style scoped lang="scss">
.empty {
  margin: 30px 0;
  text-align: center;
  width: 100%;

}
p {
  cursor: pointer;
 
}
</style>
