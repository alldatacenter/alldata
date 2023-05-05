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

import moment from 'moment';
import {
  union,
  head,
  last,
  flatMap,
  findIndex,
  minBy,
  partition,
  groupBy,
  sortBy,
  toPairs,
  orderBy,
  identity,
} from 'lodash';
import fp from 'lodash/fp';
import { Constants } from './traceConstants';

function endpointsForSpan(span: any) {
  return union(
    (span.annotations || []).map((a: any) => a.endpoint),
    (span.binaryAnnotations || []).map((a: any) => a.endpoint),
  ).filter((h) => h != null);
}

// What's the total duration of the spans in this trace?
// 最后结束的（时间戳+耗时最大）- 第一个调用的时间戳
export function traceDuration(spans: any) {
  function makeList({ timestamp, duration }: any) {
    if (!timestamp) {
      return [];
    } else if (!duration) {
      return [timestamp];
    }
    return [timestamp, timestamp + duration];
  }
  // turns (timestamp, timestamp + duration) into an ordered list
  const timestamps = fp.flow(fp.flatMap(makeList), fp.sortBy(identity))(spans);

  if (timestamps.length < 2) {
    return null;
  }
  const firstTime = head(timestamps);
  const lastTime = last(timestamps);
  return lastTime - firstTime;
}

export function getServiceNames(span: any) {
  return fp.flow(
    fp.map((ep: any) => ep.serviceName),
    fp.filter((name) => name != null && name !== ''),
    fp.uniqBy(identity),
  )(endpointsForSpan(span));
}

// export function getServiceName(span) {
//   // Most authoritative is the label of the server's endpoint
//   const annotationFromServerAddr = find(span.binaryAnnotations || [], ann =>
//     ann.key === Constants.SERVER_ADDR
//         && ann.endpoint != null
//         && ann.endpoint.serviceName != null
//         && ann.endpoint.serviceName !== '');
//   const serviceNameFromServerAddr = annotationFromServerAddr ?
//     annotationFromServerAddr.endpoint.serviceName : null;

//   if (serviceNameFromServerAddr) {
//     return serviceNameFromServerAddr;
//   }

//   // Next, the label of any server annotation, logged by an instrumented server
//   const annotationFromServerSideAnnotations = find(span.annotations || [], ann =>
//     Constants.CORE_SERVER.indexOf(ann.value) !== -1
//       && ann.endpoint != null
//       && ann.endpoint.serviceName != null
//       && ann.endpoint.serviceName !== '');
//   const serviceNameFromServerSideAnnotation = annotationFromServerSideAnnotations ?
//     annotationFromServerSideAnnotations.endpoint.serviceName : null;

//   if (serviceNameFromServerSideAnnotation) {
//     return serviceNameFromServerSideAnnotation;
//   }

//   // Next is the label of the client's endpoint
//   const annotationFromClientAddr = find(span.binaryAnnotations || [], ann =>
//     ann.key === Constants.CLIENT_ADDR
//       && ann.endpoint != null
//       && ann.endpoint.serviceName != null
//       && ann.endpoint.serviceName !== '');
//   const serviceNameFromClientAddr = annotationFromClientAddr ?
//     annotationFromClientAddr.endpoint.serviceName : null;

//   if (serviceNameFromClientAddr) {
//     return serviceNameFromClientAddr;
//   }

//   // Next is the label of any client annotation, logged by an instrumented client
//   const annotationFromClientSideAnnotations = find(span.annotations || [], ann =>
//     Constants.CORE_CLIENT.indexOf(ann.value) !== -1
//       && ann.endpoint != null
//       && ann.endpoint.serviceName != null
//       && ann.endpoint.serviceName !== '');
//   const serviceNameFromClientAnnotation = annotationFromClientSideAnnotations ?
//     annotationFromClientSideAnnotations.endpoint.serviceName : null;

//   if (serviceNameFromClientAnnotation) {
//     return serviceNameFromClientAnnotation;
//   }

//   // Finally is the label of the local component's endpoint
//   const annotationFromLocalComponent = find(span.binaryAnnotations || [], ann =>
//     ann.key === Constants.LOCAL_COMPONENT
//       && ann.endpoint != null
//       && ann.endpoint.serviceName != null
//       && ann.endpoint.serviceName !== '');
//   const serviceNameFromLocalComponent = annotationFromLocalComponent ?
//     annotationFromLocalComponent.endpoint.serviceName : null;

//   if (serviceNameFromLocalComponent) {
//     return serviceNameFromLocalComponent;
//   }

//   return null;
// }

export function getServiceName(span: any) {
  const firstAnn = span.binaryAnnotations[0];
  return firstAnn ? firstAnn.endpoint.serviceName : null;
}

function getSpanTimestamps(spans: any) {
  return flatMap(spans, (span) =>
    getServiceNames(span).map((serviceName) => ({
      name: serviceName,
      timestamp: span.timestamp,
      duration: span.duration,
    })),
  );
}

