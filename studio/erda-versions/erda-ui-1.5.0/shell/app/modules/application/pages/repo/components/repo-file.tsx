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

/* eslint-disable react/no-danger */
import React from 'react';
import pathLib from 'path';
import FileContainer from 'application/common/components/file-container';
import { FileEditor, ErdaIcon } from 'common';
import { goTo, qs, getOrgFromPath, connectCube } from 'common/utils';
import { getSplitPathBy, getInfoFromRefName } from '../util';
import Markdown from 'common/utils/marked';
import i18n from 'i18n';
import './repo-file.scss';
import appStore from 'application/stores/application';
import repoStore from 'application/stores/repo';
import routeInfoStore from 'core/stores/route';

const { parse, extract } = qs;

interface IQaItem {
  path: string;
  line: string;
  message: string;
}

interface IProps {
  maxLines?: number;
  blob: REPOSITORY.IBlob;
  name: string;
  className: string;
  path: string;
  ops?: React.ElementType | null;
  autoHeight?: boolean;
  params: any;
  appDetail: any;
  getQaResult: (param: object) => Promise<any>;
}

interface IState {
  isCheckQa: boolean;
  qaResult: IQaItem[];
  qaLine: string;
}
class RepoFile extends React.PureComponent<IProps, IState> {
  times = 0;

  timer: any;

  state = {
    isCheckQa: false,
    qaResult: [],
    qaLine: '',
  };

  componentDidMount() {
    this.checkParsed();
    this.checkQaIssue();
  }

  componentWillUnmount() {
    clearTimeout(this.timer);
  }

  checkQaIssue = () => {
    // query 中含 qa 和 line 则去请求代码质量的 issue 信息
    const { qa: qaTypes, line: lines } = parse(extract(window.location.href));
    const qaType = Array.isArray(qaTypes) ? qaTypes[0] : qaTypes;
    const line = Array.isArray(lines) ? lines[0] : lines;
    if (!qaType || !line) return;
    const { getQaResult, params } = this.props;
    const TYPE_MAP = {
      bug: 'bugs',
      vulnerability: 'vulnerabilities',
      codeSmell: 'codeSmells',
    };

    getQaResult({
      appId: params.appId,
      type: TYPE_MAP[qaType],
    }).then((data) => {
      this.setState({
        isCheckQa: true,
        qaResult: data as IQaItem[],
        qaLine: line,
      });
    });
  };

  checkParsed = () => {
    this.timer = setTimeout(() => {
      const links = document.querySelectorAll('.md-content a') as any as HTMLAnchorElement[];
      if (this.times > 5) {
        return;
      }
      this.times += 1;
      if (links.length) {
        this.changeLinkAction(links);
      } else {
        this.checkParsed();
      }
    }, 300);
  };

  changeLinkAction = (links: HTMLAnchorElement[]) => {
    links.forEach((link) => {
      if (link.hash) {
        return;
      }
      if (link.host === window.location.host) {
        link.addEventListener('click', (e: any) => {
          e.preventDefault();
          goTo(link.pathname);
        });
      } else {
        link.setAttribute('target', '_blank');
      }
    });
  };

