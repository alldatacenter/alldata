package com.qcloud.cos.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class HttpCredentialsEndpointProvider extends CredentialsEndpointProvider{
  private final String url;
  private final String path;
  private final Map<String, String> header;

  public HttpCredentialsEndpointProvider(String url, String path, Map<String, String> header) {
    this.url = url;
    this.path = path;
    this.header = header;
  }

  @Override
  public URI getCredentialsEndpoint() throws URISyntaxException {
    return new URI(url + path);
  }

  @Override
  public Map<String, String> getHeaders() {
    return header;
  }
}
