package com.cicd.helper

import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic

//Called in case of build failure. Updates failure status and reports to an existing JIRA issue (if mentioned) or creates 
//a new issue (if issue ID not mentioned)
def updateJirawithFailure(args){
    String issueID = getIssueID().toString()
    
    if(issueID.equals("")){
        issueID = createIssue(failStage: args.failStage)
        echo issueID
        addAssignee(issue: issueID)
    }
    else if(checkIssueExist(issue: issueID) == false) {
        echo "Issue doesn't exist"
        return
    }

    changeIssueStatus(issue: issueID)

    String buildNumberWithLink=getBuildNumberWithLink()
    String commentBody="{panel:bgColor=#ffebe6}\\nBuild $buildNumberWithLink Failed at stage: $args.failStage [$env.PIPELINE_NAME@$env.BRANCH_NAME]\\n{panel}\\n"
 
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
        
        if(args.sendAttachment){
            sendAttachment( issue: issueID, filePath: "$env.BUILD_FOLDER_PATH/builds/$env.BUILD_NUMBER/cucumber-html-reports**")
        }
    }
    else{
       commentBody+="{panel:bgColor==#fffae6}\\nBDD tests were not performed due to failure at an earlier stage\\n{panel}\\n"
    }

    //Build Signature
    commentBody+=getBuildSignature()

    updateComment(text: commentBody, issue:issueID)
}

