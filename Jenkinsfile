#!groovy

pipeline {
  agent none
  stages {
     stage("build") {
       agent {
         docker {
           image "ejpd/maven:3.5-jdk8"
           args "-v /home/jenkins:/home/jenkins -v /data/iscpkg:/data/iscpkg"
         }
       }
       steps {
         mvnBuild()
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
   options {
     disableConcurrentBuilds()
     buildDiscarder(logRotator(numToKeepStr:'10'))
     // use normal time in logfile
     timestamps()
   }
 }