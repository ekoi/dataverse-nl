/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dvn.core.web.login;

import edu.harvard.iq.dvn.core.admin.GroupServiceLocal;
import edu.harvard.iq.dvn.core.admin.UserServiceLocal;
import edu.harvard.iq.dvn.core.admin.VDCUser;
import edu.harvard.iq.dvn.core.vdc.VDCNetworkServiceLocal;
import edu.harvard.iq.dvn.core.web.common.VDCBaseBean;
import edu.harvard.iq.dvn.core.web.util.AccessExpressionParser;
import javax.ejb.EJB;
import com.icesoft.faces.component.ext.HtmlInputHidden;
import dk.itst.oiosaml.sp.UserAssertion;
import dk.itst.oiosaml.sp.UserAttribute;
import javax.faces.context.FacesContext;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.inject.Named;

/**
 *
 * @author mpieters
 */
@ViewScoped
@Named("SamlLoginPage")
public class SamlLoginPage extends VDCBaseBean implements java.io.Serializable {


    private final static Logger LOGGER = Logger.getLogger(SamlLoginPage.class.getPackage().getName());
    @EJB
    UserServiceLocal userService;
    @EJB
    GroupServiceLocal groupService;
    @EJB
    VDCNetworkServiceLocal vdcNetworkService;
    // ---
    String refererUrl = "";
    private String errMessage = "";
    String userId = "";
    HttpServletRequest request;
    HttpServletResponse response;
    private boolean loginFailed;
    private String redirect;
    private Long studyId;
    protected String tab;
    private HtmlInputHidden hiddenStudyId;
    private Boolean clearWorkflow = true;
    private final String assertName = "dk.itst.oiosaml.userassertion";
    private String ATTR_NAME_EMAIL = "mail";
    private String ATTR_NAME_SURNAME = "sn";
    private String ATTR_NAME_PREFIX = "prefix";
    private String ATTR_NAME_GIVENNAME = "givenName";
    private String ATTR_NAME_ROLE = "eduPersonAffiliation";
    private String ATTR_NAME_ORG = "schacHomeOrganization";
    private String ATTR_NAME_PRINCIPAL = "eduPersonPrincipalName";
    private String ACL_ADMIN = null;
    private String ACL_CREATOR = null;
    private String ACL_USER = null;
    private Boolean ALLOW_ADMIN = false;
    private Boolean USE_REFERER = false;
    private String USERID_METHOD = "attr";
    private String USERID_ATTR = "email";
    private String USERID_PREFIX = "";
    private Properties configuration = new Properties();
    private HashMap userdata = new HashMap();

    /**
     * <p>Construct a new Page bean instance.</p>
     */
    public SamlLoginPage() {
        super();
        LOGGER.log(Level.FINE, "Instantiating SamlLoginServlet");
        readPropertiesFile();
    }

