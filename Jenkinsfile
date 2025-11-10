def getCurrentVersionFromPom() {
    def pomXml = readFile('pom.xml')
    def matcher = pomXml =~ /<version>(.+?)<\/version>/
    return matcher[0][1]
}

def calculateReleaseVersion(currentVersion) {
    // Remove -SNAPSHOT suffix to get the release version
    def baseVersion = currentVersion.replace('-SNAPSHOT', '')
    def (major, minor, patch) = baseVersion.tokenize('.')

    // Fetch the latest commit message
    def commitMessage = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()

    // For snapshot versions, we can release as-is or increment based on commit message
    if (commitMessage.contains('[major]')) {
        major = major.toInteger() + 1
        minor = '0'
        patch = '0'
    } else if (commitMessage.contains('[minor]')) {
        minor = minor.toInteger() + 1
        patch = '0'
    } else if (!currentVersion.contains('-SNAPSHOT')) {
        // If current version is not snapshot, increment patch
        patch = patch.toInteger() + 1
    }
    // If it's already a snapshot and no explicit increment, use the base version

    return "${major}.${minor}.${patch}"
}

def cleanGit() {
    sh 'git fetch --all'
    sh 'git reset --hard'
    sh 'git clean -fdx'
}

def DUPLICATED_TAG = 'false'