  render() {
    const { blob, name, className, ops, autoHeight, maxLines, appDetail } = this.props;
    if (!blob.path) {
      return null;
    }

    let fileExtension = name.split('.').pop();
    let { after } = getSplitPathBy('tree');
    const { branch, tag } = getInfoFromRefName(blob.refName);
    let { pathname } = window.location;
    const curBranch = branch || tag;

    // 路径上没有tree/branch时先补全
    if (!pathname.includes('tree')) {
      after = `/${curBranch}`;
      pathname = `${pathname}/tree/${curBranch}`;
    }
    const fileSrcPrefix = `/api/${getOrgFromPath()}/repo/${appDetail.gitRepoAbbrev}/raw`;
    // 根据当前url拼接的文件路径
    const fileSrc = `${fileSrcPrefix}${after}`;
    const isFile = blob.path && blob.path.endsWith(fileExtension as string);
    // 当前url为文件路径时，内部链接要提升一级，以当前文件的目录为相对路径
    const urlIsFilePath = after.endsWith(`.${fileExtension}`);

    if (curBranch && fileExtension && (urlIsFilePath || isFile)) {
      fileExtension = fileExtension.toLowerCase();

      if (['md', 'markdown'].includes(fileExtension)) {
        // url已变化但blob还未更新，会以当前路径和旧的内容拼接图片链接导致404报错
        // getSplitPathBy中window.location.pathname自动encode过，故此处需要encode
        const afterBranch = decodeURIComponent(getSplitPathBy(curBranch).after.slice(1));
        let pass = false;
        if (!afterBranch) {
          // 仓库首页 /repo 时，blob路径没有/说明是对的
          pass = !blob.path.includes('/');
        } else if (urlIsFilePath) {
          // 在文件路径下时，对比path和blob.path
          pass = afterBranch === blob.path;
        } else {
          // 在文件夹路径下时，移除blob.path最后一段，与path进行对比
          pass = afterBranch === blob.path.split('/').slice(0, -1).join('/');
        }
        if (!pass) {
          // debugger;
          return null;
        }
        const renderFns = {
          link(href: string, _title: string, text: string) {
            let link = href;
            if (!href.startsWith('http') && !href.startsWith('#')) {
              if (urlIsFilePath) {
                link = pathLib.resolve(pathLib.dirname(pathname), link);
              } else {
                link = pathLib.resolve(pathname, link);
              }
            }

            return `<a href="${link}">${text}</a>`;
          },
          image(src: string, title: string) {
            // markdown中 src 的4种文件格式:
            // 1: http(s)://foo/bar.png -> src 不进行处理
            // 2: ./bar.png
            // 3: bar.png -> src 为相对路径，需要进行拼接处理
            // 4: /bar.png -> src 不进行处理
            let _src = src;
            if (src.startsWith('./')) {
              // src.slice(2) 是为了去掉开头的 ./
              _src = `${fileSrcPrefix}/${curBranch}/${src.slice(2)}`;
            } else if (src.startsWith('.')) {
              if (urlIsFilePath) {
                _src = pathLib.resolve(pathLib.dirname(fileSrc), src);
              } else {
                _src = pathLib.resolve(fileSrc, src);
              }
            } else if (!src.startsWith('/') && !src.startsWith('http')) {
              // 若 src 为相对路径，则对其进行路径拼接
              _src = `${fileSrcPrefix}/${curBranch}/${src}`;
            }

            return `<img src="${_src}" alt="${title || 'preview-image'}" />`;
          },
        };
        return (
          <FileContainer name={name} ops={ops} className={`repo-file ${className}`}>
            <article
              className="md-content md-key"
              dangerouslySetInnerHTML={{ __html: Markdown(blob.content, renderFns) }}
            />
          </FileContainer>
        );
      } else if (['png', 'jpg', 'jpeg', 'gif', 'bmp'].includes(fileExtension)) {
        // 路由切换时，比如 xx/a.img -> xx，after已经没有后缀了
        // 这时fileExtension还没变，要等接口返回后才变，这时再显示
        const notShow = after === '/' || !after.endsWith(fileExtension);
        return (
          <FileContainer name={name} ops={ops} className={`repo-file ${className}`}>
            <div className="text-center mt-4">
              {notShow ? null : <img style={{ maxWidth: '80%' }} src={fileSrc} alt="preview-image" />}
            </div>
          </FileContainer>
        );
      } else if (fileExtension === 'svg') {
        return (
          <FileContainer name={name} ops={ops} className={`repo-file ${className}`}>
            <div
              className="text-center mt-4"
              dangerouslySetInnerHTML={{ __html: blob.content && blob.content.replace('script', '') }}
            />
          </FileContainer>
        );
      }
    }

    if (!blob.binary) {
      const { qaResult, qaLine, isCheckQa } = this.state;
      const data = (qaResult as IQaItem[]).filter(
        (item) => item.path === blob.path && Number(item.line) === Number(qaLine),
      );
      const annotations = data.map((item) => ({
        row: Number(item.line) - 1,
        text: item.message,
        type: 'error',
      }));
      const markers = data.map((item) => ({
        startRow: Number(item.line) - 1,
        endRow: Number(item.line) - 1,
        startCol: 0,
        className: 'error-marker',
      }));

      return (
        <FileContainer name={name} ops={ops} className={`repo-file ${className}`}>
          <FileEditor
            name={name}
            fileExtension={isCheckQa ? 'text' : fileExtension === 'workflow' ? 'yml' : fileExtension || 'text'}
            value={blob.content}
            autoHeight={autoHeight}
            maxLines={maxLines}
            readOnly
            annotations={annotations}
            markers={markers}
          />
        </FileContainer>
      );
    }
    return (
      <FileContainer name={name} ops={ops} className={`repo-file ${className}`}>
        <div className="flex flex-wrap justify-center items-center raw-file-container">
          <a className="flex" href={fileSrc} target="_blank" rel="noopener noreferrer">
            <ErdaIcon className="mr-1" type="download" />
            <div className="mt-1"> {i18n.t('download')} </div>
          </a>
        </div>
      </FileContainer>
    );
  }
}

const Mapper = () => {
  const appDetail = appStore.useStore((s) => s.detail);
  const [blob, blame] = repoStore.useStore((s) => [s.blob, s.blame]);
  const { getQaResult } = repoStore.effects;
  const params = routeInfoStore.useStore((s) => s.params);
  return {
    blob,
    blame,
    params,
    appDetail,
    getQaResult,
  };
};
export default connectCube(RepoFile, Mapper);
