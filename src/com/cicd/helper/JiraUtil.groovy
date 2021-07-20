package com.cicd.helper

import groovy.json.JsonSlurperClassic

def updateJirawithFailure(args){
    String issueID = getIssueID().toString()
    
    if(issueID.equals("")){
        issueID = createIssue(failStage: args.failStage)
        addAssignee(issue: issueID)
    }
    String commentBody="{panel:bgColor=#ffebe6}\\nBuild #${env.BUILD_NUMBER} Failed at stage: $args.failStage\\n{panel}\\n"

    if(env.UNIT_TEST_REPORT){
        //XML reports
        commentBody+="{panel:bgColor=#fffae6}\\nJunit Test Reports:\\n{panel}\\n"
        commentBody+=getXML()
    }
    else{
        commentBody+="{panel:bgColor==#fffae6}\\nUnit tests were not performed due to failure at an earlier stage\\n{panel}\\n"
    }
    
    if(env.BDD_REPORT){
        //BDD Reports
        commentBody+="{panel:bgColor=#fffae6}\\nBDD Test Reports:\\n{panel}\\n"
        //commentBody+=getBDD()
        sendAttachment(attachmentLink: "$env.BUILD_FOLDER_PATH/cucumber-html-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4", issue: issueID)
    }
    else{
       commentBody+="{panel:bgColor=#fffae6}\\nBDD tests were not performed due to failure at an earlier stage\\n{panel}\\n"
    }

    //Build Signature
    //commentBody+=getBuildSignature()
    echo "Comment: $commentBody"
    updateComment(text: commentBody,issue:issueID)
}

def updateJirawithSuccess(){
    String issueID = getIssueID().toString()
    if(issueID.equals("")){
        echo "[JiraUtil] No issue updated/ no new issue created"
        return
    }
    String commentBody="Build #${env.BUILD_NUMBER} Successful\\n"

    //XML reports
    commentBody+="{panel:bgColor=#e3fcef}\\nJunit Test Reports:\\n{panel}\\n"
    commentBody+=getXML()
    
    //BDD Reports
    commentBody+="{panel:bgColor=#e3fcef}\\nBDD Test Reports:\\n{panel}\\n"
    commentBody+=getBDD()
    sendAttachment(attachmentLink: "$env.BUILD_FOLDER_PATH/cucumber-html-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4", issue: issueID)

    //Build Signature
    //commentBody+=getBuildSignature()

    echo "Comment: $commentBody"
    updateComment(text: commentBody,issue: issueID)
}

def getBuildSignature(){
    String buildSign=""
    //Build URL
    // if(env.BUILD_URL){
    //     buildSign="Build URL: [+#$env.BUILD_NUMBER+|$env.BUILD_URL]\\n"
    // }
    // else{
    //     buildSign="Build URL: +#$env.BUILD_NUMBER+\\n"
    //     echo "[JiraUtil] Warning: Jenkins URL must be set to get BUILD_URL on Jira"
    // }
    //AccountID
    String accountId= getAccountId()[0].toString()
    String commitEmail= getAccountId()[1].toString()
    if(accountID!=""){
        buildSign+="Committed by: [~accountid:$accountId]\\n"
    }
    else{
        buildSign+="Committed by: $commitEmail\\n"
    }
    //Timestamp
    String date= new Date()
    buildSign+="Committed on: $date\\n"
    return buildSign
}

