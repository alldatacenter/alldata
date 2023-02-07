import useDetail from '@/layouts/content/detail/useDetail';

const useUserDetail = () => {
  return {
    ...useDetail('user'),
  };
};

export default useUserDetail;
