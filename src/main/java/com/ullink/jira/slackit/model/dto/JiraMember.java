package com.ullink.jira.slackit.model.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Lists;

public class JiraMember implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String email;

    private JiraMember(String name, String email) {
        this.email = email;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static JiraMember getJiraMember(ApplicationUser user) {
        return new JiraMember(user.getDisplayName(), user.getEmailAddress());
    }

    public static List<JiraMember> getJiraMembers(Set<ApplicationUser> customFieldUsersForChannelMembers) {
        List<JiraMember> results = Lists.newArrayList();
        for (ApplicationUser applicationUser : customFieldUsersForChannelMembers) {
            results.add(getJiraMember(applicationUser));
        }
        return results;
    }
}