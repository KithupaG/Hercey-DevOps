pipeline {
    agent any

    tools {
        maven 'Maven for JBOSS Pipeline'
    }

    options {
        buildDiscarder(logRotator(
            daysToKeepStr: '7',
            numToKeepStr: '10',
            artifactDaysToKeepStr: '7',
            artifactNumToKeepStr: '10'
        ))
        disableConcurrentBuilds()
    }

    environment {
        // Trivy DB Cache
        TRIVY_CACHE_DIR = '/var/jenkins_home/.cache/trivy'

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

        // URL used by BOTH the readiness check and the DAST scan
        APP_URL = 'http://192.168.127.2:30080/hercey/'
    }

    parameters {
        booleanParam(
            name: 'RUN_DAST',
            defaultValue: false,
            description: 'Check this box to execute the OWASP ZAP DAST scan'
        )
        booleanParam(
            name: 'HEALTH_TEST',
            defaultValue: false,
            description: 'Check this box to run the application health test before the DAST scan (only runs together with RUN_DAST)'
        )
        booleanParam(
            name: 'UPLOAD_SCAN',
            defaultValue: false,
            description: 'Check this box to send the scan reports to DefectDojo'
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

                        python3 scripts/generate_trufflehog_report.py || true
                    '''

                    publishHTML([
                        allowMissing: true,
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
                        // Added sonar.projectKey explicitly so we can query its API afterwards
                        sh 'mvn clean compile org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.sources=src/main -Dsonar.projectKey=hercey-app'
                    }
                }
            }
        }

        stage('SonarQube Quality Gate & Export') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
                // Fetch SonarQube issues JSON report via API for DefectDojo ingestion
                withSonarQubeEnv('SonarQubeServer') {
                    sh '''
                        mkdir -p sonarqube-reports
                        curl -s -u "${SONAR_AUTH_TOKEN}:" "${SONAR_HOST_URL}/api/issues/search?componentKeys=hercey-app&ps=500" -o sonarqube-reports/sonar-report.json || true
                    '''
                }
            }
        }

        stage('Build WAR with dynamic versioning') {
            steps {
                echo "Building artifact version: ${VERSION}"
                sh "mvn clean package -DskipTests -Drevision=${VERSION}"
            }
        }

        stage('Publish Artifact to Nexus') {
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
                    echo "Building custom WildFly Docker image..."
                    sh "docker build -t ${HARBOR_IMAGE} -t ${HARBOR_IMAGE_LATEST} ."
                }
            }
        }

        stage('Trivy Security Scan') {
            steps {
                sh '''
                    mkdir -p trivy-reports

                    trivy fs --format json -o trivy-reports/trivy-fs.json .

                    # Run HTML scan (using local offline template)
                    trivy fs --scanners vuln,misconfig,secret,license \
                            --format template \
                            --template '@/var/jenkins_home/templates/html.tpl' \
                            -o trivy-reports/trivy-fs-report.html .

                    trivy fs --format cyclonedx -o trivy-reports/sbom-source.json .
                '''
            }
        }

        stage('Image Scan & SBOM') {
            steps {
                script {
                    sh '''
                        mkdir -p trivy-reports sbom-reports

                        # ---- SBOM (syft) ----
                        syft "$HARBOR_IMAGE" -o table > sbom-reports/sbom.txt
                        cat sbom-reports/sbom.txt

                        syft "$HARBOR_IMAGE" -o cyclonedx-json=sbom-reports/sbom-syft.json

                        # ---- Trivy image scan ----
                        trivy image --cache-dir "$TRIVY_CACHE_DIR" --timeout 15m --format json -o trivy-reports/trivy-image.json "$HARBOR_IMAGE"

                        trivy image --cache-dir "$TRIVY_CACHE_DIR" \
                            --format template \
                            --timeout 15m \
                            --template '@templates/html.tpl' \
                            -o trivy-reports/trivy-image-report.html \
                            "$HARBOR_IMAGE"

                        trivy image --cache-dir "$TRIVY_CACHE_DIR" --timeout 15m --format cyclonedx -o sbom-reports/sbom-trivy.json "$HARBOR_IMAGE"

                        # ---- Wrap sbom.txt in HTML so Jenkins can display it ----
                        {
                            echo '<html><head><meta charset="utf-8"><title>SBOM</title>'
                            echo '<style>body{font-family:sans-serif;margin:20px}pre{font-family:monospace;font-size:12px;overflow:auto}</style>'
                            echo '</head><body>'
                            echo "<h2>SBOM for $HARBOR_IMAGE</h2>"
                            echo '<pre>'
                            sed -e 's/&/\\&amp;/g' -e 's/</\\&lt;/g' -e 's/>/\\&gt;/g' sbom-reports/sbom.txt
                            echo '</pre></body></html>'
                        } > sbom-reports/sbom-report.html
                    '''

                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'trivy-reports',
                        reportFiles: 'trivy-image-report.html',
                        reportName: 'Trivy Image Scan',
                        reportTitles: 'Container Image Security Scan'
                    ])

                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'sbom-reports',
                        reportFiles: 'sbom-report.html',
                        reportName: 'SBOM Report',
                        reportTitles: 'Container Image SBOM'
                    ])

                    archiveArtifacts artifacts: 'sbom-reports/*', allowEmptyArchive: false
                }
            }
        }

        stage('Push image to harbor registry') {
            steps {
                withCredentials([usernamePassword(credentialsId: HARBOR_CREDS, usernameVariable: 'HARBOR_USER', passwordVariable: 'HARBOR_PASS')]) {
                    script {
                        echo "Authenticating with harbor registry..."
                        sh 'echo "$HARBOR_PASS" | docker login "$HARBOR_REGISTRY" -u "$HARBOR_USER" --password-stdin'

                        echo "Pushing images to Harbor..."
                        sh "docker push ${HARBOR_IMAGE}"
                        sh "docker push ${HARBOR_IMAGE_LATEST}"
                    }
                }
            }
        }

        // stage('Deploy WAR file to JBoss EAP VM (With Rollback & Backup)') {
        //     steps {
        //         sshagent(credentials: ['jboss-vm-ssh-key']) {
        //             script {
        //                 try {
        //                     echo "Creating backup of current deployment on VM..."
        //                     sh "ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} 'cp /tmp/hercey.war /tmp/hercey.war.bak 2>/dev/null || true'"

        //                     echo "Transferring artifact directly into the VM..."
        //                     sh "scp -o StrictHostKeyChecking=no ${WAR_PATH} ${VM_USER}@${VM_IP}:/tmp/hercey.war"

        //                     echo "Executing deployment using JBoss CLI..."
        //                     sh """
        //                         ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} \
        //                         "${JBOSS_CLI} --connect --controller=${VM_IP}:9990 --command='deploy --force /tmp/hercey.war'"
        //                     """
        //                 } catch (Exception e) {
        //                     echo "WARNING: JBoss VM deployment failed! Restoring backup WAR file..."
        //                     sh """
        //                         ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} \
        //                         "if [ -f /tmp/hercey.war.bak ]; then ${JBOSS_CLI} --connect --controller=${VM_IP}:9990 --command='deploy --force /tmp/hercey.war.bak'; fi"
        //                     """
        //                     error "JBoss VM Deployment failed and was rolled back to previous backup: ${e.message}"
        //                 }
        //             }
        //         }
        //     }
        // }
    }

    post {
        always {
            script {
                echo "Running post-build cleanup..."
                sh "docker logout ${HARBOR_REGISTRY} || true"
                sh "docker rmi ${HARBOR_IMAGE} ${HARBOR_IMAGE_LATEST} || true"
            }
            cleanWs()
        }

        success {
            echo "Pipeline completed successfully! Version ${VERSION} is active on K8S"
            sh '''
                docker image prune -f || true
                trivy clean --cache || true
            '''
        }

        failure {
            echo "Pipeline failed! Check stage logs for details."
        }
    }
}