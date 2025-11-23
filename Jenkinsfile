def getLatestDockerTag(registry, imageName, majorMinor, credFile) {
    // Query Docker registry for tags matching major.minor pattern
    try {
        def tagsJson = sh(
            returnStdout: true,
            script: """
            curl -s --netrc-file ${credFile} https://${registry}/v2/${imageName}/tags/list | \
            jq -r '.tags // [] | .[]' | \
            grep -E '^${majorMinor}\\.[0-9]+' | \
            sort -V | \
            tail -n 1
            """
        ).trim()
        
        if (tagsJson) {
            return tagsJson
        }
        return null
    } catch (Exception e) {
        sh "echo 'Could not fetch tags from registry: ${e.message}'"
        return null
    }
}

def getImageCommitSHA(registry, imageName, tag) {
    // Get the commit SHA from Docker image labels
    try {
        def manifest = withEnv(["REGISTRY=${registry}", "IMAGE_NAME=${imageName}", "TAG=${tag}"]) {
            sh(
                returnStdout: true,
                script: '''
                docker pull ${REGISTRY}/${IMAGE_NAME}:${TAG} > /dev/null 2>&1 || true
                docker inspect ${REGISTRY}/${IMAGE_NAME}:${TAG} 2>/dev/null | \
                jq -r '.[0].Config.Labels."git.commit.sha" // empty' || echo ""
                '''
            ).trim()
        }
        
        return manifest ?: null
    } catch (Exception e) {
        return null
    }
}

def calculateNextVersion(registry, imageName, baseVersion, currentCommitSHA, credFile) {
    // Extract major.minor from pom.xml version (e.g., "0.10.0-SNAPSHOT" -> "0.10")
    def versionParts = baseVersion.tokenize('.')
    def major = versionParts[0]
    def minor = versionParts[1]

    // Extract suffix if present (e.g., "0-SNAPSHOT" -> "-SNAPSHOT")
    def suffix = ""
    def patchPart = versionParts[2]
    if (patchPart.contains('-')) {
        def patchSplit = patchPart.split('-', 2)
        suffix = "-${patchSplit[1]}"
    }
    
    def majorMinor = "${major}.${minor}"
    
    // Get latest patch version from Docker registry
    def latestTag = getLatestDockerTag(registry, imageName, majorMinor, credFile)
    
    if (latestTag) {
        sh "echo 'Latest tag in registry: ${latestTag}'"
        
        // Check if this tag was built from the same commit
        def tagCommitSHA = getImageCommitSHA(registry, imageName, latestTag)
        if (tagCommitSHA && tagCommitSHA == currentCommitSHA) {
            sh "echo 'Tag ${latestTag} already exists for commit ${currentCommitSHA}, reusing it'"
            return latestTag
        }
        
        // Different commit, increment patch
        def latestPatchPart = latestTag.tokenize('.')[2]
        def latestPatchNum = latestPatchPart.split('-')[0].toInteger()
        def nextPatch = latestPatchNum + 1
        sh "echo 'Incrementing patch version from ${latestPatchNum} to ${nextPatch}'"
        return "${majorMinor}.${nextPatch}${suffix}"
    } else {
        sh "echo 'No existing tags found for ${majorMinor}.x, starting from 0'"
        return "${majorMinor}.0${suffix}"
    }
}

def cleanGit() {
    sh 'git fetch --all'
    sh 'git reset --hard'
    sh 'git clean -fdx'
}

