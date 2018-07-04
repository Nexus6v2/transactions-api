package io.bank.api.transactions.verticles;

import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.rxjava.core.AbstractVerticle;

import static java.util.stream.Collectors.toList;

public class MainVerticle extends AbstractVerticle {
    
    @Override
    public void start(Future<Void> future) {
        CompositeFuture
                .all(deployEmbeddedRedis(), deployHttpServer())
                .setHandler(future.<CompositeFuture>map(res -> null).completer());
    }
    
    @Override
    public void stop(Future<Void> future) {
        CompositeFuture.all(
                vertx.deploymentIDs()
                        .stream()
                        .map(this::undeployVerticle)
                        .collect(toList())
        ).setHandler(future.<CompositeFuture>map(c -> null).completer());
    }
    
    private Future<String> deployEmbeddedRedis() {
        return deployVerticle(EmbeddedRedisVerticle.class.getName());
    }
    
    private Future<String> deployHttpServer() {
        return deployVerticle(HttpServerVerticle.class.getName());
    }
    
    private Future<String> deployVerticle(String verticleName) {
        Future<String> f = Future.future();
        DeploymentOptions options = new DeploymentOptions();
        options.setWorker(true);
        vertx.deployVerticle(verticleName, options);
        return f;
    }
    
    private Future<Void> undeployVerticle(String deploymentId) {
        Future<Void> future = Future.future();
        vertx.undeploy(deploymentId, res -> {
            if (res.succeeded()) {
                future.complete();
            } else {
                future.fail(res.cause());
            }
        });
        return future;
    }
}
