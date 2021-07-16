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

def updateComment(Map args =[text: "www.google.com"]){
    String issue_ID=getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }

    String body = '{\\"body\\": \\"'+args.text+'\\"}'

    if(isUnix()){
        sh(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/comment\" -H \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/comment\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
    }
}

@NonCPS
def getJSON(response){
    def jsonSlurper = new JsonSlurperClassic()
    def cfg = jsonSlurper.parse(response)
    jsonSlurper=null
    return cfg
}

def updateCommentwithBDD(Map args = [filePath: "C:/"]) {
    filename = args.filePath.toString()

    if(isUnix()){
        response=sh(script:"cat $filename",returnStdout: true).trim()
    }
    else{
        response=bat(script:"type $filename",returnStdout: true).trim()
        echo response
        response=response.substring(response.indexOf("\n")+1).trim()
        echo response
    }

    def cucumber_json=getJSON(response)
    echo cucumber_json
    String table_seperator=""
    if(isUnix()){
        table_seperator="^|"
    }
    else{
        table_seperator="|"
    }

    String comment=table_seperator
    for(element in cucumber_json){
        comment += table_seperator+"*"+element.key.toString().trim()+"*"+table_seperator

        for(e in element.value) {
            comment+=table_seperator+e.toString().trim()
        }
        
        comment+=table_seperator+"\\n"
    }
    
    comment+=table_seperator
    updateComment("BDD Test Reports:\\n"+comment)
}

@NonCPS
String getXML(Map args = [path: ""]) {
    String xmlPath = args.path.toString()

    def xml = new XmlSlurper().parse(xmlPath) 

    String table_seperator = ""
    if(isUnix()) {
        table_seperator = "|"
    }
    else{
        table_seperator = "^|"
    }

    String comment = "\\n" + table_seperator
    comment += table_seperator + "*Class Name*" + table_seperator 
    comment += table_seperator + "*Test Name*" + table_seperator 
    comment += table_seperator + "*Skipped*" + table_seperator 
    comment += table_seperator + "*Failed Since*" + table_seperator

    xml.suites.suite.cases.case.each{
        c-> 
        comment += "\\n" + table_seperator
        comment += table_seperator + c.className[0].text().toString() + table_seperator
        comment += table_seperator + c.testName[0].text().toString() + table_seperator
        comment += table_seperator + c.skipped[0].text().toString() + table_seperator
        comment += table_seperator + c.failedSince[0].text().toString() + table_seperator


    } 
    comment += table_seperator
}

def xmlToComment(Map args = [path: "C:/"]){

    String comment = getXML(path: args.path.toString())

    updateComment(text: "Junit Test Reports:\\n" + comment)
}

def sendAttachment(Map args = [attachmentLink: "target/site/"]) {
    String issue_ID = getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }
    String link = args.attachmentLink.toString()

    if(isUnix()) {
        sh(script: "zip " + link + ".zip " + link)
        sh(script: "curl -s -i -X POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/attachments\" --header \"Authorization:Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + link + ".zip\"")
    }
    else {
        bat(script: "powershell Compress-Archive " + link + " " + link + ".zip")
        bat(script: "curl -s -i -X POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/attachments\" --header \"Authorization:Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + link + ".zip\"")
    }
}

def addAssignee(){
    String issue_ID=getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }
    String accountId = getAccountId().toString()

    String body ='{\\"accountId\\": \\"'+accountId+'\\"}'
    if(isUnix()){
        sh(script: "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/assignee\" -H \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/assignee\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"")
    }   
}

@NonCPS
def getAccountId(){
    String accountId = ""
    String response = ""
    //String commitEmail = "shantanud390@gmail.com"
    if(isUnix()){
        String commitEmail = sh(returnStdout: true, script: "git log -1 --pretty=format:'%ae'")
        response = sh(returnStdout: true,script:"curl --request GET \"https://mstale-test.atlassian.net/rest/api/latest/user/search?query="+commitEmail+" \" -H \"Authorization:Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==  \"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"")
    }
    else{
        String commitEmail = bat(returnStdout: true, script: "git log -1 --pretty=format:'%ae'")
        response = bat(returnStdout: true,script:"curl --request GET \"https://mstale-test.atlassian.net/rest/api/latest/user/search?query="+commitEmail+" \" -H \"Authorization:Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA== \"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"").trim()
        response = response.substring(response.indexOf("\n")+1).trim()
    }                  
    def jsonSlurper = new JsonSlurperClassic()
    parse = jsonSlurper.parseText(response)
    accountId = parse.accountId[0]
    return accountId; 
}

@NonCPS
def createIssue(){
    def jsonSlurper = new JsonSlurperClassic()
    String issueKey = ""
    String response =""
    String body = '{\\"fields\\": {\\"project\\":{\\"key\\": \\"CICD\\"},\\"summary\\": \\"New Issue Created.\\",\\"description\\": \\"Creating of an issue using project keys and issue type names using the REST API\\",\\"issuetype\\": {\\"name\\": \\"Bug\\"}}}'
    if(isUnix()){
        response  = sh(returnStdout: true,script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA== \" --header \"Content-Type:application/json\" -d \""+body+"\"")   
    }
    else{
        response  = bat(returnStdout: true,script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA== \" --header \"Content-Type:application/json\" --data-raw \""+body+"\"").trim()
        response = response.substring(response.indexOf("\n")+1).trim()
    }
    parser = jsonSlurper.parseText(response)
    issueKey = parser.key
    return issueKey;
    
} 

def getIssueID(){
    String issueKey = env.ISSUE_KEY 
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

