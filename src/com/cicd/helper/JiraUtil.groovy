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

def xmlToComment(Map args = [path: "C:/"]){
    String xmlPath = args.path.toString()

    def xmlFile = readFile xmlPath
    def xml = new XmlParser().parseText(xmlFile) 
    echo "Here"
   // for(c in xml.result.suites.suite.cases) {
       // echo xml.result.suites[0].cases[0]
   // }
    echo xml.result.suites.suite[0].name.text()
    // filename = 'cucumber-trends.json'
    // response=bat(script:"type $filename",returnStdout: true).trim()
    // response=response.substring(response.indexOf("\n")+1).trim()

    // def cucumber_json=getJSON(response)
    // echo "$cucumber_json.buildNumbers"

    // String comment="\\n^|"
    // for(element in cucumber_json){
    //     comment+="^|*"+element.key.toString().trim()+"*^|"
    // }
    // comment+="^|\\n"
    // for(element in cucumber_json){
    //     comment+="^|"+element.value[-1].toString().trim()
    // }
    // comment+="^|"
    // updateComment("BDD Test Reports:\\n"+comment)
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

    bat(script: "powershell Compress-Archive " + link + " " + link + ".zip")
    bat(script: "curl -s -i -X POST \"https://mstale-test.atlassian.net/rest/api/latest/issue/"+issue_ID+"/attachments\" --header \"Authorization:Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"X-Atlassian-Token:no-check\" --form \"file=@" + link + ".zip\"")
}

def addAssignee(Map args =[text: "60dbed7c285656006a7a6927"]){
    String issue_ID=getIssueID().toString()
    if(!issueID.equals("")){
        echo "IssueId found: $issueID"
    }
    else{
        echo "No issueID found!"
        return
    }
    String acc = args.text.toString()
    String body ='{\\"accountId\\": \\"' + acc + '\\"}'
    bat(script: "curl -g --request PUT \"https://mstale-test.atlassian.net/rest/api/latest/issue/" +issue_ID+"/assignee\" --header \"Authorization: Basic bXN0YWxlMjBAZ21haWwuY29tOkhKbFRSQ1B3YmRHMnhabVBIbnhPQUEyRA==\" --header \"Content-Type:application/json\" --data-raw \""+body+"")
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
