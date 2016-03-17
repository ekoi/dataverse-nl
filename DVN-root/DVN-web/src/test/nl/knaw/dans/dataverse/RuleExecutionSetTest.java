package nl.knaw.dans.dataverse;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Test the rule system for assigning roles using SAML attributes.
 */
public class RuleExecutionSetTest {

    private final String ATTR_NAME_EMAIL = "mail";
    private final String ATTR_NAME_SURNAME = "sn";
    private final String ATTR_NAME_PREFIX = "prefix";
    private final String ATTR_NAME_GIVENNAME = "givenName";
    private final String ATTR_NAME_ROLE = "eduPersonAffiliation";
    public final String ATTR_NAME_ORG = "schacHomeOrganization";
    private final String ATTR_NAME_PRINCIPAL = "eduPersonPrincipalName";
    public final String ATTR_NAME_ENTITLEMENT = "entitlement";
    private final String CREATOR = "urn:x-surfnet:dans.knaw.nl:dataversenl:role:dataset-creator";

    private List<Rule> allRules;
    private RuleExecutionSet res;
    private TestRuleService trs;

    @Before
    public void setUp() {
        trs = new TestRuleService();
        res = new RuleExecutionSet(trs, null, null);
        allRules = trs.findAll();
    }

    /**
     * Test that we have the right pattern for regular expression matching.
     */
    @Test
    public void testBasicPatterns() throws Exception {
        assertTrue("CREATOR pattern should match CREATOR string", Pattern.matches(CREATOR,CREATOR));
        assertTrue("DANS pattern should match DANSer's email", Pattern.matches("^[^@]+@dans.knaw.nl$", "de.danser@dans.knaw.nl"));
        assertFalse("DANS pattern should not match VU email", Pattern.matches("^[^@]+@dans.knaw.nl$", "een.user@vu.nl"));
    }
    /**
     * A user from an organisation for which no rules are defined should not match any rules.
     * @throws Exception
     */
    @Test
    public void testMatchNoRules() throws Exception {
        Map<String, String> dans = createUserProps("Companjen","Ben",null,"ben.companjen@dans.knaw.nl","dans.knaw.nl",null,"staff");
        assertTrue("No rules should match for DANS", res.getMatchedRuleCondition(dans,trs.findRulesByOrgName("dans.knaw.nl")).isEmpty());
    }

    /**
     * A user with a PThU email address and entitlement should match the entitled PThU users rule.
     * @throws Exception
     */
    @Test
    public void testGetMatchingRulesSingleEmail() throws Exception {
        Map<String,String> pthu = createUserProps("User","Test","de","test.user@pthu.nl","vu.nl",CREATOR,"employee");

        List<Rule> matchingRules = res.getMatchedRuleCondition(pthu, allRules);
        boolean pthuRuleFound = false;
        for (Rule r : matchingRules) {
            if (r.getDescription().equals("entitled PThU users")) {
                pthuRuleFound = true;
            }
        }
        assertTrue("PThU email matches", pthuRuleFound);

    }

    /**
     * A user with a PThU email address without the creator entitlement should not match the PThU entitled users rule.
     * @throws Exception
     */
    @Test
    public void testGetMatchingRulesSingleEmailNoEntitlement() throws Exception {
        Map<String,String> pthu = createUserProps("User","Test","de","test.user@pthu.nl","vu.nl",null,"employee");

        List<Rule> matchingRules = res.getMatchedRuleCondition(pthu, allRules);
        boolean pthuRuleFound = false;
        boolean vuRuleFound = false;
        for (Rule r : matchingRules) {
            if (r.getDescription().equals("entitled PThU users")) {
                pthuRuleFound = true;
            } else if (r.getDescription().equals("all VU users")) {
                vuRuleFound = true;
            }
        }
        assertTrue("All VU users rule should match", vuRuleFound);
        assertFalse("PThU rule should not match", pthuRuleFound);
    }

