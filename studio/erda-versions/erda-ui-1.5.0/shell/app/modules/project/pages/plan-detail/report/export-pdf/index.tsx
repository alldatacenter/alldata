// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { message } from 'antd';
import { loadJsFile } from 'common/utils';
import i18n from 'i18n';

const domToImageSrc = '/static/dom-to-image.min.js';
const jsPdfSrc = '/static/jspdf.min.js';

interface IProps {
  domId: string;
  children: Function;
  tip?: string;
  onFinish?: Function;
}
/**
 * @usage
 * <ExportPdf domId="report-page" tip="测试报告">
 *   <Icon type="upload" />导出报告</span>
 * </ExportPdf>
 */
const ExportPdf = ({ domId, children, tip = 'pdf', onFinish }: IProps) => {
  const exportPdf = async () => {
    const page: Element | null = document.getElementById(domId);
    if (!page) {
      message.error(`${i18n.t('dop:there is no need to export')}${tip}！`);
      return;
    }
    // XXX 2020/8/30 修复导出测试报告包含图片报错（临时方案）
    const mdContent = page.getElementsByClassName('md-content')?.[0];
    if (mdContent) {
      const str = mdContent.innerHTML;
      const matchImgTag = /<img.*?src=['"](.*?)['"].*?>/gi;
      const matchATag = /<a.*?href=['"](.*?)['"].*?>(.*?)<\/a>/gi;
      let content = str.replace(matchImgTag, (_item, src) => {
        return `<p>![image](${src})</p>`;
      });
      content = content.replace(matchATag, (_item, url, text) => {
        return `<p>![${text || 'link'}](${url})</p>`;
      });
      mdContent.innerHTML = content;
    }
    const exportLoading = message.loading(`${i18n.t('dop:exporting')}: ${tip}...`, 0);
    await loadJsFile(domToImageSrc);
    await loadJsFile(jsPdfSrc);
    const domToImage = window.domtoimage;
    const JsPdf = window.jsPDF;
    const pageWidth = (page.clientWidth * 3) / 4; // pt 和 px 之前的换算
    const pageHeight = (page.clientHeight * 3) / 4; // pt 和 px 之前的换算
    domToImage
      .toPng(page)
      .then((imageData: any) => {
        exportLoading();
        const pdf = new JsPdf({
          orientation: 'p',
          unit: 'pt',
          format: [pageHeight + 40, pageWidth + 40], // 添加20的边距
        });
        pdf.addImage(imageData, 'PNG', 20, 20, pageWidth, pageHeight);
        pdf.save(`${tip}.pdf`);
      })
      .then(() => {
        onFinish?.();
      })
      .catch(() => {
        message.error(i18n.t('dop:sorry, export failed!'));
      })
      .finally(() => {
        mdContent.innerHTML = str;
      });
  };

  if (typeof children !== 'function') {
    return i18n.t('dop:please pass in the method as the children of the exportPdf component');
  }

  return children({ exportPdf });
};

export default ExportPdf;
