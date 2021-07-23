package com.cicd.helper

import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic

def updateJirawithFailure(args){
    String issueID = getIssueID().toString()
    
    if(issueID.equals("")){
        issueID = createIssue(failStage: args.failStage)
        echo issueID
        addAssignee(issue: issueID)
    }

    String buildNumberWithLink=getBuildNumberWithLink()
    String commentBody="{panel:bgColor=#ffebe6}\\nBuild $buildNumberWithLink Failed at stage: $args.failStage\\n{panel}\\n"
 
    if(args.unitTestReport == true){
        //XML reports
        if(args.passedUT == true) {
            commentBody+="{panel:bgColor=#e3fcef}\\nJunit Test Reports:\\n{panel}\\n"
        }
        else {
            commentBody+="{panel:bgColor=#fffae6}\\nJunit Test Reports:\\n{panel}\\n"
        }
        commentBody+=getXML()
    }
    else{
        commentBody+="{panel:bgColor==#fffae6}\\nUnit tests were not performed due to failure at an earlier stage\\n{panel}\\n"
    }
    
    if(args.bddReport == true){
        //BDD Reports
        if(args.passedBDD == true) {
            commentBody+="{panel:bgColor=#e3fcef}\\nBDD Test Reports:\\n{panel}\\n"
        }
        else {
            commentBody+="{panel:bgColor=#fffae6}\\nBDD Test Reports:\\n{panel}\\n"
        }
        commentBody+=getBDD()
        sendAttachment(issue: issueID)
    }
    else{
       commentBody+="{panel:bgColor==#fffae6}\\nBDD tests were not performed due to failure at an earlier stage\\n{panel}\\n"
    }

    //Build Signature
    commentBody+=getBuildSignature()

    updateComment(text: commentBody, issue:issueID)
}

def updateJirawithSuccess(){
    String issueID = getIssueID().toString()
    if(issueID.equals("")){
        echo "[JiraUtil] No issue updated/ no new issue created"
        return
    }
    String buildNumberWithLink=getBuildNumberWithLink()
    String commentBody="{panel:bgColor=#e3fcef}\\nBuild $buildNumberWithLink Successful\\n{panel}\\n"

    //XML reports
    commentBody+="{panel:bgColor=#e3fcef}\\nJunit Test Reports:\\n{panel}\\n"
    commentBody+=getXML()
    
    //BDD Reports
    commentBody+="{panel:bgColor=#e3fcef}\\nBDD Test Reports:\\n{panel}\\n"
    commentBody+=getBDD()
    sendAttachment( issue: issueID)

    //Build Signature
    commentBody+=getBuildSignature()

    updateComment(text: commentBody,issue: issueID)
}

def getBuildNumberWithLink(){
    String buildNumberWithLink=""
    if(env.BUILD_URL){
        if(isUnix()){
            buildNumberWithLink="[+#$env.BUILD_NUMBER+|$env.BUILD_URL]"
        }
        else{
            buildNumberWithLink="[+#$env.BUILD_NUMBER+^|$env.BUILD_URL]"
        }
        
    }
    else{
        buildNumberWithLink="+#$env.BUILD_NUMBER+\\n"
        echo "[JiraUtil] Warning: Jenkins URL must be set to get BUILD_URL on Jira"
    }
    return buildNumberWithLink
}

def getBuildSignature(){
    String buildSign=""

    //AccountID
    String accountId= getAccountId().toString()
    String commitEmail= getCommitEmail().toString()
    String date= new Date()

    if(accountId!=""){
        buildSign+="Committed by: [~accountid:$accountId] on $date\\n"
    }
    else{
        buildSign+="Committed by: $commitEmail on $date\\n"
    }

    return buildSign
}

