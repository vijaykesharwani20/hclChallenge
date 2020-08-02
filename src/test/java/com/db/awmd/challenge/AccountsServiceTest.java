package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.LowBalanceException;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }
  
  @Test
  public void transferAccount() throws Exception {
	Account accountFrom = new Account("Id-123");
	accountFrom.setBalance(new BigDecimal(1000));
	this.accountsService.createAccount(accountFrom);
	
	Account accountTo = new Account("Id-1234");
	accountTo.setBalance(new BigDecimal(1000));
	this.accountsService.createAccount(accountTo);
	BigDecimal transferAmount = new BigDecimal(800);
	
	this.accountsService.transferAmount(accountFrom.getAccountId(), accountTo.getAccountId(), transferAmount);

	assertThat(this.accountsService.getAccount("Id-123").getBalance()).isEqualTo(new BigDecimal(200));

  }
  
  @Test
  public void transferAccount_failOnNegativeBalance() throws Exception {
	Account accountFrom = new Account("Id-123");
	accountFrom.setBalance(new BigDecimal(1000));
	this.accountsService.createAccount(accountFrom);
	
	Account accountTo = new Account("Id-1234");
	accountTo.setBalance(new BigDecimal(1000));
	this.accountsService.createAccount(accountTo);
	BigDecimal transferAmount = new BigDecimal(1200);
	
	try {
		this.accountsService.transferAmount(accountFrom.getAccountId(), accountTo.getAccountId(), transferAmount);
		fail("Should have failed when transferring more than balance existed.");
	}catch(LowBalanceException ex) {
		assertThat(ex.getMessage()).isEqualTo("Account id " + accountFrom.getAccountId() + " has not enough balance to transfer this much amount");
	}
  }
}
