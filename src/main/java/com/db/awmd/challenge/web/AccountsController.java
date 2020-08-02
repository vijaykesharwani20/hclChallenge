package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.LowBalanceException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.EmailNotificationService;

import java.math.BigDecimal;
import java.util.Map;

import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;
  
  @Autowired
  private EmailNotificationService emailNotificationService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }
  
  @PostMapping(path = "/transfer",consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> transferAmount(@RequestBody Map<String, String> json) {
	String sourceAccountId = json.get("sourceAccountId");
	String targetAccountId = json.get("targetAccountId");
	BigDecimal transferAmount = new BigDecimal(json.get("transferAmount"));
	if(transferAmount.compareTo(BigDecimal.ZERO) < 0)
		return new ResponseEntity<Object>(HttpStatus.BAD_REQUEST);
    log.info("Transferring amount from Id {}", sourceAccountId);

    try {
    	this.accountsService.transferAmount(sourceAccountId, targetAccountId, transferAmount);
    	emailNotificationService.notifyAboutTransfer(accountsService.getAccount(sourceAccountId), "debited amount : "+transferAmount+" successfully");
    	emailNotificationService.notifyAboutTransfer(accountsService.getAccount(targetAccountId), "credited amount : "+transferAmount+" successfully");
    } catch (LowBalanceException | InterruptedException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

}
