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

public class CanonicalDNFFinder {

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

	/**
	 * This method finds all possible boolean assignments for a given condition
	 * set this condition to true recursively and works without any database
	 * connections.
	 * 
	 * @param condition
	 * @param referenceCode
	 * @param codeTypesOnlyAllowedOnce
	 *            a list with all codetypes of available codes in given
	 *            condition.
	 * @param codeToExclusionClassMapper
	 * @return
	 */
	public static Collection<Map<String, Boolean>> find(String condition) {
		condition = condition.replace(BLANK, EMPTY_STRING);
		Collection<Map<String, Boolean>> configurations = new ArrayList<>();

		// recursive termination
		if (isSingleCode(condition)) {
			Boolean positive = isPositive(condition);

			// remove possible brackets and minus from single code to get only
			// the code
			condition = condition.replace(BRACKETOPEN, EMPTY_STRING);
			condition = condition.replace(BRACKETCLOSE, EMPTY_STRING);
			condition = condition.replace(MINUS, EMPTY_STRING);

			// create Codeconfiguration, but only positive one. Cannot be null,
			// because it will be only one code and this is not fullfittin
			// business criteria for not allowed code

			Map<String, Boolean> createNewConfiguration = new HashMap<>();
			createNewConfiguration.put(condition, positive);

			configurations.add(createNewConfiguration);
			return configurations;
		}

		// apply deMorgan if a minus is in front of bracket, otherwise remove
		// not nesessery bracket
		// Why DeMorgan? for a recursively approach and analysing of codes, the
		// CodeConfigurations will be created from buttom-up. Buttom-up means,
		// the most inner nested bracket will be analysed first. If there are
		// minus in front of brackets in, all CodeConfigurations would change in
		// logic. By this approach, Codeconfiguration will be created one time
		// and correct.
		if (outerBracketsExist(condition) && condition.startsWith(MINUS)) {
			condition = DeMorgan.applyDeMorganIfNegativeCondition(condition);
		} else {
			condition = removeOnlyOuterBracket(condition);
		}

		// split condition at OR operator but only on first bracket level. So
		// ignore brackets
		// e.g. A+B+(C/D+E+(F+G/H)+I/J)/K+L => 2 parts =>
		// A+B+(C/D+E+(F+G/H)+I/J), K+L
		Collection<String> disjunctionsTerms = splitConditionByOperator(condition, OR);

		Map<String, Collection<Map<String, Boolean>>> allDisjunctivConditions = new HashMap<String, Collection<Map<String, Boolean>>>();
		for (String disjunctionTerm : disjunctionsTerms) {

			// split condition at AND operator but only on first bracket level.
			// So ignore brackets
			// e.g. A+B+(C/D+E+(F+G/H)+I/J) => 3 parts => A, B,
			// (C/D+E+(F+G/H)+I/J)
			List<String> conjunctionTerms = splitConditionByOperator(disjunctionTerm, AND);
			ArrayList<Collection<Map<String, Boolean>>> conjunctionTermsConfigurations = new ArrayList<Collection<Map<String, Boolean>>>();

			// iterate over all conjunction Terms and add all possible
			// Configurations leads to true to configuration list
			for (String conjunctionTerm : conjunctionTerms) {

				// For every part of conjunctionTerm, call find recursively
				Collection<Map<String, Boolean>> foundConfiguration = find(conjunctionTerm);
				conjunctionTermsConfigurations.add(foundConfiguration);
			}

			// create all possible CodeConfiguration for conjunction term like
			// A, B, (C/D+E+(F+G/H)+I/J)
			Collection<Map<String, Boolean>> allConfigurationForConjunctionTerm = createConfigurationsForConjunction(
					conjunctionTermsConfigurations);

			Collection<Map<String, Boolean>> listWithConfigurationsForConjunctionTerm = allConfigurationForConjunctionTerm;
			allDisjunctivConditions.put(disjunctionTerm, listWithConfigurationsForConjunctionTerm);
		}

		// create all possible CodeConfiguration for disjunction term like
		// A, B, C/D+E/I/J
		Collection<Map<String, Boolean>> allConfigurationForDisjunctionTerm = createConfigurationsForDisjunction(
				allDisjunctivConditions);

		return allConfigurationForDisjunctionTerm;
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
