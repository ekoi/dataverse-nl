/**
 * 
 */
package nl.knaw.dans.dataverse;

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
				LOGGER.log(Level.INFO, "Search rule for " + orgAttrVal);
				Rule searchedRule = getRule(shibProps, ruleList);
				if (searchedRule == null) {
					// No rule
					LOGGER.log(Level.INFO, "No rule set for organization '"
							+ orgAttrVal + "'");
				} else {
					setRole(user, searchedRule);
				}
			}
		}

	}

	private Rule getRule(Map<String, String> shibProps, List<Rule> ruleList) {
		Rule searchedRule = null;
		for (Rule rule : ruleList) {
			Collection<RuleCondition> rcList = rule.getRuleCondition();
			boolean ruleconditionmatch = true;
			for (RuleCondition rc : rcList) {
				// Ex: rc.getAttributename() = entitlement (from the DB, column
				// attribute_name)
				// rc.getPattern() =
				// urn:x-surfnet:dans.knaw.nl:dataversenl:role:dataset-creator
				// (from the DB, column pattern)
				String attrValFromShib = shibProps.get(rc.getAttributename());
				if (!attrValFromShib.equals(rc.getPattern())) {
					ruleconditionmatch = false;
					break;
				}
			}
			if (ruleconditionmatch)
				searchedRule = rule;
		}
		return searchedRule;
	}

	private void setRole(VDCUser user, Rule searchedRule) {
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
