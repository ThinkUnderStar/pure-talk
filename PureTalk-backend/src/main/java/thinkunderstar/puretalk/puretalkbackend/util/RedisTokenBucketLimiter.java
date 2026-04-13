package thinkunderstar.puretalk.puretalkbackend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 基于 Redis + Lua 的分布式令牌桶限流器
 * 特性：
 * - 支持分布式环境（多服务器共享计数）
 * - 原子性操作（Lua 脚本保证）
 * - 自动过期清理
 */
@Component
public class RedisTokenBucketLimiter {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Lua 脚本：原子性执行令牌桶算法
     * 参数说明：
     * KEYS[1]  : Redis Key
     * ARGV[1]  : capacity（桶容量）
     * ARGV[2]  : rate（每秒生成令牌数）
     * ARGV[3]  : now（当前时间戳，秒）
     * ARGV[4]  : requested（本次请求需要的令牌数）
     * 返回值：
     * 1 = 获取令牌成功
     * 0 = 令牌不足，触发限流
     */
    private static final String TOKEN_BUCKET_SCRIPT =
            "local key = KEYS[1] " +
                    "local capacity = tonumber(ARGV[1]) " +
                    "local rate = tonumber(ARGV[2]) " +
                    "local now = tonumber(ARGV[3]) " +
                    "local requested = tonumber(ARGV[4]) " +
                    // 获取桶中当前令牌数（首次访问时初始化为满桶）
                    "local last_tokens = tonumber(redis.call('hget', key, 'tokens') or capacity) " +
                    // 获取上次刷新时间（首次访问时初始化为当前时间）
                    "local last_refresh = tonumber(redis.call('hget', key, 'refresh_time') or now) " +
                    // 计算经过的时间，并补充令牌
                    "local delta = math.max(0, (now - last_refresh) * rate) " +
                    "local new_tokens = math.min(capacity, last_tokens + delta) " +
                    // 判断令牌是否足够
                    "local allowed = new_tokens >= requested " +
                    "if allowed then " +
                    "    new_tokens = new_tokens - requested " +
                    "end " +
                    // 更新 Redis 中的数据
                    "redis.call('hset', key, 'tokens', new_tokens) " +
                    "redis.call('hset', key, 'refresh_time', now) " +
                    // 设置过期时间（避免僵尸 Key 占用内存）
                    "redis.call('expire', key, math.ceil(capacity / rate) + 10) " +
                    "return allowed and 1 or 0";

    private final DefaultRedisScript<Long> script;

    public RedisTokenBucketLimiter() {
        this.script = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, Long.class);
    }

    /**
     * 按用户限流（每个用户独立计数）
     *
     * @param userId   用户标识
     * @param capacity 桶容量（允许的最大突发流量）
     * @param rate     每秒生成的令牌数（稳态 QPS）
     * @return true = 放行，false = 触发限流
     */
    public boolean tryAcquireByUser(String userId, long capacity, long rate) {
        return tryAcquire("rate:user:" + userId, capacity, rate, 1);
    }

    /**
     * 按 IP 限流（每个 IP 独立计数）
     *
     * @param ip       IP 地址
     * @param capacity 桶容量
     * @param rate     每秒生成的令牌数
     * @return true = 放行，false = 触发限流
     */
    public boolean tryAcquireByIp(String ip, long capacity, long rate) {
        return tryAcquire("rate:ip:" + ip, capacity, rate, 1);
    }

    /**
     * 全局限流（所有请求共享计数）
     *
     * @param api      接口标识
     * @param capacity 桶容量
     * @param rate     每秒生成的令牌数
     * @return true = 放行，false = 触发限流
     */
    public boolean tryAcquireGlobal(String api, long capacity, long rate) {
        return tryAcquire("rate:global:" + api, capacity, rate, 1);
    }

    /**
     * 通用限流方法（支持自定义令牌需求数）
     *
     * @param key        Redis Key
     * @param capacity   桶容量
     * @param rate       每秒生成的令牌数
     * @param requested  本次需要的令牌数
     * @return true = 放行，false = 触发限流
     */
    public boolean tryAcquire(String key, long capacity, long rate, int requested) {
        long now = System.currentTimeMillis() / 1000; // 秒级时间戳
        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(rate),
                String.valueOf(now),
                String.valueOf(requested)
        );
        return result != null && result == 1L;
    }

    /**
     * 获取用户当前的令牌数（用于监控和调试）
     *
     * @param userId 用户标识
     * @return 当前剩余令牌数，如果 Key 不存在返回 -1
     */
    public double getUserTokens(String userId) {
        Object tokens = redisTemplate.opsForHash().get("rate:user:" + userId, "tokens");
        if (tokens == null) {
            return -1;
        }
        return Double.parseDouble(tokens.toString());
    }

    /**
     * 重置用户的限流状态（用于测试或解封）
     *
     * @param userId 用户标识
     */
    public void resetUser(String userId) {
        redisTemplate.delete("rate:user:" + userId);
    }
}