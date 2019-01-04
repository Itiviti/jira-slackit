package com.ullink.jira.slackit.fields;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.google.common.base.Splitter;
import com.ullink.jira.slackit.managers.SlackItConfigurationHolder;

public class SlackChannelCustomField extends GenericTextCFType implements SortableCustomField<String> {

    public static String SEPARATOR = ".";
    private SlackItConfigurationHolder slackItConfigurationHolder;

    public SlackChannelCustomField(CustomFieldValuePersister customFieldValuePersister, GenericConfigManager genericConfigManager, TextFieldCharacterLengthValidator textFieldCharacterLengthValidator, final JiraAuthenticationContext jiraAuthenticationContext, SlackItConfigurationHolder slackItConfigurationHolder) {
        super(customFieldValuePersister, genericConfigManager, textFieldCharacterLengthValidator, jiraAuthenticationContext);
        this.slackItConfigurationHolder = slackItConfigurationHolder;
    }

    @Override
    @Nonnull
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        String value = (String) issue.getCustomFieldValue(field);
        if (value != null) {
            Iterator<String> splitIterator = Splitter.on(SEPARATOR).omitEmptyStrings().split(value).iterator();
            if (splitIterator.hasNext()) {
                params.put("channelId", splitIterator.next());
            }
            if (splitIterator.hasNext()) {
                params.put("channelName", splitIterator.next());
            }
        }
        params.put("issue", issue);
        params.put("slackURL", slackItConfigurationHolder.getSlackBaseUrl());
        return params;
    }
}
