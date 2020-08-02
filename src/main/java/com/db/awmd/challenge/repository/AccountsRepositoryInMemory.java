package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.LowBalanceException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();
  private Lock lock1 = new ReentrantLock();
  private Lock lock2 = new ReentrantLock();

  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
        "Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Override
  public Account getAccount(String accountId) {
    return accounts.get(accountId);
  }

  @Override
  public void clearAccounts() {
    accounts.clear();
  }

  @Override
	public void transferAmount(String accountFromId, String accountToId, BigDecimal amountTransferd) throws InterruptedException {
		Account from = getAccount(accountFromId);
		BigDecimal fromBalance = from.getBalance();
		acquireLock(lock1, lock2);
		try {
			if (fromBalance.subtract(amountTransferd).compareTo(BigDecimal.ZERO) < 0)
				throw new LowBalanceException(
						"Account id " + from.getAccountId() + " has not enough balance to transfer this much amount");
			else {
				Account to = getAccount(accountToId);
				from.setBalance(fromBalance.subtract(amountTransferd));
				to.setBalance(to.getBalance().add(amountTransferd));
			}
		} finally {
			lock1.unlock();
			lock2.unlock();
		}
	}
  /*
	 * Solution for never get into the deadlock
	 */
	public void acquireLock(Lock firstlock, Lock secondLock) throws InterruptedException {
		while(true) {
			boolean gotFirstlock = false;
			boolean gotSecondlock = false;
			try {
				gotFirstlock = firstlock.tryLock();
				gotSecondlock = secondLock.tryLock();
			}
			finally {
				if(gotFirstlock && gotSecondlock)
					return;
				if(gotFirstlock)
					firstlock.unlock();
				if(gotSecondlock)
					secondLock.unlock();
			}
		}
	}
}
