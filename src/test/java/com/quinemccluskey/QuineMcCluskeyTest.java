package com.quinemccluskey;

import org.junit.Assert;
import org.junit.Test;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

public class QuineMcCluskeyTest {

	private static final String OR = "|";
	private static final String AND = "&";
	private static final String MINUS = "!";
	private static final String BRACKETOPEN = "(";
	private static final String BRACKETCLOSE = ")";

	@Test
	public void testRunSimpleTerm() {
		String term = "A+(B/C)";
		String resultJBoolExpression = JBoolExpressionDNFCreator.run(term);
		String resultQuineMcCluskey = DNFCreatorByQuineMcCluskey.run(term);

		equal("A+B/A+C", resultJBoolExpression);
	}

	/**
	 * simplify expression in order to compare it logical
	 * 
	 * @param expect
	 * @param result
	 */
	private void equal(String expect, String result) {
		String convertToJBoolOperatorsExpect = TermConverter.convertToJBoolOperators(expect);
		Expression<String> expectedExpr = ExprParser.parse(convertToJBoolOperatorsExpect);

		String convertToJBoolOperatorsTerm = TermConverter.convertToJBoolOperators(result);
		Expression<String> resultExpr = ExprParser.parse(convertToJBoolOperatorsTerm);

		expectedExpr = RuleSet.simplify(expectedExpr);
		resultExpr = RuleSet.simplify(resultExpr);
		Assert.assertEquals(expectedExpr, resultExpr);
	}
}
