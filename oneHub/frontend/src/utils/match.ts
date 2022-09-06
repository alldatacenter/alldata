/**
 * 修改Antd 的默认加载图标，改为按需加载，减小打包体积 使用Antd默认图标之前需要在此文件引用
 * webpack alias 添加:
 * '@ant-design/icons/lib/index.es': path.resolve(__dirname, '..', 'src/utils/match')
 */

// 链接：https://www.zhihu.com/question/308898834/answer/573515745

// export {
//   AuditOutlined,
//   BarChartOutlined,
//   SortDescendingOutlined,
//   LogoutOutlined,
//   SettingOutlined,
//   UserOutlined,
//   BellOutlined,
//   ExclamationCircleOutlined,
//   SearchOutlined,
//   PlusOutlined,
//   InfoCircleOutlined,
//   UploadOutlined,
//   PieChartOutlined,
//   AlertOutlined,
//   LinkOutlined,
//   ProjectOutlined,
//   FileOutlined,
//   WechatOutlined,
//   CaretDownOutlined,
//   CaretUpOutlined,
//   UpOutlined,
//   DownOutlined,
//   EyeOutlined,
//   SmileOutlined,
//   CloseOutlined,
//   LockOutlined,
// } from '@ant-design/icons/lib'
