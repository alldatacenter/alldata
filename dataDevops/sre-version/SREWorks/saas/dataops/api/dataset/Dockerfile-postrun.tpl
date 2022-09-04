FROM {{ POSTRUN_IMAGE }}
COPY ./APP-META-PRIVATE/postrun /app/postrun
RUN apk update 
