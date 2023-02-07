import useRequest from '@/services/request';

const {
  get,
} = useRequest();

export const initUmeng = async () => {
  // import umeng.js
  await import('@/assets/js/umeng.js');

  if (localStorage.getItem('useStats') !== '0') {
    const res = await get('/version');
    const version = res.data;
    const {aplus_queue} = window;

    // set meta info
    aplus_queue.push({
      action: 'aplus.setMetaInfo',
      arguments: ['globalproperty', {version}, 'OVERWRITE'],
    });

    // send pv
    window.aplus_queue.push({
      action: 'aplus.sendPV',
      arguments: [{is_auto: true}, {}]
    });
  }
};

export const getEventParamsWrapped = (eventParams?: TrackEventParams): TrackEventParamsWrapped => {
  if (!eventParams) return {};
  const res: TrackEventParamsWrapped = {};
  Object.keys(eventParams).forEach(key => {
    const value = eventParams[key];
    if (typeof value === 'function') {
      res[key] = value();
    } else {
      res[key] = value;
    }
  });
  return res;
};

export const sendEvent = (eventCode: string, eventParams?: TrackEventParams, eventType?: TrackEventType) => {
  window.aplus_queue?.push({
    action: 'aplus.record',
    arguments: [eventCode, eventType || 'CLK', getEventParamsWrapped(eventParams)],
  });
};

export const wrapTrack = (payload: TrackSendEventPayload, func: Function) => {
  const {
    eventCode,
    eventParams,
    eventType,
  } = payload;
  sendEvent(eventCode, eventParams, eventType);
  func();
};
