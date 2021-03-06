package vertx.casestudy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

import javax.inject.Named;

public class GuiceModule extends AbstractModule {

    private final Vertx vertx;



    public GuiceModule(Vertx vertx) {
        this.vertx = vertx;
    }



    @Override
    protected void configure() {
        bind(HttpResponder.class).in(Scopes.SINGLETON);
    }



    @Provides
    @Singleton
    Vertx provideVertx() {
        return this.vertx;
    }



    @Provides
    @Singleton
    ConfigRetriever provideConfigRetriever(Vertx vertx) {
        return ConfigRetriever.create(
            vertx,
            new ConfigRetrieverOptions()
                .addStore(
                    new ConfigStoreOptions()
                        .setType("file")
                        .setConfig(new JsonObject().put("path", "config.json"))
                )
                .setScanPeriod(2000)
        );
    }



    @Provides
    @Singleton
    JsonObject provideConfig(ConfigRetriever configRetriever) {
        return configRetriever.rxGetConfig()
                              .blockingGet();
    }



    @Provides
    PgPool providePgPool(Vertx vertx, JsonObject config) {
        final var dbConfig = config.getJsonObject("db");

        return PgPool.pool(
            vertx,
            new PgConnectOptions()
                .setPort(dbConfig.getInteger("port"))
                .setHost(dbConfig.getString("host"))
                .setDatabase(dbConfig.getString("database"))
                .setUser(dbConfig.getString("user"))
                .setPassword(dbConfig.getString("password")),
            new PoolOptions()
        );
    }



    @Provides
    @Singleton
    JWTAuth provideJWTAuth(Vertx vertx, JsonObject config) {
        final var jwtConfig = config.getJsonObject("jwt");

        return JWTAuth.create(
            vertx,
            new JWTAuthOptions().addPubSecKey(
                new PubSecKeyOptions()
                    .setAlgorithm(jwtConfig.getString("algorithm"))
                    .setSecretKey(jwtConfig.getString("privateKey"))
                    .setPublicKey(jwtConfig.getString("publicKey"))
            )
        );
    }



    @Provides
    @Singleton
    JWTAuthHandler provideAuthHandler(JWTAuth jwtAuth) {
        return JWTAuthHandler.create(jwtAuth);
    }



    @Provides
    @Singleton
    WebClient provideNewsApiClient(Vertx vertx, JsonObject config) {
        final var newsApiConfig = config.getJsonObject("newsapi");

        return WebClient.create(
            vertx,
            new WebClientOptions()
                .setDefaultHost(newsApiConfig.getString("host"))
                .setDefaultPort(newsApiConfig.getInteger("port"))
        );
    }
}
