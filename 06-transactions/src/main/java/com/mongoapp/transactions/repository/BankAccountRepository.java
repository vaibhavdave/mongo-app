package com.mongoapp.transactions.repository;

import com.mongoapp.transactions.model.BankAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BankAccountRepository extends MongoRepository<BankAccount, String> {
}
