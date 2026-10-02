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

        // Harbor Configuration
        HARBOR_REGISTRY = '192.168.56.50'
        HARBOR_PROJECT = 'hercey'
        HARBOR_CREDS = 'jenkins-robot'

        HARBOR_IMAGE = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${APP_NAME}:${VERSION}"
        HARBOR_IMAGE_LATEST = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${APP_NAME}:latest"

        // Nexus Repository Config
        NEXUS_HOST = 'host.docker.internal:8081'
        NEXUS_REPO = 'hercey-releases'
        NEXUS_CREDS = 'nexus-credentials'

        // VM Configuration
        VM_IP = '192.168.56.100'
        VM_USER = 'root'
        WAR_PATH = "target/${APP_NAME}.war"
        JBOSS_CLI = '/opt/jboss/jboss-eap-8.0/bin/jboss-cli.sh'
    }

    parameters {
        booleanParam(
            name: 'RUN_DAST',
            defaultValue: false,
            description: 'Check this box to execute the OWASP ZAP DAST scan'
        )
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                checkout scm
            }
        }

        stage('Secret Scanning (TruffleHog)') {
            steps {
                script {
                    echo "Scanning repository for leaked credentials with TruffleHog..."
                    sh '''
                        mkdir -p trufflehog-reports

                        docker run --rm \
                            --volumes-from $(hostname) \
                            -w $(pwd) \
                            trufflesecurity/trufflehog:latest \
                            filesystem . \
                            --config=trufflehog-config.yaml \
                            --no-verification \
                            --json > trufflehog-reports/results.json || true

                            python3 scripts/generate_trufflehog_report.py
                    '''

                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'trufflehog-reports',
                        reportFiles: 'trufflehog-report.html',
                        reportName: 'TruffleHog Secret Scan Report',
                        reportTitles: 'TruffleHog Secret Scan Report'
                    ])
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                nodejs('NodeJS 20.x') {
                    withSonarQubeEnv('SonarQubeServer') {
                        sh 'mvn clean compile org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.sources=src/main'
                    }
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
        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building custom WildFly Docker image"
                    sh "docker build -t ${HARBOR_IMAGE} -t ${HARBOR_IMAGE_LATEST} ."
                }
            }
        }
        stage('Trivy Repository & SCA Scan') {
            steps {
                script {
                    echo "Trivy Scan (Filesystem)"
                    sh '''
                        mkdir -p trivy-reports

                        # Combined FS scan using local HTML templates
                        trivy fs \
                            --scanners vuln,misconfig,secret,license \
                            --format template \
                            --template '@templates/html.tpl' \
                            -o trivy-reports/trivy-fs-report.html .
                        
                        # Generagte SBOM 
                        trivy fs --format cyclonedx -o trivy-reports/sbom-source.json .
                    '''

                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'trivy-reports',
                        reportFiles: 'trivy-fs-report.html',
                        reportName: 'Trivy Repository Scan',
                        reportTitles: 'FS Security & License Audit'
                    ])

                    archiveArtifacts artifacts: 'trivy-reports/sbom-source.json', allowEmptyArchive : false
                }
            }
        }
        stage('Trivy Image Scan & SBOM') {
            steps {
                script {
                    echo "Trivy Scan (Container)"

                    sh """
                        trivy image \
                            --format template \
                            --template '@templates/html.tpl' \
                            -o trivy-reports/trivy-image-report.html \
                            ${HARBOR_IMAGE}

                        trivy image --format cyclonedx -o trivy-reports/sbom-image.json ${HARBOR_IMAGE}
                    """

                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'trivy-reports',
                        reportFiles: 'trivy-image-report.html',
                        reportName: 'Trivy Image Scan',
                        reportTitles: 'Container Image Security Scan'
                    ])

                    archiveArtifacts artifacts: 'trivy-reports/sbom-image.json', allowEmptyArchive: false

                    sh "trivy image --severity HIGH,CRITICAL --exit-code 1 ${HARBOR_IMAGE}"
                }
            }
        }
        stage('Trivy Kubernetes Audit') {
            steps {
                script {
                    echo "Trivy Scan (Kubernetes Cluster)"
                    sh '''
                        mkdir -p trivy-reports

                        trivy k8s \
                            --report summary \
                            --format template \
                            --template '@templates/html.tpl' \
                            -o trivy-reports/trivy-k8s-report.html \
                            deployment/jboss-app || true
                    '''

                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'trivy-reports'
                        reportFiles: 'trivy-k8s-report.html',
                        reportName: 'Trivy K8S Cluster Audit',
                        reportTitles: 'Kubernetes Workload Security Audit'
                    ])
                }
            }
        }
        stage('Push image to harbor registry') {
            steps {
                withCredentials([usernamePassword(credentialsId: HARBOR_CREDS, usernameVariable: 'HARBOR_USER', passwordVariable: 'HARBOR_PASS')]) {
                    script {
                        echo "Authenticating with harbor registry"
                        sh "echo '$HARBOR_PASS' | docker login ${HARBOR_REGISTRY} -u '$HARBOR_USER' --password-stdin"

                        echo "Pushing images to Harbor..."
                        sh "docker push ${HARBOR_IMAGE}"
                        sh "docker push ${HARBOR_IMAGE_LATEST}"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes (With Rollback)') {
            steps {
                script {
                    try {
                        echo "Deploying image ${HARBOR_IMAGE} to K8S..."
                        sh "kubectl set image deployment/jboss-app jboss=${HARBOR_IMAGE}"
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
        stage('DAST Security Scan (OWASP ZAP)') {
            when {
                expression { return params.RUN_DAST == true }
            }
            steps {
                script {
                    echo "Executing OWASP ZAP Baseline Scan..."
                    sh '''
                        mkdir -p zap-reports

                        docker run --name zap_k8s_scan -u root -v $(pwd):/zap/wrk ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t http://192.168.127.2:30080/hercey/ -r zap_report_k8s.html -I || true

                        docker cp zap_k8s_scan:/zap/wrk/zap_report_k8s.html zap-reports/zap_report_k8s.html || true

                        docker rm zap_k8s_scan || true

                        docker run --name zap_jboss_scan -u root -v $(pwd):/zap/wrk ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t http://192.168.56.101:8280/hercey-fixed/ -r zap_report_jboss.html -I || true

                        docker cp zap_jboss_scan:/zap/wrk/zap_report_jboss.html zap-reports/zap_report_jboss.html || true

                        docker rm zap_jboss_scan || true
                        
                        ls -la zap-reports
                    '''
                    
                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'zap-reports',
                        reportFiles: 'zap_report_k8s.html,zap_report_jboss.html',
                        reportName: 'OWASP ZAP DAST Reports',
                        reportTitles: 'Kubernetes Scan, JBoss VM Scan'
                    ])
                }
            }
        }
    }

    post {
        always {
            cleanWs()

            script {
                echo "Running post-build cleanup..."
                sh "docker logout ${HARBOR_REGISTRY} || true"

                sh "docker rmi ${HARBOR_IMAGE} ${HARBOR_IMAGE_LATEST} || true"

                sh "docker image prune -f || true"
            }
        }

        success {
            echo "Pipeline completed successfully! version ${VERSION} is active on K8S and VM"
        }

        failure {
            echo "Pipeline failed! check stage logs for more details."
        }
    }
}