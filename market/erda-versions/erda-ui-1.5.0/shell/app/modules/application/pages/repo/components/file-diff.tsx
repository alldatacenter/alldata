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

import { Tooltip, Radio, Button } from 'antd';
import React, { useState } from 'react';
import i18n from 'i18n';
import { diff_match_patch as Diff } from 'diff-match-patch';
import { EmptyListHolder, Icon as CustomIcon, IF, BackToTop, ErdaIcon } from 'common';
import { last, map, isEmpty } from 'lodash';
import classnames from 'classnames';
import { getFileCommentMap } from './mr-comments';
import MarkdownEditor from 'common/components/markdown-editor';
import { isImage, setApiWithOrg, getOrgFromPath } from 'common/utils';
import { CommentBox } from 'application/common/components/comment-box';
import Markdown from 'common/utils/marked';
import 'requestidlecallback-polyfill';
import './file-diff.scss';
import repoStore from 'application/stores/repo';
import appStore from 'application/stores/application';
import routeInfoStore from 'core/stores/route';

const diffTool = new Diff();
const { ELSE } = IF;
const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;
const getExpandParams = (path: string, sections: any[], content: string) => {
  let bottom = false;
  let lastLineNo;
  let firstLineNo;
  let offset = 0;
  let sectionIndex = 0;
  if (content === '') {
    bottom = true;
    lastLineNo = (last(sections[sections.length - 2].lines) as any).newLineNo;
    sectionIndex = sections.length - 2;
  } else {
    sectionIndex = sections.findIndex((x) => x.lines[0].content === content);
    const currentSection = sections[sectionIndex];
    firstLineNo = currentSection.lines[1].newLineNo;
  }
  const lastSectionLine = sectionIndex > 0 && !bottom ? last(sections[sectionIndex - 1].lines) : null;
  let unfold = bottom;
  let since = bottom ? lastLineNo + 1 : 1;
  const to = bottom ? lastLineNo + 20 : firstLineNo - 1;
  if (!bottom) {
    if (lastSectionLine && firstLineNo - (lastSectionLine as any).newLineNo <= 20) {
      since = (lastSectionLine as any).newLineNo + 1;
    } else if (firstLineNo <= 21) {
      since = 1;
    } else {
      since = firstLineNo - 21;
      unfold = true;
    }
    if (lastSectionLine) {
      offset = (lastSectionLine as any).newLineNo - (lastSectionLine as any).oldLineNo;
    }
  }
  return { path, since, to, bottom, unfold, offset, sectionIndex };
};

const TemporaryStorageIcon = ({ disableComment, onClick }: { disableComment?: boolean; onClick: () => void }) =>
  disableComment ? null : <CustomIcon className="temporary-storage-icon" type="jl" onClick={onClick} />;

const CommentIcon = ({ disableComment, onClick }: { disableComment?: boolean; onClick: () => void }) =>
  disableComment ? null : <CustomIcon className="hover-active comment-icon" type="message" onClick={onClick} />;

const CommentListBox = ({ comments }: { comments: REPOSITORY.IComment[] }) => {
  if (!comments) {
    return null;
  }
  return (
    <>
      {comments.map((comment: REPOSITORY.IComment) => (
        <CommentBox
          key={comment.id}
          user={comment.author.nickName}
          time={comment.createdAt}
          action={i18n.t('dop:commented at')}
          content={Markdown(comment.note || '')}
        />
      ))}
    </>
  );
};

interface IProps {
  file: REPOSITORY.IFile;
  commentMap: {
    [prop: string]: REPOSITORY.IComment[];
  };
  title: string | React.ReactElement;
  getBlobRange?: null | Function;
  showStyle?: 'inline' | 'sideBySide';
  disableComment?: boolean;
  hideSectionTitle?: boolean;
  mode: string;
  appDetail: {
    gitRepoAbbrev: string;
  };
  forwardRef?: React.Ref<HTMLDivElement>;
  addComment: (data: object) => Promise<any>;
}

const generateDiffFilePath = (oldName: string, name: string) => {
  const old: React.ReactElement[] = [];
  const now: React.ReactElement[] = [];
  if (oldName === name) {
    return { old, now };
  }
  const diff = diffTool.diff_main(oldName, name);
  diffTool.diff_cleanupSemantic(diff);

  diff.forEach((part: any[]) => {
    const [type, content] = part;
    if (type === -1) {
      old.push(
        <span key={`${type}-${content}`} className="highlight-red">
          {content}
        </span>,
      );
    } else if (type === 1) {
      now.push(
        <span key={`${type}-${content}`} className="highlight-green">
          {content}
        </span>,
      );
    } else {
      old.push(<span key={`${type}-${content}`}>{content}</span>);
      now.push(<span key={`${type}-${content}`}>{content}</span>);
    }
  });
  return { old, now };
};

