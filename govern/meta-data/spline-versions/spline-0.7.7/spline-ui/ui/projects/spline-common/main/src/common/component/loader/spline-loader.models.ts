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


export namespace SplineLoader {

    export type Size = 'xs' | 'sm' | 'md' | 'lg'

    export const Size = {
        xs: 'xs' as Size,
        sm: 'sm' as Size,
        md: 'md' as Size,
        lg: 'lg' as Size,
    }

    export const SPINNER_DIAMETER_MAP: Readonly<Record<Size, number>> = Object.freeze<Record<Size, number>>({
        lg: 120,
        md: 50,
        sm: 36,
        xs: 24,
    })

    export type Mode = 'block' | 'inline' | 'floating' | 'cover'

    export const Mode = {
        block: 'block' as Mode,
        inline: 'inline' as Mode,
        floating: 'floating' as Mode,
        cover: 'cover' as Mode,
    }
}
