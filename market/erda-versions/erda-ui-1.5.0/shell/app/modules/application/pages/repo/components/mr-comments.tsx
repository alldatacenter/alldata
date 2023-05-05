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

import { Input, Button, Tooltip, Rate } from 'antd';
import { Avatar } from 'common';
import { fromNow } from 'common/utils';
import React from 'react';
import { FileDiff } from './file-diff';
import MarkdownEditor, { EC_MarkdownEditor } from 'common/components/markdown-editor';
import { CommentBox } from 'application/common/components/comment-box';
import Markdown from 'common/utils/marked';
import i18n from 'i18n';
import repoStore from 'application/stores/repo';
import { useMount } from 'react-use';
import appStore from 'application/stores/application';

interface IEditBox {
  value: string;
  onChange: (e: Event) => void;
  onSubmit: () => void;
  onCancel?: () => void;
}
export const CommentEditBox = ({ value, onChange, onSubmit, onCancel }: IEditBox) => {
  const prop = {} as any;
  if (value !== undefined) {
    prop.value = value;
  }
  return (
    <div className="mt-3">
      <Input.TextArea
        autoFocus
        placeholder={`${i18n.t('please enter')}...`}
        {...prop}
        onChange={onChange}
        autoSize={{ minRows: 4, maxRows: 10 }}
      />
      <div className="mt-3">
        <Button type="primary" onClick={() => onSubmit()}>
          {i18n.t('dop:post comment')}
        </Button>
        {onCancel && (
          <Button className="ml-2" onClick={() => onCancel()}>
            {i18n.t('cancel')}
          </Button>
        )}
      </div>
    </div>
  );
};

interface IDiscussion {
  comment: REPOSITORY.IComment;
  commentMap: any;
  addComment: (data: object) => Promise<any>;
}
export const Discussion = ({ comment, commentMap, addComment }: IDiscussion) => {
  const detail = appStore.useStore((s) => s.detail);
  const { type, data } = comment;
  const { oldPath, newPath } = data;
  let sections = [] as any[];
  if (type === 'diff_note' && data.diffLines) {
    sections = [{ lines: data.diffLines }];
  }
  const title = (
    <div className="font-bold">
      <Avatar name={comment.author.nickName} className="mr-1" size={28} />
      <span className="mr-1">
        <Tooltip title={comment.author.username}>{comment.author.nickName}</Tooltip> 在 {comment.data.newPath} 中
      </span>
      <span className="mx-1">{i18n.t('dop:commented at')}</span>
      {fromNow(comment.createdAt)}
    </div>
  );

  return (
    <FileDiff
      key={newPath}
      title={title}
      file={{ oldName: oldPath, name: newPath, type: 'add', sections, issues: [], index: '' } as REPOSITORY.IFile}
      showStyle="inline"
      commentMap={commentMap}
      disableComment
      hideSectionTitle
      addComment={addComment}
      mode="compare"
      appDetail={detail}
    />
  );
};

/**
 * 构建评论数据结构
 * {
 *   file1: {
 *     line1: [comment1, comment2],
 *     line2: ...
 *   },
 *   file2: ...
 * }
 */
export const getFileCommentMap = (comments: REPOSITORY.IComment[] = []) => {
  const fileCommentMap = {};
  const discussionMap = {};
  comments.forEach((a: any) => {
    discussionMap[a.discussionId] = discussionMap[a.discussionId] || [];
    if (a.type === 'diff_note') {
      const name = a.data.newPath;
      const lineKey = `${a.data.oldLine}_${a.data.newLine}`;
      fileCommentMap[name] = fileCommentMap[name] || {};
      fileCommentMap[name][lineKey] = fileCommentMap[name][lineKey] || [];
      fileCommentMap[name][lineKey].push(a, ...discussionMap[a.discussionId]);
      discussionMap[a.discussionId] = fileCommentMap[name][lineKey]; // save reference
    }
    if (a.type === 'diff_note_reply') {
      discussionMap[a.discussionId].push(a);
    }
  });
  return fileCommentMap;
};

interface ICommentList {
  comments: REPOSITORY.IComment[];
}

export const PureCommentList = ({ comments = [] }: ICommentList) => {
  const { addComment, getComments } = repoStore.effects;

  const [score, setScore] = React.useState(0);
  const editorRef = React.useRef<EC_MarkdownEditor>(null);

  useMount(() => {
    getComments();
  });

  const fileCommentMap = getFileCommentMap(comments);

  const handleSubmit = async (note: string) => {
    await addComment({
      note,
      score: score * 20,
      type: 'normal',
    });
    setScore(0);
    editorRef.current?.clear();
  };

  return (
    <div>
      {comments.map((comment: REPOSITORY.IComment) => {
        if (comment.type === 'diff_note_reply') {
          return null;
        }
        if (comment.type === 'normal') {
          return (
            <CommentBox
              key={comment.id}
              user={comment.author.nickName}
              time={comment.createdAt}
              action={i18n.t('dop:commented at')}
              content={Markdown(comment.note || '')}
            />
          );
        }
        const { newPath, oldLine, newLine } = comment.data;
        const lineKey = `${oldLine}_${newLine}`;
        // 每个文件块只显示一行相关的评论
        const curCommentMap = {
          [lineKey]: fileCommentMap[newPath][lineKey],
        };
        return <Discussion key={comment.id} comment={comment} addComment={addComment} commentMap={curCommentMap} />;
      })}
      <MarkdownEditor
        ref={editorRef}
        operationBtns={[{ text: i18n.t('submit comment'), type: 'primary', onClick: (v: string) => handleSubmit(v) }]}
      />
      <div>
        <span>{i18n.t('score')}：</span>
        <Rate allowHalf onChange={(v) => setScore(v)} value={score} />
      </div>
    </div>
  );
};

export { PureCommentList as CommentList };
