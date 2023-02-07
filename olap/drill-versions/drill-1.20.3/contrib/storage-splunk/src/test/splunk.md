# Set up Splunk Docker Env for testing
   _Note: currently `testcontainers` Maven lib is used for automatic Splunk Docker container running_
## Pull docker image
``docker pull splunk/splunk``
## Start container
``docker run -d -p 8000:8000 -p 8089:8089 -e "SPLUNK_START_ARGS=--accept-license" -e "SPLUNK_PASSWORD=password" --name splunk splunk/splunk:latest``
## Get a session key using the /services/auth/login endpoint:
``
curl -k https://localhost:8089/services/auth/login --data-urlencode username=admin --data-urlencode password=pass
``
## Open the bash console for the container:
``
docker exec -it {container_name} bash
``
   
   The response is your session key:
```
<response>
  <sessionKey>192fd3e46a31246da7ea7f109e7f95fd</sessionKey>
</response>
```
   See more details: https://docs.splunk.com/Documentation/Splunk/8.1.1/RESTUM/RESTusing#Authentication_with_HTTP_Authorization_tokens