    /**
     * <p>Callback method that is called whenever a page is navigated to,
     * either directly via a URL, or indirectly via page navigation.
     * Customize this method to acquire resources that will be needed
     * for event handlers and lifecycle methods, whether or not this
     * page is performing post back processing.</p>
     *
     * <p>Note that, if the current request is a postback, the property
     * values of the components do <strong>not</strong> represent any
     * values submitted with this request.  Instead, they represent the
     * property values that were saved for this view when it was rendered.</p>
     */
    @Override
    public void init() {
        super.init();
        if (clearWorkflow != null) {
            LoginWorkflowBean lwf = (LoginWorkflowBean) getBean("LoginWorkflowBean");
            lwf.clearWorkflowState();
        }
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        HttpSession session = (HttpSession) context.getSession(true);
        request = (HttpServletRequest) context.getRequest();
        response = (HttpServletResponse) context.getResponse();
        String protocol = resolveProtocol(request.getServerPort());
        String defaultPage = "";
        String serverPort = (request.getServerPort() != 80) ? ":" + request.getServerPort() : "";
        if (getVDCRequestBean().getCurrentVDC() != null) {
            defaultPage = protocol + "://" + request.getServerName() + serverPort + request.getContextPath() + "/dv/" + getVDCRequestBean().getCurrentVDC().getAlias();
        } else {
            defaultPage = protocol + "://" + request.getServerName() + serverPort + request.getContextPath();
        }
        if (USE_REFERER && request.getHeader("referer") != null && !request.getHeader("referer").equals("")) {
            if (request.getHeader("referer").indexOf("/login/") != -1 || request.getHeader("referer").contains("/admin/") || request.getHeader("referer").contains("/networkAdmin/")) {
                refererUrl = defaultPage;
            } else {
                refererUrl = request.getHeader("referer");
            }
        } else {
            refererUrl = defaultPage;
        }

        // see if SAML login has completed already
        UserAssertion userAssert = (UserAssertion) session.getAttribute(assertName);
        if (userAssert == null) {
            errMessage = "No assertion; this stage should never be reached; check the SAML configuration";
            loginFailed = true;
            LOGGER.log(Level.SEVERE, errMessage);
        } else {
            // SAML login complete
            LOGGER.log(Level.FINE, "Reading email attribute as \"{0}\"", ATTR_NAME_EMAIL);
            final UserAttribute attr_email = userAssert.getAttribute(ATTR_NAME_EMAIL);
            dumpUserAssertAttributes(userAssert);
            final Iterator email_list = attr_email.getValues().iterator();
            VDCUser user = null;
            try {
                while (email_list.hasNext() && user == null) {
                    String email = (String) email_list.next();
                    user = userService.findByEmail(email);
                }
                if (user != null) {
                    if (user.isActive()) {
                        LOGGER.log(Level.INFO, "User {0} is active!", user.getUserName());
                        if (!user.isNetworkAdmin() || ALLOW_ADMIN) {
                            final String forward = dvnLogin(user, studyId);
                            LOGGER.log(Level.INFO, "User forwarded to {0}", forward);
                            redirect = forward;
                            if (forward != null && forward.startsWith("/HomePage")) {
                                try {
                                    response.sendRedirect(refererUrl);
                                    //response.sendRedirect(refererUrl + redirect);
                                } catch (IOException ex) {
                                    errMessage = ex.toString();
                                    LOGGER.log(Level.SEVERE, null, ex);
                                }
                            }
                        } else {
                            loginFailed = true;
                            errMessage = "Admin access is not allowed using federated login";
                        }
                    } else {
                        loginFailed = true;
                        errMessage = "Account is not active";
                        LOGGER.log(Level.INFO, "User {0} not active!", user.getUserName());
                    }
                } else {
                    final UserAttribute usrgivenname = userAssert.getAttribute(ATTR_NAME_GIVENNAME);
                    final UserAttribute usrprefix = userAssert.getAttribute(ATTR_NAME_PREFIX);
                    final UserAttribute usrsurname = userAssert.getAttribute(ATTR_NAME_SURNAME);
                    final UserAttribute usremail = userAssert.getAttribute(ATTR_NAME_EMAIL);
                    final UserAttribute usrprincipal = userAssert.getAttribute(ATTR_NAME_PRINCIPAL);
                    final UserAttribute usrrole = userAssert.getAttribute(ATTR_NAME_ROLE);
                    final UserAttribute usrorg = userAssert.getAttribute(ATTR_NAME_ORG);

                    if (usrgivenname != null) {
                        LOGGER.log(Level.FINE, "Given Name: {0}", usrgivenname.getValue());
                        userdata.put("givenname", usrgivenname.getValue());
                    }
                    if (usrprefix != null) {
                        LOGGER.log(Level.FINE, "Prefix: {0}", usrprefix.getValue());
                        userdata.put("prefix", usrprefix.getValue());
                    }
                    if (usrsurname != null) {
                        LOGGER.log(Level.FINE, "Surname: {0}", usrsurname.getValue());
                        userdata.put("surname", usrsurname.getValue());
                    }
                    if (usremail != null) {
                        LOGGER.log(Level.FINE, "Email: {0}", usremail.getValues().toString());
                        userdata.put("email", usremail.getValues());
                    }
                    if (usrprincipal != null) {
                        LOGGER.log(Level.FINE, "Principal Name: {0}", usrprincipal.getValue());
                        userdata.put("principal", usrprincipal.getValue());
                    }
                    if (usrrole != null) {
                        LOGGER.log(Level.FINE, "Role: {0}", usrrole.getValues().toString());
                        userdata.put("role", usrrole.getValues());
                    }
                    if (usrorg != null) {
                        LOGGER.log(Level.FINE, "Organization: {0}", usrorg.getValue());
                        userdata.put("organization", usrorg.getValue());
                    }
                    final String usertype = getUserType(userdata);
                    LOGGER.log(Level.FINE, "User type: {0}", usertype);
                    final String tempusername = uniqueUserId(userdata);

                    if (!ALLOW_ADMIN && "admin".equals(usertype)) {
                        loginFailed = true;
                        errMessage = "Admin access is not allowed using federated login";
                        String name = ((usrprincipal != null) ? usrprincipal.getValue() : ((usremail != null) ? usremail.getValue() : "unknown"));
                        LOGGER.log(Level.SEVERE, "Admin login not allowed for {0}!", name);
                    } else if (usertype != null && tempusername != null) {
                        session.setAttribute("usrusertype", usertype);
                        session.setAttribute("usrusername", tempusername);
                        session.setAttribute("ALLOW_ADMIN", ALLOW_ADMIN);
                        if (usrgivenname != null) {
                            session.setAttribute("usrgivenname", usrgivenname.getValue());
                        }
                        if (usrprefix != null) {
                            session.setAttribute("usrprefix", usrprefix.getValue());
                        }
                        if (usrsurname != null) {
                            session.setAttribute("usrsurname", usrsurname.getValue());
                        }
                        if (usremail != null) {
                            session.setAttribute("usremail", usremail.getValue());
                        }
                        if (usrprincipal != null) {
                            session.setAttribute("usrprincipal", usrprincipal.getValue());
                        }
                        if (usrrole != null) {
                            session.setAttribute("usrrole", usrrole.getValue());
                        }
                        if (usrorg != null) {
                            session.setAttribute("usrorg", usrorg.getValue());
                        }
                        redirectToSamlAddAccount();
                    } else {
                        loginFailed = true;
                        if (tempusername == null) {
                            errMessage = "Unable to determine a unique user id";
                        } else {
                            errMessage = "You are not allowed to log in using your federation account";
                        }
                        String name = ((usrprincipal != null) ? usrprincipal.getValue() : ((usremail != null) ? usremail.getValue() : "unknown"));
                        LOGGER.log(Level.INFO, "Login not allowed for {0}!", name);
                    }
                }
            } catch (Exception e) {
                errMessage = e.toString();
                LOGGER.log(Level.SEVERE, null, e);
            }
        }
    }

