package com.quinemccluskey;

import org.junit.Assert;
import org.junit.Test;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

public class FactoriserTest {

	String timeOutCondition = "-(-(460/494/830)+-(M016+M276)+-(M274+ME06)+-ME05+(P15/P23/P31/P76))+-(-(460/494/830)+-(M016+M276)+-(ME05/ME06)+-100+P15)+-(-(460/494/830)+-(M016+M276)+-100+-ME05+P15)+-(-(460/494/830)+-(M035/ME05)+-100+(M16/M20)+(P15/P23/P76)+M654)+-(-(460/494/830)+-(ME05/ME06)+-100+(P15/P23/P76)+M20+M274)+-(-(460/494/830)+-100+-M016+(P15/P23/P76)+M276)+-(-(460/494/830)+-100+-M035+(P15/P23/P76)+M642)+-(-(460/494/830)+M016+M276+P31)+-(-(M016+M276)+-(M274+ME05)+(460/494)+(P15/P23))+-(-(M016+M276)+-(M274+ME05)+(460/494)+772+P15)+-(-(M016+M276)+-(M274+ME05)+(460/494)+P15)+-(-(M016+M276)+-(M274+ME05)+P31)+-(-(M035/ME05)+-100+(460/494)+(P15/P23)+017+M20+M654)+-(-100+-830+(P15/P23/P76)+M16+M274)+-(-553L+M016+M276+P31)+-((-(M005+M014)+-(M005+M642)+-(M274+ME06)+-M016+2XXL+P31)/(-(M005+M642)+-(M274+ME06)+-M276+2XXL+P31))+-((-(M274/M654)+-M035+(P15/P23/P76)+100)/(-M035+-ME05+(P15/P23/P76)+100))+-((2XXL/5XXL)+M016+M276+P31)+-((460/494/830)+M016+M276+P31)+-((460/494)+M016+M276+P31)+-((M274/M654)+(P15/P23/P31/P76)+017+ME05)+-(830+P15)+-(M016+M276+P31)+(460/494/496/498/623/625/821/822/823/829/831/835)";
	String alreadyDNF = "(460/494/496/498/623/625/821/822/823/829/831/835)";
	String timeOutConditionShort = "(460/494/830)+-(M016+M276)+-(M274+ME06)+-ME05+(P15/P23/P31/P76))";

	@Test
	public void testRunSimpleTerm() {
		checkDNFAndEquality("(A/-B)+C", "C+A/C+-B");
		checkDNFAndEquality("(A/-B)+C+D", "C+D+A/C+D+-B");
		checkDNFAndEquality("(A/-B)+C/D", "C+A/C+-B/D");
		checkDNFAndEquality("(A/-B)+(C/D)", "A+C/A+D/-B+C/-B+D");
	}

	private void checkDNFAndEquality(String term, String expected) {
		String find = Factoriser.find(term);
		System.out.println(term + "\n" + find + "\n");

		equal(term, find);
		Assert.assertEquals(expected, find);
	}

	/**
	 * simplify expression in order to compare it logical
	 * 
	 * @param expect
	 * @param result
	 */
	private void equal(String expect, String result) {
		expect = JBoolExpressionDNFCreator.run(expect);
		result = JBoolExpressionDNFCreator.run(result);

		String convertToJBoolOperatorsExpect = TermConverter.convertToJBoolOperators(expect);
		Expression<String> expectedExpr = ExprParser.parse(convertToJBoolOperatorsExpect);

		String convertToJBoolOperatorsTerm = TermConverter.convertToJBoolOperators(result);
		Expression<String> resultExpr = ExprParser.parse(convertToJBoolOperatorsTerm);

		expectedExpr = RuleSet.simplify(expectedExpr);
		resultExpr = RuleSet.simplify(resultExpr);
		Assert.assertEquals(expectedExpr, resultExpr);
	}
}
