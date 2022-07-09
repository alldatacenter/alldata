import * as regExp from "./regular";

//表单中必须输入汉字
export function validateNeedName(rule, value, callback) {
  const name = regExp.name;
  if (name.test(value)) {
    callback();
  } else {
    return callback(new Error("请输入汉字"));
  }
}

//表单中输入手机号验证
export function validateMobile(rule, value, callback) {
  const mobile = regExp.mobile;
  if (mobile.test(value)) {
    callback();
  } else {
    return callback(new Error("手机号输入错误！"));
  }
}
