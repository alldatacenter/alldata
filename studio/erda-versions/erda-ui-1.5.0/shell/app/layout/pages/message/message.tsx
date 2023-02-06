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
import messageStore, { MSG_STATUS } from 'app/layout/stores/message';
import { Holder, Icon as CustomIcon, LoadMore, ErdaIcon } from 'common';
import { Badge, Timeline, Drawer, notification, Button, Modal, message } from 'antd';
import Markdown from 'common/utils/marked';
import { map } from 'lodash';
import moment from 'moment';
import i18n from 'i18n';
import logo from 'app/static/favicon.ico';
import './message.scss';
import { useLoading } from 'core/stores/loading';
import layoutStore from 'layout/stores/layout';
import { useEffectOnce } from 'react-use';
import routeInfoStore from 'core/stores/route';

const checkPermission = () => {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission();
  }
};

const nusiNotification = (msg: string, viewMsg: Function) => {
  const key = `msg_${Date.now()}`;
  notification.open({
    duration: 3,
    key,
    message: i18n.t('default:notification'),
    description: msg,
    icon: <img src={logo} style={{ width: '24px' }} />,
    btn: (
      <Button
        type="primary"
        size="small"
        onClick={() => {
          notification.close(key);
          viewMsg();
        }}
      >
        {i18n.t('common:view')}
      </Button>
    ),
  });
};

const chromeNotification = (msg: string, viewMsg: Function) => {
  const noticeInstance = new Notification(i18n.t('default:notification'), {
    body: msg,
    icon: logo,
  });
  noticeInstance.onclick = () => {
    noticeInstance.close();
    viewMsg();
  };
};

const notifyMe = (msg: string, viewMsg: Function) => {
  if (!('Notification' in window)) {
    nusiNotification(msg, viewMsg);
  } else if (Notification.permission === 'granted') {
    chromeNotification(msg, viewMsg);
  } else if (Notification.permission === 'default') {
    Notification.requestPermission((permission) => {
      if (permission === 'granted') {
        chromeNotification(msg, viewMsg);
      }
    });
  } else {
    nusiNotification(msg, viewMsg);
  }
};

export const MessageCenter = ({ show }: { show: boolean }) => {
  const params = routeInfoStore.useStore((s) => s.params);
  const { orgName = '-' } = params || {};
  const hasOrgRef = React.useRef(orgName !== '-');
  hasOrgRef.current = orgName !== '-';

  const [list, detail, msgPaging, unreadCount] = messageStore.useStore((s) => [
    s.list,
    s.detail,
    s.msgPaging,
    s.unreadCount,
  ]);
  const { getMessageList, getMessageStats, readOneMessage, clearAll } = messageStore.effects;
  const { resetDetail } = messageStore.reducers;
  const [loadingList] = useLoading(messageStore, ['getMessageList']);
  const { switchMessageCenter } = layoutStore.reducers;
  const boxRef = React.useRef<HTMLElement>();
  const loopUnreadCountTimer = React.useRef(0 as any);

  React.useEffect(() => {
    if (hasOrgRef.current) {
      getMessageStats();
      if (show) {
        getMessageList({ pageNo: 1 });
      }
    }

    return () => {
      messageStore.reducers.resetAll();
    };
  }, [getMessageList, getMessageStats, show, orgName]);

  const viewMsg = () => {
    switchMessageCenter(true);
  };

  useEffectOnce(() => {
    const cycle = 10 * 60 * 1000;
    // 每隔5min检查一次
    const interval = 5 * 60 * 1000;
    checkPermission();
    const loop = () => {
      let timers = Number(sessionStorage.getItem('message_timer') || 0);
      if (!timers) {
        timers = Date.now();
        sessionStorage.setItem('message_timer', `${timers}`);
      }
      if (loopUnreadCountTimer.current) {
        clearTimeout(loopUnreadCountTimer.current);
      }
      loopUnreadCountTimer.current = setTimeout(() => {
        const now = Date.now();
        if (now - timers > cycle) {
          sessionStorage.setItem('message_timer', `${now}`);
          if (hasOrgRef.current) {
            getMessageStats().then((res) => {
              if (res?.hasNewUnread) {
                if (show) {
                  // resetDetail();
                  getMessageList({ pageNo: 1 });
                }
                notifyMe(i18n.t('default:you have new site message, please pay attention to check'), viewMsg);
              }
            });
          }
        }
        loop();
      }, interval);
    };
    loop();
    return () => {
      clearTimeout(loopUnreadCountTimer.current);
    };
  });

  if (!show) {
    return null;
  }

  const handleClick = (item: LAYOUT.IMsg) => {
    readOneMessage(item.id, item.status === MSG_STATUS.READ);
  };

  let curDate = '';
  const groupList: Array<{ date: string; list: LAYOUT.IMsg[] }> = [];
  list.forEach((item) => {
    const date = moment(item.createdAt).format('YYYY-MM-DD');
    if (date !== curDate) {
      groupList.push({ date, list: [item] });
      curDate = date;
    } else {
      groupList[groupList.length - 1].list.push(item);
    }
  });

  const clearAllMessage = () => {
    Modal.confirm({
      title: i18n.t('confirm to read all'),
      onOk() {
        return clearAll().then(() => message.success(i18n.t('operated successfully')));
      },
    });
  };

  return (
    <div className="message-center" ref={boxRef as React.RefObject<HTMLDivElement>}>
      <div className="header">
        <CustomIcon type="arrow-left" onClick={() => layoutStore.reducers.switchMessageCenter(null)} />
        {i18n.t('site message')}
      </div>
      <div className="content">
        <div className="summary flex justify-between">
          {i18n.t('{unreadCount} messages unread', {
            unreadCount,
          })}

          <a className="mr-6 cursor-pointer" onClick={() => clearAllMessage()}>
            {i18n.t('one key all read')}
          </a>
        </div>
        <Holder when={!list.length}>
          <Timeline>
            {map(groupList, (group) => {
              return (
                <Timeline.Item key={group.date}>
                  <div>{group.date}</div>
                  <div className="message-list">
                    {group.list.map((item) => {
                      const isUnRead = item.status === MSG_STATUS.UNREAD;
                      return (
                        <div key={item.id} className="message-item" onClick={() => handleClick(item)}>
                          <div className="message-item-content flex items-center" title={item.title}>
                            <span className="status">{isUnRead ? <Badge color="red" /> : null}</span>
                            <ErdaIcon className="mr-1" type="remind" size="16px" />
                            <span>{item.title}</span>
                          </div>
                          <div>
                            {item.unreadCount > 1 && (
                              <span className="unread-count mr-3">
                                <span className="unread-count-text">
                                  {item.unreadCount > 99 ? '99+' : item.unreadCount}
                                </span>
                              </span>
                            )}
                            <span className="message-time">{moment(item.createdAt).format('HH:mm:ss')}</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </Timeline.Item>
              );
            })}
          </Timeline>
          <LoadMore
            getContainer={() => boxRef.current}
            load={() => getMessageList({ pageNo: msgPaging.pageNo + 1 })}
            hasMore={msgPaging.hasMore}
            isLoading={loadingList}
          />
          <Drawer
            width="60%"
            visible={!!detail}
            title={detail && detail.title}
            onClose={() => resetDetail()}
            destroyOnClose
            className="site-message-drawer"
          >
            <article
              // eslint-disable-next-line react/no-danger
              dangerouslySetInnerHTML={{ __html: Markdown((detail && detail.content) || '') }}
            />
          </Drawer>
        </Holder>
      </div>
    </div>
  );
};
