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

    def updateComment(Map args =[issueID: "CICD-13", text: "Build Failure"]){
        String issue_ID=args.issue_ID.toString()
        String text=args.text.toString()
        String body = '{\\"body\\": {\\"type\\": \\"doc\\",\\"version\\": 1,\\"content\\": [{\\"type\\": \\"paragraph\\",\\"content\\": [{\\"text\\": \\"'+text+'\\" ,\\"type\\": \\"text\\"}]}]}}'
        return "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/comment\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\""
    }

    def sendAttachment(Map args = [issueID: "CICD-10", attachmentLink: "www.my_bdd.com"]) {
    //     return "curl -s -i -X POST \\
    //  -H \"Authorization:Basic c2hhbnRhbnVkMzkwQGdtYWlsLmNvbTo2YUpLV1VLTzN0bkR6SUZKNE5BRDdBNDE=\" \\
    //  -H \"X-Atlassian-Token:no-check\" \\
    //  -F \"file=@C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/jenkins-pipeline-cucumber-example/branches/feature-test-pipeline-cucumber/builds/2/cucumber-html-reports/report-feature_3809222109.html\" \\
    //  \"https://mstale-test.atlassian.net/rest/api/latest/issue/\" + issueID +\"/attachments\""
        String issue_ID = args.issueID.toString()
        return "curl -s -i -X POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/attachments\" --header \"Authorization:Basic c2hhbnRhbnVkMzkwQGdtYWlsLmNvbTo2YUpLV1VLTzN0bkR6SUZKNE5BRDdBNDE=\" --header \"X-Atlassian-Token:no-check\" --form \"file=@C:/creport33.html\""
    }
}