package com.ullink.jira.slackit.fragments;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.ullink.jira.slackit.fields.SlackChannelCustomField;
import com.ullink.jira.slackit.managers.SlackItConfigurationHolder;
import com.ullink.jira.slackit.model.dto.JiraMember;
import com.ullink.jira.slackit.model.dto.JiraMembers;
import com.ullink.jira.slackit.service.UserTokenService;

public class SlackPanel extends AbstractJiraContextProvider {

    private final UserTokenService userTokenService;
    private SlackItConfigurationHolder slackItConfigurationHolder;

    public SlackPanel(UserTokenService userTokenService, SlackItConfigurationHolder slackItConfigurationHolder) {
        this.userTokenService = checkNotNull(userTokenService);
        this.slackItConfigurationHolder = checkNotNull(slackItConfigurationHolder);
    }

    @Override
    public Map getContextMap(User arg0, JiraHelper jiraHelper) {
        Issue issue = (Issue) jiraHelper.getContextParams().get("issue");
        final String currentUserKey = ComponentAccessor.getJiraAuthenticationContext().getUser().getKey();
        final Map<String, Object> params = Maps.newHashMap();
        params.put("issue", issue);
        String token = userTokenService.getUserToken(currentUserKey);
        params.put("token", token);
        params.put("baseUrl", jiraHelper.getRequest().getRequestURI());
        params.put("contextPath", jiraHelper.getRequest().getContextPath());
        params.put("slackBaseUrl", slackItConfigurationHolder.getSlackBaseUrl());
        params.put("slackApiBaseUrl", slackItConfigurationHolder.getSlackApiBaseUrl());

        String slackChannelCustomFieldValue = slackItConfigurationHolder.getSlackChannelCustomField().getValueFromIssue(issue);
        String slackChannelId = "";
        if (slackChannelCustomFieldValue != null) {
            Iterator<String> splitIterator = Splitter.on(SlackChannelCustomField.SEPARATOR).omitEmptyStrings().split(slackChannelCustomFieldValue).iterator();
            slackChannelId = splitIterator.hasNext() ? splitIterator.next() : "";
        }
        params.put("slackChannelId", slackChannelId);

        JiraMembers jiraMembers = new JiraMembers();

        addStandardIssuecontacts(jiraMembers, issue);
        addCustomfieldsContacts(jiraMembers, issue);
        addLinkedIssuesContacts(jiraMembers, issue);

        Gson gson = new Gson();
        params.put("jiraMembers", gson.toJson(jiraMembers));

        return params;
    }

    private void addStandardIssuecontacts(JiraMembers jiraMembers, Issue issue) {
        jiraMembers.setCurrentUser(JiraMember.getJiraMember(ComponentAccessor.getJiraAuthenticationContext().getUser()));

        if (issue.getReporter() != null) {
            jiraMembers.setReporter(JiraMember.getJiraMember(issue.getReporter()));
        }
        if (issue.getAssignee() != null) {
            jiraMembers.setAssignee(JiraMember.getJiraMember(issue.getAssignee()));
        }

        List<ApplicationUser> watchers = ComponentAccessor.getWatcherManager().getWatchers(issue, Locale.ENGLISH);
        if (watchers != null) {
            for (ApplicationUser watcher : watchers) {
                jiraMembers.addWatcher(JiraMember.getJiraMember(watcher));
            }
        }

        if (issue.getComponentObjects() != null) {
            for (ProjectComponent component : issue.getComponentObjects()) {
                if (component.getComponentLead() != null) {
                    jiraMembers.addComponentLead(JiraMember.getJiraMember(component.getComponentLead()));
                }
            }
        }

        Project project = ComponentAccessor.getProjectManager().getProjectObj(issue.getProjectObject().getId());
        if (project != null && project.getProjectLead() != null) {
            jiraMembers.setProjectLead(JiraMember.getJiraMember(project.getProjectLead()));
        }

    }

    private void addLinkedIssuesContacts(JiraMembers jiraMembers, Issue issue) {
        for (ApplicationUser user : slackItConfigurationHolder.getLinkedIssuesUsersForChannelMembers(issue)) {
            jiraMembers.addLinkedIssueContact(JiraMember.getJiraMember(user));
        }
    }

    private void addCustomfieldsContacts(JiraMembers jiraMembers, Issue issue) {
        for (ApplicationUser user : slackItConfigurationHolder.getCustomFieldUsersForChannelMembers(issue)) {
            jiraMembers.addCustomFields(JiraMember.getJiraMember(user));
        }
    }
}
