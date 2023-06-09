
<template>
  <div v-show="visible" v-if="visible" ref="loadingRef" class="u-loading" :class="{'fullscreen': fullscreen}" >
    <a-spin :tip="loadingText" />
  </div>
</template>

<script lang="ts">
import { onBeforeUnmount, ref } from 'vue'

export default ({
  name: 'ULoading',
  props: {
    loadingText: {
      type: String,
      default: 'Loading'
    },
    fullscreen: {
      type: Boolean,
      default: false
    }
  },
  setup() {
    const visible = ref<boolean>(true)

    const hide = () => {
      visible.value = false
    }

    onBeforeUnmount(() => {
      visible.value = false
    })

    return {
      visible,
      hide
    }
  }
})

</script>

<style lang="less" scoped>
.u-loading {
  position: absolute;
  z-index: 999;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, .2);
  &.fullscreen {
    position: fixed;
  }
  &-img {
    width: 30px;
    height: 30px;
  }
  &-text {
    margin-top: 8px;
    user-select: none;
    color: #fff;
  }
}
</style>
