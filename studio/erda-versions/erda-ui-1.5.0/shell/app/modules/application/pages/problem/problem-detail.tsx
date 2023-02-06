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

import { ProblemPriority, getProblemType } from 'application/pages/problem/problem-form';
import React from 'react';
import { Button, Spin, Tabs, Select } from 'antd';
import { isEmpty, map, toLower } from 'lodash';
import problemStore from 'application/stores/problem';
import { useMount } from 'react-use';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import { CommentBox } from 'application/common/components/comment-box';
import MarkdownEditor from 'common/components/markdown-editor';
import { LoadMoreSelector, Avatar } from 'common';
import { useUpdate } from 'common/use-hooks';
import { fromNow, goTo } from 'common/utils';
import { getProjectList } from 'app/modules/project/services/project';
import { getProjectIterations } from 'project/services/project-iteration';
import { getIssues as getProjectIssues } from 'app/modules/project/services/issue';
import Markdown from 'common/utils/marked';
import i18n from 'i18n';
import './problem-detail.scss';

interface IProps {
  detail: PROBLEM.Ticket;
}

export const ProblemContent = ({ detail }: IProps) => {
  const { label, type, content, author, createdAt } = detail;
  const params = routeInfoStore.useStore((s) => s.params);

  const getUrl = (path: string, ticket: PROBLEM.Ticket) => {
    const { projectId, appId, orgName } = params;

    return `/${orgName}/workBench/projects/${projectId}/apps/${appId}/repo/tree/${ticket.label.branch}/${path}?qa=${ticket.type}&line=${ticket.label.line}`;
  };

  const getNote = () => {
    let note = '';

    let url;
    let linkLabel = label && label.path;
    if (linkLabel) {
      if (linkLabel.includes('/src/')) {
        linkLabel = `/src${linkLabel.split('src').pop()}`;
      } else {
        linkLabel = linkLabel.split('/').pop() || '';
      }
    }
    switch (type) {
      case 'bug':
      case 'vulnerability':
      case 'codeSmell':
        url = !isEmpty(label) ? getUrl(label.path, detail) : null;
        note = label?.path
          ? `${label.code}\n
${i18n.t('dop:jump to code')}：[${linkLabel || label.path}](${url})`
          : content;
        break;
      default:
        note = content;
        break;
    }

    return note;
  };

  let _note = getNote();
  let _content;
  if (label && label.lineCode) {
    _note = _note.replace(label.lineCode, `$t_start${label.lineCode}$t_end`);
    _content = Markdown(_note || '');
    _content = _content.replace('$t_start', '<div class="error">').replace('$t_end', '</div>');
  } else {
    _content = Markdown(_note || '');
  }

  return <CommentBox user={author} time={createdAt} action={i18n.t('dop:built in')} content={_content} />;
};

const { TabPane } = Tabs;
const { Option } = Select;

const initialState = {
  activedProject: undefined,
  activedIteration: undefined,
  activedIssueType: undefined,
  activedIssue: undefined,
  activedIssueTitle: undefined,
};

