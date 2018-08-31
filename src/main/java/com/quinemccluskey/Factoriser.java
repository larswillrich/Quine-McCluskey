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

	public static String find(String condition) {
		condition = condition.replace(BLANK, EMPTY_STRING);

		List<String> contentOfBrackets = getContentOfBrackets(condition);
		while (!contentOfBrackets.isEmpty()) {
			String firstBracket = contentOfBrackets.get(0);

			// remove brackets
			String contentOfFirstBracketWithoutBracket = firstBracket.substring(1, firstBracket.length() - 1);
			String termWithoutBracket = find(contentOfFirstBracketWithoutBracket);

			Product product = multiply(condition, firstBracket, termWithoutBracket);

			// TODO: replace also correct factor
			condition = condition.replaceFirst(Pattern.quote(product.getTermToReplace()), product.getProduct());

			contentOfBrackets = getContentOfBrackets(condition);
		}

		return condition;
	}

	private static Product multiply(String term, String termToReplace, String termWithoutBracket) {

		Product p = new Factoriser().new Product();

		String factorA = getFactorToMultiply(term, termToReplace, p);
		String[] factorBs = termWithoutBracket.split(OR);
		String product = "";
		for (String factorB : factorBs) {
			product += OR + factorA + AND + factorB;
		}
		product = product.substring(1, product.length());
		term = term.replaceFirst(Pattern.quote(termToReplace), product);
		p.setProduct(product);
		return p;
	}

	private static String getFactorToMultiply(String term, String termToReplace, Product p) {
		int indexOf = term.indexOf(termToReplace);
		int indexOfConcerningOperator = getIndexOfConcerningOperator(term, termToReplace);

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
				}
				charAt = term.charAt(indexOfVariableEnd);
			}

			p.setEndOfNewProduct(indexOfVariableEnd + 1);
			p.setStartOfNewProduct(indexOf);
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
						continue;
					}
				}

				indexOfVariableStart--;
				if (indexOfVariableStart == 0) {
					if (bracketLevel == 0) {
						stillFactor = false;
						continue;
					}
				}
				charAt = term.charAt(indexOfVariableStart);
			}

			p.setEndOfNewProduct(indexOf);
			p.setStartOfNewProduct(indexOfVariableStart);
			p.setTermToReplace(term.substring(indexOfVariableStart, indexOfConcerningOperator + 1) + termToReplace);

			factor = term.substring(indexOfVariableStart, indexOfConcerningOperator - 1);
		}

		return factor;
	}

	/**
	 * Returns prioritized operator. If both operator has same priority, return
	 * first one.
	 * 
	 * @param term
	 * @param termToReplace
	 * @return
	 */
	private static int getIndexOfConcerningOperator(String term, String termToReplace) {
		int indexOf = term.indexOf(termToReplace);

		// look at operator after closing bracket
		if (term.indexOf(termToReplace) == indexOf) {
			return indexOf + termToReplace.length();
		} else {
			int indexOfBracketBeforeBracket = indexOf;
			int indexOfBracketAfterBracket = indexOf + termToReplace.length();

			// look at character before bracket
			if (indexOfBracketAfterBracket == term.length()) {
				return indexOfBracketAfterBracket;
			}

			if (term.substring(indexOfBracketBeforeBracket, indexOfBracketBeforeBracket + 1).equals(OR)) {
				if (term.substring(indexOfBracketAfterBracket, indexOfBracketAfterBracket + 1).equals(OR)) {
					return -1;
				}
				return indexOfBracketAfterBracket;
			} else {
				return indexOfBracketBeforeBracket;
			}
		}
	}

	/**
	 * This method creates all possible boolean assignments in form of
	 * CodeConfigurations for a conjunctive term like A+B+C+(D/E/F+G)+H. In this
	 * case 5 collections will be overhanded for A,B,C, (D/E/F+G), H. Every part
	 * of condition (A,B,C...) is represented as a List of CodeConfiguratios and
	 * contains all possible configurations for this conjunctive part.
	 * 
	 * @param conjunctionTermsConfigurations
	 * @param codeTypesOnlyAllowedOnce
	 * @param codeToExclusionClassMapper
	 * @return
	 */
	private static Collection<Map<String, Boolean>> createConfigurationsForConjunction(
			Collection<Collection<Map<String, Boolean>>> conjunctionTermsConfigurations) {

		Collection<Map<String, Boolean>> results = new ArrayList<Map<String, Boolean>>();

		if (conjunctionTermsConfigurations.isEmpty()) {
			return results;
		}
		if (conjunctionTermsConfigurations.size() == 1) {
			return conjunctionTermsConfigurations.iterator().next();
		}

		Collection<Map<String, Boolean>> firstElement = conjunctionTermsConfigurations.iterator().next();
		conjunctionTermsConfigurations.remove(firstElement);
		Collection<Map<String, Boolean>> permuateConfigurations = createConfigurationsForConjunction(
				conjunctionTermsConfigurations);

		for (Map<String, Boolean> config : firstElement) {
			for (Map<String, Boolean> configsFromOther : permuateConfigurations) {
				Map<String, Boolean> newConfig = new HashMap<String, Boolean>();

				newConfig.putAll(configsFromOther);
				newConfig.putAll(config);
				results.add(newConfig);
			}
		}

		return results;
	}

	/**
	 * This method creates all possible boolean assignments in form of
	 * CodeConfigurations for a discjuntive term like A/B/C/(D+E/F+G)/H. In this
	 * case 5 collections will be overhanded for A,B,C, (D+E/F+G), H. Every part
	 * of condition (A,B,C...) is represented as a List of CodeConfiguratios and
	 * contains all possible configurations for this disjunctive part.
	 * 
	 * @param disjunctionTermsConfigurations
	 * @param codeTypesOnlyAllowedOnce
	 * @param codeToAregMasterDataMap
	 * @param codeToExclusionClassMapper
	 * @return
	 */
	private static Collection<Map<String, Boolean>> createConfigurationsForDisjunction(
			Map<String, Collection<Map<String, Boolean>>> disjunctionTermsConfigurations) {

		if (disjunctionTermsConfigurations.size() == 1) {
			return disjunctionTermsConfigurations.values().iterator().next();
		}
		Set<Map<String, Boolean>> result = new HashSet<Map<String, Boolean>>();

		Set<String> disjunctionTerms = disjunctionTermsConfigurations.keySet();

		for (String disjunctionTerm : disjunctionTerms) {

			// get current disjunctive part and remove only this from all
			// available codes. Then create all possible assignments (true,
			// false) for all lever over codes, because only one positive
			// Codeconfiguration has to be exist to make the whole expression to
			// true
			Collection<Map<String, Boolean>> positiveConfigurations = disjunctionTermsConfigurations
					.get(disjunctionTerm);
			Set<String> allOtherCodes = removeAllCodes(disjunctionTerms, disjunctionTerm);

			if (allOtherCodes.isEmpty()) {
				continue;
			}

			Collection<Map<String, Boolean>> createAllPossibleAssigmnents = createAllPossibleAssigmnents(allOtherCodes);

			for (Map<String, Boolean> positiveConfig : positiveConfigurations) {
				for (Map<String, Boolean> configOfOther : createAllPossibleAssigmnents) {
					Map<String, Boolean> newConfiguration = new HashMap<>(positiveConfig);
					newConfiguration.putAll(configOfOther);
					result.add(newConfiguration);
				}
			}
		}
		return result;
	}

	/**
	 * This method creates all possible boolean assignments (true and false) for
	 * given List of AregMasterData. Also a fix CodeConfiguration can be
	 * overhanded by an argument and will be a part of every created
	 * CodeConfiguration.
	 * 
	 * @param fixConfig
	 *            CodeConfiguration which should be part of every created
	 *            CodeConfiguration
	 * @param allCodes
	 *            for this code-list all possible configuration will be created
	 * @param codeTypesOnlyAllowedOnce
	 *            is a list defining codeTypes for given business functionality.
	 *            Is set in method find() of this class.
	 * @param codeToExclusionClassMapper
	 * @return
	 */
	private static Collection<Map<String, Boolean>> createAllPossibleAssigmnents(Collection<String> allCodes) {
		Collection<Map<String, Boolean>> result = new ArrayList<Map<String, Boolean>>();
		Map<String, Boolean> configFalse = null;
		Map<String, Boolean> configTrue = null;

		if (allCodes.size() == 1) {
			String next = allCodes.iterator().next();

			configFalse = new HashMap<>();
			configFalse.put(next, false);

			configTrue = new HashMap<>();
			configTrue.put(next, true);

			result.add(configFalse);
			result.add(configTrue);
			return result;
		}

		String next = allCodes.iterator().next();
		allCodes.remove(next);
		Collection<Map<String, Boolean>> createAllPossibleAssigmnents = createAllPossibleAssigmnents(allCodes);

		for (Map<String, Boolean> config : createAllPossibleAssigmnents) {

			configFalse = new HashMap<String, Boolean>();
			configFalse.put(next, false);
			configFalse.putAll(config);
			result.add(configFalse);

			configTrue = new HashMap<String, Boolean>();
			configTrue.put(next, true);
			configTrue.putAll(config);
			result.add(configTrue);
		}

		return result;
	}

	/**
	 * This method removes all codes from given list of conditions
	 * 
	 * @param conditions
	 * @param codesToRemove
	 * @return List of all left over codes
	 */
	private static Set<String> removeAllCodes(Set<String> conditions, String codesToRemove) {
		Set<String> allCodes = new HashSet<String>();
		for (String condition : conditions) {
			allCodes.addAll(extractCodesFromBuildability(condition));
		}

		Set<String> allCodesOfDisjunctionTerm = extractCodesFromBuildability(codesToRemove);
		allCodes.removeAll(allCodesOfDisjunctionTerm);
		return allCodes;
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

	class Product {
		String termToReplace = "";
		String product = "";
		int startOfNewProduct = -1;
		int endOfNewProduct = -1;

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

		public int getStartOfNewProduct() {
			return startOfNewProduct;
		}

		public void setStartOfNewProduct(int startOfNewProduct) {
			this.startOfNewProduct = startOfNewProduct;
		}

		public int getEndOfNewProduct() {
			return endOfNewProduct;
		}

		public void setEndOfNewProduct(int endOfNewProduct) {
			this.endOfNewProduct = endOfNewProduct;
		}

	}
}
