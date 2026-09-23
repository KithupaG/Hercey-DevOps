pipeline {
    agent any

    environment {
        VM_IP = '192.168.56.100'
        VM_USER = 'root'
        WAR_PATH = target/hercey.war
        JBOSS_CLI = '/opt/jboss/jboss-eap-8.0/bin/jboss-cli.sh'
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
        stage('Deploy WAR file to JBoss CLI') {
            steps {
                sshagent(credentials: ['jboss-domain-key']) {
                    echo "Transfering artifact directly into the VM"
                    sh "scp -o StrictHostKeyChecking=no ${WAR_PATH} ${VM_USER}@${VM_IP}:/tmp/"

                    echo "Executing zero-downtime deployment using JBOSS CLI"
                    sh """
                        ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} \
                        "${JBOSS_CLI} --connect --command='deploy --force /tmp/\$(basename ${WAR_PATH})'"
                    """

                    echo "Delete the WAR file from the VM after deployment"
                    sh "ssh -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} 'rm -f /tmp/\$(basename ${WAR_PATH})'"
                }
            }
        }
    }

}