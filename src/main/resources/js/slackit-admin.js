function reloadFile() {
    AJS.log("Calling reload on REST endpoint");
    AJS.$.getJSON("/rest/slackit/latest/admin/reloadProperties", function(json) {
        AJS.log("Got response =>");
        AJS.log(json);
        AJS.messages.info("#action-message", {
               title: json.title,
               body: json.message
            });
    });
}
