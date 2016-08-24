package com.ullink.jira.slackit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.UpdateIssueRequest;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static final String jiraHomePath = ComponentAccessor.getComponentOfType(JiraHome.class).getHomePath();
    private static final ArrayList<String> obfuscateKeys = new ArrayList<String>(Arrays.asList("key", "credential" , "secret", "token"));

    /**
     * Load a file from our standard location in JIRA_HOME (/ullink/<subpath>/<fileName>) If not found, will load from standard ClassLoader (within Tomcat
     * webapp folder
     * 
     * @param subPath
     * @param fileName
     * @param callingClass
     * @return
     */
    public static InputStream loadUllinkPropertyFile(String subPath, String fileName, Class<?> callingClass) {
        InputStream stream;

        // first try with JIRA HOME location
        StringBuffer confPath = new StringBuffer();
        confPath.append(jiraHomePath).append(System.getProperty("file.separator")).append("ullink").append(System.getProperty("file.separator"));
        if (StringUtils.isNotEmpty(subPath)) {
            confPath.append(subPath).append(System.getProperty("file.separator"));
        }
        confPath.append(fileName);
        try {
            stream = new FileInputStream(confPath.toString());
            return stream;
        } catch (FileNotFoundException e) {
            log.info("Unable to load from JIRA_HOME, looking into default location (webapp folder)");
        }

        // Second try with WebApp location
        stream = ClassLoaderUtils.getResourceAsStream(fileName, callingClass);
        if (stream != null) {
            return stream;
        }

        // All failed
        log.warn("Impossible to load the configuration file in Ullink folder '" + subPath + "' / '" + fileName + "'");
        return null;
    }
    
    /**
     * Return an ArrayList of issues linked to the current issues Only links setup correctly are considered in this list
     * 
     * @param issue issue on which the analysis should be done
     * @param validInwardLinksIds set of valid inward links
     * @param validOutwardLinksIds set of valid outward links
     * @param validIssueTypesIds set of valid issue types
     * @return linked issues.
     */
    public static ArrayList<Issue> getIssuesLinked(Issue issue, Set<String> validInwardLinksIds, Set<String> validOutwardLinksIds, Set<String> validIssueTypesIds) {
        ArrayList<Issue> linkedIssues = new ArrayList<>();
        IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        List<IssueLink> inwLinks = issueLinkManager.getInwardLinks(issue.getId());
        List<IssueLink> outwLinks = issueLinkManager.getOutwardLinks(issue.getId());
        // Process all inward links
        if (inwLinks != null) {
            for (IssueLink issueLink : inwLinks) {
                // This inward link is valid, now check if linked issue type is valid too
                if (validInwardLinksIds.contains(issueLink.getLinkTypeId().toString())
                        && (validIssueTypesIds.isEmpty() || validIssueTypesIds.contains(issueLink.getDestinationObject().getIssueTypeObject().getId()))) {
                    // Issue type is valid, add the issue to the list of valid linked issues
                    linkedIssues.add(issueManager.getIssueObject(issueLink.getSourceId()));
                }
            }
        }
        // Process all outward links
        if (outwLinks != null) {
            for (IssueLink issueLink : outwLinks) {
                if (validOutwardLinksIds.contains(issueLink.getLinkTypeId().toString())
                        && (validIssueTypesIds.isEmpty() || validIssueTypesIds.contains(issueLink.getDestinationObject().getIssueTypeObject().getId()))) {
                    // Issue type is valid, add the issue to the list of valid linked issues
                    linkedIssues.add(issueManager.getIssueObject(issueLink.getDestinationId()));
                }
            }
        }
        return linkedIssues;
    }
    
    /**
     * Parse the given config map to return the list of valid links indicated in the configuration
     * 
     * @param rawConfig Map of configuration
     * @return a Set of string, each representing a valid issue link
     */
    public static HashMap<String, HashSet<String>> parseLinks(String issuelinksConfigString) {
        log.debug("Parsing issue links configuration from properties file: '" + issuelinksConfigString + "'");
        HashSet<String> inwardLinks = new HashSet<>();
        HashSet<String> outwardLinks = new HashSet<>();
        
        List<String> listOfLinks = Arrays.asList(issuelinksConfigString.split("\\s*,\\s*"));
        for (String link : listOfLinks) {
            String dir = null;
            String id = null;
            if(StringUtils.isEmpty(link)) {
                log.warn("Bad issue link configuration: empty token");
                continue;
            }
            StringTokenizer linkSt = new StringTokenizer(link, ".");
            try {
                id = linkSt.nextToken();
                dir = linkSt.nextToken();
                if (id == null || dir == null)
                    throw new NoSuchElementException("Empty linkID or direction");
            } catch (NoSuchElementException e) {
                log.warn("Incorrect configuration token, expecting <linkID>.<direction>: " + linkSt + " ("+e.getMessage() + ")");
                continue;
            }
            if ("in".equalsIgnoreCase(dir)) {
                inwardLinks.add(id);
            } else if ("out".equalsIgnoreCase(dir)) {
                outwardLinks.add(id);
            } else {
                log.warn("This link direction : " + dir + "is not valid");
            }
        }
        HashMap<String, HashSet<String>> links = new HashMap<>();
        log.debug("Valid inward links id list loaded: " + StringUtils.join(inwardLinks.toArray(), ","));
        log.debug("Valid outward links id list loaded: " + StringUtils.join(outwardLinks.toArray(), ","));
        links.put("in", inwardLinks);
        links.put("out", outwardLinks);
        return links;
    }

    public static void updateCustomField(MutableIssue issue, CustomField cf, String fieldValue) {
        issue.setCustomFieldValue(cf, fieldValue);
        ComponentAccessor.getIssueManager().updateIssue(ComponentAccessor.getJiraAuthenticationContext().getUser(), issue,
                UpdateIssueRequest.builder().eventDispatchOption(EventDispatchOption.ISSUE_UPDATED).sendMail(false).build());
    }
    
    public static Properties sanitizeProperties(Properties rawProperties){
        Properties sanitizedProps = new Properties();
        for(Object key : rawProperties.keySet()) {
            sanitizedProps.put(key, smartObfuscate( (String) key, (String) rawProperties.get(key)));
        }
        return sanitizedProps;
    }
    
    private static boolean needToObfuscate(String in) {
        if (StringUtils.isEmpty(in)) {
            return false;
        }
        in = in.toLowerCase();
        for (String s : obfuscateKeys) {
            if (in.contains(s)){
                return true;
            }
        }
        return false;
    }
    
    private static String smartObfuscate(String key, String value) {
        //First case, no obfuscation needed
        if (!needToObfuscate(key)) {
            return value;
        }
        //Second case, be strict on passwords
        if ("password".equalsIgnoreCase("key")) {
            return "XXXXXXXXXX";
        }
        //Last case, print last digits to at least give a hint
        if (value.length() > 7) {
            return "XXXXXXX" + value.substring(value.length()-3);
        } else {
            return "XXXXXXX" + value.substring(value.length()-2);
        }
    }

}
