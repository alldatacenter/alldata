import {Store} from 'vuex';
import useForm from '@/components/form/form';
import useGitService from '@/services/git/gitService';
import {getDefaultFormComponentData} from '@/utils/form';

// get new git
export const getNewGit = (): Git => {
  return {};
};

// form component data
const formComponentData = getDefaultFormComponentData<Git>();

const useGit = (store: Store<RootStoreState>) => {
  return {
    ...useForm('git', store, useGitService(store), formComponentData),
  };
};

export default useGit;
