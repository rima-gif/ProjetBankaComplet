package com.example.ebankingbackend;

import org.apache.catalina.core.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringBootTest

class EbankingBackendApplicationTests {
    @Autowired
    private ApplicationContext applicationContext;


    @Test
    void contextLoads() {

    }
}