    private String resolveProtocol(int portNumber) {
        //String protocol = request.getProtocol().substring(0, request.getProtocol().indexOf("/")).toLowerCase();
        //Something went wrong in setting the protocol according to the request. Hard coded based on the port number
        
        switch (portNumber) {
            case 443:
                return "https";
            case 80:
            default:
                return "http";
        }
        
    }

    private String dvnLogin(VDCUser user, Long studyId) {
        LOGGER.log(Level.FINE, "dvnLogin for user {0}", user.getUserName());
        LoginWorkflowBean lwf = (LoginWorkflowBean) getBean("LoginWorkflowBean");
        LOGGER.log(Level.FINE, "Workflow available; processing login");
        return lwf.processLogin(user, studyId);
    }

    public String samlLogin() {
        // Unused as of now
        return null;
    }

    /**
     * Getter for property loginFailed.
     * @return Value of property loginFailed.
     */
    public boolean isLoginFailed() {
        return this.loginFailed;
    }

    /**
     * Setter for property loginFailed.
     * @param loginFailed New value of property loginFailed.
     */
    public void setLoginFailed(boolean loginFailed) {
        this.loginFailed = loginFailed;
    }

    /**
     * Getter for property errMessage.
     * @return Value of property errMessage.
     */
    public String getErrMessage() {
        return errMessage;
    }

    /**
     * Setter for property errMessage.
     * @param errMessage New value of property errMessage.
     */
    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    /**
     * Getter for property redirect.
     * @return Value of property redirect.
     */
    public String getRedirect() {
        return this.redirect;
    }

    /**
     * Setter for property redirect.
     * @param redirect New value of property redirect.
     */
    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    /**
     * Getter for property studyId.
     * @return Value of property studyId.
     */
    public Long getStudyId() {
        return this.studyId;
    }

    /**
     * Setter for property studyId.
     * @param studyId New value of property studyId.
     */
    public void setStudyId(Long studyId) {
        this.studyId = studyId;
    }

    /**
     * Get the value of tab
     *
     * @return the value of tab
     */
    public String getTab() {
        return tab;
    }

    /**
     * Set the value of tab
     *
     * @param tab new value of tab
     */
    public void setTab(String tab) {
        this.tab = tab;
    }

