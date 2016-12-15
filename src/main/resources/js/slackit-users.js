// Rx HTTP client (Slack API, mainly)
var HttpRxClient = (function () {
    function HttpRxClient() {
    }

    HttpRxClient.prototype.getSlackSingleChannelInfoObservable = function (channelId, token, slackApiBaseUrl) {
        var subject = new Rx.AsyncSubject();
        if (channelId === '' || !channelId) {
            // no channel ID, skip slack rest service hit & simulate slack corresponding response
            subject.onNext({
                ok: false,
                error: "channel_not_found",
                internalReject: true
            });
            subject.onCompleted();
            return subject;
        }

        AJS.$.getJSON(slackApiBaseUrl + "/channels.info?channel=" + channelId + "&token=" + token, function(json) {
            if (json.error) {
                subject.onNext({
                    ok: false,
                    error: json.error
                }); // on error continue
                subject.onCompleted();
                return;
            }

            subject.onNext({
                ok: true,
                channel: json.channel
            });
            subject.onCompleted();
        });

        return subject;
    };

    HttpRxClient.prototype.getSlackSingleChannelHistoryObservable = function (channelId, token, slackApiBaseUrl) {
        var subject = new Rx.AsyncSubject();
        if (channelId === '' || !channelId) {
            // no channel ID, skip slack rest service hit & simulate slack corresponding response
            subject.onNext({
                ok: false,
                error: "channel_not_found",
                internalReject: true
            });
            subject.onCompleted();
            return subject;
        }

        AJS.$.getJSON(slackApiBaseUrl + "/channels.history?channel=" + channelId + "&token=" + token, function(json) {
            if (json.error) {
                subject.onNext({
                    ok: false,
                    error: json.error
                }); // on error continue
                subject.onCompleted();
                return;
            }

            subject.onNext({
                ok: true,
                messages: json.messages
            });
            subject.onCompleted();
        });

        return subject;
    };

    HttpRxClient.prototype.getSlackUsersObservable = function (token, slackApiBaseUrl) {
        var subject = new Rx.AsyncSubject();
        AJS.$.getJSON(slackApiBaseUrl + "/users.list?token=" + token,
            function(json) {
                if (json.error) {
                    subject.onError(json.error);
                    subject.onCompleted();
                    return;
                }

                var users = {
                    byId: {},
                    byEmail: {}
                }; // misc indexing
                AJS.$.each(json.members, function(index, element) {
                    var o = {
                        name: element.profile.real_name,
                        email: element.profile.email,
                        id: element.id
                    };
                    users.byId[element.id] = o;
                    users.byEmail[element.profile.email] = o;
                });
                subject.onNext(users);
                subject.onCompleted();
            });

        return subject;
    };

    // Get observable for channels.list (list of limited channel objects), index by channel name
    HttpRxClient.prototype.getSlackChannelsListObservable = function (token, slackApiBaseUrl) {
        var subject = new Rx.AsyncSubject();
        AJS.$.getJSON(slackApiBaseUrl + "/channels.list?token=" + token + "&exclude_archived=0", function(json) {
            if (json.error) {
                subject.onError(json.error);
                subject.onCompleted();
                return;
            }

            var channels = {}; // name : {id, name, creator, num_members, ...}
            AJS.$.each(json.channels, function(index, element) {
                channels[element.name] = element;
            });
            subject.onNext(channels);
            subject.onCompleted();
        });

        return subject;
    };

    HttpRxClient.prototype.createSlackChannelObservable = function (channelName, token, slackApiBaseUrl) {
        var subject = new Rx.AsyncSubject();
        AJS.$.getJSON(slackApiBaseUrl + "/channels.create?name=" + channelName + "&token=" + token, function(json) {

            if (json.error) {
                subject.onNext({
                    ok: false,
                    error: json.error
                });
                subject.onCompleted();
                return;
            }

            subject.onNext({
                ok: true,
                channelId: json.channel.id
            });
            subject.onCompleted();
        });

        return subject;
    };

    HttpRxClient.prototype.joinSlackChannelObservable = function (channelName, token, slackApiBaseUrl) {
        var subject = new Rx.AsyncSubject();
        AJS.$.getJSON(slackApiBaseUrl + "/channels.join?name=" + channelName + "&token=" + token, function(json) {

            if (json.error) {
                subject.onNext({
                    ok: false,
                    error: json.error
                });
                subject.onCompleted();
                return;
            }

            subject.onNext({
                ok: true,
                channelId: json.channel.id
            });
            subject.onCompleted();
        });

        return subject;
    };

    HttpRxClient.prototype.linkSlackChannelToJiraObservable = function (servletLinkSlackChannelURL, issueKey, channelId, channelName) {
        var subject = new Rx.AsyncSubject();
        AJS.$.getJSON(servletLinkSlackChannelURL + '&issueKey=' + issueKey + "&channelId=" + channelId + "&channelName=" + channelName, function(json) {
            subject.onNext({
                ok: true
            });
            subject.onCompleted();
        });

        return subject;
    };

    HttpRxClient.prototype.inviteSlackUserToChannelObservable = function (channelId, userId, token, slackApiBaseUrl) {
        var subject = new Rx.AsyncSubject();
        AJS.$.getJSON(slackApiBaseUrl + "/channels.invite?token=" + token + "&channel=" + channelId + "&user=" + userId, function(json) {
            if (json.error) {
                subject.onNext({
                    ok: false,
                    error: json.error
                });
                subject.onCompleted();
                return;
            }

            subject.onNext({
                ok: true,
                members: json.channel.members
            });
            subject.onCompleted();
        });

        return subject;
    };

    return HttpRxClient;
}());

