package com.cicd.helper

def update(Map args =[ progressLabel: "Deployed",bddReport: "Success", reportLink:"www.my_bdd.com"]){
    String issue_ID=getIssueID().toString()

    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }

    String body = '{\\"fields\\": {\\"customfield_10034\\":[\\"'+args.progressLabel+'\\"],\\"customfield_10035\\":\\"'+args.bddReport+'\\",\\"customfield_10036\\":\\"'+args.reportLink+'\\"}}'
    bat(script: "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
}

def updateComment(Map args =[text: "www.google.com"]){
    String issue_ID=getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }

    String body = '{\\"body\\": \\"If you can see me... We did it! Text: '+args.text+'\\"}'
    bat(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/comment\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
}

def addLink(Map args =[text: http://localhost:8081/job/jenkins-pipeline-cucumber-example/job/feature-test-pipeline-cucumber/2/cucumber-html-reports/overview-features.html]){
    String issue_ID=getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }

    String body ='{\\"object\\":{\\"url\\":\\"'+args.text+'\\",\\"title\\":\\"bdd report\\"}}'
    bat(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/remotelink\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"")
}
                       
def addAssignee(Map args =[text: 60dbed7c285656006a7a6927]){
    String issue_ID=getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }

    String body ='{\\"accountId\\": \\"'+args.text+'\\"}'
    bat(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/assignee\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"")
}

def getIssueID(){
    String issueKey="CICD"
    String branchName=env.BRANCH_NAME;
    String prTitle=env.CHANGE_TITLE;
    String commitMessage=""
    String jiraIssue=""
    Boolean isIssueMentioned=true
    int issueKeyStart=0
    int issueKeyEnd=0

    commitMessage = bat(returnStdout: true, script: 'git log -1 --oneline').trim()

    issueKeyStart=branchName.indexOf(issueKey)
    issueKeyEnd=issueKeyStart
    //Search for issueKey in branchName, if available find exact issueID, else search in commitMesssage 
    if(issueKeyStart!=-1){
        issueKeyEnd=branchName.indexOf('-',issueKeyStart)+1
        while(issueKeyEnd<branchName.length() && branchName[issueKeyEnd].matches("[0-9]")){
            issueKeyEnd++ 
        }
        jiraIssue=branchName.substring(issueKeyStart,issueKeyEnd)
    }
    //Search for issueKey in commitMessage, if available find exact issueID, else search in prTitle 
    else if(commitMessage.indexOf(issueKey)!=-1){
            issueKeyStart=commitMessage.indexOf(issueKey)
            issueKeyEnd=issueKeyStart
            if(issueKeyStart!=-1){
                issueKeyEnd=commitMessage.indexOf('-',issueKeyStart)+1
                while(issueKeyEnd<commitMessage.length() && commitMessage[issueKeyEnd].matches("[0-9]")){
                    issueKeyEnd++ 
                }
                jiraIssue=commitMessage.substring(issueKeyStart,issueKeyEnd)
            }
    }
    //Search for issueKey in prTitle, if available find exact issueID, else no issueID mentioned 
    else{
        if(prTitle!=null){
            issueKeyStart=prTitle.indexOf(issueKey)
            issueKeyEnd=issueKeyStart
            if(issueKeyStart!=-1){
                issueKeyEnd=prTitle.indexOf('-',issueKeyStart)+1
                while(issueKeyEnd<prTitle.length() && prTitle[issueKeyEnd].matches("[0-9]")){
                    issueKeyEnd++ 
                }
                jiraIssue=prTitle.substring(issueKeyStart,issueKeyEnd)
            }
            else{
                //No issue mentioned
                isIssueMentioned=false
                
            }
        }
    }
    return jiraIssue;
}

def sh(args){
    return sh(args.script)
}
