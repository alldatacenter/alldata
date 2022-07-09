
import {commonUrl, getRequestWithNoToken, postRequestWithNoToken} from '@/libs/axios';


// 获取拼图验证
export const getVerifyImg = (verificationEnums) => {
  return getRequestWithNoToken(`${commonUrl}/common/common/slider/${verificationEnums}`);
};

// 拼图验证
export const postVerifyImg = (params) => {
  return postRequestWithNoToken(`${commonUrl}/common/common/slider/${params.verificationEnums}`, params);
};
