import com.cicd.helper.JiraUtil


def call(Map args =[buildMode: "mvn",jira_issue: ""]){
    def jiraUtil= new JiraUtil()
    String failedStage=env.STAGE_NAME

    pipeline{
        agent any

        //options{}

        //parameters{}

        //environment{}

        stages{
            stage("Initialize"){
                steps{
                    script{
                        failedStage=env.STAGE_NAME
                    }
                    echo "Branch name is: $env.BRANCH_NAME"
                    echo "Intializing..!"
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
                    echo "Close artificat version..!"
                    //Forcefully trying to give error
                    bat "abcd"
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    script{
                        failedStage=env.STAGE_NAME
                    }
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
                    jiraUtil.update(progressLabel: "Deployed",bddReport: "Success", reportLink:"www.my_new_bdd.com")
                    jiraUtil.updateComment("How you doing!!")
                    jiraUtil.updateCommentwithBDD()
                    jiraUtil.updateCommentwithCommitterMention()

                }
                echo "JIRA: Added BDD test reports"
            }
            failure{
                script{
                    echo "Error"
                    jiraUtil.updateCommentwithFailMessage(failedStage)
                    jiraUtil.updateCommentwithCommitterMention()
                }
            }
            //cleanup{}
        }
    }
}