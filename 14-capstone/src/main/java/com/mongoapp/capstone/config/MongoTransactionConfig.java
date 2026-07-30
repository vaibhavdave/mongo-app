package com.mongoapp.capstone.config;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Same setup as module 06 - required for OrderService.placeOrder's @Transactional to do anything. */
@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        MongoTransactionManager manager = new MongoTransactionManager(dbFactory);
        manager.setOptions(TransactionOptions.builder()
                .writeConcern(WriteConcern.MAJORITY)
                .readConcern(ReadConcern.SNAPSHOT)
                .build());
        return manager;
    }
}
