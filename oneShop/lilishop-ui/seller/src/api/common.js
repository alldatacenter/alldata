import { commonUrl, getRequest, uploadFileRequest ,uploadFile} from "@/libs/axios";

// 通过id获取子地区
export const getChildRegion = (id) => {
  return getRequest(`${commonUrl}/common/common/region/item/${id}`);
};

// 点地图获取地址信息
export const getRegion = (params) => {
  return getRequest(`${commonUrl}/common/common/region/region`, params);
};

// 获取IM接口前缀
export function getIMDetail() {
  return getRequest(`${commonUrl}/common/common/IM`);
}

//获取图片logo
export function getBaseSite() {
  return getRequest(`${commonUrl}/common/common/site`);
}

// 上传文件
export const upLoadFileMethods = (bold) => {
  console.log(bold)
  return uploadFileRequest(uploadFile, bold);
};
