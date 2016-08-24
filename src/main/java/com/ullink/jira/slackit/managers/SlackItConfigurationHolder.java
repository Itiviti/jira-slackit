package com.ullink.jira.slackit.managers;

import java.net.Proxy;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.rest.api.util.ErrorCollection;
import com.atlassian.jira.user.ApplicationUser;

/**
 * This class manages the constants for the properties file and other useful constants
 */
public interface SlackItConfigurationHolder {

    // ////////////////////////////////////////////////////////
    // SLACK IT properties
    // ////////////////////////////////////////////////////////
    String SLACK_IT_CONFIGFILE = "jira-ullink-slackit.properties";
    
    // Slack api url

    // Main configuration
    String SLACK_IT_BASE_URL = "slackit.baseurl";
    String SLACK_IT_CHANNEL_FIELD_ID = "slackit.cfID";
    String SLACK_IT_APP_CLIENT_ID = "slackit.app.client.id";
    String SLACK_IT_APP_CLIENT_SECRET = "slackit.app.client.secret";
    String SLACK_IT_JIRA_USER = "slackit.jirauser";
    String SLACK_IT_COMMENT_RESTRICTION = "slackit.comment";
    
    //JIRA rules for channel membership
    String SLACK_IT_MEMBERS_CUSTOMFIELDS = "slackit.members.customfields";
    String SLACK_IT_MEMBERS_ISSUELINKS = "slackit.members.issuelinks";

    //JIRA OAUTH path
    String JIRA_BASE_PATH = "/plugins/servlet/";
    String OAUTH_REDIRECT_URL = JIRA_BASE_PATH + "slackOauthCallback";
    String OAUTH_AUTHORIZE_URI = JIRA_BASE_PATH + "slackAuthorization";
    
    public CustomField getSlackChannelCustomField();
    
    public List<CustomField> getCustomfieldsForChannelMembers();

    public Set<ApplicationUser> getCustomFieldUsersForChannelMembers(Issue issue);
	
    public Set<ApplicationUser> getLinkedIssuesUsersForChannelMembers(Issue issue);

    public String getSlackBaseUrl();

    public String getSlackAppClientId();

    public String getSlackAppClientSecret();

	public ApplicationUser getJIRAUserForSlack();
	
    public ErrorCollection forceReload();
    
    public Proxy getProxy();
    
    public boolean hasProxy();

    public String getSlackApiBaseUrl();
    
    public Properties getProperties();

    public Long getCommentRoleID();

    public String getCommentGroupID();
}
