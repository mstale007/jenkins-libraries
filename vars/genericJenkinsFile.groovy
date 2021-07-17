import com.cicd.helper.JiraUtil

def call(Map args =[buildMode: "mvn", issueKey: ""]) { 
    def jiraUtil= new JiraUtil()
    def LAST_STAGE = ""
    def PIPELINE_ARRAY = env.JOB_NAME.split('/')
    pipeline {
        agent any

        //options{}

        //parameters{}

        environment {
            ISSUE_KEY = args.issueKey.toString()
            UNIT_TEST_REPORT = false
            BDD_REPORT = false
            FAIL_STAGE = ""
        }

        stages {
            stage("Initialize"){
                steps{
                    echo "Stage: $env.STAGE_NAME"
                    echo "Branch name is: $env.BRANCH_NAME"
                    
                    echo env.POM_ARTIFACTID
                    echo POM_ARTIFACTID
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
                        env.UNIT_TEST_REPORT = true

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
                        env.BDD_REPORT = true

                        if(isUnix()) {
                            sh "mvn -Dtest=TestRunner test"
                        }
                        else {
                            bat "mvn -Dtest=TestRunner test"
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
                    }
                }
            }
        }
        post{
            success {
                echo "Success"
                script {
                    String issueID = jiraUtil.getIssueID().toString()
                    if(!issueID.equals("")){
                        jiraUtil.updateComment(text: "Build #$env.BUILD_NUMBER: Successful", issue: issueID)
                        jiraUtil.xmlToComment(path: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/${PIPELINE_ARRAY[0]}/branches/${env.BRANCH_NAME}/builds/${env.BUILD_NUMBER}/junitResult.xml", issue: issueID)                    
                        //jiraUtil.updateCommentwithBDD(filePath: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/${PIPELINE_ARRAY[0]}/branches/${env.BRANCH_NAME}/cucumber-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4/cucumber-trends.json", issue: issueID)
                        jiraUtil.sendAttachment(attachmentLink: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/${PIPELINE_ARRAY[0]}/branches/${env.BRANCH_NAME}/builds/${env.BUILD_NUMBER}/cucumber-html-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4", issue: issueID)
                    }
                    else {
                        echo "No issue updated/ no new issue created"
                    }
                }
            }
            failure {
                echo "Failure"
                script {

                    String issueID = jiraUtil.getIssueID().toString()
                    if(issueID.equals("")){
                        issueID = jiraUtil.createIssue(failStage: LAST_STAGE)
                        jiraUtil.addAssignee(issue: issueID)
                    }

                    jiraUtil.updateComment(text: "Build #$env.BUILD_NUMBER: Failed at stage $LAST_STAGE", issue: issueID)
                    if(env.UNIT_TEST_REPORT == true) {
                        jiraUtil.xmlToComment(path: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/${PIPELINE_ARRAY[0]}/branches/${env.BRANCH_NAME}/builds/${env.BUILD_NUMBER}/junitResult.xml", issue: issueID)                    
                    }
                    else {
                        jiraUtil.updateComment(text: "Build #$env.BUILD_NUMBER: Unit tests were not performed due to failure at an earlier stage", issue: issueID)
                    }

                    if(env.BDD_REPORT == true) {
                        //jiraUtil.updateCommentwithBDD(filePath: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/${PIPELINE_ARRAY[0]}/branches/${env.BRANCH_NAME}/cucumber-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4/cucumber-trends.json", issue: issueID)
                        jiraUtil.sendAttachment(attachmentLink: "C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/jobs/${PIPELINE_ARRAY[0]}/branches/${env.BRANCH_NAME}/builds/${env.BUILD_NUMBER}/cucumber-html-reports_fb242bb7-17b2-346f-b0a4-d7a3b25b65b4", issue: issueID)
                    }
                    else {
                        jiraUtil.updateComment(text: "Build #$env.BUILD_NUMBER: BDD tests were not performed due to failure at an earlier stage", issue: issueID)
                    }
                }
            }
            //cleanup{} 
        }
    }
}