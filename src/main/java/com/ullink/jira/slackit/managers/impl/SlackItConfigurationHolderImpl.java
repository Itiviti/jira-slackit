package com.ullink.jira.slackit.managers.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
import com.atlassian.jira.issue.customfields.impl.UserCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.rest.api.util.ErrorCollection;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.ullink.jira.slackit.Utils;
import com.ullink.jira.slackit.fields.SlackChannelCustomField;
import com.ullink.jira.slackit.managers.SlackItConfigurationHolder;

public class SlackItConfigurationHolderImpl implements SlackItConfigurationHolder, LifecycleAware {

    private static final Logger LOG = Logger.getLogger(SlackItConfigurationHolderImpl.class);

    private Proxy proxy;

    private CustomFieldManager customFieldManager;
    private IssueLinkManager issueLinkManager;
    private IssueManager issueManager;
    private JiraHome jiraHome;
    private ApplicationUser jiraUser;
    private String commentRestrictionGroupId;
    private Long commentRestrictionRoleId;

    private CustomField slackItChannelIdCF;
    private Properties slackItProperties;

    private List<CustomField> channelMembersCustomfields;
    private HashMap<String, HashSet<String>> channelMembersIssueLinks;
    private ProjectRoleManager projectRoleManager;


    public SlackItConfigurationHolderImpl(JiraHome jiraHome, CustomFieldManager customFieldManager, IssueLinkManager issueLinkManager, IssueManager issueManager, ProjectRoleManager projectRoleManager) {
        slackItProperties = new Properties();
        this.customFieldManager = customFieldManager;
        this.jiraHome = jiraHome;
        this.issueLinkManager = issueLinkManager;
        this.issueManager = issueManager;
        this.projectRoleManager = projectRoleManager;
    }

    public ErrorCollection forceReload() {
        LOG.info("Reload forced for slackit configurator");
        return loadConfiguration();
    }

    private ErrorCollection loadConfiguration() {
        LOG.info("Start loading configuration");
        ErrorCollection loadingErrorCollection = new ErrorCollection();
        slackItProperties = loadPropertiesFile(loadingErrorCollection);
        if (slackItProperties != null && ! slackItProperties.isEmpty()) {
            loadSlackCustomField(loadingErrorCollection);
            loadChannelMembersCustomfields(loadingErrorCollection);
            loadChannelMembersIssueLinks(loadingErrorCollection);
            loadJiraUser(loadingErrorCollection);
            loadCommentRestriction(loadingErrorCollection);
            loadProxyConfiguration(loadingErrorCollection);
        }
        if (loadingErrorCollection.hasAnyErrors()) {
            LOG.warn("There were errors when loading configuration: ");
            for(String errorMessage : loadingErrorCollection.getErrorMessages()) {
                LOG.warn("   --> " + errorMessage);
            }
        } else {
            LOG.info("Configuration loaded successfully");
        }
        return loadingErrorCollection;
    }

    private void loadCommentRestriction(ErrorCollection loadingErrorCollection) {
        String rawCommentRestriction = getProperty(SLACK_IT_COMMENT_RESTRICTION);
        if (StringUtils.isEmpty(rawCommentRestriction)){
            commentRestrictionGroupId = null;
            commentRestrictionRoleId = null;
            return;
        }
        if (rawCommentRestriction.startsWith("role.")){
            ProjectRole visibilityRole = projectRoleManager.getProjectRole(rawCommentRestriction.replace("role.", "").trim());
            commentRestrictionRoleId = visibilityRole == null ?  null : visibilityRole.getId(); 
            LOG.info("Setting comment restriction with role " + commentRestrictionRoleId );
        }
        if (rawCommentRestriction.startsWith("group.")){
            Group visibilityGroup = ComponentAccessor.getGroupManager().getGroup(rawCommentRestriction.replace("group.", "").trim());
            commentRestrictionGroupId = visibilityGroup == null ? null : visibilityGroup.getName();
            LOG.info("Setting comment restriction with group " + commentRestrictionGroupId );
        }
        
    }

