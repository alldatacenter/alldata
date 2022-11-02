FROM ${SW_PYTHON3_IMAGE}
WORKDIR /root/test/
COPY . .
RUN pip config set global.index-url ${PYTHON_PIP} && pip config set global.trusted-host ${PYTHON_PIP_DOMAIN}
RUN pip install kubernetes
CMD ["diagnosis.py"]
ENTRYPOINT ["python3"]