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

        stage('Verify Image Digest') {
            steps {
                sh '''
                    echo "Verifying selected ECR image..."

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

                    echo "Deploying ${IMAGE_NAME}:${IMAGE_TAG} to ${params.ENVIRONMENT}"
                    echo "Target host: ${targetHost}"

                    /*
                     * Deployment command will be added after
                     * Jenkins SSH credentials are configured.
                     */
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully."
            echo "IMAGE_TAG: ${IMAGE_TAG}"
            echo "ENVIRONMENT: ${ENVIRONMENT}"
            echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
        }

        failure {
            echo "Pipeline failed."
        }
    }
}
