import React, { useEffect } from 'react'
import Chart from '@antv/g2/lib/chart/chart'

import { ChartDProps } from '@/components/Charts/data'

const defaultData = [
  { lab_x: '事例一', lab_y: 0.4, lab_z: 40 },
  { lab_x: '事例二', lab_y: 0.21, lab_z: 21 },
  { lab_x: '事例三', lab_y: 0.17, lab_z: 17 },
  { lab_x: '事例四', lab_y: 0.13, lab_z: 13 },
  { lab_x: '事例五', lab_y: 0.09, lab_z: 9 },
]

export default ({ width = 500, height = 500, data = defaultData }: ChartDProps) => {
  let chart: Chart

  const renderChart = () => {
    chart = new Chart({
      container: 'pie-component',
      autoFit: false,
      width,
      height,
    })

    chart.data(data)
    chart.coordinate('theta', {
      radius: 0.5,
    })

    chart.scale('lab_y', {
      formatter: (val: number) => {
        return `${val * 100}%`
      },
    })

    chart.tooltip({
      showTitle: false,
      showMarkers: false,
    })

    chart
      .interval()
      .position('lab_y')
      .color('lab_x')
      .label('lab_y', {
        content: (i) => {
          return `${i.lab_x}: ${i.lab_y * 100}%`
        },
      })
      .adjust('stack')

    chart.interaction('element-active')
    chart.render()
  }

  useEffect(() => {
    renderChart()
  }, [data])

  return <div id="pie-component" />
}
