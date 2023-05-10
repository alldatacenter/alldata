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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import { Button, Col, Drawer, Row, Spin, Tooltip } from 'antd';
import TraceDetail from 'trace-insight/pages/trace-detail';
import { Copy, EmptyHolder, Icon as CustomIcon, IF, SimpleLog, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { get, isEmpty, map } from 'lodash';
import moment from 'moment';
import monitorErrorStore from 'error-insight/stores/error';
import { useLoading } from 'core/stores/loading';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';

import './error-detail.scss';

const InfoCompMap = {
  log: {
    title: i18n.t('msp:log details'),
    comp: (props: any) => <SimpleLog {...props} />,
  },
  trace: {
    title: i18n.t('msp:tracing details'),
    comp: (props: any) => <TraceDetail {...props} />,
  },
};

const ErrorDetail = () => {
  const [eventIds, eventDetail] = monitorErrorStore.useStore((s) => [s.eventIds, s.eventDetail]);
  const [getEventIdsLoading, getEventDetailLoading] = useLoading(monitorErrorStore, ['getEventIds', 'getEventDetail']);
  const { getEventIds, getEventDetail } = monitorErrorStore.effects;
  const { clearEventDetail } = monitorErrorStore.reducers;

  const [{ eventIndex, showAllStacks, infoVisible, infoType, infoProps }, updater, update] = useUpdate({
    eventIndex: 0,
    showAllStacks: true,
    infoVisible: false,
    infoType: '',
    infoProps: {},
  });

  useEffectOnce(() => {
    getEventIds();
    return () => {
      clearEventDetail();
    };
  });

  const getDetail = (q?: any) => {
    const index = get(q, 'index') ?? eventIndex;
    const currentEvtId = index >= 0 && get(eventIds, `[${index}]`);
    currentEvtId && getEventDetail({ id: currentEvtId });
  };

  React.useEffect(() => {
    getDetail();
  }, [eventIds]);

  const changeEvent = (num: number | string) => {
    const reNum = num === 'first' ? -eventIndex : num === 'last' ? eventIds.length - 1 - eventIndex : (num as number);
    const newIndex = eventIndex + reNum;
    if (newIndex < 0 || newIndex > eventIds.length - 1) return;
    updater.eventIndex(newIndex);
    getDetail({ index: newIndex });
  };

  const toggleShowAllStacks = () => {
    updater.showAllStacks(!showAllStacks);
  };

  const getStackItem = (info?: { stack: MONITOR_ERROR.IStacks }) => {
    if (!info || isEmpty(info?.stack)) {
      return <EmptyHolder relative tip={i18n.t('msp:no stack')} />;
    }
    const { className, methodName, line, index } = info.stack;
    return (
      <div key={index} className="stack-item">
        {className}
        <span> in </span>
        {methodName}
        {line && line > 0 ? (
          <React.Fragment>
            <span> at line </span>
            {line}
          </React.Fragment>
        ) : null}
      </div>
    );
  };

  const getTagRender = (tags: any) => {
    if (isEmpty(tags)) return;
    const { application_name, language, project_name, runtime_name, service_name } = tags;
    const tagsObj = { application_name, language, project_name, runtime_name, service_name };
    return (
      <ul>
        {map(tagsObj, (val, key) => (
          <li key={val + key}>
            <div className="object-key">{key}</div>
            <div className="object-value">{val}</div>
          </li>
        ))}
      </ul>
    );
  };

  const getRequestRender = (data: any) => {
    if (!data) return;
    const { requestHeaders, host = '', method = '', path = '', query_string, body, cookies } = data;
    const objectRender = (objStr: any, title: string, splitStr?: string) => {
      let __content = null;
      if (objStr) {
        if (splitStr) {
          let i = 0;
          __content = map(splitStr === 'object' ? objStr : objStr.split(splitStr), (v, k) => {
            const [key, value] = splitStr === 'object' ? [k, v] : v.split('=');
            i += 1;
            return (
              <Row key={key + i}>
                <Col span={6} className="object-key">
                  {key}
                </Col>
                <Col span={18} className="object-value">
                  {value}
                </Col>
              </Row>
            );
          });
        } else {
          __content = <pre>{objStr}</pre>;
        }
      }
      return (
        <div className="request-item">
          <div className="sub-title">{title}</div>
          {__content}
        </div>
      );
    };
    return (
      <React.Fragment>
        <div className="request-info">
          <Row>
            <Col span={6} className="sub-title">
              Request
            </Col>
            <Col span={18} className="object-value">
              {`${method}`}&nbsp;&nbsp;&nbsp;&nbsp;{`${host}`}&nbsp;&nbsp;&nbsp;&nbsp;{`${path}`}
            </Col>
          </Row>
        </div>
        {objectRender(query_string, 'Query', '&')}
        {objectRender(body, 'Body')}
        {objectRender(cookies, 'Cookies', ';')}
        {objectRender(requestHeaders, 'Headers', 'object')}
      </React.Fragment>
    );
  };

  const showDrawerInfo = ({ type, payload }: { type: string; payload: object }) => {
    update({
      infoVisible: true,
      infoType: type,
      infoProps: payload,
    });
  };

  const closeDrawerInfo = () => {
    update({
      infoVisible: false,
      infoType: '',
      infoProps: {},
    });
  };
  const isFetching = getEventIdsLoading || getEventDetailLoading;
  const { comp: InfoComp, title } = InfoCompMap[infoType] || ({} as any);

  const {
    id: eventId,
    requestId,
    timestamp,
    stacks,
    tags,
    metadata,
    requestContext,
    requestHeaders,
    requestSampled,
  } = eventDetail || ({} as MONITOR_ERROR.IEventDetail);
  const { exception_message, file, message, type } = metadata || {};
  const exceptionMsg =
    exception_message && exception_message.length > 150 ? (
      <Tooltip title={exception_message} overlayClassName="error-insight-error-msg-tip">
        {exception_message}
      </Tooltip>
    ) : (
      exception_message
    );
  return (
    <Spin spinning={isFetching} wrapperClassName="error-detail">
      <IF check={!isEmpty(eventDetail)}>
        <Row>
          <Col span={18} className="left">
            <div className="content-block head-container">
              <span className="name">{type}</span>
              <span className="info">{metadata && `${metadata.class} at ${file}`}</span>
              <div className="page-info">{`${eventIndex + 1} / ${eventIds.length}`}</div>
              <div className="error-msg">{exceptionMsg}</div>
            </div>
            <div className="content-block event-container">
              <div className="event">
                <span>Event: </span>
                <span className="event-id">{eventId}</span>
                <div className="info">{moment(timestamp).format('YYYY-MM-DD HH:mm:ss')}</div>
              </div>
              <div className="arrow flex">
                <Button
                  disabled={eventIndex === 0}
                  className={`first-page ${eventIndex === 0 ? 'edge' : ''}`}
                  onClick={() => changeEvent('first')}
                >
                  <ErdaIcon className="mt-1" type="to-left" size="14px" />
                </Button>
                <Button
                  disabled={eventIndex === 0}
                  className={`prev-page ${eventIndex === 0 ? 'edge' : ''}`}
                  onClick={() => changeEvent(-1)}
                >
                  <span>{i18n.t('msp:previous')}</span>
                </Button>
                <Button
                  disabled={eventIndex === eventIds.length - 1}
                  className={`next-page ${eventIndex === eventIds.length - 1 ? 'edge' : ''}`}
                  onClick={() => changeEvent(1)}
                >
                  <span>{i18n.t('common:next')}</span>
                </Button>
                <Button
                  disabled={eventIndex === eventIds.length - 1}
                  className={`last-page ${eventIndex === eventIds.length - 1 ? 'edge' : ''}`}
                  onClick={() => changeEvent('last')}
                >
                  <ErdaIcon className="mt-1" type="to-right" size="14px" />
                </Button>
              </div>
            </div>
            <IF check={requestId}>
              <div className="content-block requestid-item">
                <span className="request-label">Request Id: </span>
                <Tooltip title={i18n.t('click to copy')}>
                  <span className="requestid-text cursor-copy">
                    <Copy>{requestId}</Copy>
                  </span>
                </Tooltip>
                <Tooltip title={i18n.t('msp:view log')}>
                  <CustomIcon
                    type="module-log"
                    className="check-request-action"
                    onClick={() =>
                      showDrawerInfo({ type: 'log', payload: { requestId, applicationId: tags.application_id } })
                    }
                  />
                </Tooltip>
                <IF check={requestSampled}>
                  <Tooltip title={i18n.t('msp:view tracing information')}>
                    <CustomIcon
                      type="module-trace"
                      className="check-request-action"
                      onClick={() => showDrawerInfo({ type: 'trace', payload: { traceId: requestId } })}
                    />
                  </Tooltip>
                </IF>
              </div>
            </IF>

            <div className="content-block msg-container">
              <div className="content-title">{i18n.t('msp:information')}：</div>
              <div className="msg-txt">{message}</div>
            </div>
            <div className="content-block stacklist-container">
              <div className="content-title stacks-title">
                {`${i18n.t('msp:error stack')}:   ${type}`}
                <IF check={stacks && stacks.length > 1}>
                  <Button className="toggle-stacks" onClick={toggleShowAllStacks}>
                    {showAllStacks ? (
                      <ErdaIcon type="down" size="20px" className="mr-0 mt-0.5" />
                    ) : (
                      <ErdaIcon type="up" size="20px" className="mr-0 mt-0.5" />
                    )}
                  </Button>
                </IF>
              </div>
              <div className="error-msg">{exceptionMsg}</div>
              <div className="stack-list">
                {stacks?.length
                  ? showAllStacks
                    ? map(stacks || [], (item) => getStackItem(item))
                    : getStackItem((stacks || [])[0])
                  : getStackItem()}
                <IF check={stacks && stacks.length > 1}>
                  <div className="stack-item omit-item" onClick={toggleShowAllStacks}>
                    {showAllStacks ? (
                      <ErdaIcon type="up" size="20px" className="mr-0 mt-0.5" />
                    ) : (
                      <ErdaIcon type="down" size="20px" className="mr-0 mt-0.5" />
                    )}
                  </div>
                </IF>
              </div>
            </div>
            <IF check={!isEmpty(requestHeaders) || !isEmpty(requestContext)}>
              <div className="content-block request-container">
                <div className="content-title">{i18n.t('request information')}</div>
                {getRequestRender({ requestHeaders, ...requestContext })}
              </div>
            </IF>
          </Col>
          <Col span={6}>
            <div className="tag-container">
              <div className="content-title">Tags</div>
              {getTagRender(tags)}
            </div>
          </Col>
        </Row>
        <IF.ELSE />
        <EmptyHolder relative />
      </IF>

      <Drawer destroyOnClose title={title} width="80%" visible={infoVisible} onClose={closeDrawerInfo}>
        {InfoComp && <InfoComp {...infoProps} />}
      </Drawer>
    </Spin>
  );
};

export default ErrorDetail;
