import {computed, onBeforeMount} from 'vue';
import useDetail from '@/layouts/content/detail/useDetail';
import useNotification from '@/components/notification/notification';
import {useStore} from 'vuex';
import {translate} from '@/utils';
import {DataItem} from 'element-plus';

const t = translate;

const useNotificationDetail = () => {
  const ns = 'notification';
  const store = useStore();
  const {
    notification: state,
  } = store.state as RootStoreState;

  const {
    id,
    form,
  } = useNotification(store);

  const triggersTitles = computed(() => [
    t('components.transfer.titles.available'),
    t('components.transfer.titles.enabled'),
  ]);

  const triggersList = computed<DataItem[]>(() => state.triggersList?.map((trigger: string) => {
    return {
      label: trigger,
      key: trigger,
    } as DataItem;
  }) || []);

  const triggersEnabled = computed<string[]>(() => {
    const {triggers} = form.value;
    return triggers || [];
  });

  const onTriggersChange = (triggers: string[]) => {
    store.commit(`${ns}/setTriggersEnabled`, triggers);
  };

  onBeforeMount(async () => {
    await store.dispatch(`${ns}/getTriggersList`);
    await store.dispatch(`${ns}/getById`, id.value);
  });

  return {
    ...useDetail('notification'),
    triggersTitles,
    triggersList,
    triggersEnabled,
    onTriggersChange,
  };
};

export default useNotificationDetail;
