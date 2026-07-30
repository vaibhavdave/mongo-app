package com.mongoapp.schemavalidation.config;

import com.mongoapp.schemavalidation.convert.MoneyReadingConverter;
import com.mongoapp.schemavalidation.convert.MoneyWritingConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new MoneyWritingConverter(), new MoneyReadingConverter()));
    }
}
