<template>
  <div class="border-box-content">
    <slot />
  </div>
</template>

<script>
export default {
  name: 'BorderBox0'
}
</script>

<style lang="scss" scoped>
.border-box-content {
  position: relative;
  width: 100%;
  height: 100%;
}
</style>
