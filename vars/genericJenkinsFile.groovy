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
            PIPELINE_NAME = "${env.JOB_NAME.split('/')[0]}"
            NEW_BRANCH_NAME=env.BRANCH_NAME.replace("/","-")
            BUILD_FOLDER_PATH = "$JENKINS_HOME/jobs/${PIPELINE_NAME}/branches/${NEW_BRANCH_NAME}/builds/${env.BUILD_NUMBER}"
        }

        stages {
            stage("Initialize") {
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
                        
                        echo "Build"
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

                        echo "Unit Tests"
                    }
                }
                post{
                    always {
                        echo "Jacoco Running"
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

                        echo "Running on localhost"
                    }
                }
            }
            stage("BDD Test"){
                steps{
                    echo "Stage: $env.STAGE_NAME"

                    script {
                        LAST_STAGE = env.STAGE_NAME
                        BDD_REPORT = true
                        
                        echo "Running BDD tests"
                    } 
                }
                post{
                    always {
                        echo "BDD succesful"
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
                    jiraUtil.updateJirawithSuccess(bddPath:"$env.WORKSPACE\\cucumber-trends.json",xmlPath:"$env.WORKSPACE\\junitResult.xml")
                }
            }
            failure {
                echo "Failure"
                script {
                    jiraUtil.updateJirawithFailure(bddPath:"$env.WORKSPACE\\cucumber-trends.json",xmlPath:"$env.WORKSPACE\\junitResult.xml",failStage: LAST_STAGE, bddReport: BDD_REPORT, unitTestReport: UNIT_TEST_REPORT, passedUT: PASSED_UT, passedBDD: PASSED_BDD)
                }
            }
            //cleanup{} 
        }
    }
}
