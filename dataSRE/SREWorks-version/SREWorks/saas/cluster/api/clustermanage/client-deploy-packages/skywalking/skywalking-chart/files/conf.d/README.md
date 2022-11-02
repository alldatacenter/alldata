If you don't want to use the default configuration files packed into the Docker image,
put your own configuration files under this directory in the corresponding component subdirectory,
`oap`, `ui`, etc.

Files under `oap/*` will override the counterparts under the Docker image's `/skywalking/config/*`, with the directory structure retained, here are some examples:

| File under `files/config.d/oap` directory | Overrides the file under Docker image's `/skywalking/config/` |
| ---- | -------- |
| `files/config.d/oap/application.yml`                 | `/skywalking/config/application.yml`                  |
| `files/config.d/oap/log4j2.xml`                      | `/skywalking/config/log4j2.xml`                       |
| `files/config.d/oap/alarm-settings.yml`              | `/skywalking/config/alarm-settings.yml`               |
| `files/config.d/oap/endpoint-name-grouping.yml`      | `/skywalking/config/endpoint-name-grouping.yml`       |
| `files/config.d/oap/oal/core.oal`                    | `/skywalking/config/oal/core.oal`                     |
| `files/config.d/oap/oal/browser.oal`                 | `/skywalking/config/oal/browser.oal`                  |
| `files/config.d/oap/oc-rules/oap.yaml`               | `/skywalking/config/oc-rules/oap.yaml`                |
| `...`                                                | `...`                                                 |