//Called in case of build success. Updates success status and reports to an existing JIRA issue (if mentioned)
def updateJirawithSuccess(args){
    String issueID = getIssueID().toString()
    if(issueID.equals("")){
        echo "[JiraUtil] No issue updated/ no new issue created"
        return
    }
    else if(checkIssueExist(issue: issueID) == false){
        echo "[JiraUtil] Issue $issueID does'nt exists, Invalid issueID mentioned"
        return
    }
    String buildNumberWithLink=getBuildNumberWithLink()
    String commentBody="{panel:bgColor=#e3fcef}\\nBuild $buildNumberWithLink Successful [$env.PIPELINE_NAME@$env.BRANCH_NAME]\\n{panel}\\n"

    //XML reports
    commentBody+="{panel:bgColor=#e3fcef}\\nJunit Test Reports:\\n{panel}\\n"
    commentBody+=getXML()
    
    //BDD Reports
    commentBody+="{panel:bgColor=#e3fcef}\\nBDD Test Reports:\\n{panel}\\n"
    commentBody+=getBDD()

    if(args.sendAttachment){
        sendAttachment( issue: issueID, filePath: "$env.BUILD_FOLDER_PATH/builds/$env.BUILD_NUMBER/cucumber-html-reports**")
    }

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

//Fetches commit details of the latest commit to the Spring Boot application. These details are updated on JIRA in the form of 
//comments.
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

//Adds a comment to the JIRA issue linked to the given issue ID.
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

def getBDD(Map args = [filePath: env.BUILD_FOLDER_PATH, issue: ""]) {

    String issueID = args.issue.toString()
    String fileName = args.filePath.toString()

    if(isUnix()){
        response=sh(script:"cat \"$fileName\\cucumber-reports**\\cucumber-trends.json\"",returnStdout: true).trim()
    }
    else{
        folderName = bat(script:"dir \"$fileName\\cucumber-reports**\" /b",returnStdout: true).trim()
        folderName=folderName.substring(folderName.indexOf("\n")+1).trim()
        if(folderName.equals("File Not Found")){
            echo "[JiraUtil] Cucmber reports File not found"
        }
        else{
            fileName = "\"$fileName\\$folderName\\cucumber-trends.json\""
        }
        response=bat(script:"type $fileName",returnStdout: true).trim()
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

//Read and parse an XML file. Used to parse Junit Test Report XML file
@NonCPS
String getXML(Map args = [path: "$env.BUILD_FOLDER_PATH/builds/$env.BUILD_NUMBER/junitResult.xml"]) {
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

//Attaches a zip file to the mentioned issue as an attachment. Used to add BDD Report as an attachment to the issue.
def sendAttachment(Map args = [ issue: "", filePath: "$env.BUILD_FOLDER_PATH/builds/$env.BUILD_NUMBER/cucumber-html-reports**"]) {
    
    String issue_ID = args.issue.toString()
    String fileName = args.filePath.toString()

    if(isUnix()) {
        sh(script: "zip " + env.NEW_BRANCH_NAME + "-BDD-Report-Build-" + env.BUILD_NUMBER + ".zip " + fileName)
        sh(script: "curl -s -i -X POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/attachments\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + env.NEW_BRANCH_NAME + "-BDD-Report-Build-" + env.BUILD_NUMBER + ".zip\"")
    }
    else {
        bat(script: "powershell Compress-Archive " + fileName + "  " + env.NEW_BRANCH_NAME + "-BDD-Report-Build-" + env.BUILD_NUMBER + ".zip")
        bat(script: "curl -s -i -X POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/attachments\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + env.NEW_BRANCH_NAME + "-BDD-Report-Build-" + env.BUILD_NUMBER + ".zip\"")
    }
}

//Changes the status of a ???Done??? JIRA issue to ???In Progress??? in case of pipeline build failure.
def changeIssueStatus(Map args = [issue: ""]){
    String issue_ID = args.issue.toString()
    String status = ""
    String response = ""
    if(isUnix()){
        response = sh(returnStdout: true,script:"curl --request GET \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"?fields=status \" -H \"Authorization:" + env.AUTH_TOKEN + " \"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"").trim()
    }
    else{
        response = bat(returnStdout: true,script:"curl --request GET \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"?fields=status \" -H \"Authorization:" + env.AUTH_TOKEN + " \"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"").trim()
        response = response.substring(response.indexOf("\n")+1).trim()
    }
    def jsonSlurper = new JsonSlurperClassic()
    parse = jsonSlurper.parseText(response)
    status = parse.fields.status.name
    if(status.equals("Done")){
        String body ='{\\"transition\\": {\\"id\\": \\"21\\"}}'
        if(isUnix()){
            sh(returnStdout: true,script: "curl -g --request POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/transitions \" --header \"Authorization:" + env.AUTH_TOKEN + " \" --header \"Content-Type:application/json\" -d \""+body+"\"")
        }
        else{
            bat(script: "curl -g --request POST \"" + env.JIRA_BOARD + "/issue/"+issue_ID+"/transitions \"  -H \"Authorization:" + env.AUTH_TOKEN + " \" --header \"Content-Type:application/json\" --data-raw \""+body+"")
        }
    }
}

//Checks if the given JIRA issue exists on JIRA.
def checkIssueExist(Map args = [issue: ""]){
     String issue_ID = args.issue.toString()    
     boolean issueExist
     String response = ""
     if(isUnix()){
        response = sh(returnStdout: true, script: "curl --request GET \"" + env.JIRA_BOARD + "/issue/"+issue_ID+" \" -H \"Authorization:" + env.AUTH_TOKEN + " \"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"").trim()
     }
     else{
        response = bat(returnStdout: true, script: "curl --request GET \"" + env.JIRA_BOARD + "/issue/"+issue_ID+" \" -H \"Authorization:" + env.AUTH_TOKEN + " \"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"").trim()
        response = response.substring(response.indexOf("\n")+1).trim()
     }
    response = response.substring(19,39)
    if(response.equals("Issue does not exist")){
        issueExist = false
    }
    else{
        issueExist = true
    }
        
    return issueExist;
}

//Assigns issue denoted by an issue ID to the user that last committed to the Spring Boot application. Used in case of 
//creation of a new issue.
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

//Finds the email ID of the user that made the last commit to the Spring Boot application.
def getCommitEmail() {
    
    String commitEmail

    if(isUnix()) {
        commitEmail = sh(returnStdout: true, script: "git log -1 --pretty=format:'%ae'")
    }
    else {
        commitEmail = bat(returnStdout: true, script: "git log -1 --pretty=format:'%%ae'")
    }
    return commitEmail
}

//Finds the JIRA account of the user from their email ID.
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

//Creates a new JIRA issue with appropriate title and description. Called when a build fails and no issue ID is found.
def createIssue(Map args = [failStage: ""]){

    if(env.JIRA_PROJECT_KEY == null || env.JIRA_PROJECT_KEY.equals("")) {
        echo "[JiraUtil] Project key not found"
        return
    }

    String response =""
    String body = '{\\"fields\\": {\\"project\\":{\\"key\\": \\"' + env.JIRA_PROJECT_KEY + '\\"},\\"summary\\": \\"Build #'  + env.BUILD_NUMBER + ' Failure\\",\\"description\\": \\"Build #' + env.BUILD_NUMBER + ' failed for job ' + env.JOB_NAME + ' at stage ' + args.failStage.toString() + '\\",\\"issuetype\\": {\\"name\\": \\"Bug\\"}}}'
    if(isUnix()){
        response  = sh(returnStdout: true,script: "curl -g --request POST \"" + env.JIRA_BOARD + "/issue/\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"Content-Type:application/json\" -d \""+body+"\"")   
    }
    else{
        response  = bat(returnStdout: true,script: "curl -g --request POST \"" + env.JIRA_BOARD + "/issue/\" --header \"Authorization:" + env.AUTH_TOKEN + "\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"").trim()
        response = response.substring(response.indexOf("\n")+1).trim()
    }
    return parseJsonForIssueId(response) 
} 

// Checks for JIRA issue ID mentioned in branch name or commit messaage by Naming convention first, If not found, check with help of Project Key mentioned JenkinsFile, else return null.
def getIssueID(){
    String issueID=getIssueFromNamingConvention()
    if(issueID.equals("")){
        issueID=getIssueFromJenkinsFile()
    }
    return issueID
}

//Fetches JIRA issue ID mentioned in the branch name or commit message from project key mentioned in JenkinsFile. If no issue ID is found, returns null.
def getIssueFromJenkinsFile(){
    String issueKey = env.JIRA_PROJECT_KEY 
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

// Fetches JIRA issue ID mentioned in the branch name or commit message (using regex only) if naming convention is followed  
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
        commitMessage=commitMessage.substring(commitMessage.indexOf(" ")+1).trim()
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