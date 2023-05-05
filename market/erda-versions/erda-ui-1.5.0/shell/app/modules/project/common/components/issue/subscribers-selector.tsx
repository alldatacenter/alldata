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

import React from 'react';
import i18n from 'i18n';
import { Menu, Dropdown, Avatar } from 'antd';
import { Icon as CustomIcon, MemberSelector, ErdaIcon } from 'common';
import userStore from 'app/user/stores';
import { useUserMap } from 'core/stores/userMap';
import issueStore from 'project/stores/issues';
import { getAvatarChars } from 'app/common/utils';

interface IProps {
  subscribers: string[];
  issueID?: number;
  issueType: string;
  projectId: string;
  data: object;
  setData: (data: object) => void;
}

export const SubscribersSelector = (props: IProps) => {
  const { subscribers: subscribersProps, issueID, issueType, projectId, setData, data } = props;
  const { id: loginUserId, ...loginUser } = userStore.getState((s) => s.loginUser);
  const { subscribe, unsubscribe, getIssueDetail, getIssueStreams, batchSubscribe } = issueStore.effects;
  const memberRef = React.useRef<{ [p: string]: Function }>(null);
  const [subscribers, setSubscribers] = React.useState<Array<string | number>>([]);
  const [usersMap, setUsersMap] = React.useState({});

  const isFollowed = subscribers.includes(loginUserId);
  const userMap = useUserMap();

  React.useEffect(() => {
    const _userMap: object = { ...userMap };
    _userMap[loginUserId] || (_userMap[loginUserId] = { ...loginUser, userId: loginUserId });
    setUsersMap({ ..._userMap });
  }, [userMap]);

  React.useEffect(() => {
    setSubscribers(subscribersProps || []);
  }, [subscribersProps]);

  React.useEffect(() => {
    if (!issueID) {
      setSubscribers([loginUserId]);
    }
  }, [issueID]);

  const updateIssueDrawer = () => {
    getIssueDetail({ id: issueID as number, type: issueType });
    getIssueStreams({ type: issueType, id: issueID as number, pageNo: 1, pageSize: 50 });
  };

  const menu = (
    <Menu>
      <Menu.Item>
        {isFollowed ? (
          <div
            className="px-3 py-1 flex items-center"
            onClick={async () => {
              if (issueID) {
                await unsubscribe({ id: issueID });
                updateIssueDrawer();
              } else {
                const index = subscribers.findIndex((item) => item === loginUserId);
                subscribers.splice(index, 1);
                setSubscribers([...subscribers]);
              }
            }}
          >
            <ErdaIcon className="mr-1" type="preview-close-one" size="14" />
            {i18n.t('dop:unfollow')}
          </div>
        ) : (
          <div
            className="flex items-center"
            onClick={async () => {
              if (issueID) {
                await subscribe({ id: issueID });
                updateIssueDrawer();
              } else {
                setSubscribers([...subscribers, loginUserId]);
              }
            }}
          >
            <ErdaIcon type="preview-open" className="mr-1" size="14" />
            {i18n.t('dop:follow')}
          </div>
        )}
      </Menu.Item>
      <Menu.Item>
        <MemberSelector
          scopeType="project"
          className="issue-member-select"
          dropdownClassName="issue-member-select-dropdown"
          scopeId={projectId}
          allowClear={false}
          ref={memberRef}
          mode="multiple"
          value={subscribers || []}
          onVisibleChange={async (visible: boolean, values: any[][]) => {
            const ids: string[] = values[0] || [];
            const options: Array<{ userId: number }> = values[1] || [];
            // this event fires too often
            if (!visible && subscribers.join(',') !== ids.join(',')) {
              if (issueID) {
                await batchSubscribe({ id: issueID, subscribers: ids });
                updateIssueDrawer();
              } else {
                const newUsers = options.filter((item) => !usersMap[item.userId]);
                newUsers.forEach((item) => {
                  usersMap[item.userId] = item;
                });
                setUsersMap({ ...usersMap });
                setSubscribers(ids);
              }
            }
          }}
          resultsRender={() => (
            <span
              className="flex items-center"
              onClick={(e) => {
                e.stopPropagation();
                memberRef.current?.show(true);
              }}
            >
              <ErdaIcon type="plus" size="14" className="mr-1" />
              {i18n.t('dop:Add Followers')}
              <ErdaIcon type="right" size="14" className="add-follower-btn" />
            </span>
          )}
        />
      </Menu.Item>
      <Menu.Divider />
      <Menu.Item>
        <div onClick={(e) => e.stopPropagation()}>
          <div className="followers-num px-3">
            {subscribers.length !== 0
              ? i18n.t('dop:{num} members are following', { num: subscribers.length })
              : i18n.t('dop:no member is concerned about it')}
          </div>
          <div className="followers px-3">
            {subscribers.map((item) => {
              const user = usersMap[item] || {};
              return (
                <div key={user.id}>
                  <Avatar src={user.avatar} size="small">
                    {user.nick ? getAvatarChars(user.nick) : i18n.t('none')}
                  </Avatar>
                  <span className="ml-1">{user.nick ?? ''}</span>
                </div>
              );
            })}
          </div>
        </div>
      </Menu.Item>
    </Menu>
  );

  return (
    <>
      {isFollowed ? (
        <CustomIcon
          type="watch"
          className="followed"
          onClick={async () => {
            if (issueID) {
              await unsubscribe({ id: issueID });
              updateIssueDrawer();
            } else {
              const index = subscribers.findIndex((item) => item === loginUserId);
              subscribers.splice(index, 1);
              setSubscribers([...subscribers]);
            }
          }}
        />
      ) : (
        <CustomIcon
          type="watch"
          className="notFollowed"
          onClick={async () => {
            if (issueID) {
              await subscribe({ id: issueID });
              updateIssueDrawer();
            } else {
              setSubscribers([...subscribers, loginUserId]);
            }
          }}
        />
      )}
      <Dropdown
        overlay={menu}
        trigger={['click']}
        overlayClassName="attention-dropdown"
        onVisibleChange={(visible) => {
          if (!visible) {
            setData({ ...data, subscribers });
          }
        }}
      >
        <span className="attention-dropdown-btn ml-1">
          {subscribers.length !== 0
            ? i18n.t('dop:{num} people followed', { num: subscribers.length })
            : i18n.t('dop:no attention')}
          <CustomIcon type="caret-down" className="ml-0.5 mr-0" />
        </span>
      </Dropdown>
    </>
  );
};
