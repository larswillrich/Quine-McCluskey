package com.quinemccluskey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class CanonicalDNFFinderTest {

	private static final String BBK_SPLITTER = "[^a-zA-Z0-9']+|[a-zA-Z0-9']+";
	private static final String REGEX_GET_BRACKETS_AND_LOGICAL_EXP_CHARS = "[\\/|\\(|\\)|\\-|\\+|\\s]+";
	private static final String SPACE_SIGN = " ";
	public final static String TRUE_CONDITION_VALUE = "true";
	public final static String FALSE_CONDITION_VALUE = "false";

	String timeOutCondition = "-(-(460/494/830)+-(M016+M276)+-(M274+ME06)+-ME05+(P15/P23/P31/P76))+-(-(460/494/830)+-(M016+M276)+-(ME05/ME06)+-100+P15)+-(-(460/494/830)+-(M016+M276)+-100+-ME05+P15)+-(-(460/494/830)+-(M035/ME05)+-100+(M16/M20)+(P15/P23/P76)+M654)+-(-(460/494/830)+-(ME05/ME06)+-100+(P15/P23/P76)+M20+M274)+-(-(460/494/830)+-100+-M016+(P15/P23/P76)+M276)+-(-(460/494/830)+-100+-M035+(P15/P23/P76)+M642)+-(-(460/494/830)+M016+M276+P31)+-(-(M016+M276)+-(M274+ME05)+(460/494)+(P15/P23))+-(-(M016+M276)+-(M274+ME05)+(460/494)+772+P15)+-(-(M016+M276)+-(M274+ME05)+(460/494)+P15)+-(-(M016+M276)+-(M274+ME05)+P31)+-(-(M035/ME05)+-100+(460/494)+(P15/P23)+017+M20+M654)+-(-100+-830+(P15/P23/P76)+M16+M274)+-(-553L+M016+M276+P31)+-((-(M005+M014)+-(M005+M642)+-(M274+ME06)+-M016+2XXL+P31)/(-(M005+M642)+-(M274+ME06)+-M276+2XXL+P31))+-((-(M274/M654)+-M035+(P15/P23/P76)+100)/(-M035+-ME05+(P15/P23/P76)+100))+-((2XXL/5XXL)+M016+M276+P31)+-((460/494/830)+M016+M276+P31)+-((460/494)+M016+M276+P31)+-((M274/M654)+(P15/P23/P31/P76)+017+ME05)+-(830+P15)+-(M016+M276+P31)+(460/494/496/498/623/625/821/822/823/829/831/835)";
	String timeOutConditionShort = "(460/494/496/498/623/625/821/822/823/829/831/835)";

	@Test
	public void testRunSimpleTerm() {
		String term = "(A/-B)+C";

		term = timeOutConditionShort;
		Collection<Map<String, Boolean>> find = CanonicalDNFFinder.find(term);
		System.out.println(term);

		printAllTrueCombinations(term, find);
		printKDNF(term, find);
	}

	private void printAllTrueCombinations(String term, Collection<Map<String, Boolean>> find) {
		for (Map<String, Boolean> map : find) {
			System.out.println(assignBooleanValuesToConditionWithoutSimplification(term, map));
		}
	}

	private void printKDNF(String term, Collection<Map<String, Boolean>> find) {
		Set<String> extractCodesFromBuildability = extractCodesFromBuildability(term);
		String dnf = "";
		for (Map<String, Boolean> map : find) {
			String conjunctive = "";
			for (String code : extractCodesFromBuildability) {
				Boolean boolean1 = map.get(code);
				if (boolean1.booleanValue() == false) {
					conjunctive += "+-" + code;
				} else {
					conjunctive += "+" + code;
				}
			}
			conjunctive = conjunctive.substring(1, conjunctive.length());
			dnf += conjunctive + "/";
		}
		dnf = dnf.substring(0, dnf.length() - 1);

		String[] split = dnf.split("/");
		for (int i = 0; i < split.length; i++) {
			System.out.println(i + 1 + ": " + split[i]);
		}
		System.out.println("number of vars: " + extractCodesFromBuildability.size());
	}

	public String assignBooleanValuesToConditionWithoutSimplification(String condition,
			Map<String, Boolean> atomicElementsToBooleanMap) {
		Pattern splittedBbkPattern = Pattern.compile(BBK_SPLITTER);
		Matcher splittedBbkMatcher = splittedBbkPattern.matcher(condition);
		List<String> finalSplittedBbk = new ArrayList<>();
		while (splittedBbkMatcher.find()) {
			String element = splittedBbkMatcher.group();
			if (atomicElementsToBooleanMap.containsKey(element)) {
				finalSplittedBbk.add(Boolean.toString(atomicElementsToBooleanMap.get(element)));
			} else {
				finalSplittedBbk.add(element);
			}
		}

		StringBuilder builder = new StringBuilder();
		for (String s : finalSplittedBbk) {
			builder.append(s);
		}
		String finalBbk = builder.toString();
		return finalBbk;
	}

	private static Set<String> extractCodesFromBuildability(String buildabilityCondition) {
		Set<String> result = new HashSet<>();
		if (buildabilityCondition == null) {
			return result;
		}
		String newBuildabilityCondition = buildabilityCondition.replaceAll(REGEX_GET_BRACKETS_AND_LOGICAL_EXP_CHARS,
				SPACE_SIGN);
		List<String> splited = Arrays.asList(newBuildabilityCondition.split(SPACE_SIGN));
		Iterator<String> iterator = splited.iterator();
		while (iterator.hasNext()) {
			String element = iterator.next();
			if (element != null && !element.isEmpty() && !TRUE_CONDITION_VALUE.equals(element)
					&& !FALSE_CONDITION_VALUE.equals(element)) {
				result.add(element);
			}
		}
		return result;
	}
}
