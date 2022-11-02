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

export namespace SplineEntityStore {

    export interface EntityState<T> {
        ids: number[] | string[]
        entities: { [key: number]: T } | { [key: string]: T }
    }

    export interface EntityUpdate<T, TKey = string | number> {
        id: TKey
        update: Partial<T>
    }

    export function getDefaultState<T>(): EntityState<T> {
        return {
            ids: [],
            entities: {},
        }
    }

    export function selectIdFn<T>(entity: T): number | string {
        return entity['id'] as number | string
    }

    export function entitiesListToDictionary<T>(entities: T[],
                                                selectEntityIdFn: (entity: T) => number | string): Record<string | number, T> {
        return entities.reduce(
            (result, currentEntity) => {
                const id = selectEntityIdFn(currentEntity)
                result[id] = currentEntity
                return result
            }, {})
    }

    // REDUCERS

    export function addAll<T, TState extends EntityState<T> = EntityState<T>>(
        entities: T[],
        state: TState,
        selectId: (entity: T) => string | number = selectIdFn): TState {

        return {
            ...state,
            ids: entities.map(selectId),
            entities: entities.reduce(
                (result, currentEntity) => {
                    const id = selectId(currentEntity)
                    result[id] = currentEntity
                    return result
                }, {}),
        }
    }

    export function addMany<T, TState extends EntityState<T> = EntityState<T>>(
        entities: T[],
        state: TState,
        selectId: (entity: T) => string | number = selectIdFn): TState {

        return {
            ...state,
            ids: [...state.ids, ...entities.map(selectId)],
            entities: {
                ...state.entities,
                ...entitiesListToDictionary<T>(entities, selectId),
            },
        }
    }

    export function upsertMany<T, TState extends EntityState<T> = EntityState<T>>(
        entities: T[],
        state: TState,
        selectId: (entity: T) => string | number = selectIdFn): TState {

        return entities.reduce(
            (newState: TState, entity: T) => {
                const id = selectId(entity)
                return newState.entities[id]
                    ? updateOne({ id, update: entity }, newState)
                    : addOne(entity, newState, selectId)
            },
            state,
        )
    }

    export function addOne<T, TState extends EntityState<T> = EntityState<T>>(
        entity: T,
        state: TState,
        selectId: (entity: T) => string | number = selectIdFn): TState {

        const id = selectId(entity)

        return {
            ...state,
            ids: [...state.ids, id],
            entities: {
                ...state.entities,
                [id]: entity,
            },
        }
    }

    export function updateOne<T, TState extends EntityState<T> = EntityState<T>>(
        payload: EntityUpdate<T>,
        state: TState): TState {

        return {
            ...state,
            entities: {
                ...state.entities,
                [payload.id]: {
                    ...state.entities[payload.id],
                    ...payload.update,
                },
            },
        }
    }

    export function updateMany<T, TState extends EntityState<T> = EntityState<T>>(
        payload: EntityUpdate<T>[],
        state: TState): TState {

        const entitiesUpdate = payload.reduce(
            (result, currentEntityUpdate) => {
                result[currentEntityUpdate.id] = {
                    ...state.entities[currentEntityUpdate.id],
                    ...currentEntityUpdate.update,
                }
                return result
            },
            {},
        )

        return {
            ...state,
            entities: {
                ...state.entities,
                ...entitiesUpdate,
            },
        }
    }

    export function removeOne<T, TState extends EntityState<T> = EntityState<T>>(
        id: number | string,
        state: TState): TState {

        const ids = (state.ids as any[]).filter(item => item !== id)

        return {
            ...state,
            ids,
            entities: ids.reduce(
                (result, currentId) => {
                    result[currentId] = state.entities[currentId]
                    return result
                }, {}),
        }
    }

    export function removeMany<T, TState extends EntityState<T> = EntityState<T>>(
        ids: number[] | string[],
        state: TState): TState {

        const newIds = (state.ids as any[])
            .filter(
                item => !(ids as any[]).includes(item),
            )

        return {
            ...state,
            ids: newIds,
            entities: newIds.reduce(
                (result, currentId) => {
                    result[currentId] = state.entities[currentId]
                    return result
                }, {}),
        }
    }

