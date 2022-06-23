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

import { cloneDeep } from 'lodash-es'
import { TypeHelpers } from 'spline-utils'

import { DtCellLayout } from './dt-row-layout.models'


export enum DtLayoutSize {
    sm = 'sm',
    md = 'md',
    lg = 'lg',
    xl = 'xl',
    xxl = 'xxl',
}

export class DtLayoutBuilder {

    private _layout: DtCellLayout

    constructor(initLayout: DtCellLayout = {}) {
        this._layout = initLayout
    }

    static addStyles(extraStyles: Partial<CSSStyleDeclaration>, layout: DtCellLayout = {}): DtCellLayout {
        return {
            ...layout,
            styles: {
                ...(layout.styles || {}),
                ...extraStyles
            },
        }
    }

    static alignCenter(layout: DtCellLayout = {}): DtCellLayout {
        return DtLayoutBuilder.addStyles(
            {
                justifyContent: 'center',
                textAlign: 'center',
            },
            layout
        )
    }

    static setCSSClass(classes: string[] | string, layout: DtCellLayout = {}): DtCellLayout {
        return {
            ...layout,
            classes: [
                ...(TypeHelpers.isArray(classes) ? classes : [classes])
            ]
        }
    }

    static addCSSClass(classes: string[] | string, layout: DtCellLayout = {}): DtCellLayout {
        return DtLayoutBuilder.setCSSClass(
            [
                ...(layout.classes || []),
                ...(TypeHelpers.isArray(classes) ? classes : [classes])
            ],
            layout
        )
    }

    static setWidth(width: string, layout: DtCellLayout = {}): DtCellLayout {
        return DtLayoutBuilder.addStyles(
            {
                maxWidth: width,
            },
            layout
        )
    }

    static visibleAfter(size: DtLayoutSize, layout: DtCellLayout = {}): DtCellLayout {
        return DtLayoutBuilder.addCSSClass(
            [`d-${size}-flex`, 'd-none'],
            layout
        )
    }

    toLayout(): DtCellLayout {
        return cloneDeep(this._layout)
    }

    addStyles(extraStyles: Partial<CSSStyleDeclaration>): this {
        this._layout = DtLayoutBuilder.addStyles(extraStyles, this._layout)
        return this
    }

    addCSSClass(classes: string[] | string): this {
        this._layout = DtLayoutBuilder.addCSSClass(classes, this._layout)
        return this
    }

    setCSSClass(classes: string[] | string): this {
        this._layout = DtLayoutBuilder.setCSSClass(classes, this._layout)
        return this
    }

    setWidth(width: string): this {
        this._layout = DtLayoutBuilder.setWidth(width, this._layout)
        return this
    }

    alignCenter(): this {
        return this.addStyles({
            justifyContent: 'center',
            textAlign: 'center',
        })
    }

    visibleAfter(size: DtLayoutSize): this {
        this._layout = DtLayoutBuilder.addCSSClass(
            [`d-${size}-flex`, 'd-none'],
            this._layout
        )
        return this
    }
}
