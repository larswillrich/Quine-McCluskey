package com.quinemccluskey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;

public class Factoriser {

	private static final String AND = "+";
	private static final String OR = "/";
	private static final String MINUS = "-";
	private static final String BRACKETOPEN = "(";
	private static final String BRACKETCLOSE = ")";
	private static final String BLANK = " ";
	private static final String EMPTY_STRING = "";

	private static final String REGEX_GET_BRACKETS_AND_LOGICAL_EXP_CHARS = "[\\/|\\(|\\)|\\-|\\+|\\s]+";
	private static final String SPACE_SIGN = " ";
	public final static String TRUE_CONDITION_VALUE = "true";
	public final static String FALSE_CONDITION_VALUE = "false";

	public static String find(String condition, int level) {
		condition = condition.replace(BLANK, EMPTY_STRING);

		List<String> contentOfBrackets = getContentOfBrackets(condition);
		while (!contentOfBrackets.isEmpty()) {
			String firstBracket = contentOfBrackets.get(0);

			// if bracket is negativ, reformat it by deMorgan
			if (condition.indexOf(firstBracket) > 0) {
				int indexOfSign = condition.indexOf(firstBracket) - 1;
				if (MINUS.equals(String.valueOf(condition.charAt(indexOfSign)))) {
					String firstBracketDeMorgan = DeMorgan.applyDeMorganIfNegativeCondition(MINUS + firstBracket);
					firstBracketDeMorgan = firstBracketDeMorgan.replaceAll(BLANK, EMPTY_STRING);
					condition = condition.replaceFirst(Pattern.quote(MINUS + firstBracket),
							BRACKETOPEN + firstBracketDeMorgan + BRACKETCLOSE);

					contentOfBrackets = getContentOfBrackets(condition);
					continue;
				}
			}

			// remove outer brackets
			String contentOfFirstBracketWithoutBracket = removeOuterBracket(firstBracket);
			String termWithoutBracket = find(contentOfFirstBracketWithoutBracket, level + 1);

			Product product = multiply(condition, firstBracket, termWithoutBracket);

			condition = condition.replaceFirst(Pattern.quote(product.getTermToReplace()), product.getProduct());

			contentOfBrackets = getContentOfBrackets(condition);
		}

		return condition;
	}

	private static Product multiply(String term, String termToReplace, String termWithoutBracket) {

		Product p = new Factoriser().new Product();
		if (term.equals(termToReplace)) {
			termToReplace = removeOuterBracket(termToReplace);

			p.setProduct(termToReplace);
			p.setTermToReplace(term);
			return p;
		}

		int indexOfConcerningOperator = getIndexOfConcerningOperator(term, termToReplace, p);

		// if there are OR operators at both sites, just remove brackets
		if (indexOfConcerningOperator == -1) {
			String termToReplaceWithoutBrackets = removeOuterBracket(termToReplace);
			p.setProduct(termToReplaceWithoutBrackets);
			p.setTermToReplace(termToReplace);
			return p;
		}

		String factorA = getFactorToMultiply(term, termToReplace, indexOfConcerningOperator, p);

		String[] factorBs = termWithoutBracket.split(OR);
		String product = "";
		for (String factorB : factorBs) {
			product += OR + factorA + AND + factorB;
		}
		product = product.substring(1, product.length());

		if (p.bothSitesWithAND) {
			p.setProduct(BRACKETOPEN + product + BRACKETCLOSE);
		} else {
			p.setProduct(product);
		}

		return p;
	}

