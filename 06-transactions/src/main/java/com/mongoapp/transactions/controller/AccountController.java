package com.mongoapp.transactions.controller;

import com.mongoapp.transactions.model.BankAccount;
import com.mongoapp.transactions.repository.BankAccountRepository;
import com.mongoapp.transactions.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankAccountRepository accountRepository;
    private final TransferService transferService;

    public AccountController(BankAccountRepository accountRepository, TransferService transferService) {
        this.accountRepository = accountRepository;
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<BankAccount> create(@Valid @RequestBody BankAccount account) {
        account.setId(null);
        account.setVersion(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountRepository.save(account));
    }

    @GetMapping
    public List<BankAccount> findAll() {
        return accountRepository.findAll();
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferSafe(@RequestParam String fromId, @RequestParam String toId,
                                           @RequestParam double amount,
                                           @RequestParam(defaultValue = "false") boolean simulateFailure) {
        transferService.transferSafe(fromId, toId, amount, simulateFailure);
        return ResponseEntity.ok(Map.of("status", "transferred", "mode", "transactional"));
    }

    @PostMapping("/transfer-unsafe")
    public ResponseEntity<?> transferUnsafe(@RequestParam String fromId, @RequestParam String toId,
                                             @RequestParam double amount,
                                             @RequestParam(defaultValue = "false") boolean simulateFailure) {
        transferService.transferUnsafe(fromId, toId, amount, simulateFailure);
        return ResponseEntity.ok(Map.of("status", "transferred", "mode", "non-transactional"));
    }
}
