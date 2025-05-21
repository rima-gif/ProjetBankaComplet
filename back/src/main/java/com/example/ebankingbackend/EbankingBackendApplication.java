package com.example.ebankingbackend;

import com.example.ebankingbackend.dtos.CurrentBankAccountDTO;
import com.example.ebankingbackend.dtos.CustomerDTO;
import com.example.ebankingbackend.dtos.SavingBankAccountDTO;
import com.example.ebankingbackend.entities.*;
import com.example.ebankingbackend.enums.AccountStatus;
import com.example.ebankingbackend.enums.OperationType;
import com.example.ebankingbackend.repositories.AccountOperationRepository;
import com.example.ebankingbackend.repositories.BankAccountRepository;
import com.example.ebankingbackend.repositories.CustomerRepository;
import com.example.ebankingbackend.services.BankAccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.security.SecureRandom;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootApplication
public class EbankingBackendApplication {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static void main(String[] args) {
        SpringApplication.run(EbankingBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(BankAccountService bankAccountService) {
        return args -> {
            Stream.of("Hassan", "Yassine", "Aziz").forEach(name -> {
                CustomerDTO customer = new CustomerDTO();
                customer.setName(name);
                customer.setEmail(name.toLowerCase() + "@gmail.com");
                bankAccountService.saveCustomer(customer);
            });

            bankAccountService.listCustomers().forEach(customer -> {
                bankAccountService.saveCurrentBankAccount(getSecureRandomAmount(90000), customer.getId(), 9000);
                bankAccountService.saveSavingBankAccount(getSecureRandomAmount(120000), customer.getId(), 5.5);

                bankAccountService.bankAccountsList().forEach(b -> {
                    String accountId;
                    if (b instanceof SavingBankAccountDTO savingBankAccountDTO) {
                        accountId = savingBankAccountDTO.getId();
                    } else if (b instanceof CurrentBankAccountDTO currentBankAccountDTO) {
                        accountId = currentBankAccountDTO.getId();
                    } else {
                        throw new IllegalStateException("Unknown account type: " + b.getClass());
                    }

                    bankAccountService.credit(accountId, 10000 + getSecureRandomAmount(120000), "Credit");
                    bankAccountService.debit(accountId, 100 + getSecureRandomAmount(1200), "Debit");
                });
            });
        };
    }

    @Bean
    CommandLineRunner start(CustomerRepository customerRepository,
                            BankAccountRepository bankAccountRepository,
                            AccountOperationRepository accountOperationRepository) {
        return args -> {
            // Create customers
            Stream.of("Hassan", "Yassine", "Aziz").forEach(name -> {
                Customer customer = new Customer();
                customer.setName(name);
                customer.setEmail(name.toLowerCase() + "@gmail.com");
                customerRepository.save(customer);
            });

            // Create current accounts
            customerRepository.findAll().forEach(cust -> {
                CurrentAccount currentAccount = new CurrentAccount();
                currentAccount.setId(UUID.randomUUID().toString());
                currentAccount.setBalance(getSecureRandomAmount(10000));
                currentAccount.setCreateAt(new Date());
                currentAccount.setStatus(AccountStatus.CREATED);
                currentAccount.setCustomer(cust);
                currentAccount.setOverDraft(9000);
                bankAccountRepository.save(currentAccount);
            });

            // Create saving accounts
            customerRepository.findAll().forEach(cust -> {
                SavingAccount savingAccount = new SavingAccount();
                savingAccount.setId(UUID.randomUUID().toString());
                savingAccount.setBalance(getSecureRandomAmount(10000));
                savingAccount.setCreateAt(new Date());
                savingAccount.setStatus(AccountStatus.CREATED);
                savingAccount.setCustomer(cust);
                savingAccount.setInterestRate(5.5);
                bankAccountRepository.save(savingAccount);
            });

            // Create operations
            bankAccountRepository.findAll().forEach(account -> {
                for (int i = 0; i < 5; i++) {
                    AccountOperation operation = new AccountOperation();
                    operation.setOperationDate(new Date());
                    operation.setAmount(getSecureRandomAmount(500));
                    operation.setType(getRandomOperationType());
                    operation.setBankAccount(account);
                    accountOperationRepository.save(operation);
                }
            });
        };
    }

    private static double getSecureRandomAmount(double max) {
        return secureRandom.nextDouble() * max;
    }

    private static OperationType getRandomOperationType() {
        return secureRandom.nextBoolean() ? OperationType.DEBIT : OperationType.CREDIT;
    }
}
