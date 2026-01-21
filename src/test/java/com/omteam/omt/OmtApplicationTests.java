package com.omteam.omt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class OmtApplicationTests {

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@Test
	void contextLoads() {
	}

}
