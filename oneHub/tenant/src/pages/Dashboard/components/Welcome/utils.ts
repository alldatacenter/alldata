export const getGrettings = () => {
  const hour = new Date().getHours()

  if (hour < 8) {
    return '早上好'
  }

  if (hour < 12) {
    return '上午好'
  }

  if (hour < 13) {
    return '中午好'
  }

  if (hour < 18) {
    return '下午好'
  }

  return '晚上好'
}
