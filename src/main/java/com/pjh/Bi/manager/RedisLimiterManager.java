package com.pjh.Bi.manager;

import com.pjh.Bi.common.ErrorCode;
import com.pjh.Bi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    public void doRateLimit(String key) {

        // 获取限流器实例
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 初始化限流器，设置每秒钟最大的处理速度为 10;
        // RRateLimiter采用令牌桶算法实现;
        // OVERALL：表示整体的速率限制，即对所有请求的速率进行限制;
        // 以下代码表示桶的容量是10，每秒产生一个令牌
        rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.SECONDS);

        // 每次调用会消耗{permits}个令牌
        if (rateLimiter.tryAcquire(1)) {
            // 允许处理业务逻辑
        } else {
            // 超过了限流速度，执行相应的处理逻辑
            throw new BusinessException(ErrorCode.TO_MANY_REQUEST);
        }
    }
}