def updateComment(Map args =[text: "", issueID: ""]){

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
def getJSON(filePath){
    def jsonSlurper = new JsonSlurperClassic()
    //Requires Danger approvals
    //File file= new File(filePath)
    //def cfg = jsonSlurper.parse(file,"UTF-8")
    def cfg = jsonSlurper.parseText(response)
    jsonSlurper=null
    return cfg
}

def getBDD(Map args = [filePath: "\"$JENKINS_HOME\\jobs\\${env.PIPELINE_NAME}\\branches\\${env.NEW_BRANCH_NAME}\\cucumber-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4\\cucumber-trends.json\"", issue: ""]) {

    String issueID = args.issue.toString()
    filename = args.filePath.toString()

    if(isUnix()){
        response=sh(script:"cat $filename",returnStdout: true).trim()
    }
    else{
        response=bat(script:"type $filename",returnStdout: true).trim()
        response=response.substring(response.indexOf("\n")+1).trim()
    }

    def cucumber_json=getJSON(response)

    String table_seperator=""
    if(isUnix()){
        table_seperator="|"
    }
    else{
        table_seperator="^|"
    }

    String comment=table_seperator
    for(element in cucumber_json){
            comment+=table_seperator+"*"+element.key.toString().trim()+"*"+table_seperator

            comment += table_seperator + element.value[-1].toString().trim() + table_seperator
            comment += "\\n"
    }
    
    comment+="\\n"
    return comment
    //updateComment(text: "BDD Test Report for build #$env.BUILD_NUMBER:\\n"+comment, issue: issueID)
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

def sendAttachment(Map args = [ issue: ""]) {
    
    String issue_ID = args.issue.toString()

    if(isUnix()) {
        sh(script: "zip " + env.BRANCH_NAME + "-BDD-Report-Build-" + env.BUILD_NUMBER + ".zip " + link + "$env.BUILD_FOLDER_PATH/cucumber-html-reports**")
        sh(script: "curl -s -i -X POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/attachments\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + env.BRANCH_NAME + "-BDD-Report-Build-" + env.BUILD_NUMBER + ".zip\"")
    }
    else {
        bat(script: "powershell Compress-Archive " + link + "$env.BUILD_FOLDER_PATH/cucumber-html-reports** " + env.BRANCH_NAME + "-BDD-Report-Build-" + env.BUILD_NUMBER + ".zip")
        bat(script: "curl -s -i -X POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/attachments\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + env.BRANCH_NAME + "-BDD-Report-Build-" + env.BUILD_NUMBER + ".zip\"")
    }
}

def addAssignee(Map args = [issue: ""]){

    String issue_ID = args.issue.toString()
    String accountId = getAccountId().toString()


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
    def jsonSlurper = new JsonSlurperClassic()
    parse = jsonSlurper.parseText(response)
    accountId = parse.accountId[0]
    return accountId; 
}

def getCommitEmail() {
    
    String commitEmail

    if(isUnix()) {
        commitEmail = sh(returnStdout: true, script: "git log -1 --pretty=format:'%ae'")
        echo "isUnix $commitEmail"
    }
    else {
        commitEmail = bat(returnStdout: true, script: "git log -1 --pretty=format:'%%ae'")
    }
    echo "email $commitEmail"
    return commitEmail
}

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
    
    return getAccountIdParser(response)
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
    String issueID=getIssueFromNamingConvention()
    if(issueID.equals("")){
        issueID=getIssueFromJenkinsfile()
    }
    return issueID
}

def getIssueFromJenkinsfile(){
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

def getIssueFromNamingConvention(){
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
        commitMessage=commitMessage.substring(commitMessage.indexOf("\n")+1).trim()
        commitMessage=commitMessage.substring(commitMessage.indexOf(" ")+1).trim()
    }

    //Example:
    //feature/CICD-13-feature-description
    //If "/" is found start from next index, else start from index 0
    issueKeyStart=branchName.indexOf("/")
    if(issueKeyStart==-1){
        issueKeyStart=0
    }
    else{
        issueKeyStart++
    }

    //Check for IssueID in branchName
    jiraIssue=checkForIssueIdRegex(message: branchName,startIndex: issueKeyStart)
    if(!jiraIssue.equals("")){
        return jiraIssue
    }
    //Check for IssueID in commitMessage
    else{
        jiraIssue=checkForIssueIdRegex(message: commitMessage,startIndex: 0)
        return jiraIssue
    }
}

//Check for pattern [A-Z]+-[0-9]+ (i.e.: issueKey-issueNumber) from given startindex
def checkForIssueIdRegex(Map args=[message:"",startIndex: 0]){
    int issueKeyStart=args.startIndex
    int issueKeyEnd=issueKeyStart
    String jiraIssue=""
    while(issueKeyEnd<args.message.length() && args.message[issueKeyEnd].matches("[A-Z]")){
        issueKeyEnd++
    }
    //If no capital letters found
    if(issueKeyEnd==issueKeyStart || args.message[issueKeyEnd]!="-"){
        return ""
    }
    //Skip "-"
    issueKeyEnd++
    Boolean isNumberPresent=false
    while(issueKeyEnd<args.message.length() && args.message[issueKeyEnd].matches("[0-9]")){
        isNumberPresent=true
        issueKeyEnd++
    }
    if(isNumberPresent){
        jiraIssue=args.message.substring(issueKeyStart,issueKeyEnd)
        return jiraIssue
    }
    else{
        return ""
    }
}