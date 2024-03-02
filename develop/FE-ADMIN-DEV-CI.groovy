def GIT_FE_URL = "http://my.gitlab.tkrt/admin/admin-fe/admin/admin-fe.git"
def GIT_WAR_URL = "http://my.gitlab.tkrt/admin/admin-fe/admin-war-fe.git"
// def SONAR_PATH = "/home/jenkins/bin/sonarscan/bin/sonar-scanner"
def FE_FOLDER = "D:\\home\\jenkins\\workspace\\admin\\FE-ADMIN-QTUD-DEV-CI"
def BUILD_PATH = "D:\\home\\jenkins\\workspace\\admin\\FE-ADMIN-DEV-CI\\build\\"
def JAR_PATH = "C:\\Program Files\\Java\\jdk-11.0.22\\bin"
def BUILD_WAR_FE = "D:\\home\\dev\\admin-fe"
def WAR_PATH = "D:\\home\\dev\\admin-fe\\DEV"
def WEB_APPS = "/u01/app/frontend-httpserver/webapps"
def GIT_USER = "Jenkins"
def GIT_USER_EMAIL = "aautokratir@tuta.io"
def GIT_PATH = "C:\\Program Files\\Git\\bin"

pipeline {
    agent {
        label "node-win"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
    }
    tools {
        nodejs 'node18'
        jdk 'java11'
    }
    stages {
        stage('Checkout') {
            steps {
                script {
                    git branch: 'dev', credentialsId: "${GIT_CREDENTIAL_ID}", url: "${GIT_FE_URL}"
                }
            }
        }
        stage('Clone Before Scan') {
            agent {
                label "node-win"
            }
            steps {
                script {
                    git branch: 'dev', credentialsId: "${GIT_CREDENTIAL_ID}", url: "${GIT_FE_URL}"
                }
            }
        }
        stage('Scan'){
            agent {
                label "node-win"
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'FE-ADMIN-DEV', usernameVariable: 'SONAR_USER', passwordVariable: 'SONAR_PASS')]) {
                        withSonarQubeEnv(credentialsId: 'FE-ADMIN-DEV', installationName: 'FE-ADMIN-DEV') {
                            sh """${SONAR_PATH} \
                                -Dsonar.projectKey=FE-ADMIN-DEV \
                                -Dsonar.sources=. \
                                -Dsonar.exclusions=./node_modules \
                                -Dsonar.host.url=http://dev.sonarqube.tkrt:9000 \
                                -Dsonar.login=\\${SONAR_USER}
                                -Dsonar.password=\\${SONAR_PASS}
                        """
                        }
                    }
                }
            }
        }
        stage ('Define TAG') {
            agent {
                label "node-win"
            }
            steps {
                script {
                    TIMESTAMP = sh(returnStdout: true, script: "date +%Y.%m.%d").trim()
                    echo "TIMESTAMP: $TIMESTAMP"
                    TAG = "${TIMESTAMP}-v${BUILD_NUMBER}" 
                    echo "TAG: $TAG"
                }
            }
        }
        stage('Build'){
                steps {
                    script {
                        bat """
                            npm run build
                        """
                    }
                }
            }
        stage ('Copy WEB-INF'){
            steps {
                dir ("${FE_FOLDER}") {
                    script {
                        bat """
                            move WEB-INF build
                        """
                    }
                }
            }
        }
        stage ('Compress Build File'){
            steps {
                dir ("${BUILD_PATH}"){
                    script {
                        bat """
                            jar -cvf admin.war *
                        """
                    }
                }
            }
        }
        stage ('Move FE file'){
            steps {
                dir("${BUILD_PATH}") {
                    script {
                        bat """
                            move admin.war ${WAR_PATH}
                        """
                    }
                }
            }
        }
        stage ('Push Git Repo'){
            steps {
                dir("${ADMIN_WAR_FE}") {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'Git-Jenkins', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                            bat """
                                git config user.email "${GIT_USER_EMAIL}"
                                git config user.name "${GIT_USER}"
                                git add .
                                git commit -m "update ${TAG}"
                                git push http://${USER}:${PASS}@${GIT_WAR_URL}
                            """
                        }
                    }
                }
            }
        }
        stage('Call To Deploy') {
            steps {
                build job: 'FE-ADMIN-DEV-CD', parameters: [string(name: 'TAG', value: "$TAG")]
            }               
        }
    }
}