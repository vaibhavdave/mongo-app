package com.mongoapp.schemavalidation.convert;

import com.mongoapp.schemavalidation.model.Money;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/** Registered in MongoConfig - runs whenever a Money field is about to be saved. */
@WritingConverter
public class MoneyWritingConverter implements Converter<Money, Document> {

    @Override
    public Document convert(Money source) {
        return new Document()
                .append("amount", new Decimal128(source.getAmount()))
                .append("currency", source.getCurrency());
    }
}
