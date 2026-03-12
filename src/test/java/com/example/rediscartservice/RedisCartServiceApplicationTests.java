package com.example.rediscartservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(properties = {
    "spring.data.redis.repositories.enabled=false",
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
        "org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration"
})
class RedisCartServiceApplicationTests {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private CacheManager cacheManager;

    @Test
    void contextLoads() {
        // Verify Spring context starts correctly
    }
}
