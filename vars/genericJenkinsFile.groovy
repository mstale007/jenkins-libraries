import com.cicd.helper.JiraUtil

def call(Map args =[buildMode: "mvn", issueKey: ""]) { 
    def jiraUtil= new JiraUtil()
    def LAST_STAGE = ""

    pipeline {
        agent any

        //options{}

        //parameters{}

        environment {
            ISSUE_KEY = args.issueKey.toString()
        }

        stages {
            stage("Initialize"){
                steps{
                    echo "Stage: $env.STAGE_NAME"
                    echo "Branch name is: $env.BRANCH_NAME"

                    script {
                        LAST_STAGE = env.STAGE_NAME
                    }
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
            stage("Build"){
                steps{
                    echo "Stage: $env.STAGE_NAME"
                    
                    script {
                        LAST_STAGE = env.STAGE_NAME

                        if(isUnix()) {
                            sh "mvn clean install -DskipTests"
                        }
                        else {
                            bat "mvn clean install -DskipTests"
                        }
                    }
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
                    echo "Stage: $env.STAGE_NAME"
                    
                    script {
                        LAST_STAGE = env.STAGE_NAME

                        if(isUnix()) {
                            sh "mvn -Dtest=UnitTests test jacoco:report"
                        }
                        else {
                            bat "mvn -Dtest=UnitTests test jacoco:report"
                        }
                    }
                }
                post{
                    always {
                        junit '**/target/surefire-reports/*.xml'
                        jacoco()
                        script {
                            jiraUtil.xmlToComment(path: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/springboot-multibranch-pipeline/branches/${env.BRANCH_NAME}/builds/${env.BUILD_NUMBER}/junitResult.xml")                    
                        }
                    }
                }
            }
            stage('Run on localhost') {
                steps {
                    echo "Stage: $env.STAGE_NAME"

                    script {
                        LAST_STAGE = env.STAGE_NAME

                        if(isUnix()) {
                            sh "java -jar target/spring-boot-rest-api-tutorial-0.0.1-SNAPSHOT.jar &"
                        }
                        else {
                            bat "START /B java -jar target/spring-boot-rest-api-tutorial-0.0.1-SNAPSHOT.jar"
                        }
                    }
                }
            }
            stage("BDD Test"){
                steps{
                    echo "Stage: $env.STAGE_NAME"

                    script {
                        LAST_STAGE = env.STAGE_NAME

                        if(isUnix()) {
                            bshat "mvn -Dtest=TestRunner test"
                        }
                        else {
                            bat "mvn -Dtest=TestRunner test"
                        }
                    } 
                }
            }
            post{
                always {
                    cucumber buildStatus: 'UNSTABLE',
                        reportTitle: 'My report',
                        fileIncludePattern: '**/*.json',
                        trendsLimit: 10,
                        classifications: [
                            [
                                'key': 'Browser',
                                'value': 'Firefox'
                            ]
                        ]
                    script {
                        jiraUtil.updateCommentwithBDD(filePath: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/springboot-multibranch-pipeline/branches/${env.BRANCH_NAME}/cucumber-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4/cucumber-trends.json")
                        jiraUtil.sendAttachment(attachmentLink: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/springboot-multibranch-pipeline/branches/${env.BRANCH_NAME}/builds/${env.BUILD_NUMBER}/cucumber-html-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4")
                    }
                }
            }
        }
        post{
            always{
                script{
                    //jiraUtil.update(progressLabel: "Deployed",bddReport: "Success", reportLink:"www.my_new_bdd.com")
                    //jiraUtil.addAssignee()
                    echo "Now creating.."
                    
                }
            }
            success {
                echo "Build success"
            }
            failure {
                script {
                    String issueID = jiraUtil.getIssueID().toString()
                    if(issueID.equals("")){
                        issueID = jiraUtil.createIssue()
                    }

                    jiraUtil.updateComment(text: "Build Failed at stage $LAST_STAGE", issue: issueID)
                
                }
            }
            //cleanup{} 
        }
    }
}