/*
 * Copyright 2020 ABSA Group Limited
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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core'

import { SgNodeControl } from '../../models'


@Component({
    selector: 'sg-node-view-control',
    templateUrl: './sg-node-view-control.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SgNodeViewControlComponent {

    @Input() nodeView: SgNodeControl.NodeView = SgNodeControl.NodeView.Detailed

    @Output() viewChanged$ = new EventEmitter<SgNodeControl.NodeView>()

    readonly NodeView = SgNodeControl.NodeView

    onToggleViewBtnClicked(): void {
        const newNodeView = this.nodeView === SgNodeControl.NodeView.Detailed
            ? SgNodeControl.NodeView.Compact
            : SgNodeControl.NodeView.Detailed

        this.viewChanged$.next(newNodeView)
    }
}
