package com.example.ebankingbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest

class EbankingBackendApplicationTests {
      @Autowired
    private ApplicationContext applicationContext;


    @Test
    void contextLoads() {
         assertNotNull(applicationContext, "Application context should not be null");
        
        // Verify key beans are available in the context
        assertTrue(applicationContext.containsBean("bankAccountService"), 
            "BankAccountService bean should be available");
        
        assertTrue(applicationContext.containsBean("customerRepository"), 
            "CustomerRepository bean should be available");
        
        assertTrue(applicationContext.containsBean("bankAccountRepository"), 
            "BankAccountRepository bean should be available");
    }
    }


