pipeline {
    agent any

    parameters {
        choice(
            name: 'IMAGE_TAG',
            choices: ['1.0', '1.1', '1.2'],
            description: 'Select the ECR image tag to deploy'
        )

        choice(
            name: 'ENVIRONMENT',
            choices: ['DEV', 'QA', 'UAT', 'PROD'],
            description: 'Select the deployment environment'
        )
    }

    environment {
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '474025757344'
        ECR_REPOSITORY = 'devops-demo-app'
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        IMAGE_NAME = "${ECR_REGISTRY}/${ECR_REPOSITORY}"

        DEV_HOST  = '172.31.16.10'
        QA_HOST   = '172.31.17.210'
        UAT_HOST  = '172.31.20.220'
        PROD_HOST = '172.31.25.181'
    }

    stages {

        stage('Checkout') {
            when {
                expression {
                    return params.ENVIRONMENT == 'DEV'
                }
            }
            steps {
                checkout scm
            }
        }

        stage('Maven Build') {
            when {
                expression {
                    return params.ENVIRONMENT == 'DEV'
                }
            }
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            when {
                expression {
                    return params.ENVIRONMENT == 'DEV'
                }
            }
            steps {
                sh '''
                    docker build \
                        -t devops-demo-app:${IMAGE_TAG} .
                '''
            }
        }

        stage('ECR Login') {
            when {
                expression {
                    return params.ENVIRONMENT == 'DEV'
                }
            }
            steps {
                sh '''
                    aws ecr get-login-password \
                        --region ${AWS_REGION} |
                    docker login \
                        --username AWS \
                        --password-stdin ${ECR_REGISTRY}
                '''
            }
        }

        stage('Docker Tag') {
            when {
                expression {
                    return params.ENVIRONMENT == 'DEV'
                }
            }
            steps {
                sh '''
                    docker tag \
                        devops-demo-app:${IMAGE_TAG} \
                        ${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }

        stage('Docker Push') {
            when {
                expression {
                    return params.ENVIRONMENT == 'DEV'
                }
            }
            steps {
                sh '''
                    docker push ${IMAGE_NAME}:${IMAGE_TAG}
                '''
            }
        }

        stage('Check ECR Image') {
            steps {
                sh '''
                    echo "Checking ECR image..."
                    echo "Repository: ${IMAGE_NAME}"
                    echo "Selected tag: ${IMAGE_TAG}"

                    aws ecr describe-images \
                        --repository-name ${ECR_REPOSITORY} \
                        --image-ids imageTag=${IMAGE_TAG} \
                        --region ${AWS_REGION} \
                        --query 'imageDetails[0].imageDigest' \
                        --output text
                '''
            }
        }

        stage('Deploy') {
            steps {
                script {
                    def targetHost = ''

                    if (params.ENVIRONMENT == 'DEV') {
                        targetHost = env.DEV_HOST
                    } else if (params.ENVIRONMENT == 'QA') {
                        targetHost = env.QA_HOST
                    } else if (params.ENVIRONMENT == 'UAT') {
                        targetHost = env.UAT_HOST
                    } else if (params.ENVIRONMENT == 'PROD') {
                        targetHost = env.PROD_HOST
                    }

                    echo "=========================================="
                    echo "Deploying Application"
                    echo "Environment : ${params.ENVIRONMENT}"
                    echo "Image Tag   : ${params.IMAGE_TAG}"
                    echo "Target Host : ${targetHost}"
                    echo "Image       : ${IMAGE_NAME}:${IMAGE_TAG}"
                    echo "=========================================="

                    sshagent(credentials: ['ec2-deploy-key']) {

                        sh """
                            ssh -o StrictHostKeyChecking=no ubuntu@${targetHost} '
                                set -e

                                echo "Logging in to Amazon ECR..."

                                aws ecr get-login-password \
                                    --region ${AWS_REGION} |
                                docker login \
                                    --username AWS \
                                    --password-stdin ${ECR_REGISTRY}

                                echo "Pulling image..."

                                docker pull ${IMAGE_NAME}:${IMAGE_TAG}

                                echo "Removing existing container if present..."

                                docker rm -f devops-demo-app-${params.ENVIRONMENT} 2>/dev/null || true

                                echo "Starting new container..."

                                docker run -d \
                                    --name devops-demo-app-${params.ENVIRONMENT} \
                                    -p 8080:8080 \
                                    ${IMAGE_NAME}:${IMAGE_TAG}

                                echo "Waiting for application to start..."

                                for i in {1..12}; do
    if curl -f http://localhost:8080; then
        break
    fi
    echo "Application not ready yet. Waiting..."
    sleep 2
done

                                echo "Checking container..."

                                docker ps \
                                    --filter name=devops-demo-app-${params.ENVIRONMENT}

                                echo "Checking application response..."

                                curl -f http://localhost:8080

                                echo ""
                                echo "Deployment completed successfully."
                            '
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "=========================================="
            echo "Pipeline completed successfully."
            echo "IMAGE_TAG   : ${IMAGE_TAG}"
            echo "ENVIRONMENT : ${ENVIRONMENT}"
            echo "IMAGE       : ${IMAGE_NAME}:${IMAGE_TAG}"
            echo "=========================================="
        }

        failure {
            echo "Pipeline failed."
        }
    }
}
