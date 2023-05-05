# Erda-ui cli

Command line interface for rapid Erda UI development.

## Install

> npm install @erda-ui/cli

## Usage

#### install dependency & initialize .env config

> erda-ui init

#### build erda-ui

> erda-ui build
>
> Options:
> --online whether is online build, default is false
> --release whether need build docker image & push, default is false
> --registry docker registry address which to push

#### do translate job

> erda-ui i18n [workDir]

#### generate service by API swagger

> erda-ui generate-service [workDir]

#### check file license header

> erda-ui check-license

#### add license header to files

> erda-ui add-license

## How to debug ts script

create a `launch.json` config in vscode. Add content below.

```
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "erda-ui-cli-debugger",
            "type": "node",
            "request": "launch",
            "runtimeExecutable": "node",
            "runtimeArgs": ["--nolazy", "-r", "ts-node/register/transpile-only"],
            "args": ["${workspaceRoot}/cli/lib/util/log.ts"], // this should be your target file
            "cwd": "${workspaceRoot}/cli",
            "internalConsoleOptions": "openOnSessionStart",
            "skipFiles": ["<node_internals>/**", "node_modules/**"]
        }
    ]
}
```
