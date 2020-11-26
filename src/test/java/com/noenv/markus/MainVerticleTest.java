package com.noenv.markus;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Base64;
import java.util.Collections;

import static com.noenv.markus.MainVerticle.*;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  private static final MultiMap headers = HttpHeaders.headers().add(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString("markus:sukram".getBytes()));
  private static final HttpClientOptions optionsHttp2 = new HttpClientOptions()
    .setSsl(true).setUseAlpn(true).setTrustStoreOptions(new JksOptions().setPath("client-truststore.jks").setPassword("wibble"))
    .setVerifyHost(true).setAlpnVersions(Collections.singletonList(HttpVersion.HTTP_2)).setProtocolVersion(HttpVersion.HTTP_2);

  private Vertx vertx;

  @Before
  public void before(final TestContext context) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class, new DeploymentOptions())
      .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void after(final TestContext context) {
    vertx.close().onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldGetHttp2OpenApiWithBasicAuth_Working(final TestContext context) {
    vertx.createHttpClient(optionsHttp2)
      .request(new RequestOptions()
        .setPort(PORT_HTTP_2)
        .setURI(PATH_OPEN_API_WORKING_SAMPLE)
        .setHeaders(headers)
      )
      .flatMap(HttpClientRequest::send)
      .flatMap(HttpClientResponse::body)
      .onSuccess(body -> context.assertEquals(EXPECTED_RESPONSE, body.toString()))
      .<Void>mapEmpty()
      .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldGetHttp2OpenApiWithBasicAuth_Broken(final TestContext context) {
    vertx.createHttpClient(optionsHttp2)
      .request(new RequestOptions()
        .setPort(PORT_HTTP_2)
        .setURI(PATH_OPEN_API_BROKEN_SAMPLE)
        .setHeaders(headers)
      )
      .flatMap(HttpClientRequest::send)
      .flatMap(HttpClientResponse::body)
      .onSuccess(body -> context.assertEquals(EXPECTED_RESPONSE, body.toString()))
      .<Void>mapEmpty()
      .onComplete(context.asyncAssertSuccess());
  }
}