	private static String getFactorToMultiply(String term, String termToReplace, int indexOfConcerningOperator,
			Product p) {
		int indexOf = term.indexOf(termToReplace);

		String factor = null;
		// look at factor after operator
		if (indexOfConcerningOperator > indexOf) {

			// skip concerningOperator and take a look at variable after
			int indexOfVariableEnd = indexOfConcerningOperator + 1;

			char charAt = term.charAt(indexOfVariableEnd);
			boolean stillFactor = true;
			int bracketLevel = 0;
			while (stillFactor || (bracketLevel != 0)) {

				if (BRACKETOPEN.compareTo(String.valueOf(charAt)) == 0) {
					bracketLevel++;
				} else if (BRACKETCLOSE.compareTo(String.valueOf(charAt)) == 0) {
					bracketLevel--;
				} else if (OR.compareTo(String.valueOf(charAt)) == 0) {
					if (bracketLevel == 0) {
						stillFactor = false;
						continue;
					}
				}

				indexOfVariableEnd++;
				if (indexOfVariableEnd == term.length()) {
					if (bracketLevel == 0) {
						stillFactor = false;
						continue;
					}
				} else {
					charAt = term.charAt(indexOfVariableEnd);
				}
			}

			p.setTermToReplace(termToReplace + term.substring(indexOfConcerningOperator, indexOfVariableEnd));
			factor = term.substring(indexOfConcerningOperator + 1, indexOfVariableEnd);

		} else {
			// skip concerningOperator and take a look at variable before
			int indexOfVariableStart = indexOfConcerningOperator - 1;

			char charAt = term.charAt(indexOfVariableStart);
			boolean stillFactor = true;
			int bracketLevel = 0;
			while (stillFactor || (bracketLevel != 0)) {

				if (BRACKETOPEN.compareTo(String.valueOf(charAt)) == 0) {
					bracketLevel++;
				} else if (BRACKETCLOSE.compareTo(String.valueOf(charAt)) == 0) {
					bracketLevel--;
				} else if (OR.compareTo(String.valueOf(charAt)) == 0) {
					if (bracketLevel == 0) {
						stillFactor = false;
						indexOfVariableStart++;
						continue;
					}
				}

				indexOfVariableStart--;
				if (indexOfVariableStart == 0) {
					if (bracketLevel == 0) {
						stillFactor = false;
						continue;
					}
				} else {
					charAt = term.charAt(indexOfVariableStart);
				}
			}

			p.setTermToReplace(term.substring(indexOfVariableStart, indexOfConcerningOperator + 1) + termToReplace);
			factor = term.substring(indexOfVariableStart, indexOfConcerningOperator);
		}
		return factor;
	}

