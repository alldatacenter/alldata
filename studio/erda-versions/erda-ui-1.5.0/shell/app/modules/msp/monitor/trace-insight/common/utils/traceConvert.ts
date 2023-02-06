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

import fp from 'lodash/fp';
import { mkDurationStr } from './traceSummary';
import { findIndex, flatMap, groupBy, Dictionary } from 'lodash';
import { ConstantNames, Constants } from 'trace-insight/common/utils/traceConstants';

interface timeMarkers {
  index: number;
  time: string;
}

interface Trace {
  traceId: string;
  duration: string;
  services: number;
  depth: number;
  totalSpans: number;
  timeMarkers: timeMarkers[];
  timeMarkersBackup: timeMarkers[];
  spans: Array<MONITOR_TRACE.ISpan>;
}

interface Entry {
  span: MONITOR_TRACE.ITraceSpan;
  children: Entry[];
}

const recursiveGetRootMostSpan = (
  idSpan: Dictionary<MONITOR_TRACE.ITraceSpan>,
  prevSpan: MONITOR_TRACE.ITraceSpan,
): MONITOR_TRACE.ITraceSpan => {
  if (prevSpan.parentSpanId && idSpan[prevSpan.parentSpanId]) {
    return recursiveGetRootMostSpan(idSpan, idSpan[prevSpan.parentSpanId]);
  }
  return prevSpan;
};

const getRootMostSpan = (traces: MONITOR_TRACE.ITraceSpan[]) => {
  const firstWithoutParent = traces.find((s) => !s.parentSpanId);
  if (firstWithoutParent) {
    return firstWithoutParent;
  }
  const idToSpanMap = fp.flow(
    fp.groupBy((s: MONITOR_TRACE.ITraceSpan) => s.id),
    fp.mapValues(([s]) => s),
  )(traces);
  return recursiveGetRootMostSpan(idToSpanMap, traces[0]);
};

const createSpanTreeEntry = (
  trace: MONITOR_TRACE.ITraceSpan,
  traces: MONITOR_TRACE.ITraceSpan[],
  indexByParentId?: Dictionary<MONITOR_TRACE.ITraceSpan[]>,
): Entry => {
  const idx =
    indexByParentId ||
    fp.flow(
      fp.filter((s: MONITOR_TRACE.ITraceSpan) => s.parentSpanId !== ''),
      fp.groupBy((s: MONITOR_TRACE.ITraceSpan) => s.parentSpanId),
    )(traces);

  return {
    span: trace,
    children: (idx[trace.id] || []).map((s: MONITOR_TRACE.ITraceSpan) => createSpanTreeEntry(s, traces, idx)),
  };
};

const treeDepths = (entry: Entry, startDepth: number): Obj<number> => {
  const initial = {};
  initial[entry.span.id] = startDepth;
  if (entry.children.length === 0) {
    return initial;
  }
  return (entry.children || []).reduce((prevMap, child) => {
    const childDepths = treeDepths(child, startDepth + 1);
    const newCombined = {
      ...prevMap,
      ...childDepths,
    };
    return newCombined;
  }, initial);
};

const toSpanDepths = (traces: MONITOR_TRACE.ITraceSpan[]) => {
  const root = getRootMostSpan(traces);
  const entry = createSpanTreeEntry(root, traces);
  return treeDepths(entry, 1);
};

/**
 *
 * @param traces
 * @return {{traceId: string; duration: number}}: duration: microsecond
 */
const traceSummary = (traces: MONITOR_TRACE.ITraceSpan[]): { traceId: string; duration: number } => {
  const duration = traces.reduce((prev, next) => {
    return prev + (next.endTime - next.startTime);
  }, 0);
  const { traceId } = traces[0];
  return {
    traceId,
    duration: duration / 1000,
  };
};

const getRootSpans = (traces: MONITOR_TRACE.ITraceSpan[]) => {
  const ids = traces.map((s) => s.id);
  return traces.filter((s) => ids.indexOf(s.parentSpanId) === -1);
};

const compareSpan = (s1: MONITOR_TRACE.ITraceSpan, s2: MONITOR_TRACE.ITraceSpan) => {
  return (s1.timestamp || 0) - (s2.timestamp || 0);
};

const childrenToList = (entry: Entry) => {
  const fpSort = (fn: ((a: Entry, b: Entry) => number) | undefined) => (list: Entry[]) => list.sort(fn);
  const deepChildren: Entry['span'][] = fp.flow(
    fpSort((e1: Entry, e2: Entry) => compareSpan(e1.span, e2.span)),
    fp.flatMap(childrenToList),
  )(entry.children || []);
  return [entry.span, ...deepChildren];
};

const traceConvert = (traces: MONITOR_TRACE.ITrace): Trace => {
  const { spans: oldSpans, duration: durationNs, depth, spanCount, serviceCount } = traces;
  if (!oldSpans?.length) {
    return {} as Trace;
  }
  const summary = traceSummary(oldSpans);
  const duration = durationNs / 1000;
  const spanDepths = toSpanDepths(oldSpans);
  const groupByParentId = groupBy(oldSpans, (s) => s.parentSpanId);
  let traceTimestamp = 0;

  const spans = flatMap(getRootSpans(oldSpans), (rootSpan) =>
    childrenToList(createSpanTreeEntry(rootSpan, oldSpans)),
  ).map((span, index) => {
    if (!index) {
      traceTimestamp = span.startTime;
    }
    const spanDuration = span.duration / 1000;
    const spanStartTs = span.startTime || traceTimestamp;
    const spanDepth = spanDepths[span.id] || 1;
    const width = ((spanDuration || 0) / duration) * 100;
    const errorType = span.tags.error ? 'critical' : 'none';

    const left = ((spanStartTs - traceTimestamp) / durationNs) * 100;

    return {
      ...span,
      spanId: span.id,
      parentId: span.parentSpanId || null,
      duration: spanDuration,
      durationStr: mkDurationStr(spanDuration),
      left: left >= 100 ? 0 : left,
      width: width < 0.1 ? 0.1 : width,
      depth: (spanDepth + 2) * 5,
      depthClass: (spanDepth - 1) % 6,
      children: (groupByParentId[span.id] || []).map((s) => s.id).join(','),
      annotations: (span.annotations || []).map((a) => ({
        isCore: Constants.CORE_ANNOTATIONS.indexOf(a.value) !== -1,
        left: ((a.timestamp - spanStartTs) / spanDuration) * 100,
        message: a.message,
        value: ConstantNames[a.value] || a.value,
        timestamp: a.timestamp,
        relativeTime: mkDurationStr(a.timestamp - traceTimestamp),
        width: 8,
      })),
      errorType,
    };
  });
  const timeMarkers = [0, 0.2, 0.4, 0.6, 0.8, 1].map((p, index) => ({
    index,
    time: mkDurationStr(duration * p),
  }));
  const timeMarkersBackup = timeMarkers;
  const spansBackup = spans;
  return {
    ...summary,
    totalSpans: spanCount,
    services: serviceCount,
    duration: mkDurationStr(duration),
    depth,
    spans,
    spansBackup,
    timeMarkers,
    timeMarkersBackup,
  };
};

export default traceConvert;
