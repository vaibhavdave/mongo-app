package com.mongoapp.schemavalidation.convert;

import com.mongoapp.schemavalidation.model.Money;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/** The inverse of MoneyWritingConverter - runs whenever a "price" document is read back. */
@ReadingConverter
public class MoneyReadingConverter implements Converter<Document, Money> {

    @Override
    public Money convert(Document source) {
        Decimal128 amount = source.get("amount", Decimal128.class);
        String currency = source.getString("currency");
        return new Money(amount == null ? null : amount.bigDecimalValue(), currency);
    }
}
