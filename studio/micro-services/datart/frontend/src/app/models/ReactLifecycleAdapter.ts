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

import React from 'react';
import ReactDom from 'react-dom';

interface ReactLifecycleAdapterProps {
  mounted: (container, options?, context?) => any;
  updated: (options: any, context?) => any;
  unmount: () => void;
  resize: (opt: any) => void;
}

export default class ReactLifecycleAdapter
  implements ReactLifecycleAdapterProps
{
  private domContainer;
  private reactComponent;
  private externalLibs;

  constructor(componentWrapper) {
    this.reactComponent = componentWrapper;
  }

  public registerImportDependencies(dependencies) {
    this.externalLibs = dependencies;
  }

  public mounted(container, options?, context?) {
    this.domContainer = container;
    return ReactDom.render(
      React.createElement(this.getComponent(), options, context),
      this.domContainer,
    );
  }

  public updated(options, context?) {
    return ReactDom.render(
      React.createElement(this.getComponent(), options, context),
      this.domContainer,
    );
  }

  public unmount() {
    ReactDom.unmountComponentAtNode(this.domContainer);
  }

  public resize(options, context?) {
    return ReactDom.render(
      React.createElement(this.getComponent(), options, context),
      this.domContainer,
    );
  }

  private getComponent() {
    if (typeof this.reactComponent === 'function') {
      return this.reactComponent({
        React,
        ReactDom,
        ...this.externalLibs,
      });
    }
    return this.reactComponent;
  }
}
