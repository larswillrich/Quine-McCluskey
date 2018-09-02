package com.quinemccluskey;

import org.junit.Assert;
import org.junit.Test;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

public class FactoriserTest {

	private static final String REGEX_GET_BRACKETS_AND_LOGICAL_EXP_CHARS = "[\\/|\\(|\\)|\\-|\\+|\\s]+";
	private static final String SPACE_SIGN = " ";
	public final static String TRUE_CONDITION_VALUE = "true";
	public final static String FALSE_CONDITION_VALUE = "false";

	String timeOutCondition = "-(-(460/494/830)+-(M016+M276)+-(M274+ME06)+-ME05+(P15/P23/P31/P76))+-(-(460/494/830)+-(M016+M276)+-(ME05/ME06)+-100+P15)+-(-(460/494/830)+-(M016+M276)+-100+-ME05+P15)+-(-(460/494/830)+-(M035/ME05)+-100+(M16/M20)+(P15/P23/P76)+M654)+-(-(460/494/830)+-(ME05/ME06)+-100+(P15/P23/P76)+M20+M274)+-(-(460/494/830)+-100+-M016+(P15/P23/P76)+M276)+-(-(460/494/830)+-100+-M035+(P15/P23/P76)+M642)+-(-(460/494/830)+M016+M276+P31)+-(-(M016+M276)+-(M274+ME05)+(460/494)+(P15/P23))+-(-(M016+M276)+-(M274+ME05)+(460/494)+772+P15)+-(-(M016+M276)+-(M274+ME05)+(460/494)+P15)+-(-(M016+M276)+-(M274+ME05)+P31)+-(-(M035/ME05)+-100+(460/494)+(P15/P23)+017+M20+M654)+-(-100+-830+(P15/P23/P76)+M16+M274)+-(-553L+M016+M276+P31)+-((-(M005+M014)+-(M005+M642)+-(M274+ME06)+-M016+2XXL+P31)/(-(M005+M642)+-(M274+ME06)+-M276+2XXL+P31))+-((-(M274/M654)+-M035+(P15/P23/P76)+100)/(-M035+-ME05+(P15/P23/P76)+100))+-((2XXL/5XXL)+M016+M276+P31)+-((460/494/830)+M016+M276+P31)+-((460/494)+M016+M276+P31)+-((M274/M654)+(P15/P23/P31/P76)+017+ME05)+-(830+P15)+-(M016+M276+P31)+(460/494/496/498/623/625/821/822/823/829/831/835)";
	String alreadyDNF = "(460/494/496/498/623/625/821/822/823/829/831/835)";
	String timeOutConditionShort1 = "(460/494/830)+-(M016+M276)+-(M274+ME06)+-ME05+(P15/P23/P31/P76)";
	String timeOutConditionShort2 = "(460/494/830)+-(M274+ME06)+-ME05+(P15/P23)";
	String timeOutConditionShort3 = "(460/494/830)+(P15/P23)";
	String timeOutConditionShort4 = "-(-(460/494/830)+-(M016+M276)+-ME05+(P31/P76))";
	String timeOutConditionShort5 = "-(-(460/494/830)+-ME05)";

	@Test
	public void testRunSimpleTerm() {
		checkDNFAndEquality("(A/-B)+C", "C+A/C+-B");
		checkDNFAndEquality("(A/-B)+C+D", "C+D+A/C+D+-B");
		checkDNFAndEquality("(A/-B)+C/D", "C+A/C+-B/D");
		checkDNFAndEquality("(A/-B)+(C/D)", "A+C/A+D/-B+C/-B+D");
		checkDNFAndEquality("(A/-B)+(C/D+E)", "A+C/A+D+E/-B+C/-B+D+E");
		checkDNFAndEquality("(A/-B)+(C/D+E)+F", "F+A+C/F+A+D+E/F+-B+C/F+-B+D+E");
		checkDNFAndEquality("(A/-B)+-(C/D+E)+F", "F+A+-C+-D/F+A+-C+-E/F+-B+-C+-D/F+-B+-C+-E");

		checkDNFAndEquality(timeOutConditionShort2, timeOutConditionShort2);
		checkDNFAndEquality(timeOutConditionShort1, timeOutConditionShort1);
		checkDNFAndEquality(timeOutConditionShort5, timeOutConditionShort5);
		checkDNFAndEquality(timeOutConditionShort4, timeOutConditionShort4);
		// checkDNFAndEquality(timeOutCondition, timeOutCondition);
	}

	private void checkDNFAndEquality(String term, String expected) {
		String find = Factoriser.find(term, 0);
		System.out.println("found dnf: \n" + term + "\n" + find + "\n");

		equal(term, find);
		equal(term, expected);
		Assert.assertTrue(QuineMcCluskeyTest.isDNF(term));
	}

	/**
	 * simplify expression in order to compare it logical
	 * 
	 * @param expect
	 * @param result
	 */
	public static boolean equal(String expect, String result, boolean withAssertion) {
		expect = JBoolExpressionDNFCreator.run(expect);
		result = JBoolExpressionDNFCreator.run(result);

		String convertToJBoolOperatorsExpect = TermConverter.convertToJBoolOperators(expect);
		Expression<String> expectedExpr = ExprParser.parse(convertToJBoolOperatorsExpect);

		String convertToJBoolOperatorsTerm = TermConverter.convertToJBoolOperators(result);
		Expression<String> resultExpr = ExprParser.parse(convertToJBoolOperatorsTerm);

		expectedExpr = RuleSet.simplify(expectedExpr);
		resultExpr = RuleSet.simplify(resultExpr);

		expect = TermConverter.reconvertToJBoolOperators(expectedExpr.toString());
		result = TermConverter.reconvertToJBoolOperators(resultExpr.toString());

		if (withAssertion) {
			System.out.println("expect: " + expect);
			System.out.println("result: " + result);
			Assert.assertEquals(expect, result);
		}
		return expect.equals(result);
	}

	public static boolean equal(String expect, String result) {
		return equal(expect, result, true);
	}
}
