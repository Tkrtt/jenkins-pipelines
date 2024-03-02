def WEB_APPS = "/u01/app/frontend-httpserver/webapps"
def DEPLOY_FOLDER = "/u01/app/frontend-httpserver/deploy/"
def FE_FOLDER = "/u01/app/frontend-httpserver"
def CONTENT_CONF = "webapps/admin"
def GIT_URL = "http://my.gitlab.tkrt/admin/admin-fe/admin-war-fe.git"
def gitlabTargetBranch = "main"
def ROOT_FOLDER = "/home/jenkins/workspace/FE-QTUD-DEV-CD"
def HOSTNAME = "dev-nt-be"
pipeline {
    agent {
        label "node-app-1"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
    }
    stages {
        stage ('Clone War Repo'){
            steps {
                script {
                    git branch: "${gitlabTargetBranch}", credentialsId: "${GIT_CREDENTIAL}", url: "${GIT_URL}"
                }
            }
        }
        stage ('Copy to WEB_APPS'){
            steps {
                dir ("${ROOT_FOLDER}/DEV"){
                    sh """
                        cp -r qtud_${TAG}.war ${WEB_APPS}
                    """
                }
            }
        }
        stage ('Remove Old War File') {
            steps {
                dir("${WEB_APPS}"){
                    script {
                        sh """
                            find "${WEB_APPS}" -type f -name "qtud_*" | head -n 1 | xargs rm -rf
                        """
                    }
                }
            }
        }
        // stage('Rename 10-File'){
        //     steps {
        //         dir("${DEPLOY_FOLDER}") {
        //             script {
        //                 sh """
        //                     sed -i "s#$CONTENT_CONF.*.war#${CONTENT_CONF}_${TAG}.war#g" ${DEPLOY_FOLDER}10-config-loader.xml
        //                 """
        //             }
        //         }
        //     }
        // }
        stage('Deploy'){
            steps {
                dir ("${FE_FOLDER}"){
                    script {
                        sh """
                             JENKINS_NODE_COOKIE=dontKillMe HOSTNAME=${HOSTNAME} sh rerunapp
                        """
                    }
                }
            }
        }
    }
}