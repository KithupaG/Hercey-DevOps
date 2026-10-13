// SAME file on dev, staging and main. The only thing that differs per branch
// is the table in the "Load environment config" stage, and it is picked
// automatically from the branch name, so merging dev -> staging -> main
// never changes behaviour or credentials by accident.

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

    // Only values that are identical in every environment live here.
    environment {
        TRIVY_CACHE_DIR = '/var/jenkins_home/.cache/trivy'

        APP_NAME    = 'hercey'
        MAJOR_MINOR = '1.0'

        HARBOR_REGISTRY = '192.168.56.50'
        NEXUS_HOST      = 'host.docker.internal:8081'
        WAR_PATH        = "target/${APP_NAME}.war"

        // VM Configuration
        VM_IP     = '192.168.56.100'
        VM_USER   = 'root'
        JBOSS_CLI = '/opt/jboss/jboss-eap-8.0/bin/jboss-cli.sh'
    }

    parameters {
        // booleanParam(
        //     name: 'SOURCE_CODE_SCANNING',
        //     defaultValue: true,
        //     description: 'Disable to stop SCM scan'
        // )
        // booleanParam(
        //     name: 'SECRET_SCANNING',
        //     defaultValue: true,
        //     description: 'Disable to stop trufflehog secret scan'
        // )
        // booleanParam(
        //     name: 'SONARQUBE_ANALYSIS',
        //     defaultValue: true,
        //     description: 'Disable to stop sonarqube scan'
        // )
        // booleanParam(
        //     name: 'SONARQUBE_QUALITY_GATE',
        //     defaultValue: true,
        //     description: 'Disable to stop sonarqube quality gate'
        // )
        // booleanParam(
        //     name: 'BUILD_WAR',
        //     defaultValue: true,
        //     description: 'Disable to stop building of the war file'
        // )
        // booleanParam(
        //     name: 'TRIVY_SECURITY_SCAN',
        //     defaultValue: true,
        //     description: 'Disable to stop trivy SCA scan'
        // )
        // booleanParam(
        //     name: 'BUILD_DOCKER_IMAGE',
        //     defaultValue: true,
        //     description: 'Disable to stop docker image being built'
        // )
        // booleanParam(
        //     name: 'TRIVY_IMAGE_SCAN_SBOM',
        //     defaultValue: true,
        //     description: 'Disable to stop trivy from scanning your image and generating an SBOM'
        // )
        // booleanParam(
        //     name: 'TRIVY_KUBERNETES_SCAN',
        //     defaultValue: true,
        //     description: 'Disable to stop trivy from scanning kubernetes manifest files'
        // )
        booleanParam(
            name: 'RUN_DAST',
            defaultValue: true,
            description: 'Check this box to execute the OWASP ZAP DAST scan'
        )
        booleanParam(
            name: 'HEALTH_TEST',
            defaultValue: true,
            description: 'Check this box to run the application health test before the DAST scan'
        )
    }

    stages {
        stage('Load environment config') {
            steps {
                script {
                    def configs = [
                        dev    : [harborProject: 'hercey-dev',     harborCreds: 'jenkins-dev-robot',     nexusRepo: 'hercey-dev',      nexusCreds: 'nexus-credentials',     versionSuffix: '-SNAPSHOT', tagPrefix: 'dev-',     appPort: '30081'],
                        staging: [harborProject: 'hercey-staging', harborCreds: 'jenkins-staging-robot', nexusRepo: 'hercey-staging',  nexusCreds: 'nexus-credentials', versionSuffix: '-rc',       tagPrefix: 'staging-', appPort: '30082'],
                        prod   : [harborProject: 'hercey',         harborCreds: 'jenkins-robot',         nexusRepo: 'hercey-releases', nexusCreds: 'nexus-credentials',         versionSuffix: '',          tagPrefix: '',         appPort: '30083']
                    ]

                    def name = 'dev'
                    if (env.BRANCH_NAME == 'main') {
                        name = 'prod'
                    } else if (env.BRANCH_NAME == 'staging') {
                        name = 'staging'
                    }
                    def c = configs[name]

                    env.DEPLOY_ENV        = name
                    env.HARBOR_PROJECT    = c.harborProject
                    env.HARBOR_CREDS      = c.harborCreds
                    env.NEXUS_REPO        = c.nexusRepo
                    env.NEXUS_CREDS       = c.nexusCreds
                    env.VERSION           = "${env.MAJOR_MINOR}.${env.BUILD_NUMBER}${c.versionSuffix}".toString()
                    env.IMAGE_TAG         = "${c.tagPrefix}${env.MAJOR_MINOR}.${env.BUILD_NUMBER}".toString()
                    env.HARBOR_IMAGE      = "${env.HARBOR_REGISTRY}/${c.harborProject}/${env.APP_NAME}:${env.IMAGE_TAG}".toString()
                    env.APP_URL           = "http://host.docker.internal:${c.appPort}/${env.APP_NAME}/".toString()
                    env.APP_HEALTH_URL    = "http://host.docker.internal:${c.appPort}/${env.APP_NAME}/health".toString()
                    env.APP_PORT          = c.appPort
                    env.SONAR_PROJECT_KEY = (name == 'prod') ? 'hercey-app' : "hercey-app-${name}".toString()
                    env.ENFORCE_SECURITY  = (name == 'dev') ? 'false' : 'true'

                    echo "Branch '${env.BRANCH_NAME}' -> environment '${env.DEPLOY_ENV}'"
                    echo "Version: ${env.VERSION} | Image: ${env.HARBOR_IMAGE} | Nexus repo: ${env.NEXUS_REPO}"
                }
            }
        }

        stage('Checkout Source Code') {
            // allOf {
            //         expression { return params.SOURCE_CODE_SCANNING == false }
            // }
            steps {
                checkout scm
            }
        }

        stage('Secret Scanning (TruffleHog)') {
            // allOf {
            //         expression { return params.SECRET_SCANNING == false }
            // }
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
        //                 sh 'mvn clean compile org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.sources=src/main -Dsonar.projectKey=$SONAR_PROJECT_KEY -Dsonar.projectName="hercey ($DEPLOY_ENV)"'
        //             }
        //         }
        //     }
        // }

        // stage('SonarQube Quality Gate') {
        //     steps {
        //         timeout(time: 5, unit: 'MINUTES') {
        //             script {
        //                 def qg = waitForQualityGate()
        //                 if (qg.status != 'OK') {
        //                     if (env.ENFORCE_SECURITY == 'true') {
        //                         error "Quality gate failed: ${qg.status}"
        //                     } else {
        //                         unstable("Quality gate status: ${qg.status}")
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }

        stage('Build WAR with dynamic versioning') {
            // allOf {
            //         expression { return params.BUILD_WAR == false }
            // }
            steps {
                echo "Building artifact version: ${env.VERSION}"
                sh 'mvn clean package -DskipTests -Drevision=$VERSION'
            }
        }

        /*
        stage('Trivy Security Scan') {
            // allOf {
            //         expression { return params.TRIVY_SECURITY_SCAN == false }
            // }
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

                    # Fail the build on HIGH/CRITICAL vulnerabilities for staging/prod.
                    if [ "$ENFORCE_SECURITY" = "true" ]; then
                        echo "Enforcing HIGH/CRITICAL vulnerability gate for ${BRANCH_NAME}..."
                        trivy fs --scanners vuln --severity HIGH,CRITICAL --exit-code 1 \
                                --format table .
                    else
                        echo "Non-blocking scan (branch ${BRANCH_NAME}); reporting only."
                    fi
                '''
            }
        }
        */

        stage('Build Docker Image') {
            // allOf {
            //         expression { return params.BUILD_DOCKER_IMAGE == false }
            // }
            steps {
                echo "Building custom WildFly Docker image: ${env.HARBOR_IMAGE}"
                sh 'docker build -t "$HARBOR_IMAGE" .'
            }
        }

        // stage('Image Scan & SBOM') {
        //     // allOf {
        //     //         expression { return params.TRIVY_IMAGE_SCAN_SBOM == false }
        //     // }
        //     steps {
        //         script {
        //             sh '''
        //                 mkdir -p trivy-reports sbom-reports

        //                 # ---- SBOM (syft) ----
        //                 syft "$HARBOR_IMAGE" -o table > sbom-reports/sbom.txt
        //                 cat sbom-reports/sbom.txt

        //                 syft "$HARBOR_IMAGE" -o cyclonedx-json=sbom-reports/sbom-syft.json

        //                 # ---- Trivy image scan ----
        //                 trivy image --cache-dir "$TRIVY_CACHE_DIR" --timeout 15m --format json -o trivy-reports/trivy-image.json "$HARBOR_IMAGE"

        //                 trivy image --cache-dir "$TRIVY_CACHE_DIR" \
        //                     --format template \
        //                     --timeout 15m \
        //                     --template '@templates/html.tpl' \
        //                     -o trivy-reports/trivy-image-report.html \
        //                     "$HARBOR_IMAGE"

        //                 trivy image --cache-dir "$TRIVY_CACHE_DIR" --timeout 15m --format cyclonedx -o sbom-reports/sbom-trivy.json "$HARBOR_IMAGE"

        //                 # ---- Wrap sbom.txt in HTML so Jenkins can display it ----
        //                 {
        //                     echo '<html><head><meta charset="utf-8"><title>SBOM</title>'
        //                     echo '<style>body{font-family:sans-serif;margin:20px}pre{font-family:monospace;font-size:12px;overflow:auto}</style>'
        //                     echo '</head><body>'
        //                     echo "<h2>SBOM for $HARBOR_IMAGE</h2>"
        //                     echo '<pre>'
        //                     sed -e 's/&/\\&amp;/g' -e 's/</\\&lt;/g' -e 's/>/\\&gt;/g' sbom-reports/sbom.txt
        //                     echo '</pre></body></html>'
        //                 } > sbom-reports/sbom-report.html
        //             '''

        //             publishHTML([
        //                 allowMissing: false,
        //                 alwaysLinkToLastBuild: true,
        //                 keepAll: true,
        //                 reportDir: 'trivy-reports',
        //                 reportFiles: 'trivy-image-report.html',
        //                 reportName: 'Trivy Image Scan',
        //                 reportTitles: 'Container Image Security Scan'
        //             ])

        //             publishHTML([
        //                 allowMissing: false,
        //                 alwaysLinkToLastBuild: true,
        //                 keepAll: true,
        //                 reportDir: 'sbom-reports',
        //                 reportFiles: 'sbom-report.html',
        //                 reportName: 'SBOM Report',
        //                 reportTitles: 'Container Image SBOM'
        //             ])

        //             archiveArtifacts artifacts: 'sbom-reports/*', allowEmptyArchive: false
        //         }
        //     }
        // }

        // Feature branches and PRs stop above: they are built and scanned,

        stage('Approve production release') {
            when { 
                branch 'main'
            }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        String typedPassword = input(
                            message: 'Verification Required',
                            ok: 'Verify and Continue',
                            parameters: [
                                password(name: 'userPassword', description: 'Enter the pipeline password')
                            ]
                        )

                        withCredentials([string(credentialsId: 'prod-approval-secret', variable: 'CORRECT_PASS')]) {
                            if (typedPassword == CORRECT_PASS) {
                                echo "Verification successful. Continuing pipeline..."
                            } else {
                                currentBuild.result = 'ABORTED'
                                error("Access Denied: Incorrect Password entered")
                            }
                        }
                    }
                }
            }
        }


        stage('Publish Artifact to Nexus') {
            when {
                anyOf {
                    branch 'dev'
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: "${NEXUS_HOST}",
                    groupId: 'com.hercey',
                    version: "${env.VERSION}",
                    repository: "${env.NEXUS_REPO}",
                    credentialsId: "${env.NEXUS_CREDS}",
                    artifacts: [
                        [artifactId: "${APP_NAME}", classifier: '', file: "${WAR_PATH}", type: 'war']
                    ]
                )
            }
        }

        stage('Push image to Harbor') {
            when {
                anyOf {
                    branch 'dev'
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: env.HARBOR_CREDS, usernameVariable: 'HARBOR_USER', passwordVariable: 'HARBOR_PASS')]) {
                    script {
                        echo "Authenticating with harbor registry as the ${env.DEPLOY_ENV} robot..."
                        sh 'echo "$HARBOR_PASS" | docker login "$HARBOR_REGISTRY" -u "$HARBOR_USER" --password-stdin'

                        echo "Pushing image to Harbor..."
                        sh 'docker push "$HARBOR_IMAGE"'
                    }
                }
            }
        }

        stage('Package & Push Helm chart to Harbor') {
            when {
                anyOf {
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: env.HARBOR_CREDS, usernameVariable: 'HARBOR_USER', passwordVariable: 'HARBOR_PASS')]) {
                    sh '''
                        export HELM_BIN="$HOME/.local/bin"
                        mkdir -p "$HELM_BIN"
                        export PATH="$HELM_BIN:$PATH"

                        if [ ! -x "$HELM_BIN/helm" ]; then
                            echo "Installing Helm..."
                            curl -fsSL https://get.helm.sh/helm-v4.1.0-linux-amd64.tar.gz -o /tmp/helm.tgz
                            tar -xzf /tmp/helm.tgz -C /tmp
                            install -m 0755 /tmp/linux-amd64/helm "$HELM_BIN/helm"
                            rm -rf /tmp/helm.tgz /tmp/linux-amd64
                        fi
                        helm version --short

                        rm -rf dist
                        helm package helm/hercey --version "$VERSION" --app-version "$IMAGE_TAG" -d dist

                        echo "$HARBOR_PASS" | helm registry login "$HARBOR_REGISTRY" -u "$HARBOR_USER" --password-stdin --plain-http || echo "WARNING: Harbor login failed; continuing."
                        helm push "dist/hercey-$VERSION.tgz" "oci://$HARBOR_REGISTRY/$HARBOR_PROJECT/charts" --plain-http || echo "WARNING: Harbor chart push failed; deployment will use the local chart."
                    '''
                }
            }
        }

        stage('Deploy with Helm') {
            when {
                anyOf {
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                withCredentials([
                    usernamePassword(credentialsId: env.HARBOR_CREDS, usernameVariable: 'HARBOR_USER', passwordVariable: 'HARBOR_PASS'),
                    file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')
                ]) {
                    sh '''
                        export PATH="$HOME/.local/bin:$PATH"

                        echo "$HARBOR_PASS" | helm registry login "$HARBOR_REGISTRY" -u "$HARBOR_USER" --password-stdin --plain-http || echo "WARNING: Harbor login failed; falling back to the local chart."

                        HELM_ARGS="--namespace $DEPLOY_ENV --create-namespace \
                            --set image.repository=$HARBOR_REGISTRY/$HARBOR_PROJECT/$APP_NAME \
                            --set image.tag=$IMAGE_TAG \
                            --set service.type=NodePort \
                            --set service.nodePort=$APP_PORT \
                            --wait --rollback-on-failure --timeout 5m \
                            --take-ownership --force-conflicts"

                        if helm upgrade --install jboss-app "oci://$HARBOR_REGISTRY/$HARBOR_PROJECT/charts/hercey" --version "$VERSION" --plain-http $HELM_ARGS; then
                            echo "Deployed jboss-app from Harbor chart oci://$HARBOR_REGISTRY/$HARBOR_PROJECT/charts/hercey:$VERSION"
                        else
                            echo "WARNING: deploy from Harbor failed; falling back to local chart dist/hercey-$VERSION.tgz"
                            helm upgrade --install jboss-app "./dist/hercey-$VERSION.tgz" $HELM_ARGS
                        fi
                    '''
                }
            }
        }

        stage('Trivy Kubernetes Audit') {
            when {
                anyOf {
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                withCredentials([file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')]) {
                    script {
                        echo "Auditing Deployed Workload on Kubernetes Cluster..."
                        sh '''
                            mkdir -p trivy-reports

                            # trivy k8s has no template format; scan to JSON, then convert to HTML.
                            trivy k8s \
                                --report summary \
                                --include-namespaces ${DEPLOY_ENV} \
                                --format json \
                                -o trivy-reports/trivy-k8s-report.json || true

                            trivy convert \
                                --format template \
                                --template '@templates/html.tpl' \
                                -o trivy-reports/trivy-k8s-report.html \
                                trivy-reports/trivy-k8s-report.json || true

                            # Fail the build on HIGH/CRITICAL findings for staging/prod.
                            if [ "$ENFORCE_SECURITY" = "true" ]; then
                                echo "Enforcing HIGH/CRITICAL audit gate for ${BRANCH_NAME}..."
                                trivy k8s --report summary --include-namespaces ${DEPLOY_ENV} \
                                    --severity HIGH,CRITICAL --exit-code 1
                            fi
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

        stage('Wait for App Readiness') {
            when {
                allOf {
                    expression { return params.HEALTH_TEST == true }
                }
                anyOf {
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                sh '''
                    MAX_ATTEMPTS=30
                    ATTEMPT=1

                    echo "Waiting for $APP_HEALTH_URL to become healthy..."

                    while true; do
                        CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$APP_HEALTH_URL" || true)
                        case "$CODE" in
                            2*|3*)
                                echo "Application is UP and HEALTHY (HTTP $CODE). Proceeding to the DAST scan."
                                exit 0
                                ;;
                        esac
                        if [ "$ATTEMPT" -ge "$MAX_ATTEMPTS" ]; then
                            echo "Error: application did not become healthy (last HTTP code: $CODE)."
                            exit 1
                        fi
                        echo "Attempt $ATTEMPT/$MAX_ATTEMPTS: HTTP $CODE, retrying in 10s..."
                        ATTEMPT=$((ATTEMPT + 1))
                        sleep 10
                    done
                '''
            }
        }

        stage('DAST Security Scan (OWASP ZAP)') {
            when {
                allOf {
                    expression { return params.RUN_DAST == true }
                }
                anyOf {
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                script {
                    echo "Executing OWASP ZAP Baseline Scan against ${APP_URL}..."
                    sh '''
                        mkdir -p zap-reports

                        # remove a leftover container from a previous failed run
                        docker rm -f zap_k8s_scan || true

                        # K8s DAST Scan: -J generates JSON for DefectDojo
                        docker run --name zap_k8s_scan -u root -v $(pwd):/zap/wrk ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t "$APP_URL" -r zap_report_k8s.html -J zap_report_k8s.json -I || true
                        docker cp zap_k8s_scan:/zap/wrk/zap_report_k8s.html zap-reports/zap_report_k8s.html || true
                        docker cp zap_k8s_scan:/zap/wrk/zap_report_k8s.json zap-reports/zap_report_k8s.json || true
                        docker rm zap_k8s_scan || true
                    '''

                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'zap-reports',
                        reportFiles: 'zap_report_k8s.html',
                        reportName: 'OWASP ZAP DAST Reports',
                        reportTitles: 'Kubernetes Scan'
                    ])
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
                sh "docker rmi ${env.HARBOR_IMAGE} || true"
            }
            cleanWs()
        }

        success {
            echo "Pipeline completed. Branch ${env.BRANCH_NAME} -> ${env.DEPLOY_ENV}, version ${env.VERSION}."
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