package com.ullink.jira.slackit.model.dto;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.Lists;

public class JiraMembers implements Serializable {

    private static final long serialVersionUID = 1L;

    private JiraMember currentUser;
    private JiraMember assignee;
    private JiraMember reporter;
    private JiraMember projectLead;
    private List<JiraMember> watchers = Lists.newArrayList();
    private List<JiraMember> componentsLead = Lists.newArrayList();
    private List<JiraMember> customFields = Lists.newArrayList();
    private List<JiraMember> linkedIssues = Lists.newArrayList();

    public JiraMembers() {
    }

    public void addWatcher(JiraMember member) {
        if (member != null)
            watchers.add(member);
    }

    public void addComponentLead(JiraMember member) {
        if (member != null)
            componentsLead.add(member);
    }

    public void addCustomFields(JiraMember member) {
        if (member != null)
            customFields.add(member);
    }

    public void addLinkedIssueContact(JiraMember member) {
        if (member != null)
            linkedIssues.add(member);
    }

    public JiraMember getAssignee() {
        return assignee;
    }

    public void setAssignee(JiraMember assignee) {
        if (assignee != null)
            this.assignee = assignee;
    }

    public JiraMember getReporter() {
        return reporter;
    }

    public void setReporter(JiraMember reporter) {
        if (reporter != null)
            this.reporter = reporter;
    }

    public JiraMember getProjectLead() {
        return projectLead;
    }

    public void setProjectLead(JiraMember projectLead) {
        if (projectLead != null)
            this.projectLead = projectLead;
    }

    public List<JiraMember> getComponentsLead() {
        return componentsLead;
    }

    public void setComponentsLead(List<JiraMember> componentsLead) {
        this.componentsLead = componentsLead;
    }

    public List<JiraMember> getWatchers() {
        return watchers;
    }

    public void setWatchers(List<JiraMember> watchers) {
        this.watchers = watchers;
    }

    public JiraMember getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(JiraMember currentUser) {
        if (currentUser != null)
            this.currentUser = currentUser;
    }

    public List<JiraMember> getcustomFields() {
        return customFields;
    }

    public void setcustomFields(List<JiraMember> customFields) {
        this.customFields = customFields;
    }

    public List<JiraMember> getLinkedIssues() {
        return linkedIssues;
    }

    public void setLinkedIssues(List<JiraMember> linkedIssues) {
        this.linkedIssues = linkedIssues;
    }

}
