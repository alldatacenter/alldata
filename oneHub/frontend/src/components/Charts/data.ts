export enum SpreadType {
  Volume = 'Volume',
  Post = 'Post',
  Read = 'Read',
  Comment = 'Comment',
  Forward = 'Forward',
  Like = 'Like',
}

export enum ChartType {
  Histogram = 'Histogram', // 柱状图
  BarGraph = 'BarGraph', // 条形图
  Pie = 'Pie', // 饼图
  Donut = 'Donut', // 环形图
  Polyline = 'Polyline', // 折线图
  WordCloud = 'WordCloud', // 词云
  Area = 'Area', // 面积图
}

// 布局
export enum ChartLayoutType {
  RichText = 'RichText', // 只有文本报告
  Canvas = 'Canvas', // 只有图表报告
  RowLayout = 'RowLayout', // 图表,文本 左右布局
  ColumnLayout = 'ColumnLayout', // 图表,文本 上下布局
}

export enum ChartTimeType {
  Day = 'Day',
  Week = 'Week',
  Month = 'Month',
  Customize = 'Customize',
}

export const spreads = [
  { text: '声量', value: SpreadType.Volume },
  { text: '发帖量', value: SpreadType.Post },
  { text: '阅读量', value: SpreadType.Read },
  { text: '评论量', value: SpreadType.Comment },
  { text: '转发量', value: SpreadType.Forward },
  { text: '点赞量', value: SpreadType.Like },
]

export const charts = [
  { text: '柱状图', value: ChartType.Histogram },
  { text: '条形图', value: ChartType.BarGraph },
  { text: '饼图', value: ChartType.Pie },
  { text: '环形图', value: ChartType.Donut },
  { text: '折线图', value: ChartType.Polyline },
]

export const layouts = [
  { text: '布局一', value: ChartLayoutType.RichText },
  { text: '布局二', value: ChartLayoutType.Canvas },
  { text: '布局三', value: ChartLayoutType.RowLayout },
  { text: '布局四', value: ChartLayoutType.ColumnLayout },
]

export const options = [
  { label: '24小时内', value: ChartTimeType.Day },
  { label: '7天内', value: ChartTimeType.Week },
  { label: '30天内', value: ChartTimeType.Month },
  { label: '自定义', value: ChartTimeType.Customize },
]

export interface ChartProps extends DataTypeProps {
  className?: string
  title: string // 标题
  hasChannel?: boolean // 是否显示分渠道checkbox
  hasHeader?: boolean // 是否显示头部过滤器
  graphicsType: string // 传播图表分析选择 - 柱状图 圆饼图
  layoutData?: string // 分析布局选择 单图表单报告,左右上下布局
}

export interface DataTypeProps {
  type: 'diffuse' | 'diffuseInc' | 'source' | 'wordCloud' // 渲染图表类型
}

export interface ChartSourceProps {
  table: SpreadTableProps
  chart: SpreadChartProps
}

export interface SpreadTableProps {
  diffuseTable: TableProps
  linksTable: TableProps
  viewsTable: TableProps
  commentsTable: TableProps
  repostsTable: TableProps
  attitudesTable: TableProps

  wordCloudTable: TableProps
}

export interface TableProps {
  table: ColProps[]
  titles: string[]
}

export interface SpreadChartProps {
  diffuseChart: LabelProps[] // 声量
  linksChart: LabelProps[] // 发帖量
  viewsChart: LabelProps[] // 阅读量
  commentsChart: LabelProps[] // 评论量
  repostsChart: LabelProps[] // 转发量
  attitudesChart: LabelProps[] // 点赞量
}

export interface ColProps {
  col0: number | string
  col1?: number | string
  col2?: number | string
}

export interface LabelProps {
  lab_x: string | number
  lab_y: string | number
  lab_z?: string | number
  url?: string
  desc?: string
}

export interface TableDataProps extends ColProps {
  key: string | number
}

export interface ChartComponentProps {
  width?: number
  height?: number
}

export interface DataItem {
  [field: string]: string | number | number[] | null | undefined
}

// 柱状图, 条形图, 饼图, 环形图, 折线图
export interface ChartDProps extends ChartComponentProps {
  data?: DataItem[]
}

// 词云图
export interface WordCloudData {
  value: number
  name: string | number
}
export interface WordCloudProps extends ChartComponentProps {
  data?: DataItem[]
}
