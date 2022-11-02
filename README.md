## [Citydatahub Cloud Management Module]

### Citydatahub Cloud Management Module Summary

+ 데이터 허브 클라우드 관리 모듈은 다양한 이종 클라우드(퍼블릭 및 프라이빗 클라우드)를 연동하고, 분산되어 있는 이종 클라우드 팜들에서 효과적으로 스마트시티 서비스를 위한 가상 자원을 제공하여 멀티 클라우드 환경에서 스마트시티 서비스 인프라를 관리하는 기술을 제공합니다.

+ 현재 데이터 허브 클라우드 관리 모듈에서 관리하고 있는 클라우드는 총 4종으로 3종의 퍼블릭 클라우드(AWS, Azure, TOAST)와 1종의 프라이빗 클라우드(OpenStack)로 관리자가 원하는 클라우드 인프라에 대한 서비스 관리 및 제어를 할 수 있습니다.

![1핵심 개요](https://user-images.githubusercontent.com/23303734/163297040-d2b9712d-0bbb-4321-869c-6c2ca2b39e4a.png)  

### Citydatahub Cloud Management Module Architecture

+ 데이터 허브 클라우드 관리 모듈은 도시 생활의 편의성 향상, 개별화된 사용자 서비스 제공, 효율적인 도시 운영, 스마트 시티 통합 운영 관리 등 스마트 시티 시스템의 다양한 데이터 저장 및 처리와 하이브리드 클라우드 인프라를 관리하는 모듈입니다.

+ 데이터 허브 클라우드 관리 모듈은 클라우드 운영 관리 기능, 퍼블릭 클라우드 연동 기능, 프라이빗 클라우드 연동 기능으로 크게 3가지 기능으로 구분됩니다.

+ 또한, 데이터 허브 클라우드 관리 모듈은 시스템 운영 관리 레이어, 시스템 오케스트레이션 레이어, 가상 자원 관리 레이어로 3가지 레이어로 구분됩니다. 

+ 시스템 운영 관리 레이어에서는 클라우드 인프라 관리, 인증 및 권한 관리, 고가용성, 실시간 통합 모니터링, 미터링/빌링, 퍼블릭/프라이빗 클라우드 연동 관리를 지원합니다.

+ 시스템 오케스트레이션 레이어에서는 가상자원 배포/관리, 가상자원 이미지 관리, 멀티 테넌트 스케줄링, 워크로드 로드밸런싱, 동적 자원 확장, 퍼블릭/프라이빗 클라우드 자원 생성 및 제어 기능을 지원합니다.

+ 마지막으로 가상 자원 관리 레이어에서는 서버 가상화, 네트워크 가상화, 스토리지 가상화, 클라우드 자원 프로비저닝, 고성능 장치 연계, 퍼블릭/프라이빗 클라우드 인증 및 관리 기능을 지원합니다.

![1](https://user-images.githubusercontent.com/23303734/163107199-28790644-ec4a-4dae-ae8f-4df0d2e38a53.png)

+ 데이터 허브 클라우드 관리 모듈에는 퍼블릭/프라이빗 클라우드를 연동 및 자원 관리, 서비스 사용자에 따른 인증/권한 관리 등을 위한 하이브리드 클라우드 관리 매니저가 있습니다.

+ 하이브리드 클라우드 관리 매니저에는 Monitoring DB, 자원 관리 모듈, 사용자 관리 모듈, 하이브리드 클라우드 연계 모듈, 오케스트레이션 관리 모듈, 오토스케일링 관리 모듈로 구성되어있으며 모듈별 상세 내용은 아래와 같습니다.

  + Monitoring DB는 모니터링 에이전트로부터 수집된 데이터를 실시간으로 저장하기 위해 사용하는 시계열 데이터베이스 입니다.
  + 자원 관리 모듈은 퍼블릭 클라우드와 프라이빗 클라우드의 자원을 관리하는 기능을 제공합니다.
  + 사용자 관리 모듈은 사용자를 인증하고 사용자의 권한에 따라 접근을 제어하는 기능을 제공합니다.
  + 하이브리드 클라우드 연계 모듈은 퍼블릭 클라우드와 프라이빗 클라우드를 하이브리드 클라우드 관리 운영 포탈에 연동하기 위한 기능을 제공합니다.
  + 오케스트레이션 관리 모듈은 클라우드 시스템 통합자동화와 가상자원의 배치 및 설정을 비롯해 프로비저닝을 중심으로 하여 통합 자동화를 관리하는 기능을 제공합니다.
  + 오토스케일링 관리 모듈은 클라우드의 자원을 효율적으로 사용하기 위해 클라우드의 자원 상태에 따라 자동으로 VM을 생성하거나 삭제 할 수 있도록 관리하는 기능을 제공합니다.

 ![1핵심 아키텍처](https://user-images.githubusercontent.com/23303734/163297093-4a518c54-6459-4db5-99a8-48b037da2a7f.png)

### Citydatahub Cloud Management Module Setting

+ Spring Boot 설치
    + Java Version : 1.8
    + 빌드 도구 : maven (https://maven.apache.org/download.cgi)
    + IDE : Intelij
+ Spring Boot Project Setting
    + Spring Boot Project 생성
    + http://start.spring.io/ 에서 생성 또는 Intelij 내 프로젝트 생성
    + import Project
+ maven plugin 추가 설치가 필요한 경우
    + Maven dependency 추가를 위해 pom.xml에 해당 Library를 추가하여 설치 가능
    + 프로젝트 생성 시 Maven > importing 에서 Import Maven projects automatically 체크를 통한 자동 Update 하도록 설정 가능
+ Generate sources and update folder for all projects
    + Code update 후 Maven update
    + root 선택후 clean -> complie -> install 순으로 Maven Update 
+ jar 파일 생성 후 jar 파일 실행

### Start Citydatahub Cloud Management Module
+ Application 실행
    + ServiceRegistryApplication
    + ApiAwsApplication
    + ApiAzureApplication
    + ApiToastApplication
    + ApiOpenstackApplication
    + clientApplication

+ Application 설정
    + 각 모듈 -> src -> main -> resources -> application.properties에서 설정
    
+ Application 확인
    + application.properties에서 적용한 port 번호로 확인 가능

+ API 규격
    + `WG1-2020-0024R01-인프라 모듈 API 현행화.hwp` 참고
