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

import { CollectionViewer, DataSource } from '@angular/cdk/collections'
import { ChangeDetectorRef, Component, ElementRef, Renderer2 } from '@angular/core'
import { BehaviorSubject, Observable, Subscription } from 'rxjs'
import { BaseComponent } from 'spline-utils'


export class MyDataSource extends DataSource<string | undefined> {
    private _length = 1000
    private _pageSize = 100
    private _cachedData = Array.from<string>({ length: this._length })
    private _fetchedPages = new Set<number>()
    private _dataStream = new BehaviorSubject<(string | undefined)[]>(this._cachedData)
    private _subscription = new Subscription()

    get length(): number {
        return this._length
    }

    connect(collectionViewer: CollectionViewer): Observable<(string | undefined)[]> {
        this._subscription.add(collectionViewer.viewChange.subscribe(range => {
            const startPage = this._getPageForIndex(range.start)
            const endPage = this._getPageForIndex(range.end - 1)
            for (let i = startPage; i <= endPage; i++) {
                this._fetchPage(i)
            }
        }))
        return this._dataStream
    }

    disconnect(): void {
        this._subscription.unsubscribe()
    }

    private _getPageForIndex(index: number): number {
        return Math.floor(index / this._pageSize)
    }

    private _fetchPage(page: number) {
        if (this._fetchedPages.has(page)) {
            return
        }
        this._fetchedPages.add(page)

        // Use `setTimeout` to simulate fetching data from server.
        setTimeout(() => {
            this._cachedData.splice(page * this._pageSize, this._pageSize,
                ...Array.from({ length: this._pageSize })
                    .map((_, i) => `Item #${page * this._pageSize + i}`))
            this._dataStream.next(this._cachedData)
        }, Math.random() * 1000 + 200)
    }
}


@Component({
    selector: 'spline-sticky-container',
    templateUrl: './sticky-container.component.html',
    styleUrls: ['./sticky-container.component.scss'],
})
export class StickyContainerComponent extends BaseComponent {


    readonly itemHeight = 50

    ds = new MyDataSource()

}
