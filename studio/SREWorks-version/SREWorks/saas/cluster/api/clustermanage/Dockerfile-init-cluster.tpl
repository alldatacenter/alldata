FROM {{ PYTHON3_IMAGE }}

RUN pip config set global.index-url {{PYTHON_PIP}} && pip config set global.trusted-host {{PYTHON_PIP_DOMAIN}}

#RUN mkdir /run

COPY ./APP-META-PRIVATE/init /run

ENTRYPOINT ["python", "/run/init-cluster.py"]
