pipeline {
  agent { label 'jenkins-agent' }

  environment {
    MAVEN_OPTS = '-Xmx1024m'
    DOCKER_HUB_USER = 'rima603'
    DOCKER_HUB_PASSWORD = credentials('dockerhub')
    SONARQUBE_SERVER = 'sonarqube'
  }

  stages {

    stage("Clean Workspace") {
      steps {
        cleanWs()
      }
    }

    stage("Checkout Code") {
      steps {
        git branch: 'main',
            credentialsId: 'github',
            url: 'https://github.com/rima-gif/ProjetBankaComplet.git'
      }
    }

    stage("Build App") {
      parallel {
        stage("Backend - Spring Boot") {
          steps {
            dir('back') {
              sh 'mvn clean install -DskipTests=true'
            }
          }
        }
        stage("Frontend - Angular") {
          steps {
            dir('front') {
              sh 'npm install'
              sh './node_modules/.bin/ng build --configuration production'
            }
          }
        }
      }
    }

    stage("Test with MySQL") {
      steps {
        script {
          sh '''
            docker-compose -f docker-compose.yml up -d mysql

            echo "Waiting for MySQL to be ready..."
            for i in {1..15}; do
              docker exec mysql-container mysqladmin ping -proot && break
              sleep 2
            done

            cd back
            mvn test -Ptest

            docker-compose down
          '''
        }
      }
    }

    stage("SonarQube Analysis") {
      steps {
        dir('back') {
          withSonarQubeEnv('sonarqube') {
            sh 'mvn sonar:sonar'
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

    stage("Security Scan - Trivy") {
      steps {
        sh """
          docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image rima603/backprojet:${BUILD_NUMBER} || true
          docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image rima603/frontprojet:${BUILD_NUMBER} || true
        """
      }
    }

    stage("Push Images to Docker Hub") {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh """
            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
            docker push rima603/backprojet:${BUILD_NUMBER}
            docker push rima603/backprojet:latest
            docker push rima603/frontprojet:${BUILD_NUMBER}
            docker push rima603/frontprojet:latest
          """
        }
      }
    }

    stage("Update K8s Manifests for ArgoCD") {
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
