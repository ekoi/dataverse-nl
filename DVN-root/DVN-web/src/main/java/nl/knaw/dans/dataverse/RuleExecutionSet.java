/**
 * 
 */
package nl.knaw.dans.dataverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.harvard.iq.dvn.core.admin.UserServiceLocal;
import edu.harvard.iq.dvn.core.admin.VDCUser;
import edu.harvard.iq.dvn.core.vdc.VDC;
import edu.harvard.iq.dvn.core.vdc.VDCServiceLocal;
import edu.harvard.iq.dvn.core.web.login.FederativeLoginPage;

/**
 * @author Eko Indarto
 *
 */
public class RuleExecutionSet {
	private final static Logger LOGGER = Logger
			.getLogger(RuleExecutionSet.class.getPackage().getName());
	RuleServiceLocal ruleService;
	VDCServiceLocal vdcService;
	UserServiceLocal userService;

	public RuleExecutionSet(RuleServiceLocal ruleService,
			VDCServiceLocal vdcService, UserServiceLocal userService) {
		this.ruleService = ruleService;
		this.vdcService = vdcService;
		this.userService = userService;
	}

	public void setUserRole(Map<String, String> shibProps, VDCUser user) {
		LOGGER.log(Level.INFO, "Rule Checks");

		String orgAttrVal = shibProps.get(FederativeLoginPage.ATTR_NAME_ORG);
		if (orgAttrVal == null || orgAttrVal.trim().equals("")) {
			LOGGER.log(Level.SEVERE, "No organization found.");
		} else {
			// Example from VU:
			// shibProps ("schacHomeOrganization","vu.nl"),
			// ("entitlement","urn:x-surfnet:dans.knaw.nl:dataversenl:role:dataset-creator")
			// the "vu.nl" as Rule name and "entitlement" is the RuleCondition
			List<Rule> ruleList = ruleService.findRuleByOrgName(orgAttrVal);
			if (ruleList == null || ruleList.isEmpty()) {
				LOGGER.log(Level.INFO, "No rule is implemented for "
						+ orgAttrVal);
			} else {
				LOGGER.log(Level.INFO, "Search rule conditon for " + orgAttrVal);
				Rule searchedRule = getMatchedRuleCondition(shibProps, ruleList);
				if (searchedRule != null)
					setRoleBasedOnRuleGoals(user, searchedRule);
				else
					LOGGER.log(Level.INFO, "No rule condition is mached.");
			}
		}

	}
	/*
	 Suppose, the RULE table has the following data:
	 id		organization description
	 1    	vu.nl         
	 2    	vu.nl
	 3    	dans
	 4      vu.nl
	 5      vu.nl
	 
	 The RULE_CONDITION table has the following records:
	 id 	ATTRIBUTE		PATTERN		RULE_ID
	 1		entitlement		abc			1
	 2      affiliation 	employee	1
	 3      entitlement     abc         2
	 4      entitlement		zzz	        5
	 
	 Suppose, the "schacHomeOrganization" properties contains vu.nl 
	 so from the "setUserRole" method, we have a rulelist that contains 2 element namely 
	 {(1,vu.nl) has rule condition (1, entitlement,abc) and (2, affilition,employee), 
	 (2,vu.nl) has rule condition (3, entitlement,abc), 
	 (4,vu.nl) has no rule condition,
	 (5,vu.nl) has rule condition (4, entitlement,zzz)}
	 
	 Case 1. The request contains rule condition where entitlement attribute with value 'abc' and affiliation with value 'xxx'
	 
	 Case 2. The request contains rule condition where entitlement with value 'abc'
	 
	 Case 3. The request contains no rule condition
	 
	 Case 4. The request contains rule condition where only entitlement with value 'zzz"
	 
	 */

	private Rule getMatchedRuleCondition(Map<String, String> shibProps, List<Rule> ruleList) {
		Rule searchedRule = null;
		for (Rule rule : ruleList) {
			Collection<RuleCondition> rcList = rule.getRuleCondition();
			if (rcList == null || rcList.isEmpty()) {
				//This is the case where a rule has no addition rule condition (Case 3)
				return rule; //leave this method.
			} else {
				boolean matchedcondition = false;
				for (RuleCondition rc : rcList) {
					// Ex: rc.getAttributename() = entitlement (from the DB, column
					// attribute_name)
					// rc.getPattern() =
					// urn:x-surfnet:dans.knaw.nl:dataversenl:role:dataset-creator
					// (from the DB, column pattern)
					String attrValFromShib = shibProps.get(rc.getAttributename());
					if ((attrValFromShib != null && !attrValFromShib.trim().equals("")) && attrValFromShib.equals(rc.getPattern())) {
						matchedcondition = true;
						break;
					}
				} 	
					
				if (matchedcondition)
					searchedRule = rule;
			}
		}
		return searchedRule;
	}

	private void setRoleBasedOnRuleGoals(VDCUser user, Rule searchedRule) {
		Collection<RuleGoal> rgList = searchedRule.getRuleGoal();
		for (RuleGoal rg : rgList) {
			String dvnAlias = rg.getDvnAlias();
			VDC vdc = vdcService.findByAlias(dvnAlias);
			userService.addVdcRole(user.getId(), vdc.getId(), rg.getRole()
					.getName());
			LOGGER.log(Level.INFO, "'" + rg.getRole().getName()
					+ "' role is assigned to user '" + user.getUserName()
					+ "' for dvn alias '" + dvnAlias + "'.");
		}
	}
}
