package com.mongoapp.transactions.config;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

    /**
     * Without this bean, @Transactional on a Mongo repository/service
     * method does nothing - Spring has no transaction manager that knows
     * how to start a Mongo session/transaction.
     *
     * The write/read concern set here apply to every transaction in this
     * app: WriteConcern.MAJORITY means a transaction isn't considered
     * committed until a majority of replica set members have it durably
     * applied (on this module's single-node replica set, that's just the
     * one node - the real payoff shows up in module 09 with multiple
     * nodes). ReadConcern.SNAPSHOT gives the transaction a consistent
     * point-in-time view of the data for its whole duration.
     */
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
