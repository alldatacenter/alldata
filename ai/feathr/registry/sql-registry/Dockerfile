FROM python:3.9

COPY ./ /usr/src

WORKDIR /usr/src
RUN pip install -r requirements.txt

# Start web server
CMD [ "uvicorn","main:app","--host", "0.0.0.0", "--port", "80" ]
