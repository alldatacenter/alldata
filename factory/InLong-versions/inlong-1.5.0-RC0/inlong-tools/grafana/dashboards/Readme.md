## 1、build inlong grafana image

```shel
docker build -t inlong/grafana .
```

## 2、run docker image

Default AGENT_URL is 127.0.0.1:9080.

Default DATAPROXY_URL is 127.0.0.1:9081.

```
docker run -id -p 3000:3000 inlong/grafana
```

You can also set AGENT_URL/DP_URL.

```shell
docker run -id -p 3000:3000 -e AGENT_URL="ip:port" -e DP_URL="ip:port" inlong/grafana
```

