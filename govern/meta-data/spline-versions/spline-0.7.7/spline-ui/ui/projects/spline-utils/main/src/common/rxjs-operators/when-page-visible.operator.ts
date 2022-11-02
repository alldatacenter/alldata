/*
 * Copyright 2021 ABSA Group Limited
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

import { fromEvent, Observable } from 'rxjs'
import { filter, repeatWhen, shareReplay, takeUntil } from 'rxjs/operators'


export function whenPageVisible() {
    const visibilitychange$ = fromEvent(document, 'visibilitychange').pipe(
        shareReplay({ refCount: true, bufferSize: 1 })
    )

    const pageVisible$ = visibilitychange$.pipe(
        filter(() => document.visibilityState === 'visible')
    )

    const pageHidden$ = visibilitychange$.pipe(
        filter(() => document.visibilityState === 'hidden')
    )

    return function <T>(source: Observable<T>) {
        return source.pipe(
            takeUntil(pageHidden$),
            repeatWhen(() => pageVisible$)
        )
    }
}
