package com.ullink.jira.slackit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.UpdateIssueRequest;
import com.atlassian.jira.issue.fields.CustomField;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static final ArrayList<String> obfuscateKeys = new ArrayList<String>(Arrays.asList("key", "credential" , "secret", "token"));

    
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
