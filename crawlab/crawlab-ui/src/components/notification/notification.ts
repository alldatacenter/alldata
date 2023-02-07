import {useRoute} from 'vue-router';
import {computed} from 'vue';
import {Store} from 'vuex';
import useForm from '@/components/form/form';
import useNotificationService from '@/services/notification/notificationService';
import {getDefaultFormComponentData} from '@/utils/form';

// form component data
const formComponentData = getDefaultFormComponentData<NotificationSetting>();

const useNotification = (store: Store<RootStoreState>) => {
  // route
  const route = useRoute();

  // notification id
  const id = computed(() => route.params.id);

  return {
    ...useForm('notification', store, useNotificationService(store), formComponentData),
    id,
  };
};

export default useNotification;
