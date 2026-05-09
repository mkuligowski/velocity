package com.mkuligowski.velocity.loads.adapters.rest;

import com.mkuligowski.velocity.loads.domain.model.Money;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public final class MoneyDeserializer extends ValueDeserializer<Money> {

    @Override
    public Money deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("load_amount must not be empty");
        }
        return Money.parseDollars(text);
    }
}
