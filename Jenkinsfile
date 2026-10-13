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
                    env.SONAR_PROJECT_KEY = (name == 'prod') ? 'hercey-app' : "hercey-app-${name}".toString()

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
        //     // allOf {
        //     //         expression { return params.SECRET_SCANNING == false }
        //     // }
        //     steps {
        //         nodejs('NodeJS 20.x') {
        //             withSonarQubeEnv('SonarQubeServer') {
        //                 sh 'mvn clean compile org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.sources=src/main -Dsonar.projectKey=$SONAR_PROJECT_KEY -Dsonar.projectName="hercey ($DEPLOY_ENV)"'
        //             }
        //         }
        //     }
        // }

        // stage('SonarQube Quality Gate') {
        //     // allOf {
        //     //         expression { return params.SECRET_ANALYSIS == false }
        //     // }
        //     steps {
        //         timeout(time: 5, unit: 'MINUTES') {
        //             script {
        //                 def qg = waitForQualityGate()
        //                 if (qg.status != 'OK') {
        //                     if (env.DEPLOY_ENV == 'dev') {
        //                         unstable("Quality gate status: ${qg.status}")
        //                     } else {
        //                         error "Quality gate failed: ${qg.status}"
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
                '''
            }
        }

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

        stage('Update GitOps repository') {
            when { 
                anyOf {
                    branch 'staging'
                    branch 'main'
                }    
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-pat-key', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    sh '''
                        export PATH="$PATH:/var/jenkins_home/bin"

                        git config --global user.name "Jenkinsci@local"
                        git config --global user.email "devops-bot@gmail.lk"

                        rm -rf K8S-Fleet-Deployment-Repo
                        git clone https://${GIT_USER}:${GIT_TOKEN}@github.com/KithupaG/K8S-Fleet-Deployment-Repo.git
                        cd K8S-Fleet-Deployment-Repo/apps/jboss-app/overlays/${DEPLOY_ENV}

                        # left side  = image text in base/deployment.yaml
                        # right side = the image this build really pushed
                        kustomize edit set image 192.168.56.50/hercey/jboss-app=${HARBOR_IMAGE}

                        echo "--- resulting images block ---"
                        grep -A4 'images:' kustomization.yaml

                        git add kustomization.yaml
                        git commit -m "release(${DEPLOY_ENV}): update jboss-app image to ${VERSION} [skip ci]"
                        git push origin main
                    '''
                }
            }
        }

        // Fleet applies the commit asynchronously. Wait until the cluster really runs the new
        // image, so the audit, health test and DAST scan all test THIS build and not the old pod.
        stage('Wait for Fleet rollout') {
            when {
                anyOf {
                    branch 'staging'
                    branch 'main'
                }   
            }
            steps {
                withCredentials([file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                        CUR=""
                        for i in $(seq 1 60); do
                            CUR=$(kubectl get deploy jboss-app -n ${DEPLOY_ENV} -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
                            [ "$CUR" = "$HARBOR_IMAGE" ] && break
                            echo "Waiting for Fleet to apply $HARBOR_IMAGE (currently: $CUR)"
                            sleep 10
                        done
                        [ "$CUR" = "$HARBOR_IMAGE" ] || { echo "Fleet did not apply the new image in time"; exit 1; }

                        kubectl rollout status deployment/jboss-app -n ${DEPLOY_ENV} --timeout=300s
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