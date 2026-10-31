pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        // check github every 2 minit, no need webhook
        pollSCM('H/2 * * * *')
    }

    parameters {
        string(name: 'ROLLBACK_TO', defaultValue: '', description: 'Image tag to roll back to, e.g. 1.0.0-5-a1b2c3d. Leave empty for normal build.')
    }

    environment {
        NAMESPACE = 'finsecure'
        DEPLOYMENT = 'txn-api'
        TRIVY_CACHE_DIR = '/var/jenkins_home/.trivy-cache'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.POM_VERSION = sh(script: './mvnw -q -ntp help:evaluate -Dexpression=project.version -DforceStdout', returnStdout: true).trim()
                    env.TAG = "${env.POM_VERSION}-${env.BUILD_NUMBER}-${env.GIT_SHORT}"
                    currentBuild.displayName = params.ROLLBACK_TO ? "#${env.BUILD_NUMBER} rollback ${params.ROLLBACK_TO}" : "#${env.BUILD_NUMBER} ${env.TAG}"
                }
                sh 'mkdir -p reports'
            }
        }

        stage('Build') {
            when { expression { !params.ROLLBACK_TO } }
            steps {
                sh './mvnw -B -ntp clean compile'
            }
        }

        stage('Test') {
            when { expression { !params.ROLLBACK_TO } }
            steps {
                sh './mvnw -B -ntp test jacoco:report'
            }
            post {
                always { junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' }
            }
        }

        stage('Package') {
            when { expression { !params.ROLLBACK_TO } }
            steps {
                sh './mvnw -B -ntp package -DskipTests'
                archiveArtifacts artifacts: 'target/app.jar', fingerprint: true
            }
        }

        stage('Security Checks') {
            when { expression { !params.ROLLBACK_TO } }
            parallel {
                stage('SonarQube Quality Gate') {
                    steps {
                        withSonarQubeEnv('sonarqube') {
                            sh './mvnw -B -ntp sonar:sonar -Dsonar.token=$SONAR_AUTH_TOKEN -Dsonar.qualitygate.wait=true -Dsonar.qualitygate.timeout=300'
                        }
                    }
                }
                stage('Dependency Scan') {
                    steps {
                        sh '''
                          trivy rootfs -q --scanners vuln --format json -o reports/trivy-dependencies.json target/app.jar
                          trivy rootfs -q --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 target/app.jar
                        '''
                    }
                }
                stage('Secret Scan') {
                    steps {
                        sh 'trivy fs -q --scanners secret --skip-dirs target --exit-code 1 .'
                    }
                }
            }
        }

        stage('Container Build') {
            when { expression { !params.ROLLBACK_TO } }
            steps {
                sh 'docker build -t ${IMAGE_REPO}:${TAG} .'
            }
        }

        stage('Image Scan') {
            when { expression { !params.ROLLBACK_TO } }
            steps {
                sh '''
                  trivy image -q --format json -o reports/trivy-image.json ${IMAGE_REPO}:${TAG}
                  trivy image -q --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 ${IMAGE_REPO}:${TAG}
                '''
            }
        }

        stage('Push') {
            when { expression { !params.ROLLBACK_TO } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'REG_USER', passwordVariable: 'REG_TOKEN')]) {
                    sh '''
                      echo "$REG_TOKEN" | docker login -u "$REG_USER" --password-stdin ${REGISTRY_HOST}
                      docker tag ${IMAGE_REPO}:${TAG} ${IMAGE_REPO}:latest
                      docker push ${IMAGE_REPO}:${TAG}
                      docker push ${IMAGE_REPO}:latest
                    '''
                }
            }
        }

        stage('Deploy') {
            when { expression { !params.ROLLBACK_TO } }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                      kubectl apply -f k8s/configmap.yaml
                      sed -e "s|IMAGE_PLACEHOLDER|${IMAGE_REPO}:${TAG}|" -e "s|CHANGE_CAUSE|jenkins build ${BUILD_NUMBER}: ${TAG}|" \
                          k8s/deployment.yaml | kubectl apply -f -
                      if ! kubectl -n $NAMESPACE rollout status deployment/$DEPLOYMENT --timeout=180s; then
                        echo "Rollout failed - rolling back to previous revision"
                        kubectl -n $NAMESPACE rollout undo deployment/$DEPLOYMENT
                        kubectl -n $NAMESPACE rollout status deployment/$DEPLOYMENT --timeout=180s
                        exit 1
                      fi
                      kubectl -n $NAMESPACE get pods -o wide
                    '''
                }
            }
        }

        stage('Smoke Test') {
            when { expression { !params.ROLLBACK_TO && env.APP_URL } }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                      if ! curl -fsS --retry 10 --retry-delay 3 --retry-all-errors "$APP_URL/health" | tee reports/smoke.json | grep -q '"status":"UP"'; then
                        echo "Smoke test failed - rolling back"
                        kubectl -n $NAMESPACE rollout undo deployment/$DEPLOYMENT
                        exit 1
                      fi
                      cat reports/smoke.json
                    '''
                }
            }
        }

        stage('Rollback') {
            when { expression { params.ROLLBACK_TO } }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                      kubectl -n $NAMESPACE set image deployment/$DEPLOYMENT app=${IMAGE_REPO}:${ROLLBACK_TO}
                      kubectl -n $NAMESPACE annotate deployment/$DEPLOYMENT kubernetes.io/change-cause="jenkins build ${BUILD_NUMBER}: rollback to ${ROLLBACK_TO}" --overwrite
                      kubectl -n $NAMESPACE rollout status deployment/$DEPLOYMENT --timeout=180s
                      kubectl -n $NAMESPACE rollout history deployment/$DEPLOYMENT
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'reports/**', allowEmptyArchive: true
        }
        success {
            emailext to: env.NOTIFY_EMAIL,
                     subject: "SUCCESS: ${env.JOB_NAME} ${currentBuild.displayName}",
                     body: "Build ${env.BUILD_URL} passed and image ${env.IMAGE_REPO}:${params.ROLLBACK_TO ?: env.TAG} is live."
        }
        failure {
            emailext to: env.NOTIFY_EMAIL,
                     subject: "FAILED: ${env.JOB_NAME} ${currentBuild.displayName}",
                     body: '''Pipeline failed, deployment did not go ahead.<br>Console: ${BUILD_URL}console<br><pre>${BUILD_LOG, maxLines=60, escapeHtml=true}</pre>''',
                     mimeType: 'text/html'
        }
        cleanup {
            cleanWs()
        }
    }
}
