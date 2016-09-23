Overview
============

# Overview
This is a JIRA plugin allowing you to integrate Slack channels and JIRA issues in an easy and non-intrusive way (much like what Atlassian do with hipchat)

From any Jira issue, you can:
* create a Slack channel to discuss the issue
* automatically invite reporter, assignees, commenters, project lead
* see the activity from within JIRA and jump directly in the discussion

# Screenshots
Invite dialog example:

![invite](https://raw.github.com/ullink/jira-slackit/master/res/static/invitedialog.png)


How to compile
============
This plugin was initially built (and tested) for JIRA 6.3.12. It does not support JIRA 7 (see other branch)

In order to compile the code and a deployable plugin, you need to have Atlassian Plugin development kit installed and working on your machine (see https://developer.atlassian.com/docs/getting-started). The initial release was built using version 6.1.0

A few commands worth noting:
* atlas-mvn eclipse:eclipse to build an Eclipse-importable project
* atlas-package to compile & generate a plugin jar file


Installation
============

# How to deploy to your Jira instance
* Copy the template properties file (jira-slackit.model.properties) on your JIRA server and rename it jira-slackit.properties 
* Update it with proper values
* In your JIRA web interface, go in Administration > Add-ons > Manage add-ons and upload the jar file
* Check in the logs for any error (you will have at least one error as you need to create a 'slack' custom field, that's covered in the Configuration below) 


# Configuration
All configuration is done directly in the properties file. Current values and reload actions are possible from the Administration section of JIRA > Add-ons > Slack-it
* Once deployed, you need to create a custom field of type 'Slack channel custom field'. This custom field will store the Slack channel id associated to an issue
* You will need to edit the properties to add the id of this custom field and reload the configuration and you are all set

People and usage
============

# Contributors
* Mickael Billot
* Arnaud Jaffre
* Francois Sergot

# License
Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) with no warranty (expressed or implied) for any purpose.
