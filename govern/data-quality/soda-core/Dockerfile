FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Etc/UTC
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

# install dependencies
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get remove -y python3.10 && \
    apt-get install -y --no-install-recommends software-properties-common gnupg2 && \
    add-apt-repository ppa:deadsnakes/ppa && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    procps \
    build-essential curl \
    libpq-dev \
    libssl-dev libffi-dev \
    python3.9 python3.9-dev python3.9-venv libpython3.9-dev libpython3.9 \
    python3.9-distutils \
    unixodbc-dev git lsb-release \
    alien odbcinst

#sqlserver support - see https://techcommunity.microsoft.com/t5/sql-server/odbc-drivers-for-ubuntu-22-04/m-p/3469347/highlight/true#M1668
RUN curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add - && \
    curl https://packages.microsoft.com/config/ubuntu/21.04/prod.list | tee /etc/apt/sources.list.d/mssql-release.list && \
    apt-get update && \
    ACCEPT_EULA=Y apt-get install -y libsasl2-dev odbcinst mssql-tools18 msodbcsql18 unixodbc-dev

# Dremio support
RUN curl -L https://download.dremio.com/arrow-flight-sql-odbc-driver/arrow-flight-sql-odbc-driver-LATEST.x86_64.rpm -o arrow-driver.rpm && \
    alien -iv --scripts arrow-driver.rpm

RUN apt-get clean -qq -y && \
    apt-get autoclean -qq -y && \
    apt-get autoremove -qq -y && \
    rm -rf /var/lib/apt/lists/*
RUN ln -s /usr/bin/python3.9 /usr/bin/python && \
    curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py && \
    python3.9 get-pip.py

WORKDIR /app

RUN pip install --upgrade pip

COPY . .

RUN pip --no-cache-dir install -r requirements.txt

RUN apt-get purge -y build-essential git curl && \
    apt-get clean -qq -y && \
    apt-get autoclean -qq -y && \
    apt-get autoremove -qq -y

ENTRYPOINT [ "soda" ]
CMD [ "scan" ]
