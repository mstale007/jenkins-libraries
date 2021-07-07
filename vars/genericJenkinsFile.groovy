import com.cicd.helper.JiraUtil

def call(Map args =[buildMode: "mvn",jira_issue: ""]){
    def jiraUtil= new JiraUtil()
    String issueKey="CICD"
    String branchName = env.BRANCH_NAME
    String prTitle = env.CHANGE_TITLE
    String commitMessage=""
    String jiraIssue=""
    Boolean isIssueMentioned=true
    int issueKeyStart=0
    int issueKeyEnd=0

    pipeline{
        agent any

        //options{}

        //parameters{}

        //environment{}

        stages{
            stage("Initialize"){
                steps{
                    script{
                        //Get commitMessage
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
                    }
                    echo "Branch name is: $env.BRANCH_NAME"
                    echo "Commit Message: $commitMessage"
                    echo "PR Tile: $prTitle"
                    echo "IssueID: $jiraIssue"
                    echo "Intializing..!"
                    bat "set"
                }
                post{
                    success{
                       echo "Success"
                    }
                    failure{
                        echo "JIRA: Initialize Failed"
                    }
                }
            }
            stage("Update Dependencies"){
                steps{
                    echo "Updating..!"
                }
                post{
                    success{
                        echo "JIRA: Update Successful"
                    }
                    failure{
                        echo "JIRA: Update Failed"
                    }
                }
            }
            stage("Build"){
                steps{
                    echo "Building..!"
                }
                post{
                    success{
                        echo "JIRA: Build Successful"
                    }
                    failure{
                        echo "JIRA: Build Failed"
                    }
                }
            }
            stage("Unit Tests"){
                steps{
                    echo "Unit Testing..!"
                }
                post{
                    success{
                        echo "JIRA: Unit Tests Successful"
                    }
                    failure{
                        echo "JIRA: Unit Tests Failed"
                    }
                }
            }
            stage("Install"){
                steps{
                    echo "Installing..!"
                }
                post{
                    success{
                        echo "JIRA: Install Successful"
                    }
                    failure{
                        echo "JIRA: Install Failed"
                    }
                }
            }
            stage("Scoverage Report"){
                steps{
                    echo "Reports running..!"
                }
                post{
                    success{
                        echo "JIRA: S Report Successful"
                    }
                    failure{
                        echo "JIRA: S Report Failed"
                    }
                }
            }
            stage("Run Sonar"){
                steps{
                    echo "Reports running..!"
                }
                post{
                    success{
                        echo "JIRA: S Report Successful"
                    }
                    failure{
                        echo "JIRA: S Report Failed"
                    }
                }
            }
            stage("Integration Test"){
                steps{
                    echo "Integration Testing..!"
                }
                post{
                    success{
                        echo "JIRA: Integration Tests Successful"
                    }
                    failure{
                        echo "JIRA: Integration Tests Failed"
                    }
                }
            }
            stage("Close artificat version"){
                steps{
                    echo "Close artificat version..!"
                }
                post{
                    success{
                        echo "JIRA: Close artificat version Successful"
                    }
                    failure{
                        echo "JIRA: Close artificat version Failed"
                    }
                }
            }
            stage("Artifactory + Docker + Package"){
                steps{
                    echo "ADP ing..!"
                }
                post{
                    success{
                        echo "JIRA: ADP Successful"
                    }
                    failure{
                        echo "JIRA: ADP Failed"
                    }
                }
            }
            stage("Deploy to INT"){
                steps{
                    echo "Deploying..!"
                }
                post{
                    success{
                        echo "JIRA: Deployment Successful"
                    }
                    failure{
                        echo "JIRA: Deployment Failed"
                    }
                }
            }
            stage("Performance Test"){
                steps{
                    echo "Performance Testing..!"
                }
                post{
                    success{
                        echo "JIRA: Performance Testing Successful"
                    }
                    failure{
                        echo "JIRA: Performance Testing Failed"
                    }
                }
            }
            stage("Staging Test"){
                steps{
                    echo "Staging..!"
                }
                post{
                    success{
                        echo "JIRA: Staging Test Successful"
                    }
                    failure{
                        echo "JIRA: Staging Test Failed"
                    }
                }
            }
        }
        post{
            always{
                script{
                    bat jiraUtil.update(issue_ID: args.jira_issue, progressLabel: "Deployed",bddReport: "Success", reportLink:"www.my_bdd.com")
                    bat jiraUtil.updateComment(issue_ID: args.jira_issue, text: "Build Failed")
                }
                echo "JIRA: Added BDD test reports"
            }
            //cleanup{}
        }
    }
}