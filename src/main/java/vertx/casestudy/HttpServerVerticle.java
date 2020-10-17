package vertx.casestudy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

public class HttpServerVerticle extends AbstractVerticle {

    private final PgPool pgPool;

    private final HeadlineCreateHandler headlineCreateHandler;

    private final HeadlineGetAllHandler headlineGetAllHandler;



    public HttpServerVerticle(Vertx vertx) {
        this.pgPool = PgPool.pool(
            vertx,
            new PgConnectOptions()
                .setPort(5432)
                .setHost("localhost")
                .setDatabase("case-study")
                .setUser("example")
                .setPassword("example"),
            new PoolOptions()
        );

        this.headlineCreateHandler = new HeadlineCreateHandler(this.pgPool);
        this.headlineGetAllHandler = new HeadlineGetAllHandler(this.pgPool);
    }



    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        final var router = Router.router(this.vertx);
        router.route()
              .handler(BodyHandler.create())
              .handler(ctx -> {
                  System.out.println("New Request: " + ctx.request().path());
                  ctx.next();
              });

        router.post("/headline")
              .handler(this.headlineCreateHandler);


        router.get("/headlines")
              .handler(this.headlineGetAllHandler);

        router.route()
              .failureHandler(ctx -> {
                  ctx.response()
                     .setStatusCode(500)
                     .end(new JsonObject().put("error", ctx.failure().toString()).encodePrettily());
              });

        this.vertx.createHttpServer(new HttpServerOptions())
                  .requestHandler(router)
                  .listen(
                      8080,
                      ar -> {
                          try {
                              if (ar.succeeded()) {
                                  startPromise.complete();
                              } else {
                                  startPromise.fail(ar.cause());
                              }
                          } catch (Throwable t) {
                              startPromise.fail(t);
                          }
                      }
                  );
    }



    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        this.pgPool.close();
        stopPromise.complete();
    }
}