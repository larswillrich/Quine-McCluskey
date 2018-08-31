package com.quinemccluskey;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

public class JBoolExpressionDNFCreator {

	public static String run(String term) {
		String convertToJBoolOperators = TermConverter.convertToJBoolOperators(term);
		Expression<String> nonStandard = ExprParser.parse(convertToJBoolOperators);

		Expression<String> dnf = RuleSet.toDNF(nonStandard);
		String reconvertToJBoolOperators = TermConverter.reconvertToJBoolOperators(dnf.toString());
		return reconvertToJBoolOperators;
	}

	public static String simplify(String term) {
		String convertToJBoolOperators = TermConverter.convertToJBoolOperators(term);
		Expression<String> nonStandard = ExprParser.parse(convertToJBoolOperators);

		Expression<String> dnf = RuleSet.simplify(nonStandard);
		String reconvertToJBoolOperators = TermConverter.reconvertToJBoolOperators(dnf.toString());
		return reconvertToJBoolOperators;
	}
}
