pipeline {
    agent any

    tools {
        maven 'Maven for JBOSS Pipeline'
    }

    environment {
        VM_IP = '192.168.56.100'
        VM_USER = 'root'
        WAR_PATH = 'target/hercey.war'
        JBOSS_CLI = '/opt/jboss/jboss-eap-8.0/bin/jboss-cli.sh'

        IMAGE_NAME = 'my-jboss-app'
        CONTAINERD_SOCK = '/run/docker/containerd/containerd.sock'
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                checkout scm
            }
        }
        stage('Build WAR file') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo "Copying compiled WAR to docker..."
                    sh "cp ${WAR_PATH} hercey.war"

                    echo "Buiilding docker image version ${BUILD_NUMBER}..."
                    sh "docker build -t ${IMAGE_NAME}:${BUILD_NUMBER}"

                    echo "Importing image into rancher desktop"
                    sh "docker save ${IMAGE_NAME}:${BUILD_NUMBER} | ctr --address ${CONTAINERD_SOCK} -n k8s.io images -"

                    echo "Updaiting kubernetes deployment..."
                    sh "kubectl set image deployment/jboss-app jboss=${IMAGE_NAME}:${BUILD_NUMBER}"

                    echo "Waiting for rollout to complete..."
                    sh "kubectl rollout status deployment/jboss-app"
                }
            }
        }
        stage('Deploy WAR file to JBoss CLI') {
            steps {
                sshagent(credentials: ['jboss-vm-ssh-key']) {
                    echo "Transfering artifact directly into the VM"
                    sh "scp -o StrictHostKeyChecking=no ${WAR_PATH} ${VM_USER}@${VM_IP}:/tmp/"

                    echo "Executing zero-downtime deployment using JBOSS CLI"
                    sh """
                        ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} \
                        "${JBOSS_CLI} --connect --controller=192.168.56.100:9990 --command='deploy --force /tmp/\$(basename ${WAR_PATH})'"
                    """

                    echo "Delete the WAR file from the VM after deployment"
                    sh "ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} 'rm -f /tmp/\$(basename ${WAR_PATH})'"
                }
            }
        }
    }

}