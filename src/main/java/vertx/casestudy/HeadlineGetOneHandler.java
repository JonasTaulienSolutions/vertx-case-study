package vertx.casestudy;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class HeadlineGetOneHandler implements Handler<RoutingContext> {

    private static final String SELECT_ONE_HEADLINE_QUERY
        = "SELECT id, source, author, title, description, published_at FROM headline WHERE headline.id = $1";

    private final PgPool pgPool;

    private final Responder responder;



    public HeadlineGetOneHandler(PgPool pgPool, Responder responder) {
        this.pgPool = pgPool;
        this.responder = responder;
    }



    @Override
    public void handle(RoutingContext ctx) {
        final var headlineId = Integer.parseInt(ctx.pathParam("id"));
        this.pgPool
            .preparedQuery(SELECT_ONE_HEADLINE_QUERY)
            .execute(Tuple.of(headlineId), ar -> respond(ctx, ar));
    }



    private void respond(RoutingContext ctx, AsyncResult<RowSet<Row>> ar) {
        try {
            if (ar.succeeded()) {
                final var row = ar.result().iterator().next();

                this.responder
                    .respond(
                        ctx,
                        HttpResponseStatus.OK,
                        HeadlineGetOneHandler.rowToJson(row)
                    );

            } else {
                this.responder
                    .respondError(
                        ctx,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        ar.cause()
                    );
            }
        } catch (Throwable t) {
            ctx.fail(t);
        }
    }



    private static JsonObject rowToJson(Row row) {
        return new JsonObject()
                   .put("id", row.getInteger("id"))
                   .put("source", row.getString("source"))
                   .put("author", row.getString("author"))
                   .put("title", row.getString("title"))
                   .put("description", row.getString("description"))
                   .put(
                       "publishedAt",
                       row.getOffsetDateTime("published_at").toString()
                   );
    }
}