    /**
     * A user with multiple email addresses asserted including one '@pthu.nl' should match the PThU email address rule.
     * @throws Exception
     */
    @Test
    public void testGetMatchingRulesMultiEmail() throws Exception {
        Map<String,String> pthu = createUserProps("User","Test","de","test.user@pthu.nl;test.user@vu.nl","vu.nl",CREATOR,"employee");

        List<Rule> matchingRules = res.getMatchedRuleCondition(pthu, trs.findRulesByOrgName(pthu.get(ATTR_NAME_ORG)));
        boolean pthuRuleFound = false;
        boolean vuRuleFound = false;
        for (Rule r : matchingRules) {
            if (r.getDescription().equals("entitled PThU users")) {
                pthuRuleFound = true;
            } else if (r.getDescription().equals("entitled VU users")) {
                vuRuleFound = true;
            }
        }
        assertTrue("PThU entitled users rule should match", pthuRuleFound);
        assertTrue("VU rule matches", vuRuleFound);

    }

    /**
     * A VU user without PThU email address should not match the rule that checks for a PThU email address.
     * @throws Exception
     */
    @Test
    public void testGetMatchingRulesVUEmail() throws Exception {
        Map<String,String> pthu = createUserProps("User","Test","de","test.user@vu.nl","vu.nl",CREATOR,"employee");

        List<Rule> matchingRules = res.getMatchedRuleCondition(pthu, trs.findRulesByOrgName(pthu.get(ATTR_NAME_ORG)));
        boolean pthuRuleFound = false;
        boolean vuRuleFound = false;
        for (Rule r : matchingRules) {
            if (r.getDescription().equals("entitled PThU users")) {
                pthuRuleFound = true;
            } else if (r.getDescription().equals("entitled VU users")) {
                vuRuleFound = true;
            }
        }
        assertFalse("PThU users rule should not match", pthuRuleFound);
        assertTrue("VU rule should match", vuRuleFound);

    }

    private Map<String, String> createUserProps(String lastName, String givenName, String prefix, String email,
                                                       String orgName, String entitlement, String affiliation) {
        Map<String, String> props = new HashMap<String, String>();
        props.put(ATTR_NAME_SURNAME, lastName);
        props.put(ATTR_NAME_GIVENNAME, givenName);
        props.put(ATTR_NAME_PREFIX, prefix);
        props.put(ATTR_NAME_EMAIL, email);
        props.put(ATTR_NAME_ORG, orgName);
        props.put(ATTR_NAME_ROLE, affiliation);
        props.put(ATTR_NAME_ENTITLEMENT, entitlement);

        return props;
    }

    class TestRuleService implements RuleServiceLocal {
        List<Rule> allRules;

        public TestRuleService() {
            allRules = new ArrayList<Rule>();
            Rule r1 = createRule("vu.nl", "all VU users");
            addRule(r1);
            Rule r2 = createRule("vu.nl", "entitled VU users");
            addCondition(r2, ATTR_NAME_ENTITLEMENT, CREATOR);
            addRule(r2);
            Rule r3 = createRule("vu.nl", "entitled PThU users");
            addCondition(r3, ATTR_NAME_ENTITLEMENT, CREATOR);
            addCondition(r3, ATTR_NAME_EMAIL, "^[^@]+@pthu\\.nl$");
            addRule(r3);

        }

        private void addRule(Rule rule) {
            allRules.add(rule);
        }

        private Rule createRule(String orgName, String description) {
            Rule rule = new Rule();
            rule.setOrgName(orgName);
            rule.setDescription(description);
            return rule;
        }

        private Rule addCondition(Rule rule, String attribute, String pattern) {
            RuleCondition rc = new RuleCondition();
            rc.setAttributename(attribute);
            rc.setPattern(pattern);
            Collection<RuleCondition> rcs = rule.getRuleCondition();
            if (rcs == null) {
                rcs = new ArrayList<RuleCondition>();
            }
            rcs.add(rc);
            rule.setRuleCondition(rcs);
            return rule;
        }

        @Override
        public List<Rule> findAll() {
            return allRules;
        }

        @Override
        public Rule findRuleById(Long id) {
            return allRules.get(id.intValue());
        }

        @Override
        public List<Rule> findRulesByOrgName(String orgName) {
            List<Rule> matching = new ArrayList<Rule>();
            for (Rule r : allRules) {
                if (r.getOrgName().equalsIgnoreCase(orgName)) {
                    matching.add(r);
                }
            }

            return matching;
        }

        @Override
        public Rule findRuleByCondition(RuleCondition rc) {
            return null;
        }


    }
}