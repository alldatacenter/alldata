const export_: LComponentsExport = {
  type: '导出类型',
  types: {
    csv: 'CSV',
    json: 'JSON',
    xlsx: 'Excel',
  },
  exporting: {
    csv: '正在导出 CSV 文件, 请稍候...',
    json: '正在导出 JSON 文件, 请稍候...',
    xlsx: '正在导出 Excel 文件, 请稍候...',
  },
  status: {
    exporting: '正在导出...',
  }
};
export default export_;
