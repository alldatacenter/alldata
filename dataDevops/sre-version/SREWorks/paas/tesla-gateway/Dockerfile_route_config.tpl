FROM {{ POSTRUN_IMAGE }}

COPY ./build/config /app

RUN chmod +x /app/start.sh 

WORKDIR /app
EXPOSE 80
ENV PYTHONPATH=/app
ENTRYPOINT ["/app/start.sh"]