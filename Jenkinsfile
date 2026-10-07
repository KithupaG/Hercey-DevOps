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

        // stage('SonarQube Analysis') {
        //     steps {
        //         nodejs('NodeJS 20.x') {
        //             withSonarQubeEnv('SonarQubeServer') {
        //                 // Added sonar.projectKey explicitly so we can query its API afterwards
        //                 sh 'mvn clean compile org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.sources=src/main -Dsonar.projectKey=hercey-app'
        //             }
        //         }
        //     }
        // }

        // stage('SonarQube Quality Gate & Export') {
        //     steps {
        //         timeout(time: 5, unit: 'MINUTES') {
        //             waitForQualityGate abortPipeline: true
        //         }
        //         // Fetch SonarQube issues JSON report via API for DefectDojo ingestion
        //         withSonarQubeEnv('SonarQubeServer') {
        //             sh '''
        //                 mkdir -p sonarqube-reports
        //                 curl -s -u "${SONAR_AUTH_TOKEN}:" "${SONAR_HOST_URL}/api/issues/search?componentKeys=hercey-app&ps=500" -o sonarqube-reports/sonar-report.json || true
        //             '''
        //         }
        //     }
        // }

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

        // ---------------------------------------------------------------
        // CHANGED: SBOM files now go to sbom-reports/, plus an HTML view.
        // The separate "Generate SBOM" stage was removed (it used the
        // wrong image name and duplicated this stage).
        // ---------------------------------------------------------------
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

        stage('Trivy Kubernetes Audit') {
            steps {
                withCredentials([file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')]) {
                    script {
                        echo "Auditing Deployed Workload on Kubernetes Cluster..."
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
                            reportDir: 'trivy-reports',
                            reportFiles: 'trivy-k8s-report.html',
                            reportName: 'Trivy K8s Cluster Audit',
                            reportTitles: 'Kubernetes Workload Security Audit'
                        ])
                    }
                }
            }
        }

        stage('Push image to harbor registry') {
            steps {
                withCredentials([usernamePassword(credentialsId: HARBOR_CREDS, usernameVariable: 'HARBOR_USER', passwordVariable: 'HARBOR_PASS')]) {
                    script {
                        echo "Authenticating with harbor registry..."
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
                withCredentials([file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')]) {
                    script {
                        sh 'kubectl cluster-info'   // fails fast if the connection is wrong
                        try {
                            echo "Deploying image ${HARBOR_IMAGE} to K8S..."
                            sh "kubectl set image deployment/jboss-app jboss=${HARBOR_IMAGE}"
                            sh "kubectl rollout status deployment/jboss-app --timeout=60s"
                        } catch (Exception e) {
                            echo "WARNING: Kubernetes deployment failed! Initiating automatic rollback..."
                            sh "kubectl rollout undo deployment/jboss-app"
                            sh "kubectl rollout status deployment/jboss-app"
                            error "Kubernetes Deployment failed and was rolled back to previous stable state: ${e.message}"
                        }
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

        stage('DAST Security Scan (OWASP ZAP)') {
            when {
                expression { return params.RUN_DAST == true }
            }
            steps {
                script {
                    echo "Executing OWASP ZAP Baseline Scan..."
                    sh '''
                        mkdir -p zap-reports

                        # K8s DAST Scan: Added -J to generate JSON report for DefectDojo
                        docker run --name zap_k8s_scan -u root -v $(pwd):/zap/wrk ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t http://192.168.127.2:30080/hercey/ -r zap_report_k8s.html -J zap_report_k8s.json -I || true
                        docker cp zap_k8s_scan:/zap/wrk/zap_report_k8s.html zap-reports/zap_report_k8s.html || true
                        docker cp zap_k8s_scan:/zap/wrk/zap_report_k8s.json zap-reports/zap_report_k8s.json || true
                        docker rm zap_k8s_scan || true
                    '''

                    // # JBoss VM DAST Scan: Added -J to generate JSON report for DefectDojo
                    // docker run --name zap_jboss_scan -u root -v $(pwd):/zap/wrk ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t http://192.168.56.101:8280/hercey-fixed/ -r zap_report_jboss.html -J zap_report_jboss.json -I || true
                    // docker cp zap_jboss_scan:/zap/wrk/zap_report_jboss.html zap-reports/zap_report_jboss.html || true
                    // docker cp zap_jboss_scan:/zap/wrk/zap_report_jboss.json zap-reports/zap_report_jboss.json || true
                    // docker rm zap_jboss_scan || true

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

        stage('Upload findings to DefectDojo') {
            steps {
                withCredentials([string(credentialsId: 'defectdojo-api-token', variable: 'DOJO_TOKEN')]) {
                    script {
                        def dojoUrl = "http://192.168.56.50:8080/api/v2/import-scan/"
                        def productName = "Hercey-Application"
                        def productType = "Research and Development"
                        def engagementName = "CI/CD Automated Scans"

                        withEnv([
                            "API_URL=${dojoUrl}",
                            "PROD_NAME=${productName}",
                            "PROD_TYPE=${productType}",
                            "ENG_NAME=${engagementName}"
                        ]) {

                            // 1. Upload SonarQube Scan
                            // if (fileExists('sonarqube-reports/sonar-report.json')) {
                            //     echo "Uploading SonarQube scan to DefectDojo..."
                            //     sh '''
                            //         curl -v -X POST "$API_URL" \
                            //         -H "Authorization: Token $DOJO_TOKEN" \
                            //         -F "scan_type=SonarQube Scan" \
                            //         -F "product_name=$PROD_NAME" \
                            //         -F "product_type_name=$PROD_TYPE" \
                            //         -F "engagement_name=$ENG_NAME" \
                            //         -F "auto_create_context=true" \
                            //         -F "close_old_findings=true" \
                            //         -F "file=@sonarqube-reports/sonar-report.json"
                            //     '''
                            // }

                            // 2. Upload TruffleHog Scan
                            if (fileExists('trufflehog-reports/results.json')) {
                                echo "Uploading TruffleHog scan to DefectDojo..."
                                sh '''
                                    curl -v -X POST "$API_URL" \
                                    -H "Authorization: Token $DOJO_TOKEN" \
                                    -F "scan_type=TruffleHog Scan" \
                                    -F "product_name=$PROD_NAME" \
                                    -F "product_type_name=$PROD_TYPE" \
                                    -F "engagement_name=$ENG_NAME" \
                                    -F "auto_create_context=true" \
                                    -F "close_old_findings=true" \
                                    -F "file=@trufflehog-reports/results.json"
                                '''
                            }

                            // 3. Upload Trivy Filesystem Scan (Service: Source Code)
                            if (fileExists('trivy-reports/trivy-fs.json')) {
                                echo "Uploading Trivy Filesystem scan to DefectDojo..."
                                sh '''
                                    curl -v -X POST "$API_URL" \
                                    -H "Authorization: Token $DOJO_TOKEN" \
                                    -F "scan_type=Trivy Scan" \
                                    -F "service=Source Code FS" \
                                    -F "product_name=$PROD_NAME" \
                                    -F "product_type_name=$PROD_TYPE" \
                                    -F "engagement_name=$ENG_NAME" \
                                    -F "auto_create_context=true" \
                                    -F "close_old_findings=true" \
                                    -F "file=@trivy-reports/trivy-fs.json"
                                '''
                            }

                            // 4. Upload Trivy Container Image Scan (Service: Container Image)
                            if (fileExists('trivy-reports/trivy-image.json')) {
                                echo "Uploading Trivy Image scan to DefectDojo..."
                                sh '''
                                    curl -v -X POST "$API_URL" \
                                    -H "Authorization: Token $DOJO_TOKEN" \
                                    -F "scan_type=Trivy Scan" \
                                    -F "service=Container Image" \
                                    -F "product_name=$PROD_NAME" \
                                    -F "product_type_name=$PROD_TYPE" \
                                    -F "engagement_name=$ENG_NAME" \
                                    -F "auto_create_context=true" \
                                    -F "close_old_findings=true" \
                                    -F "file=@trivy-reports/trivy-image.json"
                                '''
                            }

                            // 5. OWASP ZAP K8s Scan
                            if (fileExists('zap-reports/zap_report_k8s.json')) {
                                echo "Uploading OWASP ZAP K8s scan to DefectDojo..."
                                sh '''
                                    curl -v -X POST "$API_URL" \
                                    -H "Authorization: Token $DOJO_TOKEN" \
                                    -F "scan_type=ZAP Scan" \
                                    -F "service=K8s DAST" \
                                    -F "product_name=$PROD_NAME" \
                                    -F "product_type_name=$PROD_TYPE" \
                                    -F "engagement_name=$ENG_NAME" \
                                    -F "auto_create_context=true" \
                                    -F "close_old_findings=true" \
                                    -F "file=@zap-reports/zap_report_k8s.json"
                                '''
                            }

                            // 6. OWASP ZAP JBoss VM Scan
                            // if (fileExists('zap-reports/zap_report_jboss.json')) {
                            //     echo "Uploading OWASP ZAP JBoss VM scan to DefectDojo..."
                            //     sh '''
                            //         curl -v -X POST "$API_URL" \
                            //         -H "Authorization: Token $DOJO_TOKEN" \
                            //         -F "scan_type=ZAP Scan" \
                            //         -F "service=JBoss VM DAST" \
                            //         -F "product_name=$PROD_NAME" \
                            //         -F "product_type_name=$PROD_TYPE" \
                            //         -F "engagement_name=$ENG_NAME" \
                            //         -F "auto_create_context=true" \
                            //         -F "close_old_findings=true" \
                            //         -F "file=@zap-reports/zap_report_jboss.json"
                            //     '''
                            // }
                        }
                    }
                }
            }
        }

        stage('Update GitOps repository') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'github-pat-key', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                        sh '''
                            git config --global user.name "Jenkinsci@local"
                            git config --global user.email "devops-bot@gmail.lk"

                            git clone https://${GIT_USER}:${GIT_TOKEN}@github.com/KithupaG/K8S-Fleet-Deployment-Repo.git
                            cd K8S-Fleet-Deployment-Repo/apps/jboss-app/overlays/dev

                            kustomize edit set image 192.168.56.50/hercey/jboss-app=192.168.56.50/hercey/jboss-app:${VERSION}


                            git add kustomization.yaml
                            git commit -m "release: update jboss-app image to {$VERSION} [skip ci]"
                            git push origin main
                        '''
                    }
                }
            }
        }
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
            echo "Pipeline completed successfully! Version ${VERSION} is active on K8S and VM"
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