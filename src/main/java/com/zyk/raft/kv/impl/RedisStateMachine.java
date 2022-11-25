package com.zyk.raft.kv.impl;

import com.alibaba.fastjson.JSON;
import com.zyk.raft.kv.StateMachine;
import com.zyk.raft.kv.entity.Command;
import com.zyk.raft.kv.entity.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;

@Slf4j
public class RedisStateMachine implements StateMachine {
    private JedisPool jedisPool;

    private RedisStateMachine() {
        init();
    }

    @Override
    public void init() {
        GenericObjectPoolConfig redisConfig = new GenericObjectPoolConfig();
        redisConfig.setMaxTotal(100);
        redisConfig.setMaxWaitMillis(10 * 1000);
        redisConfig.setMaxIdle(100);
        redisConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(redisConfig, System.getProperty("redis.host", "127.0.0.1"), 6379);
    }

    @Override
    public void destroy() throws Throwable {
        jedisPool.close();
        log.info("destroy success");
    }

    @Override
    public void apply(LogEntry logEntry) {
        try (Jedis jedis = jedisPool.getResource()) {
            Command command = logEntry.getCommand();
            if (command == null) {
                throw new IllegalArgumentException("command can not be null, logEntry : " + logEntry.toString());

            }
            String key = command.getKey();
            jedis.set(key.getBytes(StandardCharsets.UTF_8), command.getValue().getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public LogEntry get(String key) {
        LogEntry result = null;
        try (Jedis jedis = jedisPool.getResource()) {
            result = JSON.parseObject(jedis.get(key), LogEntry.class);
        } catch (Exception e) {
            log.error("redis error ", e);
        }
        return result;
    }

    @Override
    public String getString(String key) {
        String result = null;
        try (Jedis jedis = jedisPool.getResource()) {
            result = jedis.get(key);
        } catch (Exception e) {
            log.error("redis error ", e);
        }
        return result;
    }

    @Override
    public void setString(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            log.error("redis error ", e);
        }
    }

    @Override
    public void delString(String... keys) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(keys);
        } catch (Exception e) {
            log.error("redis error ", e);
        }
    }


    public static RedisStateMachine getInstance() {
        return RedisStateMachine.RedisStateMachineLazyHolder.INSTANCE;
    }

    private static class RedisStateMachineLazyHolder {

        private static final RedisStateMachine INSTANCE = new RedisStateMachine();
    }
}
