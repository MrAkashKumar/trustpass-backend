package com.trustpass;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trustpass-test;DB_CLOSE_DELAY=-1",
        "trustpass.seed.enabled=false",
        "trustpass.integrations.openai.enabled=false",
        "trustpass.integrations.elevenlabs.enabled=false"
})
class TrustPassApplicationTest {
    @Test
    void contextLoads() {}
}
