pipeline {
    agent any

    tools {
        maven 'Maven for JBOSS Pipeline'
    }

    environment {
        // VM Configuration
        VM_IP = '192.168.56.100'
        VM_USER = 'root'
        WAR_PATH = 'target/hercey.war'
        JBOSS_CLI = '/opt/jboss/jboss-eap-8.0/bin/jboss-cli.sh'

        // Kubernetes Configuration
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
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo "Copying compiled WAR to Docker build context..."
                    sh "cp ${WAR_PATH} hercey.war"

                    echo "Building Docker image version ${BUILD_NUMBER}..."
                    sh "docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} ."

                    echo "Updating Kubernetes Deployment..."
                    sh "kubectl set image deployment/jboss-app jboss=${IMAGE_NAME}:${BUILD_NUMBER}"

                    echo "Waiting for rollout to complete..."
                    sh "kubectl rollout status deployment/jboss-app"
                }
            }
        }
        stage('Deploy WAR file to JBoss EAP VM') {
            steps {
                sshagent(credentials: ['jboss-vm-ssh-key']) {
                    echo "Transferring artifact directly into the VM..."
                    sh "scp -o StrictHostKeyChecking=no ${WAR_PATH} ${VM_USER}@${VM_IP}:/tmp/"

                    echo "Executing deployment using JBoss CLI..."
                    sh """
                        ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} \
                        "${JBOSS_CLI} --connect --controller=${VM_IP}:9990 --command='deploy --force /tmp/\$(basename ${WAR_PATH})'"
                    """

                    echo "Cleaning up temporary WAR file from VM..."
                    sh "ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} 'rm -f /tmp/\$(basename ${WAR_PATH})'"
                }
            }
        }
    }
}