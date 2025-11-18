String upstream_projects = BRANCH_NAME == 'master' ? '' : 'ejpd-stack-spring/develop'

pipeline {
  agent none
  stages {
     stage("build") {
       agent {
         docker {
           image "ejpd/maven:3-openjdk21-ubuntu"
           args "-v /home/jenkins:/home/jenkins -v /data/iscpkg:/data/iscpkg"
         }
       }
       steps {
         mvnBuild()
         sonarScan()
       }
       post {
         always {
           // save and publish buildreports
           junit allowEmptyResults: true, testResults: '**/target/*-reports/*.xml'
         }
         success {
           cleanWs()
         }
       }
     }
   }
    post {
        failure {
            // sendNotifications always informs commiters, optionally a comma or whitespace seperated list can be passed to notify additional recipients
            sendNotifications("***REMOVED***")
        }
    }
    triggers {
        upstream(upstreamProjects: upstream_projects, threshold: hudson.model.Result.SUCCESS)
    }
    options {
        // keep the 10 most recent builds
        buildDiscarder(logRotator(numToKeepStr:'10'))
        // use normal time in logfile
        timestamps()
    }
 }