pipeline {
    agent any

    triggers {
        githubPush()
    }
    
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
                checkout scm
            }
        }

        // stage('Start') {
        //     steps {
        //         script {
        //             step([$class: "GitHubPRStatusBuilder", statusMessage: [content: "Pipeline started"]])
        //             step([$class: "GitHubCommitStatusSetter", statusResultSource: [$class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: "Build started", state: "PENDING"]]]])
        //         }
        //     }
        // }

        stage('Calculate Version') {
            steps {
                script {
                    // Get current commit SHA
                    def currentCommitSHA = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    sh "echo 'Current commit SHA: ${currentCommitSHA}'"

                    // Get version from pom.xml (e.g., "0.10.0-SNAPSHOT" or "0.9.0")
                    def packageVersion = sh(returnStdout: true, script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout').trim()
                    sh "echo 'POM version: ${packageVersion}'"

                    // Use Docker registry credentials via netrc file (more secure)
                    withCredentials([usernamePassword(credentialsId: 'docker_registry_credentials', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASS')]) {
                        // Create temporary .netrc file for curl
                        def netrcFile = "${env.WORKSPACE}/.netrc-${env.BUILD_NUMBER}"
                        // Extract hostname from registry URL (remove port) for .netrc machine matching
                        def registryHost = env.DOCKER_REGISTRY.split(':')[0]
                        
                        // Create netrc file using shell to avoid Groovy interpolation of secrets
                        // Use unquoted EOF to allow shell variable expansion of REGISTRY_USER/PASS
                        sh """
                        cat > ${netrcFile} <<EOF
machine ${registryHost}
login \$REGISTRY_USER
password \$REGISTRY_PASS
EOF
                        chmod 600 ${netrcFile}
                        """
                        
                        try {
                            // Calculate next Docker tag based on registry and commit SHA
                            env.APP_VERSION = calculateNextVersion(env.DOCKER_REGISTRY, env.APP_NAME, packageVersion, currentCommitSHA, netrcFile)
                        } finally {
                            // Clean up credentials file
                            sh "rm -f ${netrcFile}"
                        }
                    }
                    
                    env.GIT_COMMIT_SHA = currentCommitSHA
                    sh "echo 'Docker tag to build: ${env.APP_VERSION}'"
                }
            }
        }

        stage('Maven Build & Unit Tests') {
            steps {
                withMaven() {
                    sh 'mvn -B clean test package'
                }
            }
        }

        stage('SonarQube analysis') {
            steps {
                withMaven() {
                    withSonarQubeEnv(env.SONAR_SERVER) {
                        // Reuse existing compiled classes from previous build; skip tests to save time
                        sh "mvn -B sonar:sonar -DskipTests -Dsonar.projectKey=${env.SONAR_PROJECT_KEY} -Dsonar.projectName='${env.SONAR_PROJECT_NAME}'"
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
                sh """
                    echo 'Building Docker image...'
                    docker build \
                        --build-arg SKIP_TESTS=true \
                        --build-arg APP_VERSION=${env.APP_VERSION} \
                        --label git.commit.sha=${env.GIT_COMMIT_SHA} \
                        --label build.timestamp=\$(date -u +%Y-%m-%dT%H:%M:%SZ) \
                        -t ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.APP_VERSION} .
                """
            }
        }

        stage('Docker Image Security Scan') {
            when {
                expression {
                   return currentBuild.currentResult == 'SUCCESS' && env.BRANCH_NAME?.startsWith('release/')
                }
            }
            steps {
                sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image --severity HIGH,CRITICAL --exit-code 0 ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.APP_VERSION}"
            }
        }

        stage('Docker Push') {
            when {
                expression {
                    return currentBuild.currentResult == 'SUCCESS'
                }
            }
            steps {
                script {
                    docker.withRegistry("https://${env.DOCKER_REGISTRY}", "docker_registry_credentials") {
                        def appImage = docker.image("${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.APP_VERSION}")
                        appImage.push()
                        
                        // Only push 'latest' tag for master branch
                        if (env.BRANCH_NAME == 'master') {
                            appImage.push('latest')
                            sh "echo 'Pushed latest tag for master branch'"
                        } else {
                            sh "echo 'Skipping latest tag (only master branch gets latest tag)'"
                        }
                    }
                }
            }
        }

       stage('Archive') {
            when {
                expression {
                    return currentBuild.currentResult == 'SUCCESS'
                }
            }
            steps {
                archiveArtifacts artifacts: "**/target/*.jar", fingerprint: true
            }
        }

        stage('Maven Deploy') {
            when {
                expression {
                    return currentBuild.currentResult == 'SUCCESS' && env.BRANCH_NAME?.startsWith('release/')
                }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'ABORTED') {
                    withMaven() {
                        script {
                            try {
                                sh 'mvn -B deploy -DskipTests'
                                sh "echo 'Maven deploy successful for version ${env.APP_VERSION}'"
                            } catch (Exception e) {
                                if (e.getMessage().contains('cannot be updated') || e.getMessage().contains('400')) {
                                    sh "echo 'Artifact ${env.APP_VERSION} already exists in repository - skipping deploy'"
                                    currentBuild.result = 'SUCCESS'
                                } else {
                                    throw e
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Cleanup') {
            steps {
                script {
                    sh "echo 'Build completed successfully'"
                    sh "echo 'Docker image: ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.APP_VERSION}'"
                }
            }
        }
    }
    post {
        always {
            script {
                try {
                    if (currentBuild.currentResult == 'SUCCESS') {
                        step([$class: "GitHubCommitStatusSetter", statusResultSource: [
                            $class: "ConditionalStatusResultSource",
                            results: [[$class: "BetterThanOrEqualBuildResult", message: "Build succeeded", state: "SUCCESS"]]
                        ]])
                        step([$class: "githubPRStatusPublisher",
                            statusMsg: [content: "Build succeeded"],
                            unstableAs: "SUCCESS"
                        ])
                    } else if (currentBuild.currentResult == 'FAILURE') {
                        step([$class: "GitHubCommitStatusSetter", statusResultSource: [
                            $class: "ConditionalStatusResultSource",
                            results: [[$class: "BetterThanOrEqualBuildResult", message: "Build failed", state: "FAILURE"]]
                        ]])
                        step([$class: "githubPRStatusPublisher",
                            statusMsg: [content: "Build failed"],
                            unstableAs: "FAILURE"
                        ])
                    } else {
                        step([$class: "GitHubCommitStatusSetter", statusResultSource: [
                            $class: "ConditionalStatusResultSource",
                            results: [[$class: "AnyBuildResult", message: "Build aborted. Result: ${currentBuild.currentResult}", state: "ERROR"]]
                        ]])
                        step([$class: "githubPRStatusPublisher",
                            statusMsg: [content: "Build aborted. Result: ${currentBuild.currentResult}"],
                            unstableAs: "ERROR"
                        ])
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
