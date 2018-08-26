package com.quinemccluskey;

public class TermConverter {

	private static final String CUSTOM_OR = "/";
	private static final String CUSTOM_AND = "+";
	private static final String CUSTOM_MINUS = "-";
	private static final String OR = "|";
	private static final String AND = "&";
	private static final String MINUS = "!";
	
	public static String convertToJBoolOperators(String term) {
		term = term.replace(CUSTOM_OR, OR);
		term = term.replace(CUSTOM_AND, AND);
		term = term.replace(CUSTOM_MINUS, MINUS);
		return term;
	}

	public static String reconvertToJBoolOperators(String term) {
		term = term.replace(OR, CUSTOM_OR);
		term = term.replace(AND, CUSTOM_AND);
		term = term.replace(MINUS, CUSTOM_MINUS);
		return term;
	}
}
