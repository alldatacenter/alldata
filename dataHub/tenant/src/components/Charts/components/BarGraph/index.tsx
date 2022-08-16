import React, { useEffect } from 'react'
import Chart from '@antv/g2/lib/chart/chart'
import { ChartDProps } from '@/components/Charts/data'

const defaultData = [
  { lab_x: '巴西', lab_y: 18203 },
  { lab_x: '印尼', lab_y: 23489 },
  { lab_x: '美国', lab_y: 29034 },
  { lab_x: '印度', lab_y: 104970 },
  { lab_x: '中国', lab_y: 131744 },
]

export default ({ width = 500, height = 500, data = defaultData }: ChartDProps) => {
  let chart: Chart

  const renderChart = () => {
    chart = new Chart({
      container: 'barGraph-component',
      autoFit: false,
      width,
      height,
    })

    chart.data(data)
    chart.scale('lab_y', { nice: true })
    chart.coordinate().transpose()
    chart.tooltip({
      showMarkers: false,
    })
    chart.interaction('active-region')
    chart.interval().position('lab_x*lab_y')
    chart.render()
  }

  useEffect(() => {
    renderChart()
  }, [])

  return <div id="barGraph-component" />
}