    /**
     * Getter for property hiddenStudyId.
     * @return Value of property hiddenStudyId.
     */
    public HtmlInputHidden getHiddenStudyId() {
        return this.hiddenStudyId;
    }

    /**
     * Setter for property hiddenStudyId.
     * @param hiddenStudyId New value of property hiddenStudyId.
     */
    public void setHiddenStudyId(HtmlInputHidden hiddenStudyId) {
        this.hiddenStudyId = hiddenStudyId;
    }

    public Boolean isClearWorkflow() {
        return clearWorkflow;
    }

    public void setClearWorkflow(Boolean clearWorkflow) {
        this.clearWorkflow = clearWorkflow;
    }

    private void readPropertiesFile() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        HttpSession session = (HttpSession) context.getSession(true);
        final String pFileName = session.getServletContext().getRealPath("/WEB-INF") + "/SamlLogin.properties";

        try {
            LOGGER.log(Level.INFO, "Read configuration from properties file");
            configuration.load(new FileInputStream(pFileName));
            /* User attribute names */
            if (configuration.containsKey("saml.attributes.email")) {
                ATTR_NAME_EMAIL = configuration.getProperty("saml.attributes.email");
                LOGGER.log(Level.FINE, "Email attribute: {0}", ATTR_NAME_EMAIL);
            }
            if (configuration.containsKey("saml.attributes.givenname")) {
                ATTR_NAME_GIVENNAME = configuration.getProperty("saml.attributes.givenname");
                LOGGER.log(Level.FINE, "Given name attribute: {0}", ATTR_NAME_GIVENNAME);
            }
            if (configuration.containsKey("saml.attributes.givenname")) {
                ATTR_NAME_SURNAME = configuration.getProperty("saml.attributes.surname");
                LOGGER.log(Level.FINE, "Surname attribute: {0}", ATTR_NAME_SURNAME);
            }
            if (configuration.containsKey("saml.attributes.prefix")) {
                ATTR_NAME_PREFIX = configuration.getProperty("saml.attributes.prefix");
                LOGGER.log(Level.FINE, "Surname prefix attribute: {0}", ATTR_NAME_PREFIX);
            }
            if (configuration.containsKey("saml.attributes.organization")) {
                ATTR_NAME_ORG = configuration.getProperty("saml.attributes.organization");
                LOGGER.log(Level.FINE, "Organization attribute: {0}", ATTR_NAME_ORG);
            }
            if (configuration.containsKey("saml.attributes.role")) {
                ATTR_NAME_ROLE = configuration.getProperty("saml.attributes.role");
                LOGGER.log(Level.FINE, "Role attribute: {0}", ATTR_NAME_ROLE);
            }
            /* Role access */
            if (configuration.containsKey("saml.access.admin")) {
                ACL_ADMIN = configuration.getProperty("saml.access.admin");
            }
            if (configuration.containsKey("saml.access.creator")) {
                ACL_CREATOR = configuration.getProperty("saml.access.creator");
            }
            if (configuration.containsKey("saml.access.user")) {
                ACL_USER = configuration.getProperty("saml.access.user");
            }
            /* User name generation */
            if (configuration.containsKey("saml.username.method")) {
                USERID_METHOD = configuration.getProperty("saml.username.method");
            }
            if (configuration.containsKey("saml.username.attribute")) {
                USERID_ATTR = configuration.getProperty("saml.username.attribute");
            }
            if (configuration.containsKey("saml.username.prefix")) {
                USERID_PREFIX = configuration.getProperty("saml.username.prefix");
            }
            if (configuration.containsKey("saml.redirect.referer")) {
                USE_REFERER = "yes".equalsIgnoreCase(configuration.getProperty("saml.redirect.referer"));
            }
            /* Admin access */
            if (configuration.containsKey("saml.access.allow.admin")) {
                ALLOW_ADMIN = "yes".equalsIgnoreCase(configuration.getProperty("saml.access.allow.admin"));
            }
        } catch (IOException ex) {
            Logger.getLogger(SamlLoginPage.class.getName()).log(Level.SEVERE, "Could not open properties file " + pFileName, ex);
        }
    }

    private Boolean evaluateAccess(final String acl, final HashMap userdata) {
        Boolean access = false;
        try {
            AccessExpressionParser aep = new AccessExpressionParser(acl);
            access = aep.evaluate(userdata);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error evaluating access", e);
        }
        return access;
    }

    private String getUserType(HashMap userdata) {
        if (ACL_ADMIN != null && evaluateAccess(ACL_ADMIN, userdata)) {
            LOGGER.log(Level.FINE, "User type Admin");
            return "admin";
        }
        if (ACL_CREATOR != null && evaluateAccess(ACL_CREATOR, userdata)) {
            LOGGER.log(Level.FINE, "User type Creator");
            return "creator";
        }
        if (ACL_USER != null && evaluateAccess(ACL_USER, userdata)) {
            LOGGER.log(Level.FINE, "User type User");
            return "user";
        }
        return null;
    }
    
    public String getUUID() {
        return UUID.randomUUID().toString().toLowerCase();
    }

    public String stripNonAlphaNum(String in) {
        String out = "";
        if (in == null) {
            return "";
        }
        int l = in.length();
        for (int i = 0; i < l; i++) {
            char c = in.charAt(i);
            if ((c >= 'A' & c <= 'Z')
                    || (c >= 'a' & c <= 'z')
                    || (c >= '0' & c <= '9')) {
                out += c;
            }
        }
        return out;
    }

    private String uniqueUserId(HashMap data) {
        if ("uuid".equalsIgnoreCase(USERID_METHOD)) {
            LOGGER.log(Level.FINE, "User Id generation method: UUID");
            String uuid = stripNonAlphaNum(getUUID());
            LOGGER.log(Level.INFO, "Generated user id {0}.", uuid);
            return uuid;
        }
        if ("attr".equalsIgnoreCase(USERID_METHOD)) {
            LOGGER.log(Level.FINE, "User Id generation method: attribute {0}", USERID_ATTR);
            String base = null;
            if ("surname".equalsIgnoreCase(USERID_ATTR)) {
                base = (String) data.get("surname");
            } else if ("givenname".equalsIgnoreCase(USERID_ATTR)) {
                base = (String) data.get("givenname");
            } else if ("principal".equalsIgnoreCase(USERID_ATTR)) {
                base = (String) data.get("principal");
            } else { // email
                final List emaillist = (List) data.get("email");
                if (emaillist != null && emaillist.size() > 0) {
                    base = (String) emaillist.get(0); // first item in list
                }
            }
            if (base != null) {
                if (USERID_PREFIX != null) {
                    base = USERID_PREFIX + base;
                }
                String tuid = stripNonAlphaNum(base);
                String xuid = tuid;
                LOGGER.log(Level.FINE, "Base user id {0}.", tuid);
                int n = 0;
                Boolean unique = false;
                while (!unique) {
                    VDCUser user = userService.findByUserName(xuid);
                    unique = (user == null);
                    if (!unique) {
                        n++;
                        xuid = tuid + Integer.toString(n);
                    }
                }
                LOGGER.log(Level.INFO, "Generated user id {0}.", xuid);
                return xuid;
            }
        }
        LOGGER.log(Level.SEVERE, "Unable to generate user id");
        return null;
    }

    private HttpServletResponse getHttpResponse() {
        if (response == null) {
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            HttpSession session = (HttpSession) context.getSession(true);
            response = (HttpServletResponse) context.getResponse();
        }
        return response;
    }

    private void redirectToSamlAddAccount() {
        FacesContext fc = javax.faces.context.FacesContext.getCurrentInstance();
        HttpServletResponse hresponse = (javax.servlet.http.HttpServletResponse) fc.getExternalContext().getResponse();
        String requestContextPath = fc.getExternalContext().getRequestContextPath();
        try {
            hresponse.sendRedirect(requestContextPath + "/faces/login/SamlAddAccountPage.xhtml");
            fc.responseComplete();
        } catch (IOException ex) {
            throw new RuntimeException("IOException thrown while trying to redirect to addaccount");
        }
    }

    private void dumpUserAssertAttributes(UserAssertion userAssert) {
        LOGGER.log(Level.FINEST, "Dump of received attributes:");
        ArrayList l = new ArrayList(userAssert.getAllAttributes());
        for (Iterator it = l.iterator(); it.hasNext();) {
            UserAttribute a = (UserAttribute) it.next();
            String aname = a.getName();
            String aval  = a.getValues().toString();
            LOGGER.log(Level.FINEST, "{0}: {1}", new String[]{aname, aval});
        }
    }
}
