package io.bank.api.transactions.verticles;

import com.lambdaworks.redis.RedisURI;
import io.bank.api.transactions.dao.RedisDao;
import io.bank.api.transactions.handlers.AccountsHandler;
import io.bank.api.transactions.handlers.TransactionsHandler;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.ErrorHandler;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpServerVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(HttpServerVerticle.class);
    private static final String APPLICATION_JSON = "application/json";
    
    private static final int HTTP_SERVER_PORT = 8080;
    private static final int REDIS_PORT = 6379;
    
    private HttpServer server;
    private RedisDao redisDao;
    
    @Override
    public void start(Future<Void> startFuture) {
        RedisURI redisURI = RedisURI.builder()
                .withHost("localhost")
                .withPort(REDIS_PORT)
                .build();
        redisDao = new RedisDao(redisURI);
    
        //Initializing http server
        final Router router = createRouter();
        server = vertx.createHttpServer();
        
        server.requestStream().handler(router::accept);
        
        server.rxListen(HTTP_SERVER_PORT)
                .subscribe(httpServer -> LOG.info("HTTP server started on port: " + server.actualPort()));
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        if (server == null) {
            stopFuture.complete();
            return;
        }
        server.close(server -> stopFuture.complete());
    }
    
    private Router createRouter() {
        Router router = Router.router(vertx);
    
        // Common handlers
        router.route().consumes(APPLICATION_JSON);
        router.route().produces(APPLICATION_JSON);
        router.route().failureHandler(ErrorHandler.create(true));
        router.route().handler(BodyHandler.create());
        router.route().handler(context -> {
            context.response().headers().add(CONTENT_TYPE.toString(), APPLICATION_JSON);
            context.next();
        });
        
        // Healthcheck
        router.route("/health").handler(context -> context.response().end("{ \"status\": \"UP\" }"));
    
        // Account request handlers
        AccountsHandler accountsHandler = new AccountsHandler(redisDao);
        router.get("/accounts").handler(accountsHandler::getAllAccounts);
        router.get("/accounts/:accountId").handler(accountsHandler::getAccount);
        router.post("/accounts").handler(accountsHandler::createAccount);
        router.delete("/accounts/:accountId").handler(accountsHandler::deleteAccount);
        router.get("/accounts/:accountId/transactions").handler(accountsHandler::getAccountsTransactions);
    
        // Transaction request handlers
        TransactionsHandler transactionsHandler = new TransactionsHandler(redisDao);
        router.get("/transactions").handler(transactionsHandler::getAllTransactions);
        router.get("/transactions/:transactionId").handler(transactionsHandler::getTransaction);
        router.post("/transactions").handler(transactionsHandler::createTransaction);
    
        return router;
    }
}
