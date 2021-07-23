package com.cicd.helper
class JiraUpdater{

    JiraUpdater(){
    }
    def update(Map args =[issueID: "CICD-13", progressLabel: "Deployed",bddReport: "Success", reportLink:"www.my_bdd.com"]){
        String issue_ID=args.issue_ID.toString()
        String progressLabel=args.progressLabel.toString()
        String bddReport=args.bddReport.toString()
        String reportLink=args.reportLink.toString()
        String body = '{\\"fields\\": {\\"customfield_10034\\":[\\"'+progressLabel+'\\"],\\"customfield_10035\\":\\"'+bddReport+'\\",\\"customfield_10036\\":\\"'+reportLink+'\\"}}'
        return "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\""
    }
}