    //
    // ADAPTER
    //

    export type AdapterSortFn<T, TState> = (entityLeft: T, entityRight: T, state: TState) => number;

    export interface Adapter<T, TState extends EntityState<T> = EntityState<T>> {
        addAll: (entities: T[], state: TState) => TState
        addMany: (entities: T[], state: TState) => TState
        upsertMany: (entities: T[], state: TState) => TState
        addOne: (entity: T, state: TState) => TState
        updateOne: (payload: EntityUpdate<T>, state: TState) => TState
        updateMany: (payload: EntityUpdate<T>[], state: TState) => TState
        removeOne: (id: number | string, state: TState) => TState
        removeMany: (ids: number[] | string[], state: TState) => TState
        selectIdFn: (entity: T) => number | string
        sortFn?: AdapterSortFn<T, TState>
    }

    export interface AdapterOptions<T, TState> {
        selectIdFn?: (entity: T) => number | string
        sortFn?: AdapterSortFn<T, TState>
    }

    export function getDefaultAdapterOptions<T, TState>(): AdapterOptions<T, TState> {
        return {
            selectIdFn: (entity: T) => selectIdFn<T>(entity),
        }
    }

    export function createAdapter<T, TState extends EntityState<T> = EntityState<T>>(
        options?: AdapterOptions<T, TState>,
    ): Adapter<T, TState> {
        const adapterOptions = { ...getDefaultAdapterOptions(), ...options }

        return {
            addAll: (entities: T[], state: TState) => {
                const newState = addAll<T, TState>(entities, state, adapterOptions.selectIdFn)
                return applySorting<T, TState>(newState, adapterOptions.sortFn)
            },
            addMany: (entities: T[], state: TState) => {
                const newState = addMany<T, TState>(entities, state, adapterOptions.selectIdFn)
                return applySorting<T, TState>(newState, adapterOptions.sortFn)
            },
            addOne: (entity: T, state: TState) => {
                const newState = addOne<T, TState>(entity, state, adapterOptions.selectIdFn)
                return applySorting<T, TState>(newState, adapterOptions.sortFn)
            },
            updateOne: (payload: EntityUpdate<T>, state: TState) => {
                const newState = updateOne<T, TState>(payload, state)
                return applySorting<T, TState>(newState, adapterOptions.sortFn)
            },
            updateMany: (payload: EntityUpdate<T>[], state: TState) => {
                const newState = updateMany<T, TState>(payload, state)
                return applySorting<T, TState>(newState, adapterOptions.sortFn)
            },
            upsertMany: (entities: T[], state: TState) => {
                const newState = upsertMany<T, TState>(entities, state, adapterOptions.selectIdFn)
                return applySorting<T, TState>(newState, adapterOptions.sortFn)
            },
            removeOne: (id: number | string, state: TState) => removeOne<T, TState>(id, state),
            removeMany: (ids: number[] | string[], state: TState) => removeMany<T, TState>(ids, state),
            selectIdFn: adapterOptions.selectIdFn,
            sortFn: adapterOptions.sortFn,
        }
    }

    function applySorting<T, TState extends EntityState<T> = EntityState<T>>(
        state: TState,
        sortFn?: AdapterSortFn<T, TState>,
    ): TState {
        if (!sortFn) {
            return state
        }

        const sortedIds = [...state.ids]
            .sort(
                (a: number | string, b: number | string) => sortFn(selectOne<T>(a, state), selectOne<T>(b, state), state),
            )
        return {
            ...state,
            ids: sortedIds,
        }

    }

    // SELECTORS

    export function selectAll<T, TState extends EntityState<T> = EntityState<T>>(state: TState): T[] {
        return (state.ids as any[])
            .map(id => state.entities[id])
    }

    export function selectByIds<T, TState extends EntityState<T> = EntityState<T>>(state: TState, ids: number[] | string[]): T[] {
        return (ids as any[])
            .map(id => state.entities[id])
    }

    export function selectOne<T, TState extends EntityState<T> = EntityState<T>>(id: number | string, state: TState): T | undefined {
        return state.entities[id]
    }

}