// ------------- main
var SlackItClient = (function () {
    function SlackItClient(pcontext) {
        this.context = pcontext;
        this.context.slackChannelPreferredName = ('jira_' + this.context.jiraIssueKey.replace(/-/g, "_")).toLowerCase();
        this.rxClient = new HttpRxClient();
    }

    // SlackIt dialog on screen
    SlackItClient.prototype.onSlackItDialogShown = function(slackData) {
        var _this = this;
        console.log('onSlackItDialogShown()');

        // fetch channels list
        AJS.$('#loadingSpinner').spin();
        this.rxClient.getSlackChannelsListObservable(this.context.token, this.context.slackApiBaseUrl)
            .subscribe(
                function(slackChannelsData) {
                    console.log('* channels: ' + JSON.stringify(slackChannelsData));
                    AJS.$('#loadingDiv').hide();
                    AJS.$('#loadingSpinner').spinStop();
                    slackData.channels = slackChannelsData;

                    // check channel doesn't already exist
                    var matchCandidate = '';
                    if (slackChannelsData.hasOwnProperty(_this.context.slackChannelPreferredName)) {
                        matchCandidate = _this.context.slackChannelPreferredName;
                    }

                    if (matchCandidate !== '') {
                        _this.onSlackItDialogDoStepLinkExistingChannel(slackData, matchCandidate);
                    } else {
                        _this.onSlackItDialogDoStepCreateChannel(slackData);
                    }
                },
                function(error) {
                    console.log('Error: ' + error);
                    AJS.$('#loadingSpinner').spinStop();
                    AJS.messages.error({
                        title: 'Channel list load error',
                        body: 'Unable to load channels list. Please reload page. Error: ' + error
                    });
                },
                function() {});
    };

    SlackItClient.prototype.run = function() {
        var _this = this;

        console.log('SlackItClient running:');
        console.log('slackApiBaseUrl: ' + this.context.slackApiBaseUrl);
        console.log('slackChannelId: ' + this.context.slackChannelId);
        console.log('jiraIssueKey: ' + this.context.jiraIssueKey);
        console.log('slackChannelPreferredName: ' + this.context.slackChannelPreferredName);
        console.log('servletLinkSlackChannelURL: ' + this.context.servletLinkSlackChannelURL);

        var slackSource = Rx.Observable.zip(
            this.rxClient.getSlackUsersObservable(this.context.token, this.context.slackApiBaseUrl),
            this.rxClient.getSlackSingleChannelInfoObservable(this.context.slackChannelId, this.context.token, this.context.slackApiBaseUrl),
            this.rxClient.getSlackSingleChannelHistoryObservable(this.context.slackChannelId, this.context.token, this.context.slackApiBaseUrl),
            function(restUsers, restSingleChannelInfo, restSingleChannelHistory) {
                return {
                    users: restUsers,
                    channel: restSingleChannelInfo,
                    history: restSingleChannelHistory
                };
            }
        );

        if (!this.context.token) {
            this.toggleSlackAuthorization(true);
        } else {
            var subscription = slackSource.subscribe(
                function(slackData) {
                    _this.onSlackDataReady(slackData);
                },
                function(err) {
                    console.log('Error: ' + err);
                    _this.onSlackDataError(err);
                },
                function() {
                    console.log('Completed');
                }
            );
        }
    };

    SlackItClient.prototype.hashUserEmail = function(email) {
        return 'guest-' + email.toLowerCase().replace(/@/g, "-").replace(/\./g, "-"); // hash not to break DOM ids
    };

    SlackItClient.prototype.alterGuestList = function(jiraMembersSubArray, guestList, type, add) {
        var _this = this;
        AJS.$.each(jiraMembersSubArray, function(index, e) {
            var key = type + '-' + _this.hashUserEmail(e.email);
            if (add && !guestList.hasOwnProperty(key)) {
                guestList[key] = e;
            } else if (!add && guestList.hasOwnProperty(key)) {
                delete guestList[key];
            }
        });
    };

    SlackItClient.prototype.onSlackItDialogRedrawGuestList = function(guestList) {
        var _this = this;
        var body = '';
        var displayedEmails = [];
        AJS.$.each(guestList, function(key, e) {
            var closeKey = key + '-close';
            if (displayedEmails.indexOf(e.email) < 0) {
                var item = '<span id="' + key + '" class="aui-label aui-label-closeable">' + e.name + '<span id="' + closeKey + '" class="aui-icon aui-icon-close" ></span></span>';
                body += item + '&nbsp;';
                displayedEmails.push(e.email);
            }
        });
        AJS.$('#slack-invite-guestslist').html('<b>' + body + '</b>');

        // bind remove guest click
        AJS.$.each(guestList, function(key, e) {
            var closeKey = key + '-close';
            AJS.$(('#' + closeKey)).click(function() {
                delete guestList[key];
                _this.onSlackItDialogRedrawGuestList(guestList);
            });
        });
    };

    SlackItClient.prototype.onSlackItDialogDoStepInviteUsers = function(slackData, channelName) {
        var _this = this;
        var guestList = {};
        console.log('onSlackItDialogDoStepInviteUsers(): channel=' + channelName);

        AJS.$('#slack-invite-users').show();
        AJS.$('#slack-invite-toggle-watchers').click(function() {
            _this.alterGuestList(_this.context.jira_members.watchers || [], guestList, 'watchers', this.checked);
            _this.onSlackItDialogRedrawGuestList(guestList);
        });
        AJS.$('#slack-invite-toggle-reporter').click(
            function() {
                _this.alterGuestList([_this.context.jira_members.reporter] || [], guestList, 'reporter', this.checked);
                _this.onSlackItDialogRedrawGuestList(guestList);
            });
        AJS.$('#slack-invite-toggle-projectlead').click(
            function() {
                _this.alterGuestList([_this.context.jira_members.projectLead] || [], guestList, 'projectlead', this.checked);
                _this.onSlackItDialogRedrawGuestList(guestList);
            });
        AJS.$('#slack-invite-toggle-componentslead').click(
            function() {
                _this.alterGuestList(_this.context.jira_members.componentsLead || [], guestList, 'componentslead', this.checked);
                _this.onSlackItDialogRedrawGuestList(guestList);
            });
        AJS.$('#slack-invite-toggle-customFields').click(
            function() {
                _this.alterGuestList(_this.context.jira_members.customFields || [], guestList, 'customFields', this.checked);
                _this.onSlackItDialogRedrawGuestList(guestList);
            });
        AJS.$('#slack-invite-toggle-linkedissuescontacts').click(
            function() {
                _this.alterGuestList(_this.context.jira_members.linkedIssues || [], guestList, 'linkedissuescontacts', this.checked);
                _this.onSlackItDialogRedrawGuestList(guestList);
            });
        AJS.$('#slack-setupchannel-dialog-submit').off().click(function() {
            _this.onSlackItDialogSubmitClicked(slackData, channelName, guestList);
        });
    };

     SlackItClient.prototype.onSlackItDialogSubmitClicked = function(slackData, channelName, guestList) {
        // Submit
        var _this = this;
        var channelId = '';
        if (slackData.channels.hasOwnProperty(channelName)) {
            channelId = slackData.channels[channelName].id;
        }

        console.log('onSlackItDialogSubmitClicked(): channel=' + channelName + ' (id=' + channelId + ')' + ', guestList: ' + JSON.stringify(guestList));

        var nbGuests = 0;
        for (var k in guestList) {
            if (guestList.hasOwnProperty(k)) {
                nbGuests++;
            }
        }
        if (nbGuests === 0) {
            alert('Slack is for chatting, you need at least one participant to do that!');
            return;
        }

        // Enter status display mode
        AJS.$('#aui-dialog2-content-status-user-panel').hide();
        AJS.$('#aui-dialog2-content-status-panel').show();

        AJS.$('#slack-setupchannel-dialog-submit').html('Working...');
        AJS.$('#slack-setupchannel-dialog-submit').off().click(function() {});

        if (slackData.context.skipCreateChannel && channelId !== '') {
            this.onSlackItDialogSubmitClickedDoInvitePhase(slackData, channelName, guestList, channelId);
            return;
        }

        AJS.$('#status-channel-creating').show();
        AJS.$('#status-channel-creating-spinner').spin();

        // Channel creation needed
        channelName = AJS.$('#slackChannelCreateInput').val().toLowerCase();
        if (!channelName || channelName.length < 3) {
            AJS.$('#slackChannelCreateInputError').show();
            return;
        }
        AJS.$('#slackChannelCreateInputError').hide();

        console.log('Ready to create ' + channelName);
        this.rxClient.createSlackChannelObservable(channelName, this.context.token, this.context.slackApiBaseUrl).subscribe(
            function(createData) {
                if (createData.ok) {
                    console.log('Channel "#' + channelName + '" created, id: ' + createData.channelId);
                    AJS.$('#status-channel-creating-spinner').spinStop();
                    AJS.$('#status-channel-creating').hide();
                    AJS.$('#status-channel-created').show();
                    _this.onSlackItDialogSubmitClickedDoInvitePhase(slackData, channelName, guestList, createData.channelId);
                    return;
                } else if (createData.error == "name_taken") {
                    // Overcome channel already exists
                    console.log('Channel "#' + channelName + '" already exists. Re resolve channel id...');
                    AJS.$('#status-channel-creating-spinner').spinStop();
                    AJS.$('#status-channel-creating').hide();
                    AJS.$('#status-channel-exists-fetchdata').show();

                    _this.rxClient.getSlackChannelsListObservable(_this.context.token, _this.context.slackApiBaseUrl).subscribe(
                        function(slackChannelsData) {
                            console.log('* channels: ' + JSON.stringify(slackChannelsData));
                            slackData.channels = slackChannelsData;
                            if (!slackChannelsData.hasOwnProperty(channelName)) {
                                // Can't overcome this error
                                alert('Channel creation refused (name_taken), but unable to find channel data. Please reload page. (channelName=' + channelName + ')');
                                return;
                            }

                            var channelId = slackChannelsData[channelName].id;
                            console.log('Reresolution ok for "#' + channelName + '" : ' + channelId);
                            _this.onSlackItDialogSubmitClickedDoInvitePhase(slackData, channelName, guestList, channelId);
                        },
                        function(error) {
                            console.log('Error: ' + error);
                            alert('Channel creation refused (name_taken), but error while fetching channel data. Please reload page. (channelName=' + channelName + ')');
                        },
                        function() {});
                } else {
                    // unrecoverable error
                    AJS.messages.error({
                        title: 'Channel creation error',
                        body: 'Unable to create channel, error: ' + slackData.error
                    });
                }

                AJS.$('#slack-channel-creation').hide();
            },
            function(error) {
                console.log('Error: ' + error);
            },
            function() {}
        );
     };

    SlackItClient.prototype.onSlackItDialogSubmitClickedDoInvitePhase = function(slackData, channelName, guestList, channelId) {
        var _this = this;
        console.log('onSlackItDialogSubmitClickedDoInvitePhase(): channel=' + channelName + ' (id=' + channelId + ')' + ', jiraIssueKey=' + this.context.jiraIssueKey + ', guestList: ' + JSON.stringify(guestList));
        AJS.$('#status-channel-invite').show();
        AJS.$('#status-channel-invite-spinner').spin();

        // Resolve Email to slack ID
        var nbGuests = 0;
        var emailsSet = [];
        AJS.$.each(guestList, function(index, e) {
            if (emailsSet.indexOf(e.email) < 0) {
                if (slackData.users.byEmail.hasOwnProperty(e.email.toLowerCase())) {
                    var slackUser = slackData.users.byEmail[e.email.toLowerCase()];
                    var id = slackUser.id;
                    console.log('invite: ' + e.email + ' -> ' + id);
                    e.slackId = id;
                }
                nbGuests++; // include even unresolved guests
                emailsSet.push(e.email);
            }
        });

        // Send invite wave
        AJS.$('#status-channel-gueststatus').show();
        AJS.$('#status-channel-gueststatus').append('<span class="aui-icon aui-icon-small aui-iconfont-success"></span> Preparing channel ' + '<a href="' + this.context.slackBaseUrl + '/messages/' + channelName + '/" target="_blank">#' + channelName + '</a><br/>');

        this.inviteRepliesReceived = 0;
        emailsSet = [];
        AJS.$.each(guestList, function(index, user) {
            if (emailsSet.indexOf(user.email) >= 0)
                return;

            emailsSet.push(user.email);
            console.log('Send invite: ' + user.slackId);
            _this.onSlackItInvitePhaseInviteUser(_this, user, channelId, channelName, nbGuests);
        });
    };

    SlackItClient.prototype.onSlackItInvitePhaseInviteUser = function(self, user, channelId, channelName, nbGuests) {
        var _this = self;
        _this.rxClient.inviteSlackUserToChannelObservable(channelId, user.slackId, _this.context.token, _this.context.slackApiBaseUrl).subscribe(
            function(inviteData) {
                if (!inviteData.error || inviteData.error == 'already_in_channel' || inviteData.error == 'cant_invite_self') {
                    AJS.$('#status-channel-gueststatus').append('<span class="aui-icon aui-icon-small aui-iconfont-success"></span> Invite ok for ' + user.email + '<br/>');
                    console.log('Invite ok for ' + JSON.stringify(user));
                } else {
                    AJS.$('#status-channel-gueststatus').append('<span class="aui-icon aui-icon-small aui-iconfont-warning"></span> Unable to invite ' + user.email + ': ' + inviteData.error + '<br/>');
                    console.log('Error: invite KO for ' + JSON.stringify(user));
                }

                _this.inviteRepliesReceived++;
                console.log('Reply ' + _this.inviteRepliesReceived + ' / ' + nbGuests);
                if (_this.inviteRepliesReceived < nbGuests)
                    return;

                // All done, update UI
                AJS.$('#status-channel-invite-spinner').spinStop();
                AJS.$('#status-channel-invite').hide();
                AJS.$('#status-channel-gueststatus').append('<br/><span class="aui-icon aui-icon-small aui-iconfont-success"></span> <b>All done!</b><br/>');

                // notify jira servlet to save slack channelId
                _this.rxClient.linkSlackChannelToJiraObservable(_this.context.servletLinkSlackChannelURL, _this.context.jiraIssueKey, channelId, channelName).subscribe(
                    function(e) {
                        console.log('Notified backend to link ' + _this.context.jiraIssueKey + ' / ' + channelId);
                    },
                    function(error) {
                        console.log('Error: ' + error);
                        alert('Something went wrong while saving slack channel id in jira. Please reload page and try again. Error: ' + error);
                    },
                    function() {}
                );

                AJS.$('#slack-setupchannel-dialog-submit').html('Return to Jira!');
                AJS.$('#slack-setupchannel-dialog-submit').off().click(
                    function() {
                        AJS.dialog2("#slack-setupchannel-dialog").hide();
                        location.reload();
                    }
                );

                // Make current user join the channel
                _this.rxClient.joinSlackChannelObservable(channelName, _this.context.token, _this.context.slackApiBaseUrl).subscribe(
                    function(joinData) {
                        if (joinData.ok) {
                            console.log('User joined channel "#' + channelName + '" (auto join)');
                        }
                    },
                    function(error) {
                        console.log('Error: ' + error);
                    },
                    function() {}
                );
            },
            function(error) {},
            function() {}
        );
    };

    SlackItClient.prototype.onSlackItDialogDoStepLinkExistingChannel = function(slackData, channelName) {
        console.log('onSlackItDialogDoStepLinkExistingChannel(): candidate channel=' + channelName + '(id=' + slackData.channels[channelName].id + '). Ready to link up with Slack channel...');
        AJS.$('#slack-channel-useexisting-holder').show();
        AJS.$('#slackChannelFoundInput').val(channelName);

        slackData.context.skipCreateChannel = true;
        this.onSlackItDialogDoStepInviteUsers(slackData, channelName);
    };

    // SlackIt dialog - do step create channel
    SlackItClient.prototype.onSlackItDialogDoStepCreateChannel = function(slackData) {
        console.log('onSlackItDialogDoStepCreateChannel()');
        AJS.$('#slack-channel-create-holder').show();
        AJS.$('#slackChannelCreateInput').val(this.context.slackChannelPreferredName);

        slackData.context.skipCreateChannel = false;
        this.onSlackItDialogDoStepInviteUsers(slackData, this.context.slackChannelPreferredName);
    };

    SlackItClient.prototype.onSlackDataReady = function(slackData) {
        var _this = this;
        console.log('onSlackDataReady()');
        console.log('* slackChannelId: ' + this.context.slackChannelId + ', slackChannelPreferredName: ' + this.context.slackChannelPreferredName);
        slackData.context = {};

        this.context.jira_members = AJS.$.parseJSON(this.context.jira_membersStr.replace(/&quot;/g, '"'));
        console.log('jiraMembers reporter: ' + this.context.jira_members.reporter.name);

        this.toggleSlackAuthorization(false);

        // * SlackIt pannel
        AJS.$('#createSlack').hide();
        if (!slackData.channel.ok) {
            console.log('Channel info couldn\'t be queried, error: ' + slackData.channel.error + ', for channelId=' + this.context.slackChannelId);

            if ("channel_not_found" === slackData.channel.error) {
                // channel_not_found: either channel needs to be created, or a channel exists but is not linked to jira yet (no slackChannelId)
                console.log('Channel needs creation/link.');
                AJS.$("#createSlack").click(
                    function() {
                        AJS.dialog2("#slack-setupchannel-dialog").on("show", function() {
                            _this.onSlackItDialogShown(slackData);
                        });
                        AJS.dialog2("#slack-setupchannel-dialog").show();
                    });
                AJS.$('#createSlack').show();
            } else {
                console.log('Unhandled Slack error: ' + slackData.channel.error);
            }
        } else {
            console.log('Slack channel exists and is linked up already');

            var channelName = slackData.channel.channel.name;
            var channelLink = this.context.slackBaseUrl + "/messages/" + channelName + "/";
            AJS.$('#channelSlack-header-channame').html('#' + channelName);
            AJS.$('#channelSlack-header-channame').attr("href", channelLink);
            AJS.$('#channelSlack-history-seemore').attr("href", channelLink);

            if (slackData.channel.channel.hasOwnProperty('is_archived') && slackData.channel.channel.is_archived) {
                console.log('channel: is_archived');
                AJS.$('#channelSlack-header-chanstatus').html('archived');
                AJS.$('#channelSlack-header-chanstatus').addClass("aui-lozenge aui-lozenge-current");
            } else {
                AJS.$('#channelSlack-header-chanstatus').removeClass("aui-lozenge aui-lozenge-current");
            }

            var membersBody = '';
            AJS.$.each(slackData.channel.channel.members, function(index, e) {
                membersBody += '<span class="aui-label">' + slackData.users.byId[e].name + '</span>&nbsp;';
            });
            AJS.$('#channelSlack-header-chanmembers').html(membersBody);

            var body = '';
            var nbMessages = 0;
            var filteredType = ['channel_join', 'channel_leave', 'channel_archive'];
            AJS.$.each(slackData.history.messages, function(index, e) {
                if (e.type == 'message' && filteredType.indexOf(e.subtype) < 0) {
                    body += '<div class="slackMsg">';
                    body += '<span><b>' + slackData.users.byId[e.user].name + '</span></b>&nbsp;';
                    body += '<span class="slackMsgDate">' + _this.timeConverter(_this.parseSlackTSField(e)) + '</span><br/>';
                    body += e.text;
                    body += '</div>';
                    nbMessages++;
                }
            });

            if (nbMessages === 0) {
                body += '<div class="slackMsg">No messages</div>';
            }

            AJS.$('#channelSlack-history').append(body);
            AJS.$("#channelSlack").show();

            if (AJS.$('#channelSlack-history').height() > 300) {
                AJS.$('#channelSlack-history').height(300);
            }
        }

    };

    SlackItClient.prototype.onSlackDataError = function(error) {
        console.log('Listener# onSlackDataError: ' + error);
        this.toggleSlackAuthorization(true);
    };

    SlackItClient.prototype.parseSlackTSField = function(e) {
        if (e.ts !== '') {
            var n = e.ts.indexOf(".");
            if (n > -1) {
                return e.ts.substring(0, n);
            }
        }
        return '';
    };

    // Forked from http://stackoverflow.com/a/6078873
    SlackItClient.prototype.timeConverter = function(UNIX_timestamp) {
        var a = new Date(UNIX_timestamp * 1000);
        var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        var year = a.getFullYear();
        var month = months[a.getMonth()];
        var date = a.getDate();
        var hour = a.getHours() < 10 ? '0' + a.getHours() : a.getHours();
        var min = a.getMinutes() < 10 ? '0' + a.getMinutes() : a.getMinutes();
        var sec = a.getSeconds() < 10 ? '0' + a.getSeconds() : a.getSeconds();
        var time = date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec;
        return time;
    };

    SlackItClient.prototype.toggleSlackAuthorization = function(showConnect) {
        console.log('toggleSlackAuthorization(): ' + showConnect);
        if (showConnect) {
            AJS.$('#slackAuthorization').show();
            AJS.$('#slackAuthorizationSlackLogout').hide();
        } else {
            AJS.$('#slackAuthorization').hide();
            AJS.$('#slackAuthorizationSlackLogout').show();
        }
    };

    return SlackItClient;
}());
