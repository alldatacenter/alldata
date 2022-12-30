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

import { Service } from "../baseService";
import { ConfigType, FeaturesConfig } from './config.type';
import _ from 'lodash'

/**
 * A simple configuration service.  Reads information from a global "DebeziumUiConfig" variable
 * that is typically included via JSONP.
 */
export class ConfigService implements Service {
    private config: ConfigType = {
        artifacts: {
            type: "rest",
            url: "http://localhost:8080/api/",
            newUrl: "http://localhost:8080/"
        },
        "deployment.mode": "",
        features: {
            readOnly: false
        },
        mode: "dev",
        ui: {
            contextPath: '',
            url: "http://localhost:8888/"
        }
    };

    constructor() {
        const w: any = window;

        if (w.UI_CONFIG?.mode === "prod") {
            this.config = _.extend({}, this.config, w.UI_CONFIG, {
              artifacts: {
                type: "rest",
                url: `${__webpack_public_path__}/api`,
                newUrl: `${__webpack_public_path__}`
              },
            });
            console.info("[ConfigService] Applied UI_CONFIG:");
            console.info(w.UI_CONFIG);
          }
    }

    public init(): void {
        // Nothing to init (done in c'tor)
    }

    public artifactsType(): string | null {
        if (!this.config.artifacts) {
            return null;
        }
        return this.config.artifacts.type;
    }

    public artifactsUrl(): string | null {
        if (!this.config.artifacts) {
            return null;
        }
        return this.config.artifacts.url;
    }

    public newArtifactsUrl(): string | null {
        if (!this.config.artifacts) {
            return null;
        }
        return this.config.artifacts.newUrl;
    }

    public deploymentMode(): string {
        return this.config['deployment.mode'];
    }

    public uiUrl(): string {
        if (!this.config.ui || !this.config.ui.url) {
            return "";
        }
        return this.config.ui.url;
    }

    public uiContextPath(): string|undefined {
        if (!this.config.ui || !this.config.ui.contextPath) {
            return undefined;
        }
        return this.config.ui.contextPath;
    }

    public features(): FeaturesConfig {
        if (!this.config.features) {
            return {};
        }
        return this.config.features;
    }

    public featureReadOnly(): boolean {
        if (!this.config.features || !this.config.features.readOnly) {
            return false;
        }
        return this.config.features.readOnly;
    }

}
