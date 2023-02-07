import {useStore} from 'vuex';
import useDetail from '@/layouts/content/detail/useDetail';
import {setupGetAllList} from '@/utils/list';

const useScheduleDetail = () => {
  // store
  const store = useStore();

  setupGetAllList(store, [
    'node',
    'spider',
  ]);

  return {
    ...useDetail('schedule'),
  };
};

export default useScheduleDetail;
