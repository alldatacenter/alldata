#Spline UI Development Guide

---

##Application structure

- All shared stuff placed in the `/ui/projects` and separated into 4 main categories/libraries:

    - ###spline-api

        Represents DTO layer for the Spline consumer API.

    - ###spline-utils
    
        Generic stuff, which can be reused in any application. There cannot be any dependency on any other part of the application.

    - ###spline-common
    
        Generic UI related components/modules. These components can be reused in other applications. It can have dependencies on spline-utils.

    - ###spline-shared

        Spline UI application specific stuff. Smarter components/containers which need to be shared between application modules. 
        It can have dependencies on spline-utils, spline-common, spline-api.


- Feature modules placed in the `/ui/src/modules`
  
    Each module can depend on any shared library from `/ui/projects`, but cannot have any dependencies on any other module from `/ui/src/modules` or `/ui/src/app`.


- AppModule, from `/ui/src/app` defines the skeleton of the application, it is a glue for the feature modules. It defines the application layout and routing, but it cannot have any dependencies on feature modules except routing definitions. It can be depended only on stuff from /ui/projects.

---

##The structure of shared libraries


```
    library-name
        main
            src
                public-api.ts
            
            assets
                i18n
                    library-name
                        en.json                    
            
            styles
                library-name
                    ...
                    index.scss
                library-name.scss
           
                
        secondary-endpoint            
            src
                public-api.ts
            assets
                i18n
                    library-name.secondary-endpoint
                        en.json                    
            
            styles
                library-name.secondary-endpoint
                    ...
                    index.scss
                library-name.secondary-endpoint.scss                
            package.json
            
        package.json             
```

- Each library has the main and secondary entry points:

    - Main entry point directory structure (all code in the directory `main` & the package.json should be at the level of the `main` directory)

        ```
            library-name
                main
                    assets
                    styles           
                    src
                        public-api.ts
                package.json             
        ```

    - Secondary entry point directory structure (package.json should be inside the secondary endpoint directive)
        ```
            library-name
                secondary-endpoint
                    assets
                    styles           
                    src
                        public-api.ts
                    package.json
        ```

- All SCSS files should be placed in the relevant `styles` directory instead of styles definition directly in the component with `styleUrls` property.
- All i18n files should be placed in the relevant i18n directory.

---

    Copyright 2020 ABSA Group Limited
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
