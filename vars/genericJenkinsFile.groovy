import com.cicd.helper.JiraUtil

def call(Map args =[buildMode: "mvn", issueKey: ""]) { 
    def jiraUtil = new JiraUtil()
    def LAST_STAGE = ""
    def BDD_REPORT = false
    def UNIT_TEST_REPORT = false
    def PASSED_UT = false
    def PASSED_BDD = false

    pipeline {
        agent any

        //options{}

        //parameters{}

        environment {
            ISSUE_KEY = args.issueKey.toString()
            FAIL_STAGE = ""
            PIPELINE_NAME = "${env.JOB_NAME.split('/')[0]}"
            PROJECT_NAME = readMavenPom().getArtifactId()
            PROJECT_VERSION = readMavenPom().getVersion()
            NEW_BRANCH_NAME=env.BRANCH_NAME.replace("/","-")
            BUILD_FOLDER_PATH = "$JENKINS_HOME/jobs/${PIPELINE_NAME}/branches/${NEW_BRANCH_NAME}/builds/${env.BUILD_NUMBER}"
        }

        stages {
            stage("Initialize") {
                steps{
                    echo "Stage: $env.STAGE_NAME"
                    echo "Branch name is: $env.BRANCH_NAME"
                    bat "set"
                    echo "$junit"
                    
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
            stage("Load Env Variables") {
                steps {
                    load "env-vars/env.groovy"
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
                        UNIT_TEST_REPORT = true

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
                    success {
                        script {
                            PASSED_UT = true
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
                            sh "java -jar target/" + env.PROJECT_NAME + "-" + env.PROJECT_VERSION + ".jar &"
                        }
                        else {
                            bat "START /B java -jar target/" + env.PROJECT_NAME + "-" + env.PROJECT_VERSION + ".jar"
                        }
                    }
                }
            }
            stage("BDD Test"){
                steps{
                    echo "Stage: $env.STAGE_NAME"

                    script {
                        LAST_STAGE = env.STAGE_NAME
                        BDD_REPORT = true
                        
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
                    success {
                        script {
                            PASSED_BDD = true
                        }
                    }
                }
            }
        }
        post{
            success {
                echo "Success"
                script {
                    jiraUtil.updateJirawithSuccess()
                }
            }
            failure {
                echo "Failure"
                script {
                    jiraUtil.updateJirawithFailure(failStage: LAST_STAGE, bddReport: BDD_REPORT, unitTestReport: UNIT_TEST_REPORT, passedUT: PASSED_UT, passedBDD: PASSED_BDD)
                }
            }
            //cleanup{} 
        }
    }
}
