/* 
工具类
*/

// 生成十位字母加数字随机数
const getRandom = () => {
  // 生成十位字母加数字随机数
  let arr = [];
  for (let i = 0; i < 1000; i++) {
    let n = Math.random()
      .toString(36)
      .substr(2, 5);
    arr.push(n);
  }
  // 去重
  let ukeys = [];
  for (let i = 0; i < arr.length; i++) {
    if (ukeys.indexOf(arr[i]) === -1) {
      ukeys.push(arr[i]);
    }
  }
  let keys = "";
  for (let i = 0; i < ukeys.length; i++) {
    keys += ukeys[i];
  }
  return keys.substr(0, 5);
};

export default {
  getRandom // 生成十位字母加数字随机数
}