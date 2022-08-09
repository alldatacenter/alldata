//表单中必填
export function validateRequired(rule, value, callback) {
  if (value != void 0 || value != null) {
    callback();
  } else {
    return callback(new Error("必填项不能为空"));
  }
}

// 验证必填项
export function handleSubmit(that, name) {
  let flag = false;
  that.$refs[name].validate(valid => {
    if (valid) {
      flag = true;
      return flag;
    } else {
      that.$Message.error("请正确填写内容!");
      return flag;
    }
  });
  return flag
}
