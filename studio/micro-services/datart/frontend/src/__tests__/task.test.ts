/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import i18n from 'i18next';
import getQueryData from '../task';

describe('Test getQueryData', () => {
  i18n.changeLanguage('zh', (err, t) => {
    if (err) return console.log('something went wrong loading', err);
    t('key'); // -> same as i18next.t
  });
  test('should get Chart Query data', () => {
    const dataStr = JSON.stringify({
      createBy: 'ef35a5652acb43179624207a4da17399',
      createTime: '2022-07-20 10:39:25',
      description: null,
      download: true,
      id: '71a5933b21fe4611b71fee13baadffe8',
      index: 39.5,
      name: 'age',
      orgId: 'bffcb3ba6a5b4f0bbbfd97482ed8df5b',
      parentId: null,
      permission: null,
      queryVariables: [],
      status: 1,
      thumbnail: null,
      updateBy: 'ef35a5652acb43179624207a4da17399',
      updateTime: '2022-08-05 17:17:48',
      config:
        '{"aggregation":true,"chartConfig":{"datas":[{"label":"dimension","key":"dimension","required":true,"type":"group","limit":[0,1],"drillable":true,"rows":[{"uid":"805f046b-f141-4bee-adef-a0fdc5f8165f","colName":"age","type":"STRING","subType":"UNCATEGORIZED","category":"field","children":[]}]},{"label":"metrics","key":"metrics","required":true,"rows":[{"uid":"e6acfc63-fd0d-4da9-a8d6-d862a0eb66ba","colName":"education","type":"STRING","subType":"UNCATEGORIZED","category":"field","children":[],"aggregate":"COUNT"},{"uid":"51eefa7f-4ac7-4693-9681-c2b19799e554","colName":"chartAge","type":"NUMERIC","category":"computedField","children":[],"aggregate":"SUM"}],"type":"aggregate","limit":[1,999]},{"label":"filter","key":"filter","type":"filter","allowSameField":true,"rows":[{"uid":"2857a0f3-3f66-41c7-9e25-2a8b1a326b51","colName":"age","type":"STRING","subType":"UNCATEGORIZED","category":"field","children":[],"aggregate":"NONE","filter":{"visibility":"hide","condition":{"name":"age","type":2,"value":[{"key":29,"label":29,"isSelected":true},{"key":38,"label":38,"isSelected":true},{"key":10,"label":10,"isSelected":true},{"key":26,"label":26,"isSelected":true},{"key":1,"label":1,"isSelected":true}],"visualType":"STRING","operator":"IN"},"width":"auto"}}]},{"label":"colorize","key":"color","type":"color","limit":[0,1]},{"label":"info","key":"info","type":"info"}],"styles":[{"label":"bar.title","key":"bar","comType":"group","rows":[{"label":"common.borderStyle","key":"borderStyle","value":{"type":"solid","width":0,"color":"#ced4da"},"comType":"line","rows":[]},{"label":"bar.radius","key":"radius","comType":"inputNumber","rows":[]},{"label":"bar.width","key":"width","value":0,"comType":"inputNumber","rows":[]},{"label":"bar.gap","key":"gap","value":0.1,"comType":"inputPercentage","rows":[]}]},{"label":"label.title","key":"label","comType":"group","rows":[{"label":"label.showLabel","key":"showLabel","value":true,"comType":"checkbox","rows":[]},{"label":"label.position","key":"position","value":"top","comType":"labelPosition","rows":[]},{"label":"viz.palette.style.font","key":"font","value":{"fontFamily":"PingFang SC","fontSize":"12","fontWeight":"normal","fontStyle":"normal","color":"#495057"},"comType":"font","rows":[]}]},{"label":"legend.title","key":"legend","comType":"group","rows":[{"label":"legend.showLegend","key":"showLegend","value":true,"comType":"checkbox","rows":[]},{"label":"legend.type","key":"type","value":"scroll","comType":"legendType","rows":[]},{"label":"legend.selectAll","key":"selectAll","value":true,"comType":"checkbox","rows":[]},{"label":"legend.position","key":"position","value":"right","comType":"legendPosition","rows":[]},{"label":"legend.height","key":"height","value":0,"comType":"inputNumber","rows":[]},{"label":"viz.palette.style.font","key":"font","value":{"fontFamily":"PingFang SC","fontSize":"12","fontWeight":"normal","fontStyle":"normal","color":"#495057"},"comType":"font","rows":[]}]},{"label":"xAxis.title","key":"xAxis","comType":"group","rows":[{"label":"common.showAxis","key":"showAxis","value":true,"comType":"checkbox","rows":[]},{"label":"common.inverseAxis","key":"inverseAxis","comType":"checkbox","rows":[]},{"label":"common.lineStyle","key":"lineStyle","value":{"type":"solid","width":1,"color":"#ced4da"},"comType":"line","rows":[]},{"label":"common.showLabel","key":"showLabel","value":true,"comType":"checkbox","rows":[]},{"label":"viz.palette.style.font","key":"font","value":{"fontFamily":"PingFang SC","fontSize":"12","fontWeight":"normal","fontStyle":"normal","color":"#495057"},"comType":"font","rows":[]},{"label":"common.rotate","key":"rotate","value":0,"comType":"inputNumber","rows":[]},{"label":"common.showInterval","key":"showInterval","value":false,"comType":"checkbox","rows":[]},{"label":"common.overflow","key":"overflow","value":"break","comType":"select","rows":[]},{"label":"common.interval","key":"interval","value":0,"comType":"inputNumber","rows":[]},{"label":"common.dataZoomPanel","key":"dataZoomPanel","comType":"dataZoomPanel","rows":[]}]},{"label":"yAxis.title","key":"yAxis","comType":"group","rows":[{"label":"common.showAxis","key":"showAxis","value":true,"comType":"checkbox","rows":[]},{"label":"common.inverseAxis","key":"inverseAxis","value":false,"comType":"checkbox","rows":[]},{"label":"common.lineStyle","key":"lineStyle","value":{"type":"solid","width":1,"color":"#ced4da"},"comType":"line","rows":[]},{"label":"common.showLabel","key":"showLabel","value":true,"comType":"checkbox","rows":[]},{"label":"viz.palette.style.font","key":"font","value":{"fontFamily":"PingFang SC","fontSize":"12","fontWeight":"normal","fontStyle":"normal","color":"#495057"},"comType":"font","rows":[]},{"label":"common.showTitleAndUnit","key":"showTitleAndUnit","value":true,"comType":"checkbox","rows":[]},{"label":"common.unitFont","key":"unitFont","value":{"fontFamily":"PingFang SC","fontSize":"12","fontWeight":"normal","fontStyle":"normal","color":"#495057"},"comType":"font","rows":[]},{"label":"common.nameLocation","key":"nameLocation","value":"center","comType":"nameLocation","rows":[]},{"label":"common.nameRotate","key":"nameRotate","value":90,"comType":"inputNumber","rows":[]},{"label":"common.nameGap","key":"nameGap","value":20,"comType":"inputNumber","rows":[]},{"label":"common.min","key":"min","comType":"inputNumber","rows":[]},{"label":"common.max","key":"max","comType":"inputNumber","rows":[]},{"label":"yAxis.open","key":"modal","comType":"group","rows":[{"label":"yAxis.open","key":"YAxisNumberFormat","comType":"YAxisNumberFormatPanel","rows":[]}]}]},{"label":"splitLine.title","key":"splitLine","comType":"group","rows":[{"label":"splitLine.showHorizonLine","key":"showHorizonLine","value":true,"comType":"checkbox","rows":[]},{"label":"common.lineStyle","key":"horizonLineStyle","value":{"type":"dashed","width":1,"color":"#ced4da"},"comType":"line","rows":[]},{"label":"splitLine.showVerticalLine","key":"showVerticalLine","value":true,"comType":"checkbox","rows":[]},{"label":"common.lineStyle","key":"verticalLineStyle","value":{"type":"dashed","width":1,"color":"#ced4da"},"comType":"line","rows":[]}]},{"label":"viz.palette.style.margin.title","key":"margin","comType":"group","rows":[{"label":"viz.palette.style.margin.containLabel","key":"containLabel","value":true,"comType":"checkbox","rows":[]},{"label":"viz.palette.style.margin.left","key":"marginLeft","value":"5%","comType":"marginWidth","rows":[]},{"label":"viz.palette.style.margin.right","key":"marginRight","value":"5%","comType":"marginWidth","rows":[]},{"label":"viz.palette.style.margin.top","key":"marginTop","value":"5%","comType":"marginWidth","rows":[]},{"label":"viz.palette.style.margin.bottom","key":"marginBottom","value":"5%","comType":"marginWidth","rows":[]}]}],"settings":[{"label":"viz.palette.setting.paging.title","key":"paging","comType":"group","rows":[{"label":"viz.palette.setting.paging.pageSize","key":"pageSize","value":100,"comType":"inputNumber","rows":[]}]},{"label":"reference.title","key":"reference","comType":"group","rows":[{"label":"reference.open","key":"panel","comType":"reference","rows":[]}]}],"interactions":[{"label":"drillThrough.title","key":"drillThrough","value":false,"comType":"checkboxModal","rows":[{"label":"drillThrough.title","key":"setting","comType":"interaction.drillThrough","rows":[]}]},{"label":"viewDetail.title","key":"viewDetail","value":true,"comType":"checkboxModal","rows":[{"label":"viewDetail.title","key":"setting","value":{"event":"left","mapper":"all"},"comType":"interaction.viewDetail","rows":[]}]}]},"chartGraphId":"cluster-column-chart","computedFields":[{"expression":"[age]","category":"computedField","name":"chartAge","type":"NUMERIC"},{"expression":"[birthday]","category":"computedField","name":"chart birthday","type":"DATE"}]}',
      view: {
        config:
          '{"concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0,"expensiveQuery":false,"version":"1.0.0-beta.2"}',
        createBy: 'ef35a5652acb43179624207a4da17399',
        createTime: '2021-11-19 11:05:17',
        description: null,
        id: 'e83684bc8e92431dbfb164195abe443e',
        index: 8,
        isFolder: false,
        model:
          '{"hierarchy":{"日期":{"name":"日期","type":"STRING","role":"hierachy","children":[{"index":0,"type":"DATE","category":"UNCATEGORIZED","name":"birthday","path":["birthday"]}],"index":0},"age":{"index":1,"type":"STRING","category":"UNCATEGORIZED","name":"age","path":["age"]},"city":{"index":2,"type":"STRING","category":"UNCATEGORIZED","name":"city","path":["city"]},"education":{"index":3,"type":"STRING","category":"UNCATEGORIZED","name":"education","path":["education"]},"id":{"index":4,"type":"STRING","category":"UNCATEGORIZED","name":"id","path":["id"]},"married":{"index":5,"type":"STRING","category":"UNCATEGORIZED","name":"married","path":["married"]},"name":{"index":6,"type":"STRING","category":"UNCATEGORIZED","name":"name","path":["name"]},"nation":{"index":7,"type":"STRING","category":"UNCATEGORIZED","name":"nation","path":["nation"]},"sex":{"index":8,"type":"STRING","category":"UNCATEGORIZED","name":"sex","path":["sex"]},"salary":{"index":9,"type":"NUMERIC","category":"UNCATEGORIZED","name":"salary","path":["salary"]}},"columns":{"city":{"name":["city"],"type":"STRING","category":"UNCATEGORIZED"},"birthday":{"name":["birthday"],"type":"DATE","category":"UNCATEGORIZED"},"married":{"name":["married"],"type":"STRING","category":"UNCATEGORIZED"},"age":{"name":["age"],"type":"STRING","category":"UNCATEGORIZED"},"name":{"name":["name"],"type":"STRING","category":"UNCATEGORIZED"},"salary":{"name":["salary"],"type":"NUMERIC","category":"UNCATEGORIZED"},"education":{"name":["education"],"type":"STRING","category":"UNCATEGORIZED"},"nation":{"name":["nation"],"type":"STRING","category":"UNCATEGORIZED"},"sex":{"name":["sex"],"type":"STRING","category":"UNCATEGORIZED"},"id":{"name":["id"],"type":"STRING","category":"UNCATEGORIZED"}},"version":"1.0.0-beta.4"}',
        name: '学生信息',
        orgId: 'bffcb3ba6a5b4f0bbbfd97482ed8df5b',
        parentId: null,
        permission: null,
        script: 'SELECT * FROM root',
        sourceId: '0bd1fcafb94a424e916d54fcd414f4ff',
        status: 1,
        type: null,
        updateBy: 'ef35a5652acb43179624207a4da17399',
        updateTime: '2022-07-20 18:26:38',
        viewId: 'e83684bc8e92431dbfb164195abe443e',
      },
    });
    const params = getQueryData('chart', dataStr);

    expect(JSON.parse(params).downloadParams).toEqual([
      {
        cache: false,
        cacheExpires: 0,
        concurrencyControl: true,
        concurrencyControlMode: 'DIRTYREAD',
        viewId: 'e83684bc8e92431dbfb164195abe443e',
        aggregators: [
          {
            alias: 'COUNT(education)',
            column: ['education'],
            sqlOperator: 'COUNT',
          },
          { alias: 'SUM(chartAge)', column: ['chartAge'], sqlOperator: 'SUM' },
        ],
        groups: [{ alias: 'age', column: ['age'] }],
        filters: [
          {
            aggOperator: null,
            column: ['age'],
            sqlOperator: 'IN',
            values: [
              { value: 29, valueType: 'STRING' },
              { value: 38, valueType: 'STRING' },
              { value: 10, valueType: 'STRING' },
              { value: 26, valueType: 'STRING' },
              { value: 1, valueType: 'STRING' },
            ],
          },
        ],
        orders: [],
        pageInfo: { countTotal: false, pageSize: 100 },
        functionColumns: [{ alias: 'chartAge', snippet: '[age]' }],
        columns: [],
        script: false,
      },
    ]);
  });

  test('should get Chart Query data has computed', () => {
    const dataStr = JSON.stringify({
      config:
        '{"aggregation":true,"chartConfig":{"datas":[{"actions":{"NUMERIC":["aggregate","alias","format","sortable"],"STRING":["alias","sortable"],"DATE":["alias","sortable","dateLevel"]},"label":"mixed","key":"mixed","required":true,"type":"mixed","rows":[{"uid":"8ee5af74-3927-433f-806c-21469eafcdbd","field":"root.birthday","colName":"root.birthday@date_level_delimiter@AGG_DATE_YEAR","type":"DATE","category":"dateLevelComputedField","expression":"AGG_DATE_YEAR([root].[birthday])"},{"uid":"ac82d0ef-b162-4ec3-a534-4c48229c448b","colName":"root.age","type":"STRING","category":"field","children":[]},{"uid":"4090e82e-cc8a-4af1-b664-191a14067549","colName":"viewComputerField_age","type":"NUMERIC","category":"computedField","children":[],"aggregate":"SUM"},{"uid":"52a13c93-a529-4f9b-889c-9ec5c3825a79","colName":"root.salary","type":"NUMERIC","category":"field","children":[],"aggregate":"SUM"}]},{"label":"filter","key":"filter","type":"filter","disableAggregate":true}],"styles":[{"label":"header.title","key":"header","comType":"group","rows":[{"label":"header.open","key":"modal","comType":"group","options":{"type":"modal","modalSize":"middle"},"rows":[{"label":"header.styleAndGroup","key":"tableHeaders","comType":"tableHeader"}]}]},{"label":"column.conditionalStyle","key":"column","comType":"group","rows":[{"label":"column.open","key":"modal","comType":"group","options":{"type":"modal","modalSize":"middle"},"rows":[{"label":"column.list","key":"list","comType":"listTemplate","rows":[],"options":{},"template":{"label":"column.listItem","key":"listItem","comType":"group","rows":[{"label":"column.columnStyle","key":"columnStyle","comType":"group","options":{"expand":true},"rows":[{"label":"column.useColumnWidth","key":"useColumnWidth","default":false,"comType":"checkbox"},{"label":"column.columnWidth","key":"columnWidth","default":100,"options":{"min":0},"watcher":{"deps":["useColumnWidth"]},"comType":"inputNumber"},{"label":"style.align","key":"align","default":"default","comType":"fontAlignment","options":{"translateItemLabel":true,"items":[{"label":"@global@.style.alignDefault","value":"default"},{"label":"viz.common.enum.fontAlignment.left","value":"left"},{"label":"viz.common.enum.fontAlignment.center","value":"center"},{"label":"viz.common.enum.fontAlignment.right","value":"right"}]}}]},{"label":"column.conditionalStyle","key":"conditionalStyle","comType":"group","options":{"expand":true},"rows":[{"label":"column.conditionalStylePanel","key":"conditionalStylePanel","comType":"conditionalStylePanel"}]}]}}]}]},{"label":"style.title","key":"style","comType":"group","rows":[{"label":"style.enableFixedHeader","key":"enableFixedHeader","default":true,"comType":"checkbox","value":true},{"label":"style.enableBorder","key":"enableBorder","default":true,"comType":"checkbox","value":true},{"label":"style.enableRowNumber","key":"enableRowNumber","default":false,"comType":"checkbox","value":false},{"label":"style.leftFixedColumns","key":"leftFixedColumns","default":0,"options":{"min":0},"comType":"inputNumber","value":0},{"label":"style.rightFixedColumns","key":"rightFixedColumns","default":0,"options":{"min":0},"comType":"inputNumber","value":0},{"label":"style.autoMergeFields","key":"autoMergeFields","comType":"select","options":{"mode":"multiple"}},{"label":"style.tableSize","key":"tableSize","default":"small","comType":"select","options":{"translateItemLabel":true,"items":[{"label":"@global@.tableSize.default","value":"default"},{"label":"@global@.tableSize.middle","value":"middle"},{"label":"@global@.tableSize.small","value":"small"}]},"value":"small"}]},{"label":"style.tableHeaderStyle","key":"tableHeaderStyle","comType":"group","rows":[{"label":"common.backgroundColor","key":"bgColor","default":"#f8f9fa","comType":"fontColor","value":"#f8f9fa"},{"label":"style.font","key":"font","comType":"font","default":{"fontFamily":"-apple-system, \\"Segoe UI\\", Roboto, \\"Helvetica Neue\\", Arial, \\"Noto Sans\\", sans-serif, \\"Apple Color Emoji\\", \\"Segoe UI Emoji\\", \\"Segoe UI Symbol\\", \\"Noto Color Emoji\\"","fontSize":12,"fontWeight":"bold","fontStyle":"normal","color":"#495057"},"value":{"fontFamily":"-apple-system, \\"Segoe UI\\", Roboto, \\"Helvetica Neue\\", Arial, \\"Noto Sans\\", sans-serif, \\"Apple Color Emoji\\", \\"Segoe UI Emoji\\", \\"Segoe UI Symbol\\", \\"Noto Color Emoji\\"","fontSize":12,"fontWeight":"bold","fontStyle":"normal","color":"#495057"}},{"label":"style.align","key":"align","default":"left","comType":"fontAlignment","value":"left"}]},{"label":"style.tableBodyStyle","key":"tableBodyStyle","comType":"group","rows":[{"label":"style.oddFontColor","key":"oddFontColor","default":"#000","comType":"fontColor","value":"#000"},{"label":"style.oddBgColor","key":"oddBgColor","default":"rgba(0,0,0,0)","comType":"fontColor","value":"rgba(0,0,0,0)"},{"label":"style.evenFontColor","key":"evenFontColor","default":"#000","comType":"fontColor","value":"#000"},{"label":"style.evenBgColor","key":"evenBgColor","default":"rgba(0,0,0,0)","comType":"fontColor","value":"rgba(0,0,0,0)"},{"label":"style.fontSize","key":"fontSize","comType":"fontSize","default":12,"value":12},{"label":"style.fontFamily","key":"fontFamily","comType":"fontFamily","default":"-apple-system, \\"Segoe UI\\", Roboto, \\"Helvetica Neue\\", Arial, \\"Noto Sans\\", sans-serif, \\"Apple Color Emoji\\", \\"Segoe UI Emoji\\", \\"Segoe UI Symbol\\", \\"Noto Color Emoji\\"","value":"-apple-system, \\"Segoe UI\\", Roboto, \\"Helvetica Neue\\", Arial, \\"Noto Sans\\", sans-serif, \\"Apple Color Emoji\\", \\"Segoe UI Emoji\\", \\"Segoe UI Symbol\\", \\"Noto Color Emoji\\""},{"label":"style.fontWeight","key":"fontWeight","comType":"fontWeight","default":"normal","value":"normal"},{"label":"style.fontStyle","key":"fontStyle","comType":"fontStyle","default":"normal","value":"normal"},{"label":"style.align","key":"align","default":"default","comType":"fontAlignment","options":{"translateItemLabel":true,"items":[{"label":"@global@.style.alignDefault","value":"default"},{"label":"viz.common.enum.fontAlignment.left","value":"left"},{"label":"viz.common.enum.fontAlignment.center","value":"center"},{"label":"viz.common.enum.fontAlignment.right","value":"right"}]},"value":"default"}]}],"settings":[{"label":"paging.title","key":"paging","comType":"group","rows":[{"label":"paging.enablePaging","key":"enablePaging","default":true,"comType":"checkbox","options":{"needRefresh":true},"value":true},{"label":"paging.pageSize","key":"pageSize","default":100,"comType":"inputNumber","options":{"needRefresh":true,"step":1,"min":0},"watcher":{"deps":["enablePaging"]},"value":100}]},{"label":"summary.title","key":"summary","comType":"group","rows":[{"label":"summary.aggregateFields","key":"aggregateFields","comType":"select","options":{"mode":"multiple"}},{"label":"common.backgroundColor","key":"summaryBcColor","default":"rgba(0, 0, 0, 0)","comType":"fontColor","value":"rgba(0, 0, 0, 0)"},{"label":"viz.palette.style.font","key":"summaryFont","comType":"font","default":{"fontFamily":"-apple-system, \\"Segoe UI\\", Roboto, \\"Helvetica Neue\\", Arial, \\"Noto Sans\\", sans-serif, \\"Apple Color Emoji\\", \\"Segoe UI Emoji\\", \\"Segoe UI Symbol\\", \\"Noto Color Emoji\\"","fontSize":"14","fontWeight":"normal","fontStyle":"normal","color":"black"},"value":{"fontFamily":"-apple-system, \\"Segoe UI\\", Roboto, \\"Helvetica Neue\\", Arial, \\"Noto Sans\\", sans-serif, \\"Apple Color Emoji\\", \\"Segoe UI Emoji\\", \\"Segoe UI Symbol\\", \\"Noto Color Emoji\\"","fontSize":"14","fontWeight":"normal","fontStyle":"normal","color":"black"}}]}],"interactions":[{"label":"drillThrough.title","key":"drillThrough","comType":"checkboxModal","default":false,"options":{"modalSize":"middle"},"rows":[{"label":"drillThrough.title","key":"setting","comType":"interaction.drillThrough"}],"value":false},{"label":"viewDetail.title","key":"viewDetail","comType":"checkboxModal","default":false,"options":{"modalSize":"middle"},"rows":[{"label":"viewDetail.title","key":"setting","comType":"interaction.viewDetail"}],"value":false}],"i18ns":[{"lang":"zh-CN","translation":{"common":{"backgroundColor":"背景颜色"},"header":{"title":"表头分组","open":"打开","styleAndGroup":"表头分组"},"column":{"open":"打开样式设置","list":"字段列表","sortAndFilter":"排序与过滤","enableSort":"开启列排序","basicStyle":"基础样式","useColumnWidth":"启用固定列宽","columnWidth":"列宽","columnStyle":"列样式","columnStylePanel":"列样式配置器","conditionalStyle":"条件样式","conditionalStylePanel":"条件样式配置器","align":"对齐方式","enableFixedCol":"开启固定列宽","fixedColWidth":"固定列宽度设置","font":"字体与样式"},"style":{"title":"表格样式","enableFixedHeader":"固定表头","enableBorder":"显示边框","enableRowNumber":"启用行号","leftFixedColumns":"左侧固定列","rightFixedColumns":"右侧固定列","autoMergeFields":"自动合并列内容","tableSize":"表格大小","tableHeaderStyle":"表头样式","tableBodyStyle":"表体样式","bgColor":"背景颜色","font":"字体","align":"对齐方式","alignDefault":"默认","fontWeight":"字体粗细","fontFamily":"字体","oddBgColor":"奇行背景色","oddFontColor":"奇行字体色","evenBgColor":"偶行背景色","evenFontColor":"偶行字体色","fontSize":"字体大小","fontStyle":"字体样式"},"tableSize":{"default":"默认","middle":"中","small":"小"},"summary":{"title":"数据汇总","aggregateFields":"汇总列"},"paging":{"title":"常规","enablePaging":"启用分页","pageSize":"每页行数"}}},{"lang":"en-US","translation":{"common":{"backgroundColor":"Background Color"},"header":{"title":"Table Header Group","open":"Open","styleAndGroup":"Header Group"},"column":{"open":"Open Style Setting","list":"Field List","sortAndFilter":"Sort and Filter","enableSort":"Enable Sort","basicStyle":"Baisc Style","useColumnWidth":"Use Column Width","columnWidth":"Column Width","columnStyle":"Column Style","columnStylePanel":"Column Style Panel","conditionalStyle":"Conditional Style","conditionalStylePanel":"Conditional Style Panel","align":"Align","enableFixedCol":"Enable Fixed Column","fixedColWidth":"Fixed Column Width","font":"Font and Style"},"style":{"title":"Table Style","enableFixedHeader":"Enable Fixed Header","enableBorder":"Show Border","enableRowNumber":"Enable Row Number","leftFixedColumns":"LeftFixed Columns","rightFixedColumns":"Right Fixed Columns","autoMergeFields":"Auto Merge Column Content","tableSize":"Table Size","tableHeaderStyle":"Table Header Style","tableBodyStyle":"Table Body Style","font":"Font","align":"Align","alignDefault":"Default","fontWeight":"Font Weight","fontFamily":"Font Family","oddBgColor":"Odd Row Background Color","evenBgColor":"Even Row Background Color","oddFontColor":"Odd Row Font Color","evenFontColor":"Even Row Font Color","fontSize":"Font Size","fontStyle":"Font Style"},"tableSize":{"default":"Default","middle":"Middle","small":"Small"},"summary":{"title":"Summary","aggregateFields":"Summary Fields"},"paging":{"title":"Paging","enablePaging":"Enable Paging","pageSize":"Page Size"}}}]},"chartGraphId":"mingxi-table","computedFields":[{"category":"dateLevelComputedField","name":"root.birthday（Year）","type":"DATE","expression":"AGG_DATE_YEAR([root].[birthday])"}]}',
      createBy: '06b6305528694968bc7085ecdabe4a5a',
      createTime: '2022-07-25 11:40:30',
      description: null,
      download: true,
      id: '936d21480bd1464ea11f4c0278a61e0c',
      index: 0,
      name: 'RC0_测试所有的计算字段',
      orgId: '5f55705f4aee49c58fa2318e69141d1a',
      parentId: '216f890fa11b4d0c80342b8fdde03a09',
      permission: null,
      queryVariables: [],
      status: 1,
      thumbnail: null,
      updateBy: '06b6305528694968bc7085ecdabe4a5a',
      updateTime: '2022-07-25 11:40:31',
      viewId: '6236737d64954c8bac6cbaf478e17ff2',
      view: {
        config:
          '{"version":"1.0.0-RC.0","concurrencyControl":true,"concurrencyControlMode":"DIRTYREAD","cache":false,"cacheExpires":0,"expensiveQuery":false}',
        createBy: '06b6305528694968bc7085ecdabe4a5a',
        createTime: '2022-07-22 11:08:21',
        description: null,
        id: '6236737d64954c8bac6cbaf478e17ff2',
        index: 0,
        isFolder: false,
        model:
          '{"version":"1.0.0-RC.0","columns":{"root.education":{"name":["[\\"root\\",\\"education\\"]"],"type":"STRING","category":"UNCATEGORIZED"},"root.id":{"name":["[\\"root\\",\\"id\\"]"],"type":"STRING","category":"UNCATEGORIZED"},"root.name":{"name":["[\\"root\\",\\"name\\"]"],"type":"STRING","category":"UNCATEGORIZED"},"root.salary":{"name":["[\\"root\\",\\"salary\\"]"],"type":"NUMERIC","category":"UNCATEGORIZED"},"root.city":{"name":["[\\"root\\",\\"city\\"]"],"type":"STRING","category":"UNCATEGORIZED"},"root.birthday":{"name":["[\\"root\\",\\"birthday\\"]"],"type":"DATE","category":"UNCATEGORIZED"},"root.nation":{"name":["[\\"root\\",\\"nation\\"]"],"type":"STRING","category":"UNCATEGORIZED"},"root.married":{"name":["[\\"root\\",\\"married\\"]"],"type":"STRING","category":"UNCATEGORIZED"},"root.age":{"name":["[\\"root\\",\\"age\\"]"],"type":"STRING","category":"UNCATEGORIZED"},"root.sex":{"name":["[\\"root\\",\\"sex\\"]"],"type":"STRING","category":"UNCATEGORIZED"}},"hierarchy":{"root.education":{"name":"root.education","type":"STRING","category":"UNCATEGORIZED","path":["root","education"]},"root.id":{"name":"root.id","type":"STRING","category":"UNCATEGORIZED","path":["root","id"]},"root.name":{"name":"root.name","type":"STRING","category":"UNCATEGORIZED","path":["root","name"]},"root.salary":{"name":"root.salary","type":"NUMERIC","category":"UNCATEGORIZED","path":["root","salary"]},"root.city":{"name":"root.city","type":"STRING","category":"UNCATEGORIZED","path":["root","city"]},"root.birthday":{"name":"root.birthday","type":"DATE","category":"UNCATEGORIZED","path":["root","birthday"]},"root.nation":{"name":"root.nation","type":"STRING","category":"UNCATEGORIZED","path":["root","nation"]},"root.married":{"name":"root.married","type":"STRING","category":"UNCATEGORIZED","path":["root","married"]},"root.age":{"name":"root.age","type":"STRING","category":"UNCATEGORIZED","path":["root","age"]},"root.sex":{"name":"root.sex","type":"STRING","category":"UNCATEGORIZED","path":["root","sex"]}},"computedFields":[{"expression":"[root].[age]","category":"computedField","name":"viewComputerField_age","type":"NUMERIC","isViewComputedFields":true}]}',
        name: 'STRUCT_北京学生数据_root_7.22',
        orgId: '5f55705f4aee49c58fa2318e69141d1a',
        parentId: '24a4f646fd334205abdfe27e7227fd55',
        permission: null,
        script:
          '{"table":["root"],"columns":"\\"all\\"","joins":[],"sourceId":"d2c6a6e5e647415288029bbb313bb9fc"}',
        sourceId: 'd2c6a6e5e647415288029bbb313bb9fc',
        status: 1,
        type: 'STRUCT',
        updateBy: '06b6305528694968bc7085ecdabe4a5a',
        updateTime: '2022-07-22 11:11:23',
      },
    });
    const params = getQueryData('chart', dataStr);

    expect(JSON.parse(params).downloadParams).toEqual([
      {
        cache: false,
        cacheExpires: 0,
        concurrencyControl: true,
        concurrencyControlMode: 'DIRTYREAD',
        viewId: '6236737d64954c8bac6cbaf478e17ff2',
        aggregators: [
          {
            alias: 'SUM(viewComputerField_age)',
            column: ['viewComputerField_age'],
            sqlOperator: 'SUM',
          },
          {
            alias: 'SUM(root.salary)',
            column: ['root', 'salary'],
            sqlOperator: 'SUM',
          },
        ],
        groups: [
          {
            alias: 'root.birthday@date_level_delimiter@AGG_DATE_YEAR',
            column: ['root.birthday@date_level_delimiter@AGG_DATE_YEAR'],
          },
          { alias: 'root.age', column: ['root', 'age'] },
        ],
        filters: [],
        orders: [],
        pageInfo: { countTotal: true, pageSize: 100 },
        functionColumns: [
          {
            alias: 'root.birthday@date_level_delimiter@AGG_DATE_YEAR',
            snippet: 'AGG_DATE_YEAR([root].[birthday])',
          },
          { alias: 'viewComputerField_age', snippet: '[root].[age]' },
        ],
        columns: [],
        script: false,
      },
    ]);
  });
});