enum ACTION {
  ADD = 'add',
  DELETE = 'delete',
  RENAME = 'rename',
}

export const FileDiff = ({
  file,
  commentMap,
  title,
  getBlobRange = null,
  showStyle,
  addComment,
  disableComment,
  hideSectionTitle,
  mode,
  forwardRef,
  appDetail,
}: IProps) => {
  const [expandedFile, setExpandedFile] = React.useState(null);
  const memoFile = React.useMemo(() => {
    return expandedFile || file;
  }, [expandedFile, file]);
  const { oldName, name, type, sections, issues = [], isBin = false, index: commitId } = memoFile;
  const diffSize = (sections || []).reduce((size, value) => size + value.lines.length, 0);
  const DIFF_SIZE_LIMIT = 200;
  const [leftCommentEditVisible, setLeftCommentEditVisible] = useState({});
  const [rightCommentEditVisible, setRightCommentEditVisible] = useState({});
  const [isExpanding, setFileExpanding] = useState(diffSize < DIFF_SIZE_LIMIT);
  const [hasLS, setHasLs] = useState({});
  const [isShowLS, setIsShowLS] = useState({});

  const { projectId, appId, mergeId } = routeInfoStore.useStore((s) => s.params);

  if (!sections) {
    if (type === ACTION.ADD || type === ACTION.DELETE || type === ACTION.RENAME || isBin) {
      // TODO isBin 如何显示需要后续处理
      const { old, now } = generateDiffFilePath(oldName, name);
      const fileSrcPrefix = `/api/${getOrgFromPath()}/repo/${appDetail.gitRepoAbbrev}/raw`;
      const fileIsImage = isImage(name);
      const imageAddress = fileIsImage ? `${fileSrcPrefix}/${commitId}/${name}` : '';

      const text =
        {
          [ACTION.ADD]: <ErdaIcon type="file-addition" className="text-base text-green" />,
          [ACTION.DELETE]: <ErdaIcon type="delete1" className="text-base text-red" />,
          [ACTION.RENAME]: i18n.t('dop:file moved'),
        }[type] || '';

      return (
        <div ref={forwardRef} className="file-diff">
          <IF check={type === 'rename'}>
            <div className="file-title-move">
              <div className="font-bold nowrap">
                <ErdaIcon type="file-code-one" size="14" className="mr-2" />
                {old}
              </div>
              <ErdaIcon type="arrow-right" className="file-move-arrow" />
              <div className="font-bold nowrap">{now}</div>
            </div>
            <div className="file-static-info">{text}</div>
            <IF.ELSE />
            <div className="file-title inline-flex justify-between items-center">
              <div className="font-bold nowrap">
                <ErdaIcon type="file-code-one" size="14" className="mr-2" />
                {name} {text || null}
              </div>
            </div>
            <IF check={fileIsImage}>
              <div className="text-center my-4">
                <img src={setApiWithOrg(imageAddress)} alt={`${name || 'preview-image'}`} />
              </div>
            </IF>
          </IF>
        </div>
      );
    }
    return null;
  }
  const issueLineMap = {};
  issues.forEach((issue) => {
    issueLineMap[issue.line] = issue;
  });

  const toggleLeftCommentEdit = (lineKey: string, visible: boolean) => {
    setLeftCommentEditVisible({
      ...leftCommentEditVisible,
      [lineKey]: visible,
    });
    setIsShowLS({
      ...isShowLS,
      [lineKey]: false,
    });
  };

  const toggleRightCommentEdit = (lineKey: string, visible: boolean) => {
    setRightCommentEditVisible({
      ...rightCommentEditVisible,
      [lineKey]: visible,
    });
  };

  const handleRightGetLS = (lineKey: string, visible: boolean) => {
    setRightCommentEditVisible({
      ...leftCommentEditVisible,
      [lineKey]: visible,
    });
    setIsShowLS({
      ...isShowLS,
      [lineKey]: true,
    });
  };

  const handleLeftGetLS = (lineKey: string, visible: boolean) => {
    setLeftCommentEditVisible({
      ...leftCommentEditVisible,
      [lineKey]: visible,
    });
    setIsShowLS({
      ...isShowLS,
      [lineKey]: true,
    });
  };

  const handleSetLS = (lineKey: string, content: string) => {
    const timestamp = new Date().getTime();
    localStorage.setItem(
      `mr-comment-${projectId}-${appId}-${mergeId}-${name}-${lineKey}`,
      JSON.stringify({ content, timestamp }),
    );
    setHasLs({
      ...hasLS,
      [lineKey]: true,
    });
  };

  return (
    <div ref={forwardRef} className="file-diff">
      <div
        className="file-title"
        onClick={() => {
          setFileExpanding(!isExpanding);
        }}
      >
        {title || (
          <div className="font-bold flex items-center">
            <IF check={!isExpanding}>
              <ErdaIcon type="right-one" size="18px" className="mr-2" />
              <ELSE />
              <ErdaIcon type="caret-down" size="18px" className="mr-2" />
            </IF>
            <ErdaIcon type="file-code-one" size="14" className="mr-2" />
            {name}
          </div>
        )}
      </div>
      <IF check={!isExpanding}>
        <div className="file-content-collapsed">
          {i18n.t('dop:comparison result has been folded')}。
          <span
            className="click-to-expand text-link"
            onClick={() => {
              setFileExpanding(!isExpanding);
            }}
          >
            {i18n.t('dop:click to expand')}
          </span>
        </div>
        <ELSE />
        <table>
          {sections.map((section, i) => {
            const prefixMap = {
              delete: '-',
              add: '+',
            };

            const fileKey = `${name}_${i}`;
            return (
              <tbody key={fileKey} className="file-diff-section">
                {section.lines.map(({ oldLineNo, newLineNo, type: actionType, content }, lineIndex) => {
                  if (hideSectionTitle && actionType === 'section') {
                    return null;
                  }

                  const hasWhiteSpace = content.match(/^\s+/);
                  let _content: any = content;
                  let paddingLeft = 0;
                  if (hasWhiteSpace && content.startsWith('\t')) {
                    const spaceLength = hasWhiteSpace[0].length;
                    // tab缩进使用2个em空白显示
                    paddingLeft = spaceLength * 2;
                    _content = _content.slice(spaceLength);
                  }
                  let leftContent = content;
                  let rightContent = content;
                  if (actionType === 'add') {
                    leftContent = '';
                    rightContent = content;
                  } else if (actionType === 'delete') {
                    leftContent = content;
                    rightContent = '';
                  }
                  const lineIssue = issueLineMap[newLineNo];
                  if (lineIssue) {
                    const { textRange, message } = lineIssue;
                    _content = (
                      <span>
                        <span>{content.slice(0, textRange.startOffset)}</span>
                        <Tooltip title={message}>
                          <span className="issue-word">
                            {content.slice(textRange.startOffset, textRange.endOffset)}
                          </span>
                        </Tooltip>
                        <span>{content.slice(textRange.endOffset)}</span>
                      </span>
                    );
                  }
                  const isUnfoldLine = oldLineNo === -1 && newLineNo === -1;
                  const unfoldClassName = isUnfoldLine && getBlobRange ? 'unfold-btn' : '';
                  const oldPrefix = oldLineNo > 0 ? oldLineNo : '';
                  const newPrefix = newLineNo > 0 ? newLineNo : '';
                  const actionPrefix = prefixMap[actionType] || '';
                  const codeStyle: React.CSSProperties = {
                    paddingLeft: `${paddingLeft}em`,
                    whiteSpace: 'pre-wrap',
                    display: 'inline-block',
                  };
                  const handleExpand = async () => {
                    if (oldLineNo !== -1 || newLineNo !== -1 || !getBlobRange) {
                      return;
                    }
                    const params = getExpandParams(file.name, sections, content);
                    const updatedFile = await getBlobRange({ ...params, type: mode });
                    setExpandedFile(updatedFile);
                  };
                  const lineKey = `${oldLineNo}_${newLineNo}`;
                  const comments = commentMap[lineKey];
                  // 编辑框显示条件：只判断icon点击后的状态
                  const showLeftCommentEdit = leftCommentEditVisible[lineKey];
                  // icon显示条件：未禁用、无数据（有的话通过回复按钮显示编辑框）、lineNo不为-1、编辑框未显示
                  const showLeftCommentIcon = !disableComment && !comments && oldLineNo !== -1 && !showLeftCommentEdit;
                  // 如果有数据或者编辑框，显示追加行
                  const showLeftCommentLine = comments || showLeftCommentEdit;
                  // 暂存key
                  const tsKey = `mr-comment-${projectId}-${appId}-${mergeId}-${name}-${lineKey}`;

                  const tsCommentObj = localStorage.getItem(tsKey);

                  const tsComment = tsCommentObj ? JSON.parse(tsCommentObj) : {};

                  // 暂存icon展示条件：可评论，存在暂存内容
                  const showTsCommentIcon = !disableComment && !!tsCommentObj;

                  const showRightCommentEdit = rightCommentEditVisible[lineKey];
                  const showRightCommentIcon =
                    !disableComment && !comments && newLineNo !== -1 && !showRightCommentEdit;
                  const showRightCommentLine = comments || showRightCommentEdit;

                  const addCommentFn = (_data: object) => {
                    let data = {
                      type: 'diff_note',
                      oldPath: oldName,
                      newPath: name,
                      oldLine: oldLineNo,
                      newLine: newLineNo,
                      ..._data,
                    } as any;
                    if (comments) {
                      data = {
                        type: 'diff_note_reply',
                        discussionId: comments[comments.length - 1].discussionId,
                        ..._data,
                      };
                    }
                    if (tsCommentObj) {
                      localStorage.removeItem(tsKey);
                    }
                    return addComment(data);
                  };

                  const lineCls = classnames({
                    'file-diff-line': true,
                    [actionType]: true,
                    'issue-line': lineIssue,
                  });
                  if (showStyle === 'inline') {
                    const showCommentEdit = showLeftCommentEdit;
                    const showCommentLine = comments || showCommentEdit;
                    let toggleEditFn = toggleLeftCommentEdit;
                    if (oldLineNo < 0) {
                      toggleEditFn = toggleRightCommentEdit;
                    }

                    return (
                      <React.Fragment key={`${lineKey}_${lineIndex}`}>
                        <tr className={lineCls}>
                          {/* <td className={lineIssue ? 'issue-td' : 'none-issue-td'}>
                                {lineIssue ? <Icon className="issue-icon" type="exclamation-circle" /> : null}
                              </td> */}
                          <td
                            className={`diff-line-num old-line ${unfoldClassName}`}
                            onClick={handleExpand}
                            data-prefix={oldPrefix}
                          >
                            {showTsCommentIcon && (
                              <TemporaryStorageIcon onClick={() => handleLeftGetLS(lineKey, true)} />
                            )}
                            <IF check={showLeftCommentIcon || showRightCommentIcon}>
                              <CommentIcon onClick={() => toggleLeftCommentEdit(lineKey, true)} />
                            </IF>
                          </td>
                          <td
                            className={`diff-line-num new-line ${unfoldClassName}`}
                            onClick={handleExpand}
                            data-prefix={newPrefix}
                          />
                          <td className="diff-line-content" data-prefix={actionPrefix}>
                            <pre>
                              <code style={codeStyle}>{_content}</code>
                            </pre>
                          </td>
                        </tr>
                        <IF check={showCommentLine}>
                          <tr>
                            <td colSpan={2} />
                            <td className="comment-box-td">
                              <CommentListBox comments={comments} />
                              <IF check={showCommentEdit}>
                                <MarkdownEditor
                                  value={isShowLS[lineKey] ? tsComment.content : null}
                                  operationBtns={[
                                    {
                                      text: i18n.t('dop:post comment'),
                                      type: 'primary',
                                      onClick: (v) =>
                                        addCommentFn({
                                          note: v,
                                        }).then(() => toggleEditFn(lineKey, false)),
                                    },
                                    {
                                      text: i18n.t('dop:temporary storage'),
                                      onClick: (v) => handleSetLS(lineKey, v),
                                    },
                                    {
                                      text: i18n.t('common:discard'),
                                      onClick: () => toggleEditFn(lineKey, false),
                                    },
                                  ]}
                                />
                                <ELSE />
                                <Button onClick={() => toggleEditFn(lineKey, true)}>{i18n.t('dop:reply')}</Button>
                              </IF>
                            </td>
                          </tr>
                        </IF>
                      </React.Fragment>
                    );
                  }
                  return (
                    <React.Fragment key={`${lineKey}`}>
                      <tr className={lineCls}>
                        {/* <td data-prefix={oldPrefix} className={lineIssue ? 'issue-td' : 'none-issue-td'}>
                              {lineIssue ? <Icon className="issue-icon" type="exclamation-circle" /> : null}
                            </td> */}
                        <td
                          className={`diff-line-num old-line ${unfoldClassName}`}
                          onClick={handleExpand}
                          data-prefix={oldPrefix}
                        >
                          {showTsCommentIcon && <TemporaryStorageIcon onClick={() => handleLeftGetLS(lineKey, true)} />}
                          {showLeftCommentIcon && <CommentIcon onClick={() => toggleLeftCommentEdit(lineKey, true)} />}
                        </td>
                        <td className="diff-line-content" data-prefix={leftContent === '' ? '' : actionPrefix}>
                          <pre>
                            <code style={codeStyle}>{leftContent}</code>
                          </pre>
                        </td>
                        <td
                          className={`diff-line-num new-line ${unfoldClassName}`}
                          onClick={handleExpand}
                          data-prefix={newPrefix}
                        >
                          {showTsCommentIcon && (
                            <TemporaryStorageIcon onClick={() => handleRightGetLS(lineKey, true)} />
                          )}
                          {showRightCommentIcon && (
                            <CommentIcon onClick={() => toggleRightCommentEdit(lineKey, true)} />
                          )}
                        </td>
                        <td className="diff-line-content" data-prefix={rightContent === '' ? '' : actionPrefix}>
                          <pre>
                            <code style={codeStyle}>{rightContent}</code>
                          </pre>
                        </td>
                      </tr>
                      <IF check={showLeftCommentLine || showRightCommentLine}>
                        <tr>
                          <td />
                          <td className="comment-box-td">
                            <IF check={oldLineNo > 0}>
                              <CommentListBox comments={comments} />
                              <IF check={comments && !showLeftCommentEdit}>
                                <Button onClick={() => toggleLeftCommentEdit(lineKey, true)}>
                                  {i18n.t('dop:reply')}
                                </Button>
                              </IF>
                            </IF>
                            <IF check={showLeftCommentEdit}>
                              <MarkdownEditor
                                value={isShowLS[lineKey] ? tsComment.content : null}
                                operationBtns={[
                                  {
                                    text: i18n.t('dop:post comment'),
                                    type: 'primary',
                                    onClick: (v) =>
                                      addCommentFn({
                                        note: v,
                                      }).then(() => toggleLeftCommentEdit(lineKey, false)),
                                  },
                                  {
                                    text: i18n.t('common:discard'),
                                    onClick: () => toggleLeftCommentEdit(lineKey, false),
                                  },
                                ]}
                              />
                            </IF>
                          </td>
                          <td />
                          <td className="comment-box-td">
                            <IF check={newLineNo > 0}>
                              <CommentListBox comments={comments} />
                              <IF check={comments && !showRightCommentEdit}>
                                <Button onClick={() => toggleRightCommentEdit(lineKey, true)}>
                                  {i18n.t('dop:reply')}
                                </Button>
                              </IF>
                            </IF>
                            <IF check={showRightCommentEdit}>
                              <MarkdownEditor
                                value={isShowLS[lineKey] ? tsComment.content : null}
                                operationBtns={[
                                  {
                                    text: i18n.t('dop:post comment'),
                                    type: 'primary',
                                    onClick: (v) =>
                                      addCommentFn({
                                        note: v,
                                      }).then(() => toggleRightCommentEdit(lineKey, false)),
                                  },
                                  {
                                    text: i18n.t('common:discard'),
                                    onClick: () => toggleRightCommentEdit(lineKey, false),
                                  },
                                ]}
                              />
                            </IF>
                          </td>
                        </tr>
                      </IF>
                    </React.Fragment>
                  );
                })}
              </tbody>
            );
          })}
        </table>
      </IF>
    </div>
  );
};

