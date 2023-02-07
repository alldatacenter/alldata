import {Store} from 'vuex';
import {getDefaultService} from '@/utils/service';

const useNotificationService = (store: Store<RootStoreState>): Services<NotificationSetting> => {
  const ns = 'notification';

  return {
    ...getDefaultService<NotificationSetting>(ns, store),
  };
};

export default useNotificationService;
