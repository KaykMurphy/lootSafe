package com.lootsafe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"lootsafe.security.encryption-key=1234567890123456",
		"lootsafe.security.admin-api-key=test-admin-key",
		"lootsafe.secret-key=test-webhook-secret",
		"lootsafe.access-token=test-access-token"
})
class LootSafeApplicationTests {

	@Test
	void contextLoads() {
	}

}
