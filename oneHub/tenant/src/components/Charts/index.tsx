
import React, { useEffect, useState } from 'react'
import { Card, Checkbox, Empty, Radio, Select, Skeleton, Table, DatePicker } from 'antd'
import { RadioChangeEvent } from 'antd/es/radio'
import { ColumnsType } from 'antd/es/table'
import { useQuery } from '@apollo/client'
import { useParams } from 'umi'
import BarGraph from '@/components/Charts/components/BarGraph'
import Donut from '@/components/Charts/components/Donut'
import Histogram from '@/components/Charts/components/Histogram'
import Area from '@/components/Charts/components/Area'
import Pie from '@/components/Charts/components/Pie'
import Polyline from '@/components/Charts/components/Polyline'
import WordCloud from '@/components/Charts/components/WordCloud'
import {
  ChartLayoutType,
  ChartProps,
  charts,
  ChartSourceProps,
  ChartTimeType,
  ChartType,
  layouts,
  options,
  spreads,
  SpreadType,
  TableDataProps,
  TableProps,
} from '@/components/Charts/data'
import { transformDate, getDay, getMonth, getWeek, mobileCheck } from '@/utils/utils'
import { QUERY_ANALYSE_CHART } from '@/pages/Database/Event/GQL'
import { EventAnalysisApi } from '@/types/queryAPI'
import classNames from 'classnames'
import isEmpty from 'lodash/isEmpty'
import moment from 'moment'

import styles from './index.less'

