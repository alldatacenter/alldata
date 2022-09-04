FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/python:2.7.18-alpine

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

COPY . /app-webpy
WORKDIR /app-webpy

RUN apk add g++ musl-dev python-dev libffi-dev openssl-dev make
RUN cd /app-webpy/tesla-faas && python -m pip install -r requirements.txt --trusted-host mirrors.aliyun.com -i http://mirrors.aliyun.com/pypi/simple && python setup.py bdist_wheel
RUN python -m pip install /app-webpy/tesla-faas/dist/tesla_faas2-2.1.1-py2.py3-none-any.whl --trusted-host mirrors.aliyun.com -i http://mirrors.aliyun.com/pypi/simple

ENTRYPOINT [ "gunicorn","--workers", "1", "--bind", "0.0.0.0:7001", "app:app" ]