    private void loadProxyConfiguration(ErrorCollection loadingErrorCollection) {
        List<Proxy> proxies;
        try {
            proxies = ProxySelector.getDefault().select(new URI(getProperty(SLACK_IT_BASE_URL)));
        } catch (URISyntaxException e) {
            loadingErrorCollection.addErrorMessage("Error when setting system proxy for Slack requests, using direct connection by default: " + e.getMessage());
            proxy = null;
            return;
        }
        Proxy proxySetup = proxies.get(0);
        if (proxySetup.type() == Proxy.Type.DIRECT) {
            LOG.info("No proxy configured, using direct access");
            proxy = null;
        } else {
            proxy = proxySetup;
            LOG.info("Using TOMCAT proxy configuration for Slack:" + proxy.toString());
        }
    }

    private void loadJiraUser(ErrorCollection loadingErrorCollection) {
        String userkey = getProperty(SLACK_IT_JIRA_USER);
        if (!StringUtils.isEmpty(userkey)) {
            jiraUser = ComponentAccessor.getUserManager().getUserByKey(userkey);
        }
        if (jiraUser == null) {
            loadingErrorCollection.addErrorMessage("JIRA user for slack is not defined or unknown for key " + SLACK_IT_JIRA_USER + " and value '" + userkey + "'");
            return;
        }
    }

    private void loadChannelMembersIssueLinks(ErrorCollection loadingErrorCollection) {
        LOG.info("Loading issue links to use for channel members candidates");
        channelMembersIssueLinks = Utils.parseLinks(getProperty(SLACK_IT_MEMBERS_ISSUELINKS));
        LOG.info("Loading done");
        return;
    }

    private void loadChannelMembersCustomfields(ErrorCollection loadingErrorCollection) {
        LOG.info("Loading user custom fields holding channel members candidates");
        String listOfIDs = getProperty(SLACK_IT_MEMBERS_CUSTOMFIELDS);
        channelMembersCustomfields = new ArrayList<>();
        if (StringUtils.isEmpty(listOfIDs)) {
            loadingErrorCollection.addErrorMessage("No specific customfields setup for addition slack channel membership");
            return;
        }
        List<String> listOfFields = Arrays.asList(listOfIDs.split("\\s*,\\s*"));
        CustomField tmp;
        for (String id : listOfFields) {
            tmp = customFieldManager.getCustomFieldObject("customfield_" + id);
            if (tmp == null) {
                loadingErrorCollection.addErrorMessage("Unknown custom field setup in " + SLACK_IT_MEMBERS_CUSTOMFIELDS + " : '" + id);
                return;
            } else if (tmp.getCustomFieldType() instanceof UserCFType || tmp.getCustomFieldType() instanceof MultiUserCFType) {
                LOG.info("Adding customfield '" + tmp.getFieldName() + "' as possible channel member");
                channelMembersCustomfields.add(tmp);
            } else {
                loadingErrorCollection.addErrorMessage("Custom field '" + tmp.getFieldName() + "'/'" + tmp.getId() + "' is not a valid user or multiuser field, skipping");
                return;
            }
        }
        LOG.info("List of custom fields for channel members initialized with " + channelMembersCustomfields.size() + " customfields");
    }

