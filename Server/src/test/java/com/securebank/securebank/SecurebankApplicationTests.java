package com.securebank.securebank;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires MySQL via Docker — run manually with Docker Compose up")
class SecurebankApplicationTests {

	@Test
	void contextLoads() {
	}

}