// returns 'critical' if one of the spans has an ERROR binary annotation, else
// returns 'transient' if one of the spans has an ERROR annotation, else
// returns 'none'
export function getTraceErrorType(spans: any) {
  let traceType = 'none';
  for (let i = 0; i < spans.length; i++) {
    const span = spans[i];
    if (findIndex(span.binaryAnnotations || [], (ann: any) => ann.key === Constants.ERROR) !== -1) {
      return 'critical';
    } else if (
      traceType === 'none' &&
      findIndex(span.annotations || [], (ann: any) => ann.value === Constants.ERROR) !== -1
    ) {
      traceType = 'transient';
    }
  }
  return traceType;
}

function endpointEquals(e1: any, e2: any) {
  return (e1.ipv4 === e2.ipv4 || e1.ipv6 === e2.ipv6) && e1.port === e2.port && e1.serviceName === e2.serviceName;
}

export function traceSummary(spans: any = []) {
  if (spans.length === 0 || !spans[0].timestamp) {
    return null;
  }
  const duration = traceDuration(spans) || 0; // 获取span时间轴长（最大时间-最小时间）
  const endpoints = fp.flow(
    // 筛选出所有的终端(获取binaryAnnotations中的serviceName)
    fp.flatMap(endpointsForSpan),
    fp.uniqWith(endpointEquals),
  )(spans);
  const { traceId, timestamp } = spans[0];

  const spanTimestamps = getSpanTimestamps(spans);

  const errorType = getTraceErrorType(spans);
  return {
    traceId,
    timestamp,
    duration,
    spanTimestamps,
    endpoints,
    errorType,
  };
}

export function totalServiceTime(stamps: any, acc = 0): any {
  // This is a recursive function that performs arithmetic on duration
  // If duration is undefined, it will infinitely recurse. Filter out that case
  const filtered = stamps.filter((s: any) => s.duration);
  if (filtered.length === 0) {
    return acc;
  }
  const ts = minBy(filtered, (s: any) => s.timestamp);
  const [current, next] = partition(
    filtered,
    (t) => t.timestamp >= ts.timestamp && t.timestamp + t.duration <= ts.timestamp + ts.duration,
  );
  const endTs = Math.max(...current.map((t) => t.timestamp + t.duration));
  return totalServiceTime(next, acc + (endTs - ts.timestamp));
}

function formatDate(timestamp: number, utc: any) {
  let m = moment(timestamp / 1000);
  if (utc) {
    m = m.utc();
  }
  return m.format('MM-DD-YYYYTHH:mm:ss.SSSZZ');
}

export function getGroupedTimestamps(summary: any) {
  return groupBy(summary.spanTimestamps, (sts) => sts.name);
}

export function getServiceDurations(groupedTimestamps: any) {
  return sortBy(
    toPairs(groupedTimestamps).map(([name, sts]: any) => ({
      name,
      count: sts.length,
      max: parseInt(`${Math.max(...sts.map((t: any) => t.duration)) / 1000}`, 10),
    })),
    'name',
  );
}

export function mkDurationStr(duration: any) {
  if (duration < 1000) {
    return `${duration}μs`;
  } else if (duration < 1000000) {
    return `${(duration / 1000).toFixed(3)}ms`;
  }
  return `${(duration / 1000000).toFixed(3)}s`;
}

export function traceSummariesToMustache(serviceName: any = null, traceSummaries: any, utc = false) {
  if (traceSummaries.length === 0) {
    return [];
  }
  const maxDuration = Math.max(...traceSummaries.map((s: any) => s.duration)) / 1000;

  const convertedTraces = traceSummaries.map((t: any) => {
    const duration = t.duration / 1000;
    const groupedTimestamps = getGroupedTimestamps(t);
    const serviceDurations = getServiceDurations(groupedTimestamps);

    let serviceTime;
    if (!serviceName || !groupedTimestamps[serviceName]) {
      serviceTime = 0;
    } else {
      serviceTime = totalServiceTime(groupedTimestamps[serviceName]);
    }

    const startTs = formatDate(t.timestamp, utc);
    const durationStr = mkDurationStr(t.duration);
    const servicePercentage = parseInt(`${(parseFloat(`${serviceTime}`) / parseFloat(t.duration)) * 100}`, 10);
    const spanCount = Object.values(groupedTimestamps).reduce((s: any, a: any) => s + a.length, 0);
    const width = parseInt(`${(parseFloat(`${duration}`) / parseFloat(`${maxDuration}`)) * 100}`, 10);
    const infoClass = t.errorType === 'none' ? '' : `trace-error-${t.errorType}`;

    return {
      traceId: t.traceId,
      startTs,
      timestamp: t.timestamp,
      duration,
      durationStr,
      servicePercentage,
      spanCount,
      serviceDurations,
      width,
      infoClass,
    };
  });
  return orderBy(convertedTraces, ['timestamp'], ['asc']);
}
