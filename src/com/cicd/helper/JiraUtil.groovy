package com.cicd.helper
import groovy.json.JsonSlurperClassic

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
    if(isUnix()){
        sh(script: "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"\" -H \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
    }
}


def updateCommentwithBDD(){
    filename = 'cucumber-trends.json'

    if(isUnix()){
        response=bat(script:"type $filename",returnStdout: true).trim()
    }
    else{
        response=sh(script:"cat $filename",returnStdout: true).trim()
    }
    response=response.substring(response.indexOf("\n")+1).trim()

    def cucumber_json=getJSON(response)
    echo "$cucumber_json.buildNumbers"

    String comment="\\n^|"
    for(element in cucumber_json){
        comment+="^|*"+element.key.toString().trim()+"*^|"
    }
    comment+="^|\\n"
    for(element in cucumber_json){
        comment+="^|"+element.value[-1].toString().trim()
    }
    comment+="^|"
    updateComment("BDD Test Reports:\\n"+comment)
}

@NonCPS
def getJSON(response){
    def jsonSlurper = new JsonSlurperClassic()
    def cfg = jsonSlurper.parseText(response)
    jsonSlurper=null
    return cfg
}

def updateComment(text){
    String issue_ID=getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }

    String body = '{\\"body\\": \\"'+text+'\\"}'

    if(isUnix()){
        sh(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/comment\" -H \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/comment\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
    }
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

    if(isUnix()){
        commitMessage = sh(returnStdout: true, script: 'git log -1 --oneline').trim()
    }
    else{
        commitMessage = bat(returnStdout: true, script: 'git log -1 --oneline').trim()
    }
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