    private void loadSlackCustomField(ErrorCollection loadingErrorCollection) {
        LOG.debug("Setting SlackIt custom field defined by the property '" + SLACK_IT_CHANNEL_FIELD_ID + "'");
        if (!slackItProperties.containsKey(SLACK_IT_CHANNEL_FIELD_ID)) {
            loadingErrorCollection.addErrorMessage("Cannot load custom field, key '" + SLACK_IT_CHANNEL_FIELD_ID + "' is not found in properties => " + slackItProperties.keySet());
            return;
        }
        String rawID2 = "customfield_" + slackItProperties.getProperty(SLACK_IT_CHANNEL_FIELD_ID);
        CustomField cf = customFieldManager.getCustomFieldObject(rawID2);
        if (cf == null || !(cf.getCustomFieldType() instanceof SlackChannelCustomField)) {
            loadingErrorCollection.addErrorMessage("Cannot load the slack it custom field identified by '" + rawID2 + "'. Field is either unknown or not of the right type ");
            return;
        } else {
            slackItChannelIdCF = cf;
            LOG.info("SlackIt customfield loaded as " + slackItChannelIdCF.getName() + " / " + slackItChannelIdCF.getId());
        }
    }

    private Properties loadPropertiesFile(ErrorCollection loadingErrorCollection) {
        LOG.info("Loading configuration file '" + SLACK_IT_CONFIGFILE + "'");
        InputStream stream = null;
        Properties propsFromFile = new Properties();

        StringBuilder confPath = new StringBuilder();
        confPath.append(jiraHome.getHomePath()).append(System.getProperty("file.separator")).append(SLACK_IT_CONFIGFILE);
        LOG.info("Configuration file location is expected at '" + confPath.toString() + "'");
        try {
            stream = new FileInputStream(confPath.toString());
        } catch (FileNotFoundException e) {
            loadingErrorCollection.addErrorMessage("Unable to load configuration file from JIRA_HOME, this should be the standard file location, please fix this (trying with standard ClasLoader as fallback)");
            return null;
        }
        try {
            propsFromFile.load(stream);
        } catch (IOException e) {
            loadingErrorCollection.addErrorMessage("Exception when loading configuration file '" + SLACK_IT_CONFIGFILE + "' : " + e.getMessage());
            return null;
        }

        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                LOG.warn("Exception when closing file stream :" + e.getMessage(), e);
            }
        }
        if (propsFromFile.isEmpty()) {
            LOG.warn("Empty porperties file loaded");
            return null;
        }
        LOG.debug("Successfull loading of " + propsFromFile.size() + " properties => " + propsFromFile.keySet());
        return propsFromFile;
    }

    private String getProperty(final String key, final String alt_value) {
        if (slackItProperties == null) {
            loadConfiguration();
            if (slackItProperties == null) {
                LOG.error("Impossible to proceed with request for property '" + key + "', property file is not loaded");
                return "";
            }
        }
        if (key == null) {
            LOG.warn("Trying to retrieve a property using a null key, spooky....");
            return null;
        }
        String result = slackItProperties.getProperty(key);
        if ((result == null) || (result.trim().length() == 0)) {
            if (alt_value != null) {
                result = alt_value;
                LOG.info("Configuration key missing '" + key + "' using alternative hard coded '" + alt_value + "'");
            } else {
                LOG.info("Configuration key missing '" + key + "' but no alternative hard coded value, setting to null");
                return null;
            }
        }
        return result.trim();
    }

    private String getProperty(final String key) {
        return getProperty(key, null);
    }

    @Override
    public String getSlackBaseUrl() {
        return getProperty(SLACK_IT_BASE_URL);
    }

    @Override
    public CustomField getSlackChannelCustomField() {
        if (slackItChannelIdCF == null) {
            loadConfiguration();
        }
        return slackItChannelIdCF;
    }

    @Override
    public String getSlackAppClientId() {
        return getProperty(SLACK_IT_APP_CLIENT_ID);
    }

    @Override
    public String getSlackAppClientSecret() {
        return getProperty(SLACK_IT_APP_CLIENT_SECRET);
    }

    @Override
    public ApplicationUser getJIRAUserForSlack() {
        return jiraUser;
    }

    @Override
    public List<CustomField> getCustomfieldsForChannelMembers() {
        return channelMembersCustomfields;
    }

    private List<Issue> getValidLinkedIssuesForChannelMembers(Issue issue) {
        ArrayList<Issue> linkedIssues = new ArrayList<>();
        List<IssueLink> inwLinks = issueLinkManager.getInwardLinks(issue.getId());
        List<IssueLink> outwLinks = issueLinkManager.getOutwardLinks(issue.getId());

        // Process all inward links
        if (inwLinks != null) {
            for (IssueLink issueLink : inwLinks) {
                if (channelMembersIssueLinks.get("in").contains(issueLink.getLinkTypeId().toString())) {
                    linkedIssues.add(issueManager.getIssueObject(issueLink.getSourceId()));
                }
            }
        }
        // Process all outward links
        if (outwLinks != null) {
            for (IssueLink issueLink : outwLinks) {
                if (channelMembersIssueLinks.get("out").contains(issueLink.getLinkTypeId().toString())) {
                    linkedIssues.add(issueManager.getIssueObject(issueLink.getDestinationId()));
                }
            }
        }
        return linkedIssues;
    }

    /**
     * Retrieve the list of Jira users matching the list of customfields defined in properties To be used as candidate for channel members at creation
     * 
     * @param issue the issue to analyze
     * @return a list of Jira users
     */
    @SuppressWarnings("unchecked")
    public Set<ApplicationUser> getCustomFieldUsersForChannelMembers(Issue issue) {
        Set<ApplicationUser> members = new HashSet<>();
        for (CustomField cf : channelMembersCustomfields) {
            if (cf.getCustomFieldType() instanceof UserCFType && (issue.getCustomFieldValue(cf) != null)) {
                members.add((ApplicationUser) issue.getCustomFieldValue(cf));
            } else if ((cf.getCustomFieldType() instanceof MultiUserCFType) && (issue.getCustomFieldValue(cf) != null)) {
                members.addAll((Collection<ApplicationUser>) issue.getCustomFieldValue(cf));
            }
        }
        return members;
    }

    /**
     * Retrieve the list of Jira users related to the linked issues defined in properties To be used as candidate for channel members at creation
     * 
     * @param issue
     * @return
     */
    public Set<ApplicationUser> getLinkedIssuesUsersForChannelMembers(Issue issue) {
        Set<ApplicationUser> members = new HashSet<>();
        List<Issue> linkedIssues = getValidLinkedIssuesForChannelMembers(issue);
        for (Issue linkedIssue : linkedIssues) {
            members.add(linkedIssue.getReporter());
            members.add(linkedIssue.getAssignee());
            members.addAll(getCustomFieldUsersForChannelMembers(linkedIssue));
        }
        //Remove null users (like unassigned)
        members.remove(null);
        return members;
    }

    @Override
    public Proxy getProxy() {
        return proxy;
    }

    @Override
    public String getProxyAsString() {
        return (proxy == null ? "DIRECT" : proxy.toString());
    }

    @Override
    public boolean hasProxy() {
        return (proxy != null);
    }

    @Override
    public String getSlackApiBaseUrl() {
        return getSlackBaseUrl() + "/api";
    }
    
    @Override
    public Properties getProperties(){
        return ((slackItProperties != null) ? Utils.sanitizeProperties(slackItProperties) : new Properties());
    }

    @Override
    public void onStart() {
        LOG.info("Starting SlackIt plugin configurator");
        loadConfiguration();
        LOG.info("SlackIt plugin configurator started");
    }
    
    @Override
    public String getCommentVisibility() {
        return (commentRestrictionRoleId != null ? "Role = " + commentRestrictionRoleId : "No role restriction") 
                + "  "+ (commentRestrictionGroupId != null ? "Group = " + commentRestrictionGroupId : "No group restriction");
    }

    @Override
    public Long getCommentRoleID() {
        return commentRestrictionRoleId;
    }

    @Override
    public String getCommentGroupID() {
        return commentRestrictionGroupId;
    }

    @Override
    public void onStop() {
        LOG.info("Lifycle plugin STOP - Nothing to do");
    }

}
