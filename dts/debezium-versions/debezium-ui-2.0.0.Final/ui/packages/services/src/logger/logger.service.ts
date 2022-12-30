/**
 * @license
 * Copyright 2020 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import {Service} from "../baseService";

/**
 * A simple logger service.
 */
export class LoggerService implements Service {

    public init(): void {
        // Nothing to init
    }

    public debug(message?: any, ...optionalParams: any[]): void {
        console.debug(message, ...optionalParams);
    }

    public info(message?: any, ...optionalParams: any[]): void {
        console.info(message, ...optionalParams);
    }

    public warn(message?: any, ...optionalParams: any[]): void {
        console.warn(message, ...optionalParams);
    }

    public error(message?: any, ...optionalParams: any[]): void {
        console.error(message, ...optionalParams);
    }

}
