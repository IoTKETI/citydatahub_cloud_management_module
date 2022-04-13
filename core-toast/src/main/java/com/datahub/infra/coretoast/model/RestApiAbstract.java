package com.datahub.infra.coretoast.model;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author ksh1006@innogrid.com
 * @date  2020-05-26
 * @brief RESTful API 구현을 위한 추상클래스
 * @details
 */
public abstract class RestApiAbstract{
  Logger logger = LoggerFactory.getLogger(RestApiAbstract.class);

  private String useToken;

  static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  public static final JsonFactory JSON_FACTORY = new JacksonFactory();

  protected static final String KR_IDENTITY_DOMAIN = "https://api-identity.infrastructure.cloud.toast.com/v2.0/";
  protected static final String KR_INSTANCE_DOMAIN = "https://kr1-api-instance.infrastructure.cloud.toast.com/v2/";
  protected static final String KR_IMAGE_DOMAIN = "https://kr1-api-image.infrastructure.cloud.toast.com/v2/";
  protected static final String KR_BLOCK_DOMAIN = "https://kr1-api-block-storage.infrastructure.cloud.toast.com/v2/";
  protected static final String KR_NETWORK_DOMAIN = "https://kr1-api-network.infrastructure.cloud.toast.com/v2.0/";

  protected HttpRequestFactory jsonRequestFactory;

  protected HttpRequestFactory httpRequestFactory;

  public void receiveToken(String tokenValue) {
    this.useToken = tokenValue;
    logger.error("Receive Token is : {} ", useToken);
  }

  public RestApiAbstract(){
    jsonRequestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("X-Auth-Token", useToken);
        httpRequest.setParser(new JsonObjectParser(JSON_FACTORY));
        httpRequest.setHeaders(httpHeaders);
      }
    });

    httpRequestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("X-Auth-Token", "8c302cda5fd046369169fc25674a419b");
        httpHeaders.setContentType("application/x-www-form-urlencoded");
        httpHeaders.setCacheControl("no-cache");

        httpRequest.setParser(new UrlEncodedParser());
        httpRequest.setHeaders(httpHeaders);
      }
    });

  }
}