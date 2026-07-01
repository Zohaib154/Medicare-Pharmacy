package com.medicare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "java.awt.headless=true")
@ActiveProfiles("h2")
class PharmacyApplicationTests {

    @Test
    void contextLoads() {
    }
}

