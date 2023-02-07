import {Store} from 'vuex';
import {getDefaultService} from '@/utils/service';

const useGitService = (store: Store<RootStoreState>): Services<Git> => {
  const ns = 'git';

  return {
    ...getDefaultService<Git>(ns, store),
  };
};

export default useGitService;
