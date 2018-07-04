package io.bank.api.transactions.verticles;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import redis.embedded.RedisServer;

public class EmbeddedRedisVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedRedisVerticle.class);
    private static final int DEFAULT_PORT = 6379;
    
    private RedisServer redisServer;
    
    @Override
    public void start(Future<Void> future) {
        try {
            redisServer = new RedisServer(DEFAULT_PORT);
            redisServer.start();
            LOG.info("Redis started on port: " + redisServer.ports());
            future.complete();
        } catch (Exception e) {
            future.fail(e);
        }
    }
    
    @Override
    public void stop(Future<Void> future) {
        if (redisServer != null) {
            redisServer.stop();
            redisServer = null;
        }
        future.complete();
    }
}
