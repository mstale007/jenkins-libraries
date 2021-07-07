import com.cicd.helper.*

def call(Map args =[buildMode: "mvn",jira_issue: ""]){
    def jira_updater= new JiraUpdater()
    pipeline{
        agent any

        tools { 
            maven "Maven"
        }

        //options{}

        //parameters{}

        //environment{}

        stages{
            stage("Initialize"){
                steps{
                    echo "Branch name is: $env.BRANCH_NAME"
                    echo "Jira issue no: $args.jira_issue"
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
                    bat "mvn clean install -DskipTests"
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
                    bat "mvn -Dtest=UnitTests test"
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
            stage('Run on localhost') {
                steps {
                    bat "START /B java -jar target/spring-boot-rest-api-tutorial-0.0.1-SNAPSHOT.jar"
                }
            }
            stage("BDD Test"){
                steps{
                    echo "Performance test"
                    bat "mvn -Dtest=TestRunner test"
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
                    echo "Performance test"
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
                         script{
                            //tools.update()

                            bat jira_updater.update(issue_ID: args.jira_issue, progressLabel: "Deployed",bddReport: "Success", reportLink:"www.my_bdd.com")
                        }
                    }
                    failure{
                        echo "JIRA: Staging Test Failed"
                    }
                }
            }
        }
        post{
            always{
                echo "JIRA: Added BDD test reports"
            }
            //cleanup{}
        }
    }
}
