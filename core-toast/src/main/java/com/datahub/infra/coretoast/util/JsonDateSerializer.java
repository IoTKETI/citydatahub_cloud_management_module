package com.datahub.infra.coretoast.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JsonDateSerializer extends JsonSerializer<String> {
    private static Logger logger = LoggerFactory.getLogger(JsonDateSerializer.class);

    @Override
    public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        try {
            value=getDate(value);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        jgen.writeString(value);
    }

    public String getDate(String inputdate) throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(inputdate);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

}