const FileDiffWithRef = React.forwardRef((props: any, ref: any) => <FileDiff {...props} forwardRef={ref} />) as any;

interface IDiff {
  files: REPOSITORY.IFile[];
  filesChanged: boolean;
  totalAddition: number;
  totalDeletion: number;
}

interface IDiffProps {
  mode: any;
  diff: IDiff;
  from: string;
  to: string;
  comments: REPOSITORY.IComment[];
  disableComment?: boolean;
}
const FilesDiff = (props: IDiffProps) => {
  const [showStyle, setShowStyle] = React.useState('inline');
  const [expandDiffFiles, setExpandDiffFiles] = React.useState(false);
  const [diffFileRefs, setDiffFileRefs] = React.useState({});
  const { getBlobRange, addComment: addCommentFunc } = repoStore.effects;
  const [renderList, setRenderList] = React.useState([] as REPOSITORY.IFile[]);
  const appDetail = appStore.useStore((s) => s.detail);

  React.useEffect(() => {
    if (props.diff) {
      const { files } = props.diff;
      const refs = {};
      (files || []).forEach((file: any) => {
        refs[file.name] = React.createRef();
      });
      setDiffFileRefs(refs);
    }
  }, [props.diff]);

  const showToggle = (e: any) => {
    setShowStyle(e.target.value);
  };

  const addComment = (data: object) => {
    const { from, to } = props;
    return addCommentFunc({
      oldCommitId: to,
      newCommitId: from,
      ...data,
    });
  };

  const onToggleDiffFiles = () => {
    setExpandDiffFiles(!expandDiffFiles);
  };

  const navigateToFile = (fileName: string) => {
    const ref = diffFileRefs[fileName];
    if (ref.current) {
      ref.current.scrollIntoView();
    } else {
      const target = document.querySelector('#main');
      const frameFunc = () => {
        target && (target.scrollTop += 500);
        if (!diffFileRefs[fileName].current) {
          window.requestAnimationFrame(frameFunc);
        } else {
          diffFileRefs[fileName].current.scrollIntoView();
        }
      };
      window.requestAnimationFrame(frameFunc);
    }
  };

  const { diff, comments, disableComment } = props;

  const renderDiffFiles = React.useCallback(
    (leftCount: number) => {
      window.requestIdleCallback(() => {
        const oneTimeCount = leftCount >= 10 ? 10 : leftCount; // 每次渲染10个文件
        const newList = renderList.concat(diff.files.slice(renderList.length, renderList.length + oneTimeCount));
        setRenderList(newList);
      });
    },
    [diff, renderList],
  );

  React.useEffect(() => {
    if (diff && diff.files && renderList.length < diff.files.length) {
      renderDiffFiles(diff.files.length - renderList.length);
    }
  }, [diff, diff.files, renderDiffFiles, renderList]);

  if (!diff || !diff.files) {
    return <EmptyListHolder />;
  }
  const { filesChanged, totalAddition, totalDeletion } = diff;
  const fileCommentMap = getFileCommentMap(comments);

  return (
    <div>
      <BackToTop />
      <div className={expandDiffFiles ? 'commit-summary-expand' : 'commit-summary-hide'}>
        <div className="commit-summary">
          <div>
            <span>
              {i18n.t('dop:share')}
              <span className="changed-count ml-2">
                {filesChanged} {i18n.t('dop:changed file(s)')}
              </span>
              <Tooltip title={expandDiffFiles ? i18n.t('dop:collapse file') : i18n.t('dop:expand file')}>
                <span className="ml-2 cursor-pointer df-icon" onClick={onToggleDiffFiles}>
                  {expandDiffFiles ? <CustomIcon type="sq" /> : <CustomIcon type="zk" />}
                </span>
              </Tooltip>
              <span className="add-count ml-2">
                {totalAddition} {i18n.t('dop:additions')}
              </span>
              <span className="del-count ml-2">
                {totalDeletion} {i18n.t('dop:deletions')}
              </span>
            </span>
          </div>
          <div className="toggle-view-btn">
            <RadioGroup onChange={showToggle} defaultValue={showStyle}>
              <RadioButton value="inline">{i18n.t('dop:single line')}</RadioButton>
              <RadioButton value="sideBySide">{i18n.t('dop:side-by-side')}</RadioButton>
            </RadioGroup>
          </div>
        </div>
        <div className="diff-file-list">
          {map(diff.files, (file) => (
            <div key={file.name} className="diff-file cursor-pointer" onClick={() => navigateToFile(file.name)}>
              <div className="diff-count">
                <span className="diff-add-icon">+{file.addition}</span>
                <span className="diff-del-icon">-{file.deletion}</span>
              </div>
              <div className="diff-file-path nowrap">{file.name}</div>
            </div>
          ))}
        </div>
      </div>
      <div className="file-diff-list">
        {renderList.map((file: REPOSITORY.IFile) => {
          const commentMap = fileCommentMap[file.name] || {};
          let ref = null;
          if (!isEmpty(diffFileRefs)) {
            ref = diffFileRefs[file.name];
          }
          return (
            <FileDiffWithRef
              key={file.name}
              file={file}
              commentMap={commentMap}
              getBlobRange={getBlobRange}
              showStyle={showStyle}
              disableComment={disableComment}
              addComment={addComment}
              mode={props.mode}
              ref={ref}
              appDetail={appDetail}
            />
          );
        })}
      </div>
    </div>
  );
};

export default FilesDiff;