def update(Map args =[ progressLabel: "Deployed",bddReport: "Success", reportLink:"www.my_bdd.com", issue: ""]){
    
    String issue_ID = args.issue.toString()

    String body = '{\\"fields\\": {\\"customfield_10034\\":[\\"'+args.progressLabel+'\\"],\\"customfield_10035\\":\\"'+args.bddReport+'\\",\\"customfield_10036\\":\\"'+args.reportLink+'\\"}}'

    if(isUnix()){
        sh(script: "curl -g --request PUT \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"\" -H \"Authorization:" + env.AUTH_TOKEN + "\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request PUT \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
    }
}

def updateComment(Map args =[text: "", issue: ""]){

    String issue_ID = args.issue.toString()

    String body = '{\\"body\\": \\"'+args.text+'\\"}'

    if(isUnix()){
        sh(script: "curl -g --request POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/comment\" -H \"Authorization:" + env.AUTH_TOKEN + "\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/comment\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
    }
}

@NonCPS
def getJSON(response){
    def jsonSlurper = new JsonSlurperClassic()
    def cfg = jsonSlurper.parseText(response)
    jsonSlurper=null
    return cfg
}

def getBDD(Map args = [filePath: "$JENKINS_HOME\\jobs\\${env.PIPELINE_NAME}\\branches\\${env.BRANCH_NAME}\\cucumber-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4\\cucumber-trends.json", issue: ""]) {

    String issueID = args.issue.toString()
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
    
    String table_seperator=""
    if(isUnix()){
        table_seperator="^|"
    }
    else{
        table_seperator="|"
    }

    String comment=table_seperator
    for(element in cucumber_json){
        echo element.key.toString()
        comment += table_seperator+"*"+element.key.toString().trim()+"*"+table_seperator

        //comment+=table_seperator+element.value[-1].toString().trim()        
        comment+=table_seperator+"\\n"
    }
    
    comment+=table_seperator
    comment+="\\n"
    return comment
    //updateComment(text: "BDD Test Report for build #$env.BUILD_NUMBER:\\n"+comment, issue: issueID)
}

def BDDtoComment(args){
    String comment=getBDD()
    updateComment(text: comment,issue: args.issueID)
}

@NonCPS
String getXML(Map args = [path: "$env.BUILD_FOLDER_PATH/junitResult.xml"]) {
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
    comment += "\\n"
    return comment 
}

def xmlToComment(Map args = [path: "C:/", issue: ""]){

    String comment = getXML(path: args.path.toString())
    String issue_ID = args.issue.toString()

    updateComment(text: "Junit Test Reports for Build #$env.BUILD_NUMBER:\\n" + comment, issue: issue_ID)
}

def sendAttachment(Map args = [attachmentLink: "target/site/", issue: ""]) {
    
    String issue_ID = args.issue.toString()
    String link = args.attachmentLink.toString()

    if(isUnix()) {
        sh(script: "zip " + link + ".zip " + link)
        sh(script: "curl -s -i -X POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/attachments\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + link + ".zip\"")
    }
    else {
        bat(script: "powershell Compress-Archive " + link + " " + link + ".zip")
        bat(script: "curl -s -i -X POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/attachments\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + link + ".zip\"")
    }
}

def addAssignee(Map args = [issue: ""]){

    String issue_ID = args.issue.toString()
    String accountId = getAccountId()[0].toString()


    String body ='{\\"accountId\\": \\"'+accountId+'\\"}'
    if(isUnix()){
        sh(script: "curl -g --request PUT \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/assignee\" -H \"Authorization:" + env.AUTH_TOKEN + "\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request PUT \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/assignee\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"Content-Type:application/json\" --data-raw \""+body+"")
    }   
}

@NonCPS
String getAccountIdParser(response) {
    if(response.length!=0){
        def jsonSlurper = new JsonSlurperClassic()
        parse = jsonSlurper.parseText(response)
        accountId = parse.accountId[0]
        return accountId; 
    }
    else{
        return ""
    }
}


//Return Jira acountID from commitEmail, if no accountID exists return commitEmail
def getAccountId(){
    String accountId = ""
    String response = ""
    
    if(isUnix()){
        String commitEmail = sh(returnStdout: true, script: "git log -1 --pretty=format:'%ae'")
        response = sh(returnStdout: true,script:"curl --request GET \"" + env.JIRA_BOARD + "/user/search?query="+commitEmail+" \" -H \"Authorization:" + env.AUTH_TOKEN + "\"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"")
    }
    else{
        String commitEmail = bat(returnStdout: true, script: "git log -1 --pretty=format:'%%ae'")
        commitEmail=commitEmail.substring(commitEmail.indexOf(">")+1).trim()
        commitEmail=commitEmail.substring(commitEmail.indexOf("\n")+1).trim()
        commitEmail=commitEmail[1..-2]
        echo "commitEmail : $commitEmail"
        response = bat(returnStdout: true,script:"curl --request GET \"" + env.JIRA_BOARD + "/user/search?query="+commitEmail+" \" -H \"Authorization:" + env.AUTH_TOKEN + "\"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"").trim()
        response = response.substring(response.indexOf("\n")+1).trim()
    }
    response = getAccountIdParser(response)
    return [response,commitEmail]
}

@NonCPS
String parseJsonForIssueId(response) {
    String issueKey = ""
    def jsonSlurper = new JsonSlurperClassic()
    parser = jsonSlurper.parseText(response)
    issueKey = parser.key
    return issueKey; 
}

def createIssue(Map args = [failStage: ""]){
    String response =""
    String body = '{\\"fields\\": {\\"project\\":{\\"key\\": \\"' + env.ISSUE_KEY + '\\"},\\"summary\\": \\"Build #'  + env.BUILD_NUMBER + ' Failure\\",\\"description\\": \\"Build #' + env.BUILD_NUMBER + ' failed for job ' + env.JOB_NAME + ' at stage ' + args.failStage.toString() + '\\",\\"issuetype\\": {\\"name\\": \\"Bug\\"}}}'
    if(isUnix()){
        response  = sh(returnStdout: true,script: "curl -g --request POST \"" + env.JIRA_BOARD + "/issue/\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"Content-Type:application/json\" -d \""+body+"\"")   
    }
    else{
        response  = bat(returnStdout: true,script: "curl -g --request POST \"" + env.JIRA_BOARD + "/issue/\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"").trim()
        response = response.substring(response.indexOf("\n")+1).trim()
    }
    return parseJsonForIssueId(response) 
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