pipeline {
    agent any
    environment {
        APP_NAME = 'campaign-controller-api-rest'
        SONAR_SERVER = 'LabSonarQube'
        SONAR_PROJECT_NAME = 'campaign_controller_api'
        SONAR_PROJECT_KEY = 'campaign_controller_api'
        SONAR_SOURCES = './src'
        SONAR_SONAR_LOGIN = 'adam-stegienko'
        DOCKER_REGISTRY = 'registry.stegienko.com:8443'
    }
    options {
        timestamps()
    }
    tools {
        maven 'Maven'
        jdk 'JDK'
        dockerTool '26.1.1'
    }
    stages {

        stage('Start') {
            steps {
                script {
                    step([$class: "GitHubCommitStatusSetter", statusResultSource: [$class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: "Build started", state: "PENDING"]]]])
                }
            }
        }

        stage('Clean Workspace') {
            steps {
                sshagent(['jenkins_github_np']) {
                    cleanGit()
                    sh 'git tag -d $(git tag -l) > /dev/null 2>&1'
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/master']],
                    doGenerateSubmoduleConfigurations: 'false',
                    extensions: [
                        [$class: 'CloneOption', noTags: false, shallow: false]
                    ],
                    submoduleCfg: [],
                    userRemoteConfigs: [[
                        credentialsId: 'jenkins_github_np',
                        url: 'git@github.com:adam-stegienko/campaign-controller-api-rest.git'
                    ]]
                ])
            }
        }

        stage('Calculate Version') {
            steps {
                script {
                    // Get current version from pom.xml
                    def currentVersion = getCurrentVersionFromPom()
                    env.CURRENT_VERSION = currentVersion
                    
                    // Calculate release version
                    env.APP_VERSION = calculateReleaseVersion(currentVersion)

                    // Check if the latest commit already has a tag or contains [skip ci]
                    def commitMessage = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
                    def latestCommitTag = ''
                    try {
                        latestCommitTag = sh(returnStdout: true, script: 'git tag --contains HEAD').trim()
                    } catch (Exception e) {}
                    
                    if (latestCommitTag || commitMessage.contains('[skip ci]')) {
                        DUPLICATED_TAG = 'true'
                        sh "echo 'Tag ${latestCommitTag} already exists for the latest commit or commit contains [skip ci]. DUPLICATED_TAG env var is set to: '${DUPLICATED_TAG}"
                    } else {
                        sh "echo 'Current version: ${env.CURRENT_VERSION}'"
                        sh "echo 'Release version: ${env.APP_VERSION}'"
                        sh "echo DUPLICATED_TAG: ${DUPLICATED_TAG}"
                    }
                }
            }
        }

        stage('Set Release Version for Build') {
            when {
                expression {
                    return DUPLICATED_TAG == 'false'
                }
            }
            steps {
                withMaven() {
                    sh "mvn versions:set -DnewVersion=${env.APP_VERSION} -DgenerateBackupPoms=false"
                    sh "echo 'Set version to ${env.APP_VERSION} for build (not committed)'"
                }
            }
        }

        stage('SonarQube analysis') {
            steps {
                withMaven() {
                    withSonarQubeEnv(env.SONAR_SERVER) {
                        sh "mvn clean verify sonar:sonar -Dsonar.projectKey=${env.SONAR_PROJECT_KEY} -Dsonar.projectName='${env.SONAR_PROJECT_NAME}'"
                    }
                }
            }
        }

        stage('Docker Build') {
            when {
                expression {
                    return currentBuild.currentResult == 'SUCCESS'
                }
            }
            steps {
                script {
                    // Copy Maven settings for Docker build (Jenkins has this file)
                    sh 'cp ~/.m2/settings.xml maven-settings.xml'
                    
                    // Build Docker image (tests already passed in SonarQube stage)
                    sh """
                        docker build \
                        --build-arg APP_VERSION=${env.APP_VERSION} \
                        --build-arg SKIP_TESTS=true \
                        -t ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.APP_VERSION} .
                    """
                    
                    // Clean up settings file
                    sh 'rm -f maven-settings.xml'
                }
            }
        }

        stage('Docker Image Security Scan') {
            when {
                expression {
                   return currentBuild.currentResult == 'SUCCESS'
                }
            }
            steps {
                sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v cache_dir:/opt/cache aquasec/trivy image --severity HIGH,CRITICAL --exit-code 0 --timeout 10m0s ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.APP_VERSION}"
            }
        }

        stage('Docker Push') {
            when {
                expression {
                    return currentBuild.currentResult == 'SUCCESS' && DUPLICATED_TAG == 'false'
                }
            }
            steps {
                script {
                    docker.withRegistry("https://${env.DOCKER_REGISTRY}", "docker_registry_credentials") {
                        def appImage = docker.image("${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.APP_VERSION}")
                        appImage.push()
                        appImage.push('latest')
                    }
                }
            }
        }

        // stage('Archive') {
        //     when {
        //         expression {
        //             return currentBuild.currentResult == 'SUCCESS' && DUPLICATED_TAG == 'false'
        //         }
        //     }
        //     steps {
        //         archiveArtifacts artifacts: "**/target/${env.APP_NAME}*.jar", fingerprint: true
        //     }
        // }

        stage('Maven Deploy') {
            when {
                expression {
                    return currentBuild.currentResult == 'SUCCESS' && DUPLICATED_TAG == 'false'
                }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'ABORTED') {
                    withMaven() {
                        sh 'mvn clean deploy'
                    }
                }
            }
        }

        stage('Tag Release') {
            when {
                expression {
                    return currentBuild.currentResult == 'SUCCESS' && DUPLICATED_TAG == 'false'
                }
            }
            steps {
                script {
                    sshagent(['jenkins_github_np']) {
                        sh "git config --global user.email 'adam.stegienko1@gmail.com'"
                        sh "git config --global user.name 'Adam Stegienko'"
                        
                        // Create and push tag only - no POM modifications
                        sh "git tag ${env.APP_VERSION}"
                        sh "git push origin tag ${env.APP_VERSION}"
                        
                        sh "echo 'Tagged release ${env.APP_VERSION} successfully'"
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                try {
                    if (currentBuild.currentResult == 'SUCCESS') {
                        step([$class: "GitHubCommitStatusSetter", statusResultSource: [$class: "ConditionalStatusResultSource", results: [[$class: "BetterThanOrEqualBuildResult", message: "Build succeeded", state: "SUCCESS"]]]])
                    } else if (currentBuild.currentResult == 'FAILURE'){
                        step([$class: "GitHubCommitStatusSetter", statusResultSource: [$class: "ConditionalStatusResultSource", results: [[$class: "BetterThanOrEqualBuildResult", message: "Build failed", state: "FAILURE"]]]])
                    } else {
                        step([$class: "GitHubCommitStatusSetter", statusResultSource: [$class: "ConditionalStatusResultSource", results: [[$class: "BetterThanOrEqualBuildResult", message: "Build aborted", state: "ERROR"]]]])
                    }
                } catch (Exception e) {
                    // Suppress/log nothing
                }
            }
            emailext body: "Build ${currentBuild.currentResult}: Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\nMore info at: ${env.BUILD_URL}",
                 from: 'jenkins+blueflamestk@gmail.com',
                 subject: "${currentBuild.currentResult}: Job '${env.JOB_NAME}' (${env.BUILD_NUMBER})",
                 to: 'adam.stegienko1@gmail.com'
        }
    }
}
