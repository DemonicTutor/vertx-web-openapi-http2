package com.noenv.markus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.openapi.RouterBuilder;

public final class MainVerticle extends AbstractVerticle {

  public static final int PORT_HTTP_2 = 8443;
  public static final String EXPECTED_RESPONSE = "YPPIE";

  private static final String PATH_OPEN_API_BROKEN = "/broken";
  private static final String PATH_OPEN_API_WORKING = "/working";
  private static final String OPEN_API_OPERATION_SAMPLE = "sample";
  public static final String PATH_OPEN_API_BROKEN_SAMPLE = PATH_OPEN_API_BROKEN + "/" + OPEN_API_OPERATION_SAMPLE;
  public static final String PATH_OPEN_API_WORKING_SAMPLE = PATH_OPEN_API_WORKING + "/" + OPEN_API_OPERATION_SAMPLE;

  @Override
  public void start(final Promise<Void> promise) {
    final var router = Router.router(vertx);

    final var authHandler = BasicAuthHandler.create(PropertyFileAuthentication.create(vertx, "auth.properties"));

    router.route().failureHandler(this::failure);

    final var apiRouterBroken =
    RouterBuilder.create(vertx, "swagger.yaml")
      .onSuccess(routerBuilder -> routerBuilder.securityHandler("apiKey", authHandler))
      .onSuccess(routerBuilder -> routerBuilder.operation(OPEN_API_OPERATION_SAMPLE).handler(this::sample))
      .map(RouterBuilder::createRouter)
      .onSuccess(innerApiRouter -> router.mountSubRouter(PATH_OPEN_API_BROKEN, innerApiRouter));

    final var apiRouterWorking =
      RouterBuilder.create(vertx, "swagger.yaml")
        .onSuccess(routerBuilder -> routerBuilder.bodyHandler(null)) // remove body handler
        .onSuccess(routerBuilder -> routerBuilder.securityHandler("apiKey", authHandler))
        .onSuccess(routerBuilder -> routerBuilder.operation(OPEN_API_OPERATION_SAMPLE).handler(this::sample))
        .map(RouterBuilder::createRouter)
        .onSuccess(innerApiRouter -> router.mountSubRouter(PATH_OPEN_API_WORKING, innerApiRouter));

    final var httpServer =
      vertx.createHttpServer(new HttpServerOptions()
        .setSsl(true)
        .setUseAlpn(true)
        .setPemKeyCertOptions(new PemKeyCertOptions().setCertPath("server-cert.pem").setKeyPath("server-key.pem"))
      )
        .requestHandler(router)
        .listen(PORT_HTTP_2);

    CompositeFuture.all(apiRouterWorking, apiRouterBroken, httpServer)
      .<Void>mapEmpty()
      .onComplete(promise);
  }

  private void failure(final RoutingContext context) {
    final var response = context.response();
    response.putHeader("content-type", "text/plain");
    response.setStatusCode(500);
    response.end(context.failure().getMessage());
  }

  private void sample(final RoutingContext context) {
    final var response = context.response();
    response.putHeader("content-type", "text/plain");
    response.end(EXPECTED_RESPONSE);
  }
}
