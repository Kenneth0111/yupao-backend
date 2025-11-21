package com.example.usercenter.service;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.example.usercenter.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("String", "cat");
        valueOperations.set("Integer", 16);
        valueOperations.set("Double", 100.00);
        User user = new User();
        user.setId(1L);
        user.setUsername("鱼皮");
        user.setUserAccount("1");
        user.setAvatarUrl("");
        user.setProfile("");
        user.setGender(0);
        user.setUserPassword("1");
        user.setPhone("158");
        user.setEmail("163");
        user.setTags("java");
        user.setUserStatus(0);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);
        user.setUserRole(0);
        user.setPlanetCode("1");
        valueOperations.set("User", user);

        // 查
        Object string = valueOperations.get("String");
        Assertions.assertEquals("cat", string);
        Object integer = valueOperations.get("Integer");
        Assertions.assertEquals(16, integer);
        Object aDouble = valueOperations.get("Double");
        Assertions.assertEquals(100.00, aDouble);
        System.out.println(valueOperations.get("User"));

        redisTemplate.expire("String", 5, TimeUnit.MINUTES);
        redisTemplate.expire("Integer", 10, TimeUnit.MINUTES);
        redisTemplate.expire("Double", 15, TimeUnit.MINUTES);
        redisTemplate.expire("User", 20, TimeUnit.MINUTES);
    }
}
