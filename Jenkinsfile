pipeline {
  agent { label 'jenkins-agent' }

  environment {
    MAVEN_OPTS = '-Xmx1024m'
    DOCKER_HUB_USER = 'rima603'
    DOCKER_HUB_PASSWORD = credentials('dockerhub') 
    SONARQUBE_SERVER = 'sonarqube'
  }

  stages {
    stage("Cleanup Workspace") {
      steps {
        cleanWs()
      }
    }

    stage("Checkout from SCM") {
      steps {
        git(
          branch: 'main',
          credentialsId: 'github',
          url: 'https://github.com/rima-gif/ProjetBankaComplet.git',
          poll: true,
          changelog: true
        )
      }
    }

    stage("Build Application") {
      parallel {
        stage("SpringBoot") {
          steps {
            dir('back') {
              sh 'mvn clean package -DskipTests'
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

    stage("Test") {
      steps {
        script {
          sh 'cd back && mvn clean test -Ptest'
        }
      }
    }

    stage("Sonar") {
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
        failure {
          echo "SonarQube analysis failed"
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
        script {
            sh """
                docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image rima603/backprojet:${BUILD_NUMBER} || true
                docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image rima603/frontprojet:${BUILD_NUMBER} || true
            """
        }
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
  }
}
