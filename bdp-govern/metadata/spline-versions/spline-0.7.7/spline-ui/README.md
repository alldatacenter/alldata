[![Maven Central](https://maven-badges.herokuapp.com/maven-central/za.co.absa.spline.ui/project/badge.svg)](https://search.maven.org/search?q=g:za.co.absa.spline.ui)
[![TeamCity build (develop)](https://teamcity.jetbrains.com/app/rest/builds/aggregated/strob:%28locator:%28buildType:%28id:OpenSourceProjects_AbsaOSS_SplineUi_AutomaticBuilds%29,branch:develop%29%29/statusIcon.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=OpenSourceProjects_AbsaOSS_SplineUi_AutomaticBuilds&branch=develop&tab=buildTypeStatusDiv)
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=AbsaOSS_spline-ui&metric=alert_status)](https://sonarcloud.io/dashboard?id=AbsaOSS_spline-ui)
[![SonarCloud Maintainability](https://sonarcloud.io/api/project_badges/measure?project=AbsaOSS_spline-ui&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=AbsaOSS_spline-ui)
[![SonarCloud Reliability](https://sonarcloud.io/api/project_badges/measure?project=AbsaOSS_spline-ui&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=AbsaOSS_spline-ui)
[![SonarCloud Security](https://sonarcloud.io/api/project_badges/measure?project=AbsaOSS_spline-ui&metric=security_rating)](https://sonarcloud.io/dashboard?id=AbsaOSS_spline-ui)

## Table of Contents

<!--ts-->

* [Spline UI](#spline-ui)
    * [Download](#download)
    * [Usage](#usage)
        * [Running as a Docker container](#run-as-docker)
        * [Running a WAR-file](#run-as-war)
        * [Running as embedded CDN resource](#run-as-cdn)
    * [Building from sources](#build)
        * [TL;DR](#tldr)
        * [Building a Docker](#build-docker)
        * [Building UI-core (SPA) alone](#build-core)
    * [Versioning strategy](#versioning)

<!-- Added by: wajda, at: Mon 17 May 17:10:10 CEST 2021 -->

<!--te-->

<a id="spline-ui"></a>
# Spline UI

The UI for [Spline project](https://absaoss.github.io/spline/)

Spline UI app is implemented as a _single-page application (SPA)_, available alone as well as packed as a Docker image or a WAR-file. Choose according to your
deployment preferences.

<a id="download"></a>
## Download

- [Docker](https://hub.docker.com/r/absaoss/spline-web-ui)
- [WAR-file](https://search.maven.org/search?q=g:za.co.absa.spline.ui%20AND%20p:war)

<a id="usage"></a>
## Usage

Spline UI needs to know where Spline Consumer API sits. Spline Consumer API endpoint is accessed from the user browser directly, so use appropriate IP
or host name.

<a id="run-as-docker"></a>

### Running as a Docker container

```shell script
docker container run \
      -e SPLINE_CONSUMER_URL=http://localhost:8080/consumer \
      -p 9090:8080 \
      absaoss/spline-web-ui:latest
```

Open Spline UI in a browser - http://localhost:9090

<a id="run-as-war"></a>
### Running a WAR-file

Download a WAR-file using the link above, and deploy it into any J2EE-compatible Web Container, e.g. Tomcat, Jetty, JBoss etc.

A WAR-file provides several alternative ways how to set configuration parameters:

- JNDI

  (for example in a `context.xml` in the Tomcat server)
    ```xml
    <Environment name="spline/consumer/url" value="..." type="java.lang.String"/>
    ```

- JVM property
    ```shell script
    $JAVA_OPTS -Dspline.consumer.url=...
    ```

- System environment variable
    ```shell script
    export SPLINE_CONSUMER_URL=...
    ```

<a id="run-as-cdn"></a>
### Running as embedded CDN resource

It is possible to use compiled UI sources placed at some CDN server. In that the application can be configured with query parameters like so:

```html

<iframe
    src="https://cdn.jsdelivr.net/path-to-compiled-app-assets/index.html?_splineConsumerApiUrl=ENCODED_CONSUMER_API_PATH&_targetUrl=ENCODED_APP_PATH&_isEmbeddedMode=true">
</iframe>
```

All available config query parameters list:

| Name        | Description | Default | Required |
| ----------- | ----------- | ------------- | -----:   |
| `_splineConsumerApiUrl` | Spline Consumer API URI | | true
| `_isEmbeddedMode`       | Embedded mode settings | `false` | false
| `_targetUrl`            | App will be redirected to that url right after initialization. The path should start with `/` | `/` | false

`_splineConsumerApiUrl` is a required query parameter.

#### How to build assets for your CDN

1. Install the LTS version of Node.js: https://nodejs.org/en/download/
2. Install the latest version of NPM: `npm i -g npm@latest`
3. Install dependencies:
    1. Go to the ui directory: `cd <root>/ui`
    2. Run `npm ci`
4. Build the project:
    1. Go to the ui directory: `cd <root>/ui`
    2. Run `npm run build::CDN`
5. CDN resources is everything inside `<root>/ui/dist`

<a id="build"></a>
## Building from sources

<a id="tldr"></a>
### TL;DR

```shell script
mvn clean install
```

<a id="build-docker"></a>
### Building a Docker

```shell script
cd deployment/web
mvn install -P docker -D dockerfile.repositoryUrl=my 
```

It will create `my/spline-web-ui` image and two tags: `$version` and `latest`

See [Building Spline docker images](https://github.com/AbsaOSS/spline-getting-started/blob/main/building-docker.md) for details.

<a id="build-core"></a>
### Building UI-core (SPA) alone

#### Config

Config file should be placed in the directory:

- after build in the dist folder `/dist/assets/config.json`
- or before build `/src/assets/config.json` (this option applies for development)

The example of the config can be found here: `/src/assets/config.example.json`

The list of all configuration properties along with their default values are stored in the `config.default.json`.
Properties provided in the `config.json` overwrite corresponding properties from the `config.default.json`.

#### Development server

Run `npm start` for a dev server. Navigate to `http://localhost:4300/`. The app will automatically reload if you change any of the source files.

#### Build

Run `npm run build` to build the project. The build artifacts will be stored in the `dist/` directory. When build completes
replace `SPLINE_UI_DEPLOY_CONTEXT` in the `dist/app/index.html` accordingly to your deployment schema, and verify/create `dist/app/assets/config.json`
as described above.

<a id="versioning"></a>
## Versioning strategy

Spline UI version number mimics _Semantic Versioning_ structure, although it doesn't strictly follow it. The reason is that it's difficult to define
what is a _Public API_[^1] of the Spline UI in terms of _Semantic Versioning_.

But for the sake of consistency, Spline UI version uses a similar three-digit pattern with the _pre-release_ and _build_ labels, but slightly
different meaning of the _major_ and _minor_ components.

- The _major version_ component indicates the Spline REST major version it supports with guarantee. The Spline server with the major version that is
  higher than the UI major version can also be supported, depending on the reason why that major version was incremented at the first place. (Spline
  server exposes multiple different APIs, and a given major version might expose exactly the same Consumer API that the previous one did).

- The _minor version_ component is incremented when new important features or significant internal changes are introduced.

- The _patch version_ component is incremented on bugfixes and small enhancements.

[^1]: [Semantic Versioning](https://semver.org/) specification relies on the definition of the _Public API_ that the given application exposes and the
changes to which is communicated by incrementing the version numbers.

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
