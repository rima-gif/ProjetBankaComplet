pipeline {
  agent { label 'jenkins-agent' }

  environment {
    MAVEN_OPTS = '-Xmx1024m'
    DOCKER_HUB_USER = 'rima603'
    DOCKER_HUB_PASSWORD = credentials('dockerhub')
    SONARQUBE_SERVER = 'sonarqube'
    SONAR_HOST_URL = 'http://192.168.40.111:9000'
    SONAR_AUTH_TOKEN = credentials('jenkins-sonarqube-token	')

    
  }

  stages {
    stage("Cleanup Workspace") {
      steps {
        cleanWs()
      }
    }

    stage("Checkout Application Code") {
      steps {
        git(
          branch: 'main',
          credentialsId: 'github',
          url: 'https://github.com/rima-gif/ProjetBankaComplet.git'
        )
      }
    }

    stage("Build Application") {
      parallel {
        stage("SpringBoot") {
          steps {
            dir('back') {
              sh 'mvn clean install -DskipTests=true'
            }
          }
        }
        stage("Angular") {
          steps {
            dir('front') {
              sh 'npm install'
              sh './node_modules/.bin/ng build --configuration production'
            }
          }
        }
      }
    }

   stage('Start MySQL for Tests') {
  steps {
    script {
      sh '''
        docker rm -f mysql-test || true
        docker run -d --name mysql-test -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=BankDB -p 3306:3306 mysql:8.0
        echo "Waiting for MySQL to be ready..."
        for i in {1..15}; do
          docker exec mysql-test mysqladmin ping -proot && break
          sleep 2
        done
      '''
    }
  }
}


    stage("Run Backend Tests") {
      steps {
        dir('back') {
         sh 'mvn test'

        }
      }
    }

    stage('Stop MySQL') {
      steps {
        sh '''
          docker stop mysql-test
          docker rm mysql-test
        '''
      }
    }

    stage("SonarQube Analysis") {
      environment {
        SONAR_AUTH_TOKEN = credentials('jenkins-sonarqube-token')
      }
      steps {
        dir('back') {
          withSonarQubeEnv('sonarqube') {
            sh "mvn sonar:sonar -Dsonar.host.url=${env.SONAR_HOST_URL} -Dsonar.login=${env.SONAR_AUTH_TOKEN}"
          }
        }
      }
      post {
        success {
          script {
            timeout(time: 1, unit: 'MINUTES') {
              def qualityGate = waitForQualityGate()
              if (qualityGate.status != 'OK') {
                error "SonarQube Quality Gate failed: ${qualityGate.status}"
              }
            }
          }
        }
      }
    }


    stage("Build Docker Images") {
      steps {
        script {
          dir('back') {
            sh """
              docker build -t rima603/backprojet:${BUILD_NUMBER} .
              docker tag rima603/backprojet:${BUILD_NUMBER} rima603/backprojet:latest
            """
          }
          dir('front') {
            sh """
              docker build -t rima603/frontprojet:${BUILD_NUMBER} .
              docker tag rima603/frontprojet:${BUILD_NUMBER} rima603/frontprojet:latest
            """
          }
        }
      }
    }

    stage("Trivy Security Scan") {
      steps {
        sh """
          docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image rima603/backprojet:${BUILD_NUMBER} || true
          docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image rima603/frontprojet:${BUILD_NUMBER} || true
        """
      }
    }

    stage("Push Docker Images to Docker Hub") {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh """
            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
            docker push rima603/frontprojet:${BUILD_NUMBER}
            docker push rima603/frontprojet:latest
            docker push rima603/backprojet:${BUILD_NUMBER}
            docker push rima603/backprojet:latest
          """
        }
      }
    }

    stage("Update Kubernetes Manifests") {
      steps {
        dir('k8s-manifests') {
          git branch: 'main', credentialsId: 'github', url: 'https://github.com/rima-gif/k8s-manifests.git'

          sh """
            sed -i 's|image: rima603/backprojet:.*|image: rima603/backprojet:${BUILD_NUMBER}|' backend/deployment.yaml
            sed -i 's|image: rima603/frontprojet:.*|image: rima603/frontprojet:${BUILD_NUMBER}|' frontend/deployment.yaml
          """

          sh '''
            git config user.email "achourryma971@gmail.com"
            git config user.name "rima-gif"
            git add backend/deployment.yaml frontend/deployment.yaml
            git commit -m "Update image tags to build ${BUILD_NUMBER}" || echo "No changes to commit"
            git push origin main
          '''
        }
      }
    }
  }
}
