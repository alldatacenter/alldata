/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  ChartMouseEvent,
  ChartStatus,
  IChart,
  IChartLifecycle,
} from 'app/types/Chart';
import { ChartConfig, ChartDataConfig } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { BrokerContext, BrokerOption } from 'app/types/ChartLifecycleBroker';
import ChartMetadata from 'app/types/ChartMetadata';
import { isInRange } from 'app/utils/internalChartHelper';

class Chart implements IChart, IChartLifecycle {
  private _state: ChartStatus = 'init';
  private _stateHistory: ChartStatus[] = [];

  public meta: ChartMetadata;
  public config?: ChartConfig;
  public dataset?: ChartDataSetDTO;
  public dependency: string[] = [];
  public isISOContainer: boolean | string = false;
  public useIFrame: boolean = true;
  public mouseEvents?: ChartMouseEvent[] = [];

  set state(state: ChartStatus) {
    this._state = state;
    this._stateHistory.push(state);
  }

  get state() {
    return this._state;
  }

  constructor(id: string, name: string, icon?: string, requirements?: []) {
    this.meta = {
      id,
      name,
      icon: icon,
      requirements,
    };
    this.state = 'init';
  }

  public init(config: any) {
    this.config = config;
  }

  public registerMouseEvents(events: Array<ChartMouseEvent>) {
    this.mouseEvents = events;
  }

  public isMatchRequirement(targetConfig?: ChartConfig): boolean {
    if (!targetConfig) {
      return true;
    }
    return this.isMatchRequiredSectionLimitation(
      this.config?.datas,
      targetConfig?.datas,
    );
  }

  public getDependencies(): string[] {
    return this.dependency;
  }

  public onMount(options: BrokerOption, context: BrokerContext): void {
    throw new Error(`${this.meta.name} - onMount Method Not Implemented.`);
  }

  public onUpdated(options: BrokerOption, context: BrokerContext): void {
    throw new Error(`${this.meta.name} - onUpdated Method Not Implemented.`);
  }

  public onUnMount(options: BrokerOption, context: BrokerContext): void {
    throw new Error(`${this.meta.name} - onUnMount Method Not Implemented.`);
  }

  public onResize(options: BrokerOption, context: BrokerContext): void {}

  private isMatchRequiredSectionLimitation(
    current?: ChartDataConfig[],
    target?: ChartDataConfig[],
  ) {
    const getDrillableRowCount = (
      isDrillable = false,
      originalRowLength = 0,
    ) => {
      return isDrillable ? Math.min(1, originalRowLength) : originalRowLength;
    };

    return (current || [])
      .filter(cc => Boolean(cc?.required))
      .every(cc => {
        // The typed chart config section relation matching logic:
        // 1. If section type exactly match, use it
        // 2. Else if, section type and key exactly match, use it
        // 3. Else if, section is drillable and target rows length is min value between 1 and rows length
        // 4. Else, current section will match all target typed sections
        const tc = target?.filter(tc => tc.type === cc.type) || [];
        if (tc?.length > 1) {
          const subTc = tc?.find(stc => stc.key === cc.key);
          if (!subTc) {
            const subTcTotalLength = tc
              .flatMap(tc => tc.rows)
              ?.filter(Boolean)?.length;
            return isInRange(
              cc?.limit,
              getDrillableRowCount(cc?.drillable, subTcTotalLength),
            );
          }
          return isInRange(
            cc?.limit,
            getDrillableRowCount(cc?.drillable, subTc?.rows?.length),
          );
        }
        return isInRange(
          cc?.limit,
          getDrillableRowCount(cc?.drillable, tc?.[0]?.rows?.length),
        );
      });
  }
}

export default Chart;
