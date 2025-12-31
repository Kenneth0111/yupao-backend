package com.example.yupao.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yupao.model.domain.User;
import com.example.yupao.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private List<Long> mainUserList = Arrays.asList(1L);

    @Scheduled(cron = "0 0/30 * * * ?")
    public void doCacheRecommendUsers() {
        RLock rLock =redissonClient.getLock("yupao:precachejob:docache:lock");
        try {
            if (rLock.tryLock(0, -1, TimeUnit.SECONDS)) {
                for (Long userId : mainUserList) {
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    queryWrapper.ne("id", userId)
                            .eq("isDelete", 0)
                            .orderByDesc("createTime");
                    Page<User> userPage = userService.page(new Page<>(1, 10), queryWrapper);
                    String redisKey = String.format("yupao:user:recommend:%s", userId);
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    // 写缓存
                    try {
                        valueOperations.set(redisKey, userPage, 40, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("PreCacheJob interrupted", e);
        } finally {
            // 只释放自己的锁
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }
}