const TicketDetail = () => {
  const [detail, comments] = problemStore.useStore((s) => [s.detail, s.comments]);
  const { getTicketDetail, getTicketComments, createTicketComments, closeTicket } = problemStore.effects;
  const [getTicketDetailLoading, getTicketCommentsLoading] = useLoading(problemStore, [
    'getTicketDetail',
    'getTicketComments',
  ]);
  const [{ activedProject, activedIteration, activedIssueType, activedIssue, activedIssueTitle }, , update] =
    useUpdate(initialState);

  useMount(() => {
    getTicketDetail();
    getTicketComments();
  });

  const handleSubmit = (content: string) => {
    if (!content) {
      return;
    }

    createTicketComments({
      content,
      commentType: 'normal',
    });
  };

  const closedBtn =
    detail.status === 'open' ? (
      <div className="top-button-group">
        <Button type="primary" onClick={() => closeTicket()}>
          {i18n.t('close')}
        </Button>
      </div>
    ) : null;

  const type = getProblemType().find((t) => t.value === detail.type);
  const priority = ProblemPriority.find((t: any) => t.value === detail.priority);

  const getProjects = (q: any) => {
    return getProjectList({ ...q }).then((res: any) => res.data);
  };

  const getIterations = (q: any) => {
    if (!activedProject) return;
    return getProjectIterations({ ...q }).then((res: any) => res.data);
  };

  const getIssues = (q: any) => {
    if (!(activedProject && activedIteration && activedIssueType)) return;
    return getProjectIssues({ ...q }).then((res: any) => res.data);
  };

  const handleAssociationIssue = () => {
    createTicketComments({
      commentType: 'issueRelation',
      irComment: {
        issueID: activedIssue || 0,
        issueTitle: activedIssueTitle || '',
        projectID: activedProject || 0,
        iterationID: activedIteration || 0,
        issueType: toLower(activedIssueType) || '',
      },
    });
  };
  return (
    <div className="comments-container">
      {closedBtn}
      <Spin spinning={getTicketDetailLoading}>
        <div>
          <div className="detail-title mb-2">{detail.title}</div>
          <div className="mb-5">
            <span className="mr-5">
              <span className="detail-property">{i18n.t('type')}: </span>
              <span className="detail-value">{type ? type.name : '-'}</span>
            </span>
            <span>
              <span className="detail-property">{i18n.t('dop:emergency level')}: </span>
              <span className="detail-value">{priority ? priority.name : '-'}</span>
            </span>
          </div>
        </div>
        <ProblemContent detail={detail} />
        <div className="comments-section">
          <span className="comments-section-text">{i18n.t('dop:comment area')}</span>
          <div className="section-line" />
        </div>
        <Spin spinning={getTicketCommentsLoading}>
          {comments.map((comment) =>
            comment.commentType === 'issueRelation' ? (
              <div className="comments-association-box">
                <Avatar name={comment.author} showName size={28} />
                <span className="mx-1">{i18n.t('at')}</span>
                <span className="mx-1">{fromNow(comment.createdAt)}</span>
                <span className="mx-1">{i18n.t('dop:associated issue')}</span>
                <span
                  className="text-link"
                  onClick={() => {
                    let page = '';
                    const { issueType, projectID, issueID } = comment.irComment;
                    switch (issueType) {
                      case 'task':
                        page = goTo.pages.taskList;
                        break;
                      case 'bug':
                        page = goTo.pages.bugList;
                        break;
                      default:
                        break;
                    }
                    goTo(page, { projectId: projectID, taskId: issueID, jumpOut: true });
                  }}
                >
                  {comment.irComment.issueTitle}
                </span>
              </div>
            ) : (
              <CommentBox
                className="mb-4"
                key={comment.id}
                user={comment.author}
                time={comment.createdAt}
                action={i18n.t('dop:commented at')}
                content={comment.content}
              />
            ),
          )}
        </Spin>
        <Tabs>
          <TabPane tab={i18n.t('comment')} key="comment">
            <MarkdownEditor
              maxLength={5000}
              operationBtns={[
                {
                  text: i18n.t('dop:submit comments'),
                  type: 'primary',
                  onClick: (v) => handleSubmit(v),
                },
              ]}
            />
          </TabPane>
          <TabPane tab={i18n.t('relate to issue')} key="relate">
            <div className="flex justify-between items-center">
              <div className="selecter-wrap flex items-center justify-start flex-1">
                <LoadMoreSelector
                  className="selecter-item"
                  value={activedProject}
                  getData={getProjects}
                  placeholder={i18n.t('dop:please select project')}
                  dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
                    total,
                    list: map(list, (project) => {
                      const { name, id } = project;
                      return {
                        ...project,
                        label: name,
                        value: id,
                      };
                    }),
                  })}
                  onChange={(val) => {
                    update({
                      ...initialState,
                      activedProject: val as any,
                    });
                  }}
                />
                <LoadMoreSelector
                  className="selecter-item"
                  value={activedIteration}
                  getData={getIterations}
                  extraQuery={{ projectID: activedProject }}
                  showSearch={false}
                  placeholder={i18n.t('dop:please select iteration')}
                  onChange={(val) => {
                    update({
                      activedIteration: val as any,
                      activedIssueType: undefined,
                      activedIssue: undefined,
                      activedIssueTitle: undefined,
                    });
                  }}
                  dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
                    total,
                    list: map(list, (iteration) => {
                      const { title, id } = iteration;
                      return {
                        ...iteration,
                        label: title,
                        value: id,
                      };
                    }),
                  })}
                />
                <Select
                  className="selecter-item"
                  placeholder={i18n.t('dop:please select issue type')}
                  value={activedIssueType}
                  onSelect={(val) => {
                    update({
                      activedIssueType: val as any,
                      activedIssue: undefined,
                      activedIssueTitle: undefined,
                    });
                  }}
                >
                  <Option value="TASK">{i18n.t('task')}</Option>
                  <Option value="BUG">{i18n.t('bug')}</Option>
                </Select>
                <LoadMoreSelector
                  className="selecter-item"
                  value={activedIssue}
                  getData={getIssues}
                  extraQuery={{ projectID: activedProject, iterationID: activedIteration, type: activedIssueType }}
                  showSearch={false}
                  placeholder={i18n.t('dop:please select issue')}
                  dataFormatter={({ list, total }: { list: any[]; total: number }) => ({
                    total,
                    list: map(list, (issue) => {
                      const { title, id } = issue;
                      return {
                        ...issue,
                        label: title,
                        value: id,
                      };
                    }),
                  })}
                  onChange={(val, opts) => {
                    update({
                      activedIssue: val as any,
                      activedIssueTitle: opts.title as any,
                    });
                  }}
                />
              </div>
              <div className="options-wrap">
                <Button className="mr-2" type="primary" disabled={!activedIssue} onClick={handleAssociationIssue}>
                  {i18n.t('association')}
                </Button>
                <Button onClick={() => update(initialState)}>{i18n.t('reset')}</Button>
              </div>
            </div>
          </TabPane>
        </Tabs>
      </Spin>
    </div>
  );
};

export default TicketDetail;
