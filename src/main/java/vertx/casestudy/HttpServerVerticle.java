package vertx.casestudy;

import com.google.inject.Inject;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vertx.casestudy.auth.LoginHandler;
import vertx.casestudy.headline.HeadlineCreateHandler;
import vertx.casestudy.headline.HeadlineGetAllHandler;

public class HttpServerVerticle extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final HeadlineCreateHandler headlineCreateHandler;

    private final HeadlineGetAllHandler headlineGetAllHandler;

    private final LoginHandler loginHandler;



    @Inject
    public HttpServerVerticle(
        HeadlineCreateHandler headlineCreateHandler,
        HeadlineGetAllHandler headlineGetAllHandler,
        LoginHandler loginHandler
    ) {
        this.headlineCreateHandler = headlineCreateHandler;
        this.headlineGetAllHandler = headlineGetAllHandler;
        this.loginHandler = loginHandler;
    }



    @Override
    public Completable rxStart() {
        final var v1 = Router.router(vertx);

        v1.post()
          .handler(BodyHandler.create());

        v1.route()
          .handler(ctx -> {
              log.info("New request {} {}", ctx.request().method(), ctx.request().path());
              ctx.next();
          })
          .failureHandler(ctx -> {
              log.error("Error", ctx.failure());

              ctx.response()
                 .setStatusCode(500)
                 .end(new JsonObject().put("error", ctx.failure().getMessage()).encode());
          });

        v1.get("/headlines").handler(this.headlineGetAllHandler);
        v1.post("/headlines").handler(this.headlineCreateHandler);
        v1.post("/login").handler(this.loginHandler);

        final var router = Router.router(vertx);
        router.mountSubRouter("/api/v1", v1);

        return this.vertx
            .createHttpServer()
            .requestHandler(router)
            .rxListen(8080)
            .ignoreElement();
    }
}
