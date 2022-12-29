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

import { CloneValueDeep } from 'utils/object';
import { APP_SEMANTIC_VERSIONS } from './constants';
import { IDomainEvent } from './types';

/**
 * Migration Event Dispatcher
 * [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
 * @class EventDispatcher
 * @template TDomainModel
 */
class MigrationEventDispatcher<TDomainModel extends { version?: string }> {
  domainEvents: IDomainEvent<TDomainModel>[] = [];

  constructor(...events: IDomainEvent<TDomainModel>[]) {
    this.domainEvents = events;
  }

  public process(model: TDomainModel, drawback?: TDomainModel) {
    const drawbackModel = drawback || CloneValueDeep(model);

    try {
      return this.domainEvents.reduce((acc, event) => {
        if (this.shouldDoEventSourcing(acc.version, event.version)) {
          return event.run(acc) || acc;
        }
        return acc;
      }, model);
    } catch {
      return drawbackModel;
    }
  }

  private shouldDoEventSourcing(
    modelVersion?: string,
    eventVersion?: string,
  ): boolean {
    const modelVerIndex = APP_SEMANTIC_VERSIONS.findIndex(
      v => v === modelVersion,
    );
    const currentEventVerIndex = APP_SEMANTIC_VERSIONS.findIndex(
      v => v === eventVersion,
    );
    if (currentEventVerIndex === 0) {
      return modelVerIndex < currentEventVerIndex;
    } else if (currentEventVerIndex > 0) {
      const domainEventIndex = this.domainEvents.findIndex(
        dEvent => dEvent.version === eventVersion,
      );
      if (domainEventIndex <= 0) {
        return modelVerIndex < currentEventVerIndex;
      } else {
        const prevDomainEventIndex = domainEventIndex - 1;
        const prevEventVersion =
          this.domainEvents[prevDomainEventIndex].version;

        const prevEventVerIndex = APP_SEMANTIC_VERSIONS.findIndex(
          v => v === prevEventVersion,
        );
        return (
          prevEventVerIndex <= modelVerIndex &&
          modelVerIndex < currentEventVerIndex
        );
      }
    }
    return false;
  }
}

export default MigrationEventDispatcher;
