<template>
  <cl-nav-action-group>
    <cl-nav-action-fa-icon :icon="['fa', 'tools']"/>
    <cl-nav-action-item>
      <cl-fa-icon-button :icon="['fa', 'play']" :tooltip="t('common.actions.run')" type="success" @click="onRun"/>
    </cl-nav-action-item>
    <!--TODO: implement-->
    <cl-nav-action-item v-if="false">
      <cl-fa-icon-button :icon="['fa', 'clone']" :tooltip="t('common.actions.clone')" type="info"/>
    </cl-nav-action-item>
    <!--TODO: implement-->
    <cl-nav-action-item v-if="false">
      <cl-fa-icon-button :icon="['far', 'star']" plain :tooltip="t('common.actions.bookmark')" type="warning"/>
    </cl-nav-action-item>
  </cl-nav-action-group>

  <!-- Dialogs (handled by store) -->
  <cl-run-spider-dialog v-if="activeDialogKey === 'run'"/>
  <!-- ./Dialogs -->
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import {useStore} from 'vuex';
import useSpider from '@/components/spider/spider';
import {useI18n} from 'vue-i18n';
import {sendEvent} from '@/admin/umeng';

export default defineComponent({
  name: 'SpiderDetailActionsCommon',
  setup() {
    // i18n
    const {t} = useI18n();

    // store
    const ns = 'spider';
    const store = useStore();

    const onRun = () => {
      store.commit(`${ns}/showDialog`, 'run');

      sendEvent('click_spider_detail_actions_run');
    };

    return {
      ...useSpider(store),
      onRun,
      t,
    };
  },
});
</script>

<style lang="scss" scoped>

</style>
