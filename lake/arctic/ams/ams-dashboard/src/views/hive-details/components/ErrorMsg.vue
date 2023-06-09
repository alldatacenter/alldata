<template>
  <a-modal
    :visible="true"
    :width="560"
    :title="`${$t('errorMessage')}`"
    :footer="null"
    @cancel="emit('cancle')"
    class="upgrade-error"
    >
    <p class="msg">{{ props.msg }}</p>
  </a-modal>
</template>
<script lang="ts" setup>

const props = defineProps<{ msg: string }>()
const emit = defineEmits<{
 (e: 'cancle'): void
}>()

</script>
<style lang="less" scoped>
.upgrade-error {
  .msg {
    min-height: 172px;
    max-height: 320px;
    overflow-y: auto;
  }
}
</style>
