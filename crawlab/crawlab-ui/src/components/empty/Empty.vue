<template>
  <div class="empty">
    <cl-img-empty/>
    <div class="description">
      {{ t(description) }}
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import ImgEmpty from '@/components/empty/ImgEmpty.vue';
import {useI18n} from 'vue-i18n';

export default defineComponent({
  name: 'Empty',
  props: {
    description: {
      type: String,
      required: false,
      default: 'components.empty.noDataAvailable'
    }
  },
  setup() {
    const {t} = useI18n();

    return {
      t,
    };
  },
});
</script>

<style lang="scss" scoped>
.empty {
  max-height: 240px;
  display: flex;
  flex-direction: column;

  .description {
    margin-top: 10px;
    display: flex;
    justify-content: center;
  }
}
</style>
