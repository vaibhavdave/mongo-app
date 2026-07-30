package com.mongoapp.transactions.service;

import com.mongoapp.transactions.exception.InsufficientFundsException;
import com.mongoapp.transactions.exception.SimulatedFailureException;
import com.mongoapp.transactions.model.BankAccount;
import com.mongoapp.transactions.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final BankAccountRepository accountRepository;

    public TransferService(BankAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * @Transactional wraps both save() calls in a single Mongo session
     * transaction. If anything after the debit throws - including the
     * simulated failure below - every write this method made is rolled
     * back as if none of it happened. Compare with transferUnsafe().
     */
    @Transactional
    public void transferSafe(String fromId, String toId, double amount, boolean simulateFailureAfterDebit) {
        BankAccount from = accountRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("No account " + fromId));
        if (from.getBalance() < amount) {
            throw new InsufficientFundsException("Account " + fromId + " has insufficient funds");
        }
        from.setBalance(from.getBalance() - amount);
        accountRepository.save(from);

        if (simulateFailureAfterDebit) {
            throw new SimulatedFailureException("Simulated failure after debit, before credit");
        }

        BankAccount to = accountRepository.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("No account " + toId));
        to.setBalance(to.getBalance() + amount);
        accountRepository.save(to);
    }

    /**
     * Same two writes, no @Transactional. If the simulated failure fires,
     * the debit from transferSafe's failure case has already been
     * committed - the source account permanently loses the money with no
     * corresponding credit anywhere. Run both endpoints with
     * simulateFailure=true and compare the resulting balances.
     */
    public void transferUnsafe(String fromId, String toId, double amount, boolean simulateFailureAfterDebit) {
        BankAccount from = accountRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("No account " + fromId));
        if (from.getBalance() < amount) {
            throw new InsufficientFundsException("Account " + fromId + " has insufficient funds");
        }
        from.setBalance(from.getBalance() - amount);
        accountRepository.save(from);

        if (simulateFailureAfterDebit) {
            throw new SimulatedFailureException("Simulated failure after debit, before credit - money is now gone");
        }

        BankAccount to = accountRepository.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("No account " + toId));
        to.setBalance(to.getBalance() + amount);
        accountRepository.save(to);
    }
}
