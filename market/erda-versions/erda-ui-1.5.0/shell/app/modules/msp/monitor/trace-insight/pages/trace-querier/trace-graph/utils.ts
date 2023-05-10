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

import { mkDurationStr } from 'trace-insight/common/utils/traceSummary';
import { useLayoutEffect, useRef } from 'react';

const TOOLTIP_OFFSET = 4;

export function listToTree(arr: MONITOR_TRACE.ISpanItem[] = []) {
  const list = arr.map((x) => ({ ...x, children: [] })).sort((a, b) => a.startTime - b.startTime);
  const treeMap = {};
  const roots = [] as MONITOR_TRACE.ISpanItem[];
  const existIds = [];
  let min = Infinity;
  let max = -Infinity;

  for (let i = 0; i < list.length; i++) {
    const node = list[i];
    treeMap[node?.id] = i;
    existIds.push(node?.id);
    min = Math.min(min, node?.startTime);
    max = Math.max(max, node?.endTime);
  }

  for (let i = 0; i < list.length; i += 1) {
    const node = list[i];
    const parentSpanId = node?.parentSpanId;
    if (parentSpanId !== '' && existIds.includes(parentSpanId)) {
      list[treeMap[parentSpanId]].children.push(node);
    } else {
      roots.push(node);
    }
  }

  return { roots, min, max };
}

export const displayTimeString = (time: number) => {
  return time && time !== -Infinity ? mkDurationStr(time / 1000) : 0;
};

export function useSmartTooltip({ mouseX, mouseY }: { mouseX: number; mouseY: number }) {
  const ref = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => {
    const element = ref.current;
    if (element != null) {
      if (mouseY + TOOLTIP_OFFSET + element?.offsetHeight >= window.innerHeight) {
        if (mouseY - TOOLTIP_OFFSET - element.offsetHeight > 0) {
          element.style.top = `${mouseY - element.offsetHeight - TOOLTIP_OFFSET}px`;
        } else {
          element.style.top = '0px';
        }
      } else {
        element.style.top = `${mouseY + TOOLTIP_OFFSET}px`;
      }

      if (mouseX + TOOLTIP_OFFSET + element.offsetWidth >= window.innerWidth) {
        if (mouseX - TOOLTIP_OFFSET - element.offsetWidth > 0) {
          element.style.left = `${mouseX - element.offsetWidth - TOOLTIP_OFFSET}px`;
        } else {
          element.style.left = '0px';
        }
      } else {
        element.style.left = `${mouseX + TOOLTIP_OFFSET}px`;
      }
    }
  });

  return ref;
}