	/**
	 * Returns prioritized operator. If both operator has same priority, return
	 * first one.
	 * 
	 * @param term
	 * @param termToReplace
	 * @param p
	 * @return
	 */
	private static int getIndexOfConcerningOperator(String term, String termToReplace, Product p) {
		int indexOf = term.indexOf(termToReplace);

		// look at operator after closing bracket
		if (term.indexOf(termToReplace) == 0) {
			if (term.substring(indexOf + termToReplace.length(), indexOf + termToReplace.length() + 1).equals(OR)) {
				return -1;
			}
			return indexOf + termToReplace.length();
		} else {
			int indexOfOperatorBeforeBracket = indexOf - 1;
			int indexOfBracketAfterBracket = indexOf + termToReplace.length();

			// look at character before bracket
			if (indexOfBracketAfterBracket == term.length()) {
				if (term.substring(indexOfOperatorBeforeBracket, indexOfOperatorBeforeBracket + 1).equals(OR)) {
					return -1;
				}
				return indexOfOperatorBeforeBracket;
			}

			while (true) {
				if (term.substring(indexOfOperatorBeforeBracket, indexOfOperatorBeforeBracket + 1).equals(OR)) {
					if (term.substring(indexOfBracketAfterBracket, indexOfBracketAfterBracket + 1).equals(OR)) {
						return -1;
					}

					// Has to be an AND after bracket
					return indexOfBracketAfterBracket;
				} else if (term.substring(indexOfOperatorBeforeBracket, indexOfOperatorBeforeBracket + 1).equals(AND)) {
					if (term.substring(indexOfBracketAfterBracket, indexOfBracketAfterBracket + 1).equals(AND)) {
						p.setBothSitesWithAND(true);
						return indexOfOperatorBeforeBracket;
					}

					return indexOfOperatorBeforeBracket;
				} else {
					indexOfOperatorBeforeBracket--;
					indexOfBracketAfterBracket++;
				}
			}
		}
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

	private static int getNumberOfCodes(String buildabilityCondition) {
		if (buildabilityCondition == null) {
			return 0;
		}
		String newBuildabilityCondition = buildabilityCondition.replaceAll(REGEX_GET_BRACKETS_AND_LOGICAL_EXP_CHARS,
				SPACE_SIGN);
		List<String> splited = Arrays.asList(newBuildabilityCondition.split(SPACE_SIGN));
		return splited.size();
	}

	/**
	 * This method removes only outer brackets. Also if there is a minus, the
	 * possible
	 * 
	 * @param condition
	 * @return
	 */
	private static String removeOuterBracket(String condition) {
		while (outerBracketsExist(condition)) {
			if (!isPositive(condition)) {
				condition = condition.substring(1, condition.length());
			}
			if (condition.startsWith(BRACKETOPEN) && condition.endsWith(BRACKETCLOSE)) {
				condition = condition.substring(1, condition.length() - 1);
			}
		}
		return condition;
	}

	private static boolean isPositive(String condition) {
		if (outerBracketsExist(condition)) {
			if (condition.startsWith(MINUS)) {
				return false;
			}
		}
		if (isSingleCode(condition)) {
			if (condition.indexOf(MINUS) != -1) {
				return false;
			}
		}
		return true;
	}

	private static boolean outerBracketsExist(String condition) {
		if (condition.startsWith(MINUS)) {
			condition = condition.substring(1, condition.length());
		}
		List<String> contentOfBrackets = getContentOfBrackets(condition);
		if (contentOfBrackets.isEmpty()) {
			return false;
		}
		if (contentOfBrackets.get(0).length() == condition.length()) {
			return true;
		}

		return false;
	}

	/**
	 * Code can be in form with brackets and with minus in front.
	 * 
	 * @param condition
	 * @return
	 */
	private static boolean isSingleCode(String condition) {
		if (condition.contains(OR)) {
			return false;
		}
		if (condition.contains(AND)) {
			return false;
		}
		return true;
	}

	/**
	 * this method detects all brackets on same level and returns a list of them
	 * 
	 * @param term
	 * @return
	 */
	private static List<String> getContentOfBrackets(String term) {
		List<String> foundBrackets = new ArrayList<String>();

		if (!term.contains(BRACKETOPEN)) {
			return foundBrackets;
		}

		int counter = 0;

		int bracketPointer = 0;
		while (term.length() > 0) {
			int indexOfBracketClose = term.indexOf(BRACKETCLOSE, bracketPointer);
			int indexOfBracketOpen = term.indexOf(BRACKETOPEN, bracketPointer);

			if (indexOfBracketClose == -1) {
				return foundBrackets;
			}

			if (indexOfBracketOpen != -1 && indexOfBracketOpen < indexOfBracketClose) {
				// bracket open
				counter++;
				bracketPointer = indexOfBracketOpen + 1;
			} else {
				// bracket close
				counter--;
				bracketPointer = indexOfBracketClose + 1;

				if (counter == 0) {
					bracketPointer = 0;
					int indexOfNextBracket = term.indexOf(BRACKETOPEN);

					String substring = term.substring(indexOfNextBracket + 1, indexOfBracketClose);
					foundBrackets.add(BRACKETOPEN + substring + BRACKETCLOSE);

					String operator = term.substring(0, indexOfNextBracket);
					term = term.replaceFirst(Pattern.quote(operator + BRACKETOPEN + substring + BRACKETCLOSE),
							EMPTY_STRING);
				}
			}
		}

		return foundBrackets;
	}

	private static void analyseTerm(String term, int level) {
		System.out.print(level + ".) length: " + term.length());
		System.out.println(" number brackets: " + countBrackets(term));
		Set<String> extractCodesFromBuildability = extractCodesFromBuildability(term);
		System.out.print(extractCodesFromBuildability.size() + "/");
		System.out.println(getNumberOfCodes(term));
	}

	private static int countBrackets(String term) {
		String termWithoutBracket = term.substring(1, term.length() - 1);
		List<String> contentOfBrackets = getContentOfBrackets(termWithoutBracket);
		int brackets = 1;
		for (String string : contentOfBrackets) {
			brackets += countBrackets(string);
		}
		return brackets;
	}

	class Product {
		String termToReplace = "";
		String product = "";
		boolean bothSitesWithAND = false;

		public boolean isBothSitesWithAND() {
			return bothSitesWithAND;
		}

		public void setBothSitesWithAND(boolean bothSitesWithAND) {
			this.bothSitesWithAND = bothSitesWithAND;
		}

		public String getTermToReplace() {
			return termToReplace;
		}

		public void setTermToReplace(String termToReplace) {
			this.termToReplace = termToReplace;
		}

		public String getProduct() {
			return product;
		}

		public void setProduct(String product) {
			this.product = product;
		}
	}
}
