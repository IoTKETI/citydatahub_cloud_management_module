package com.datahub.infra.coretoast.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JsonDateDeserializer extends JsonDeserializer<String> {

	private static Logger logger = LoggerFactory.getLogger(JsonDateDeserializer.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
			throws IOException, JsonProcessingException {
		String date = jsonParser.getText();
		if(date == null || date.equals("")) return null;
		try {
			date = getDate(date);
			return date;
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return date;

	}
	public String getDate(String inputdate) throws ParseException {
		Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(inputdate);
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
	}


}