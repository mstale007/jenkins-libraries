package com.cicd.helper
import groovy.json.JsonSlurperClassic

def update(Map args =[ progressLabel: "Deployed",bddReport: "Success", reportLink:"www.my_bdd.com"]){
    String issueID=getIssueID().toString()

    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }

    String body = '{\\"fields\\": {\\"customfield_10034\\":[\\"'+args.progressLabel+'\\"],\\"customfield_10035\\":\\"'+args.bddReport+'\\",\\"customfield_10036\\":\\"'+args.reportLink+'\\"}}'
    if(isUnix()){
        sh(script: "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issueID+"\" -H \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issueID+"\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
    }
}


def updateCommentwithBDD(){
    filename = 'cucumber-trends.json'

    if(isUnix()){
        response=sh(script:"cat $filename",returnStdout: true).trim()
        echo "$response"
    }
    else{
        response=bat(script:"type $filename",returnStdout: true).trim()
        response=response.substring(response.indexOf("\n")+1).trim()
    }

    def cucumber_json=getJSON(response)
    echo "$cucumber_json.buildNumbers"

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
    }
    comment+=table_seperator+"\\n"
    for(element in cucumber_json){
        comment+=table_seperator+element.value[-1].toString().trim()
    }
    comment+=table_seperator
    updateComment("{panel:bgColor=#e3fcef}\\nBDD Test Reports:\\n{panel}\\n"+comment)
}

@NonCPS
def getJSON(response){
    def jsonSlurper = new JsonSlurperClassic()
    def cfg = jsonSlurper.parseText(response)
    jsonSlurper=null
    return cfg
}

def updateComment(text){
    String issueID=getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }

    String body = '{\\"body\\": \\"'+text+'\\"}'

    if(isUnix()){
        sh(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issueID+"/comment\" -H \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" -H \"Content-Type:application/json\" -d \""+body+"\"")
    }
    else{
        bat(script: "curl -g --request POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issueID+"/comment\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"\"")
    }
}

def updateCommentwithFailMessage(stageName){
    updateComment("{panel:bgColor=#ffebe6}\\nBuild Failed at stage: $stageName\\n{panel}\\n")
}

def updateCommentwithCommitterMention(){
    String accountId=getAccountId()
    updateComment("Committed by: [~accountid:$accountId]")
}

def updateCommentwithTimestamp(){
    String date= new Date()
    echo "Committed on: $date"
    updateComment("Committed on: $date")
}

def getAccountId(){
    String accountId = ""
    String response = ""
    //String commitEmail = "shantanud390@gmail.com"
    if(isUnix()){
        String commitEmail = sh(returnStdout: true, script: "git log -1 --pretty=format:'%ae'")
        response = sh(returnStdout: true,script:"curl --request GET \"https://mstale-test.atlassian.net/rest/api/latest/user/search?query="+commitEmail+" \" -H \"Authorization:Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==  \"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"")
    }
    else{
        String commitEmail = bat(returnStdout: true, script: "git log -1 --pretty=format:'%%ae'")
        commitEmail=commitEmail.substring(commitEmail.indexOf(">")+1).trim()
        commitEmail=commitEmail.substring(commitEmail.indexOf("\n")+1).trim()
        commitEmail=commitEmail[1..-2]
        response = bat(returnStdout: true,script:"curl --request GET \"https://mstale-test.atlassian.net/rest/api/latest/user/search?query="+commitEmail+" \" -H \"Authorization:Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA== \"  -H \"Accept: application/json \" -H \"Content-Type: application/json\"").trim()
        response = response.substring(response.indexOf("\n")+1).trim()
    }                  
    def jsonSlurper = new JsonSlurperClassic()
    parse = jsonSlurper.parseText(response)
    accountId = parse.accountId[0]
    return accountId; 
}


def getIssueID(){
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
    if(jiraIssue!=""){
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
