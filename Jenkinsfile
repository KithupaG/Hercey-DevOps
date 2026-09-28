pipeline {
    agent any

    tools {
        maven 'Maven for JBOSS Pipeline'
    }

    environment {
        // Dynamic Versioning
        APP_NAME = 'hercey'
        MAJOR_MINOR = '1.0'
        VERSION = "${MAJOR_MINOR}.${BUILD_NUMBER}"

        // Nexus Repository Config
        NEXUS_HOST = '192.168.56.1:8081'
        NEXUS_REPO = 'hercey-releases'
        NEXUS_CREDS = 'nexus-credentials'

        // VM Configuration
        VM_IP = '192.168.56.100'
        VM_USER = 'root'
        WAR_PATH = "target/${APP_NAME}.war"
        JBOSS_CLI = '/opt/jboss/jboss-eap-8.0/bin/jboss-cli.sh'

        // Kubernetes Configuration
        IMAGE_NAME = 'my-jboss-app:${VERSION}'
        CONTAINERD_SOCK = '/run/docker/containerd/containerd.sock'
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                checkout scm
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQubeServer') {
                    sh "mvn clean compile org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=${APP_NAME} -Dsonar.projectName=${APP_NAME}"
                }
            }
        }

        stage('SonarQube Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Buil WAR with dynamic versioning') {
            steps {
                echo "Building artifact version: ${VERSION}"
                sh "mvn clean package -DskipTests -Drevision=${VERSION}"
            }
        }

        stage('Public Artifact to Nexus') {
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: "${NEXUS_HOST}",
                    groupId: 'com.hercey',
                    version: "${VERSION}",
                    repository: "${NEXUS_REPO}",
                    credentialsId: "${NEXUS_CREDS}",
                    artifacts: [
                        [artifactId: "${APP_NAME}", classifier: '', file: "${WAR_PATH}", type: 'war']
                    ]
                )
            }
        }
        stage('Trivy Scanning') {
            steps {
                script {
                    echo "Copying WAR and building Docker image"
                    sh "cp ${WAR_PATH} hercey.war"
                    sh "docker build -t ${IMAGE_NAME} ."

                    echo "Scanning image for High/Critical vulnerabilities"
                    sh "trivy image --severity HIGH,CRITICAL --exit-code 1 ${IMAGE_NAME}"
                }
            }
        }

        stage('Deploy to Kubernetes (With Rollback)') {
            steps {
                script {
                    try {
                        echo "Deploying image version ${VERSION} to K8S..."
                        sh "kubectl set image deployment/jboss-app jboss=${IMAGE_NAME}"
                        sh "kubectl rollout status deployment/jboss-app --timeout=60s"
                    }catch (Exception e) {
                        echo "WARNING: Kubernetes deployment failed! Initiating automatic rollback..."
                        sh "kubectl rollout undo deployment/jboss-app"
                        sh "kubectl rollout status deployment/jboss-app"
                        error "Kubernetes Deployment failed and was rolled back to previous stable state: ${e.message}"
                    }
                }
            }
        }

        stage('Deploy WAR file to JBoss EAP VM (With Rollback & Backup)') {
            steps {
                sshagent(credentials: ['jboss-vm-ssh-key']) {
                    script {
                        try {
                            echo "Creating backup of current deployment on VM"
                            sh "ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} 'cp /tmp/hercey.war /tmp/hercey.war.bak 2>/dev/null || true'"

                            echo "Transferring artifact directly into the VM..."
                            sh "scp -o StrictHostKeyChecking=no ${WAR_PATH} ${VM_USER}@${VM_IP}:/tmp/hercey.war"

                            echo "Executing deployment using JBoss CLI..."
                            sh """
                                ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} \
                                "${JBOSS_CLI} --connect --controller=${VM_IP}:9990 --command='deploy --force /tmp/hercey.war'"
                            """
                        }catch(Exception e) {
                            echo "WARNING: Jboss VM deployment failed! Restoring backup WAR file..."
                            sh """
                                ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} \
                                "if [ -f /tmp/hercey.war.bak ]; then ${JBOSS_CLI} --connect --controller=${VM_IP}:9990 --command='deploy --force /tmp/hercey.war.bak'; fi"
                            """
                            error "JBoss VM Deployment failed and was rolled back to previous backup: ${e.message}"
                        }

                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }

        success {
            echo "Pipeline completed successfully! version ${VERSION} is active on K8S and VM"
        }

        failure {
            echo "Pipeline failed! check stage logs for more details."
        }
    }
}