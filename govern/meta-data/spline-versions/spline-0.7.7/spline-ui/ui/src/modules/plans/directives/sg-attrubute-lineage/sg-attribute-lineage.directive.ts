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


import { AfterContentInit, Directive, forwardRef, Host, Inject, Input, OnChanges, SimpleChanges } from '@angular/core'
import { GraphComponent } from '@swimlane/ngx-graph'
import { OperationAttributeLineage, OperationAttributeLineageType } from 'spline-api'
import { getLinkDomSelector, getNodeDomSelector, SplineGraphComponent } from 'spline-common/graph'
import { SgContainerComponent } from 'spline-shared/graph'

import { extractAttributeLineageNodesMap } from '../../models/attribute'


@Directive({
    selector: 'sg-container[sgAttributeLineage]',
})
export class SgAttributeLineageDirective implements AfterContentInit, OnChanges {

    readonly domRelationDelayInMs = 500

    @Input() sgAttributeLineage: OperationAttributeLineage | null

    get splineGraph(): SplineGraphComponent {
        return this.sgContainerComponent.splineGraph
    }

    constructor(
        @Host() @Inject(forwardRef(() => SgContainerComponent)) private sgContainerComponent: SgContainerComponent,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {

        const {sgAttributeLineage} = changes

        if (sgAttributeLineage && !sgAttributeLineage.isFirstChange()) {
            setTimeout(
                () => this.applyLineageStyles(changes.sgAttributeLineage.currentValue, this.splineGraph.ngxGraphComponent),
                this.domRelationDelayInMs
            )

        }
    }

    ngAfterContentInit(): void {
        setTimeout(
            () => this.applyLineageStyles(this.sgAttributeLineage, this.splineGraph.ngxGraphComponent),
            this.domRelationDelayInMs
        )
    }

    private applyLineageStyles(attributeLineage: OperationAttributeLineage | null, ngxGraphComponent: GraphComponent): void {
        const attributesLineageNodesMap = extractAttributeLineageNodesMap(attributeLineage)

        const cssClassesMap = {
            [OperationAttributeLineageType.Usage]: 'sg-attribute-lineage--usage',
            [OperationAttributeLineageType.Lineage]: 'sg-attribute-lineage--lineage',
            [OperationAttributeLineageType.Impact]: 'sg-attribute-lineage--impact',
            none: 'sg-attribute-lineage--none',
        }

        const lineageTypesList = Object.values(OperationAttributeLineageType)

        function applyStylesToDomElm(selector: string,
                                     rootElmRef: HTMLElement,
                                     evaluationFn: (lineageType: OperationAttributeLineageType) => boolean,
        ): void {
            const elementRef = rootElmRef.querySelector(selector)
            if (elementRef) {
                let hasAnyType = false
                lineageTypesList
                    .forEach(lineageType => {
                        const refCssClassName = cssClassesMap[lineageType]
                        if (evaluationFn(lineageType)) {
                            elementRef.classList.add(refCssClassName)
                            hasAnyType = true
                        }
                        else {
                            elementRef.classList.remove(refCssClassName)
                        }
                    })

                // no type styles
                if (hasAnyType || attributeLineage === null) {
                    elementRef.classList.remove(cssClassesMap.none)
                }
                else {
                    elementRef.classList.add(cssClassesMap.none)
                }
            }
        }

        ngxGraphComponent.nodes
            .forEach(item =>
                applyStylesToDomElm(
                    getNodeDomSelector(item.id),
                    ngxGraphComponent.chart.nativeElement,
                    (lineageType) => attributesLineageNodesMap[lineageType].has(item.id),
                ),
            )

        ngxGraphComponent.links
            .forEach(item =>
                applyStylesToDomElm(
                    getLinkDomSelector(item.id),
                    ngxGraphComponent.chart.nativeElement,
                    (lineageType) => {
                        switch (lineageType) {
                            case OperationAttributeLineageType.Lineage:
                                return attributesLineageNodesMap[lineageType].has(item.source)
                                    && (
                                        attributesLineageNodesMap[lineageType].has(item.target)
                                        || attributesLineageNodesMap[OperationAttributeLineageType.Usage].has(item.target)
                                    )

                            case OperationAttributeLineageType.Impact:
                                return attributesLineageNodesMap[lineageType].has(item.target)
                                    && (
                                        attributesLineageNodesMap[lineageType].has(item.source)
                                        || attributesLineageNodesMap[OperationAttributeLineageType.Usage].has(item.source)
                                    )

                            default:
                                return attributesLineageNodesMap[lineageType].has(item.source)
                        }
                    },
                ),
            )
    }
}
