import React, { Component } from 'react';
import JSXRender from "../../JSXRender";
import properties from '../../../../../properties';
import ReactMarkdown from 'react-markdown'
import "./index.less";


export default class MarkdownComp extends Component {
  // remarkPlugins={[remarkGfm]}
  render() {
    let { widgetConfig = {}, widgetData = '' } = this.props;
    let { paddingNumTop = 20, paddingNumLeft = 20 } = widgetConfig;
    let initData = "请配置markdown文档"
    let finalData = widgetData || initData
    return <div className="markdown-react" style={{ paddingTop: (paddingNumTop && Number(paddingNumTop)) || 20, paddingLeft: (paddingNumLeft && Number(paddingNumLeft)) || 20, paddingRight: (paddingNumLeft && Number(paddingNumLeft)) || 20 }}>
      <ReactMarkdown escapeHtml={false} source={finalData} />
    </div>
  }
}