export default ({
  className,
  title = '传播声量趋势', // 标题
  type = 'diffuse',
  hasChannel = false, // 是否显示分渠道checkbox
  hasHeader = true, // 是否显示头部过滤器
  graphicsType, // 传播图表分析选择 - 柱状图 圆饼图
  layoutData = ChartLayoutType.RowLayout,
}: ChartProps) => {
  const { id }: { id: string } = useParams()
  const [spread, setSpread] = useState<string>(SpreadType.Volume)
  const [time, setTime] = useState<string>(ChartTimeType.Month)
  const [graphics, setGraphics] = useState<string>(graphicsType)
  const [layout, setLayout] = useState<string>(layoutData)
  const [single, setSingle] = useState<boolean>(true)

  const [chart, setChart] = useState<any>([])
  const [tabCols, setTabCols] = useState<ColumnsType<TableDataProps>>([])
  const [table, setTable] = useState<TableDataProps[]>([])

  const [startTime, setStartTime] = useState<string>(getMonth().startDate)
  const [endTime, setEndTime] = useState<string>(getMonth().endDate)
  const [dataType, setDataType] = useState<'diffuse' | 'diffuseInc' | 'source' | 'wordCloud'>(type)
  const [sourceName, setSourceName] = useState<string>('')

  const queryAnalysis = useQuery<EventAnalysisApi>(QUERY_ANALYSE_CHART, {
    variables: { id, startTime, endTime, dataType, sourceName, topN: 10 },
    fetchPolicy: 'no-cache',
  })

  useEffect(() => {
    if (mobileCheck()) {
      setLayout(ChartLayoutType.ColumnLayout)
    }
  }, [])

  // 处理报告表格数据
  const handleData = (dataSource: ChartSourceProps, filter: string) => {
    let newChart: any
    let object: TableProps

    const { table: TableRes, chart: ChartRes } = dataSource
    if (hasHeader) {
      const {
        diffuseTable,
        linksTable,
        viewsTable,
        commentsTable,
        repostsTable,
        attitudesTable,
      } = TableRes
      const {
        diffuseChart,
        linksChart,
        viewsChart,
        commentsChart,
        repostsChart,
        attitudesChart,
      } = ChartRes
      const array = [
        { text: SpreadType.Volume, chart: diffuseChart, table: diffuseTable },
        { text: SpreadType.Post, chart: linksChart, table: linksTable },
        { text: SpreadType.Read, chart: viewsChart, table: viewsTable },
        { text: SpreadType.Comment, chart: commentsChart, table: commentsTable },
        { text: SpreadType.Forward, chart: repostsChart, table: repostsTable },
        { text: SpreadType.Like, chart: attitudesChart, table: attitudesTable },
      ]
      const find = array.find((i) => i.text === filter) || array[0]
      const { chart: filterChart, table: filterTables } = find
      newChart = filterChart
      object = filterTables
    } else {
      newChart = ChartRes
      object = TableRes.wordCloudTable
    }

    const { table: filterTable, titles: filterTitle } = object
    const newTable = filterTable.map((i, key) => ({ ...i, key }))
    const columns: any = Object.keys(
      !isEmpty(filterTable) ? filterTable[0] : { col0: '', col1: '' },
    ).map((i, s) => ({
      title: filterTitle[s],
      dataIndex: String(i),
      align: 'center',
    }))

    return { columns, newTable, newChart }
  }

  useEffect(() => {
    if (queryAnalysis.loading || !queryAnalysis.data) return
    const { columns, newTable, newChart } = handleData(
      queryAnalysis.data?.eventAnalyseBasestat.data,
      spread,
    )
    setTabCols(columns)
    setTable(newTable)
    setChart(newChart)
  }, [queryAnalysis.loading, queryAnalysis.data, spread])

  useEffect(() => {
    if (ChartTimeType.Customize === time) return
    const timeArray = [
      { text: ChartTimeType.Day, value: getDay() },
      { text: ChartTimeType.Week, value: getWeek() },
      { text: ChartTimeType.Month, value: getMonth() },
    ]
    const { startDate, endDate } = (timeArray.find((i) => i.text === time) || timeArray[1]).value
    setStartTime(startDate)
    setEndTime(endDate)
  }, [time])

  useEffect(() => {
    if (hasChannel) {
      setGraphics(single ? ChartType.Histogram : ChartType.Polyline)
      setDataType(single ? 'diffuse' : 'diffuseInc')
      setSourceName(single ? '' : '新浪微博')
    }
  }, [single])

  const onRangPickerOK = (e: any) => {
    const [start, end] = e
    if (start && end) {
      setStartTime(transformDate(start))
      setEndTime(transformDate(end))
    }
  }

  const extraJSX = (() => {
    const cls = layout === ChartLayoutType.ColumnLayout ? styles.headerColumn : styles.headerRow
    const clazz = classNames(`${styles.chartExtra} ${cls}`)

    return (
      <section className={clazz}>
        <article className={styles.extraLeft}>
          <Radio.Group
            defaultValue={spread}
            value={spread}
            onChange={(e: RadioChangeEvent) => setSpread(e.target.value)}
          >
            {spreads.map((i) => (
              <Radio.Button key={i.text} value={i.value}>
                {i.text}
              </Radio.Button>
            ))}
          </Radio.Group>

          <Select
            className={styles.graphicsSelect}
            defaultValue={graphics}
            onChange={(value: string) => setGraphics(value)}
          >
            {charts.map((i) => (
              <Select.Option key={i.text} value={i.value}>
                {i.text}
              </Select.Option>
            ))}
          </Select>

          <Select
            className={styles.layoutSelect}
            defaultValue={layout}
            onChange={(value: string) => setLayout(value)}
          >
            {layouts.map((i) => (
              <Select.Option key={i.value} value={i.value}>
                {i.text}
              </Select.Option>
            ))}
          </Select>

          {hasChannel && (
            <Checkbox
              className={styles.checkbox}
              checked={single}
              onChange={(e) => setSingle(e.target.checked)}
            >
              {mobileCheck ? '分渠道' : '分渠道展示'}
            </Checkbox>
          )}
        </article>

        <div className={styles.extraRight}>
          <Radio.Group
            options={options}
            onChange={(e) => setTime(e.target.value)}
            value={time}
            optionType="button"
          />
          {time === ChartTimeType.Customize && (
            <DatePicker.RangePicker
              className={styles.rangePicker}
              showTime
              value={[moment(startTime), moment(endTime)]}
              format="YYYY-MM-DD HH:mm:ss"
              disabledDate={(current) => {
                return current > moment().endOf('day')
              }}
              onOk={(e) => onRangPickerOK(e)}
            />
          )}
        </div>
      </section>
    )
  })()

  const description = () => {
    const cls = layout === ChartLayoutType.ColumnLayout ? styles.column : styles.row
    const clazz = classNames(`${styles.chartDescription} ${cls}`)

    return (
      <section className={clazz}>
        {layout !== ChartLayoutType.RichText && (
          <article className={styles.chartContent}>
            {!isEmpty(chart) ? (
              <>
                {graphics === ChartType.Histogram && <Histogram data={chart} />}
                {graphics === ChartType.BarGraph && <BarGraph data={chart} />}
                {graphics === ChartType.Pie && <Pie data={chart} />}
                {graphics === ChartType.Polyline && <Polyline data={chart} />}
                {graphics === ChartType.Donut && <Donut data={chart} />}
                {graphics === ChartType.WordCloud && <WordCloud data={chart} />}
                {graphics === ChartType.Area && <Area data={chart} />}
              </>
            ) : (
              <Empty className={styles.emptyChart} description={false} />
            )}
          </article>
        )}
        {layout !== ChartLayoutType.Canvas && (
          <div className={styles.report}>
            <Table
              size="small"
              rowKey="key"
              columns={tabCols}
              dataSource={table}
              pagination={false}
              bordered={false}
              scroll={{ y: 500 }}
            />
          </div>
        )}
      </section>
    )
  }

  return (
    <main className={`${styles.main} ${className}`}>
      <Card title={title}>
        <Skeleton loading={queryAnalysis.loading} paragraph={{ rows: 14 }} active>
          {hasHeader && extraJSX}
          <Card.Meta avatar="" title="" description={description()} />
        </Skeleton>
      </Card>
    </main>
  )
}
