package com.quinemccluskey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;

public class DeMorgan {

	private static final String AND = "+";
	private static final String OR = "/";
	private static final String MINUS = "-";
	private static final String BRACKETOPEN = "(";
	private static final String BRACKETCLOSE = ")";
	private static final String EMPTY_STRING = "";

	/**
	 * This method apply DeMorgan on boolean expressions. But only for
	 * expressions, where a minus is in front of bracket
	 * 
	 * @param condition
	 * @return
	 */
	public static String applyDeMorganIfNegativeCondition(String condition) {
		StringBuilder denMorganString = new StringBuilder();

		condition = JBoolExpressionDNFCreator.simplify(condition);

		// check pattern of term to be treated
		if (outerBracketsExist(condition) && condition.startsWith(MINUS)) {
			condition = removeOnlyOuterBracket(condition);
		} else {
			return condition;
		}

		List<String> splitConditionByOR = splitConditionByOperator(condition, OR);
		for (String disjunctionTerm : splitConditionByOR) {
			List<String> splitConditionByAnd = splitConditionByOperator(disjunctionTerm, AND);

			StringBuilder denMorganConjunctions = new StringBuilder();
			for (String conjunctionTerm : splitConditionByAnd) {
				String changeSign = changeSign(conjunctionTerm);
				denMorganConjunctions.append(changeSign + OR);
			}

			// remove last operator
			if (denMorganConjunctions.lastIndexOf(OR) == denMorganConjunctions.length() - 1) {
				denMorganConjunctions.deleteCharAt(denMorganConjunctions.length() - 1);
			}

			denMorganString.append(denMorganConjunctions.toString() + AND);
		}

		// remove last operator
		if (denMorganString.length() != 0 && denMorganString.lastIndexOf(AND) == denMorganString.length() - 1) {
			denMorganString.deleteCharAt(denMorganString.length() - 1);
		}
		return denMorganString.toString();
	}

	private static String changeSign(String condition) {
		boolean positive = isPositive(condition);
		if (positive) {
			if (isSingleCode(condition)) {
				return MINUS + condition;
			} else {
				if (outerBracketsExist(condition)) {
					return MINUS + condition;
				} else {
					return MINUS + BRACKETOPEN + condition + BRACKETCLOSE;
				}
			}
		} else {
			if (isSingleCode(condition)) {
				if (condition.startsWith(MINUS + BRACKETOPEN)) {
					return removeOnlyOuterBracket(condition);
				}
				if (condition.startsWith(BRACKETOPEN + MINUS)) {
					String removeOnlyOuterBracket = removeOnlyOuterBracket(condition);
					return removeOnlyOuterBracket.substring(1, removeOnlyOuterBracket.length());
				}
			}
			return removeMinusFromCode(condition);
		}
	}

	/**
	 * This method removes only outer brackets. Also if there is a minus, the
	 * possible
	 * 
	 * @param condition
	 * @return
	 */
	private static String removeOnlyOuterBracket(String condition) {
		if (outerBracketsExist(condition)) {
			if (!isPositive(condition)) {
				condition = condition.substring(1, condition.length());
			}
			if (condition.startsWith(BRACKETOPEN) && condition.endsWith(BRACKETCLOSE)) {
				return condition.substring(1, condition.length() - 1);
			}
		}
		return condition;
	}

	private static List<String> splitConditionByOperator(String expression, String separator) {
		List<String> splitSubExpressions = new ArrayList<>();
		Splitter splitter = Splitter.on(separator);
		Iterator<String> iterator = splitter.split(expression).iterator();

		StringBuilder conditionPart = new StringBuilder();
		while (iterator.hasNext()) {
			conditionPart.append(iterator.next());
			if (StringUtils.countMatches(conditionPart.toString(), "(") == StringUtils
					.countMatches(conditionPart.toString(), ")")) {
				splitSubExpressions.add(conditionPart.toString());
				conditionPart.setLength(0);
			} else {
				conditionPart.append(separator);
			}
		}
		return splitSubExpressions;
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
		if (contentOfBrackets.get(0).length() == condition.length() - 2) {
			return true;
		}

		return false;
	}

	private static String removeMinusFromCode(String condition) {
		return condition.substring(1, condition.length());
	}

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
	 * thid method detects all brackets on same level and returns a list of them
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
					foundBrackets.add(substring);

					String operator = term.substring(0, indexOfNextBracket);
					term = term.replaceFirst(Pattern.quote(operator + BRACKETOPEN + substring + BRACKETCLOSE),
							EMPTY_STRING);
				}
			}
		}

		return foundBrackets;
	}
}
