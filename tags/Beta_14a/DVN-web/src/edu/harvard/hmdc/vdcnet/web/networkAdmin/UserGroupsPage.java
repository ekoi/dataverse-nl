/*
 * UserGroupsPage.java
 *
 * Created on October 20, 2006, 4:24 PM
 * 
 */
package edu.harvard.hmdc.vdcnet.web.networkAdmin;

import edu.harvard.hmdc.vdcnet.admin.GroupServiceLocal;
import edu.harvard.hmdc.vdcnet.admin.UserGroup;
import edu.harvard.hmdc.vdcnet.web.common.StatusMessage;
import edu.harvard.hmdc.vdcnet.web.common.VDCBaseBean;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.event.ActionEvent;

/**
 * <p>Page bean that corresponds to a similarly named JSP page.  This
 * class contains component definitions (and initialization code) for
 * all components that you have defined on this page, as well as
 * lifecycle methods and event handlers where you may add behavior
 * to respond to incoming events.</p>
 */
public class UserGroupsPage extends VDCBaseBean {
    @EJB GroupServiceLocal groupService;
 
    
    public void init() {
        super.init();
        statusMessage =  (StatusMessage)getRequestMap().get("statusMessage");
        
        initGroupData();
    }
    
    private void initGroupData() {
        groups = new ArrayList<UserGroupsInfoBean>();
        List<UserGroup> userGroups = groupService.findAll();
        for (Iterator it = userGroups.iterator(); it.hasNext();) {
            UserGroup elem = (UserGroup) it.next();
            groups.add(new UserGroupsInfoBean(elem));
        }
       
    }
    
    /** 
     * <p>Construct a new Page bean instance.</p>
     */
    public UserGroupsPage() {
    }


    /** 
     * <p>Callback method that is called after the component tree has been
     * restored, but before any event processing takes place.  This method
     * will <strong>only</strong> be called on a postback request that
     * is processing a form submit.  Customize this method to allocate
     * resources that will be required in your event handlers.</p>
     */
    public void preprocess() {
    }

    /** 
     * <p>Callback method that is called just before rendering takes place.
     * This method will <strong>only</strong> be called for the page that
     * will actually be rendered (and not, for example, on a page that
     * handled a postback and then navigated to a different page).  Customize
     * this method to allocate resources that will be required for rendering
     * this page.</p>
     */
    public void prerender() {
    }

    /** 
     * <p>Callback method that is called after rendering is completed for
     * this request, if <code>init()</code> was called (regardless of whether
     * or not this was the page that was actually rendered).  Customize this
     * method to release resources acquired in the <code>init()</code>,
     * <code>preprocess()</code>, or <code>prerender()</code> methods (or
     * acquired during execution of an event handler).</p>
     */
    public void destroy() {
    }

    /**
     * Holds value of property goups.
     */
    List<UserGroupsInfoBean> groups; 
 
    /**
     * Getter for property goups.
     * @return Value of property goups.
     */
    public  List<UserGroupsInfoBean> getGoups() { 
        return groups;
    }

    /**
     * Holds value of property statusMessage.
     */
    private StatusMessage statusMessage;

    /**
     * Getter for property statusMessage.
     * @return Value of property statusMessage.
     */
    public StatusMessage getStatusMessage() {
        return this.statusMessage;
    }

    /**
     * Setter for property statusMessage.
     * @param statusMessage New value of property statusMessage.
     */
    public void setStatusMessage(StatusMessage statusMessage) {
        this.statusMessage = statusMessage;
    }

  public void deleteGroup(ActionEvent ae) {
        UserGroupsInfoBean bean=(UserGroupsInfoBean)dataTable.getRowData();
        UserGroup userGroup = bean.getGroup();
        groupService.remove(userGroup.getId());
        initGroupData();  // Re-fetch list to reflect Delete action
        
        
    }

    /**
     * Holds value of property dataTable.
     */
    private HtmlDataTable dataTable;

    /**
     * Getter for property dataTable.
     * @return Value of property dataTable.
     */
    public HtmlDataTable getDataTable() {
        return this.dataTable;
    }

    /**
     * Setter for property dataTable.
     * @param dataTable New value of property dataTable.
     */
    public void setDataTable(HtmlDataTable dataTable) {
        this.dataTable = dataTable;
    }
   
}

