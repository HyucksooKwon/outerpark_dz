
![CB371771-D06B-4169-9DF1-0393C15AFEDC_4_5005_c](https://user-images.githubusercontent.com/82069747/122333349-f7782680-cf72-11eb-919e-780b81f96e37.jpeg)



# OuterPark(연극 예약)



## 0. 서비스 시나리오


### 기능적 요구사항

```
1. 공연관리자가 예약가능한 연극과 좌석수를 등록한다.
2. 고객이 연극 좌석을 예약한다.
3. 연극이 예약되면 예약된 좌석수 만큼 예약가능한 좌석수에서 차감된다.
4. 예약이 되면 티켓배송을 위한 준비 작업을 진행한다.
5. 고객이 연극 예약을 취소할 수 있다. 
6. 예약이 취소되면 티켓배송이 취소된다.
7. 고객이 모든 진행내역을 볼 수 있어야 한다.
```

### 비기능적 요구사항
```
1. 트랜잭션
    1.1 예약 가능한 좌석이 부족하면 예약이 되지 않는다. --> Sync 호출
    1.2 예약이 취소되면 예약가능한 좌석수가 증가한다. --> SAGA
2. 장애격리
    2.1 배송 서비스가 동작하지 않아도 예약은 365일 24시간 받을 수 있어야 한다. --> Async (event-driven), Eventual Consistency
    2.2 예약으로 인해 공연(뮤지컬)관리시스템의 부하가 과중하면 예약을 잠시동안 받지 않고 잠시 후에 예약을 하도록 유도한다. --> Circuit breaker, fallback
3. 성능
    3.1 고객이 상시 예약내역을 조회 할 수 있도록 성능을 고려하여 별도의 view(MyPage)로 구성한다. --> CQRS
```



## 1. 분석/설계

### Event Storming 결과
![image](https://user-images.githubusercontent.com/84000853/124477763-5b0dab00-dddf-11eb-88ae-c92fbed439d0.png)


## 2. 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각각의 포트넘버는 8081 ~ 8085 이다)
```
  cd drama
  mvn spring-boot:run  
  
  cd reservation
  mvn spring-boot:run  

  cd delivery
  mvn spring-boot:run

  cd customercenter
  mvn spring-boot:run  

```

### 2.1. DDD 의 적용

msaez.io를 통해 구현한 Aggregate 단위로 Entity를 선언 후, 구현을 진행하였다.

Entity Pattern과 Repository Pattern을 적용하기 위해 Spring Data REST의 RestRepository를 적용하였다.

**Musical 서비스의 drama.java**

```java
package outerparkdz;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Drama_table")
public class Drama {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long dramaId;
    private String name;
    private Integer reservableSeat;

    @PostPersist
    public void onPostPersist(){
        DramaRegistered dramaRegistered = new DramaRegistered();
        BeanUtils.copyProperties(this, dramaRegistered);
        dramaRegistered.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        SeatModified seatModified = new SeatModified();
        BeanUtils.copyProperties(this, seatModified);
        seatModified.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getDramaId() {
        return dramaId;
    }

    public void setDramaId(Long dramaId) {
        this.dramaId = dramaId;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getReservableSeat() {
        return reservableSeat;
    }

    public void setReservableSeat(Integer reservableSeat) {
        this.reservableSeat = reservableSeat;
    }
}

```

**Delivery 서비스의 PolicyHandler.java**

```java
package outerparkdz;

import outerparkdz.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReserved_PrepareShip(@Payload Reserved reserved){

        if(!reserved.validate()) return;

        System.out.println("\n\n##### listener PrepareShip : " + reserved.toJson() + "\n\n");

        // Sample Logic //
        Delivery delivery = new Delivery();
        delivery.setReservationId(reserved.getId());
        delivery.setStatus("ShipPrepared");
        deliveryRepository.save(delivery);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_CanceledDelivery(@Payload Canceled canceled){

        if(!canceled.validate()) return;

        System.out.println("\n\n##### listener CanceledDelivery : " + canceled.toJson() + "\n\n");

        // Cancel shipment
        Delivery delivery = deliveryRepository.findByReservationId(canceled.getId());
        delivery.setStatus("DeliveryCanceled");
        deliveryRepository.save(delivery);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}
}
 
```

DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.


### 2.2. Polyglot Persistence 구조
drama, delivery, customercenter 서비스는 H2 DB를 사용하게끔 구성되어 있고
reservation 서비스는 HSQLDB 를 사용하도록 구성되어 있어서, DB 부분을 Polyglot 구조로 동작하도록 처리하였다.


**drama 서비스의 pom.xml 내 DB 설정부분**

![image](https://user-images.githubusercontent.com/84003381/122390349-db917680-cfac-11eb-9895-e8bb50b8c4e6.png)


**drama 서비스 spring boot 기동 로그**

![image](https://user-images.githubusercontent.com/84003381/122398314-ba348880-cfb4-11eb-9593-77770a8e27f8.png)


**reservation 서비스의 pom.xml 내 DB 설정부분**

![image](https://user-images.githubusercontent.com/84003381/122391171-b3564780-cfad-11eb-9dd1-5d4850e148f6.png)


**reservation 서비스 spring boot 기동 로그**

![image](https://user-images.githubusercontent.com/84003381/122398334-be60a600-cfb4-11eb-8915-3eb916e0d831.png)



### 2.3. Gateway 적용

**gateway > application.yml 설정**
![image](https://user-images.githubusercontent.com/84000853/124479799-97420b00-dde1-11eb-998b-67e522dd011c.png)

**gateway 테스트**

```
http POST http://gateway:8080/dramas dramaId="2" name="Frozen" reservableSeat=100 
```

![image](https://user-images.githubusercontent.com/84000853/124480332-1cc5bb00-dde2-11eb-9b08-845265e542f3.png)

![image](https://user-images.githubusercontent.com/84000853/124480643-74fcbd00-dde2-11eb-92a6-34493d0624b3.png)


### 2.4. Saga, CQRS, Correlation, Req/Resp

뮤지컬 예약 시스템은 각 마이크로 서비스가 아래와 같은 기능으로 구성되어 있으며,
마이크로 서비스간 통신은 기본적으로 Pub/Sub 을 통한 Event Driven 구조로 동작하도록 구성하였음.

![image](https://user-images.githubusercontent.com/84000853/124481744-94481a00-dde3-11eb-97f1-6e6900baa9b2.png)


<구현기능별 요약>
```
[Saga]
- 마이크로 서비스간 통신은 Kafka를 통해 Pub/Sub 통신하도록 구성함. 이를 통해 Event Driven 구조로 각 단계가 진행되도록 함
- 아래 테스트 시나리오의 전 구간 참조

[CQRS]
- customercenter (myPage) 서비스의 경우의 경우, 각 마이크로 서비스로부터 Pub/Sub 구조를 통해 받은 데이터를 이용하여 자체 DB로 View를 구성함.
- 이를 통해 여러 마이크로 서비스에 존재하는 DB간의 Join 등이 필요 없으며, 성능에 대한 이슈없이 빠른 조회가 가능함.
- 테스트 시나리오의 3.4 과 5.4 항목에 해당

[Correlation]
- 예약을 하게되면 reservation > delivery > MyPage로 주문이 Assigned 되고, 주문 취소가 되면 Status가 deliveryCancelled로 Update 되는 것을 볼 수 있다.
- 또한 Correlation Key를 구현하기 위해 각 마이크로서비스에서 관리하는 데이터의 Id값을 전달받아서 서비스간의 연관 처리를 수행하였다.
- 이 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.

[Req/Resp]
- musical 마이크로서비스의 잔여좌석수를 초과한 예약 시도시에는, reservation 마이크로서비스에서 예약이 되지 않도록 처리함
- FeignClient 를 이용한 Req/Resp 연동
- 테스트 시나리오의 2.1, 2.2, 2.3 항목에 해당하며, 동기호출 결과는 3.1(예약성공시)과 5.1(예약실패시)에서 확인할 수 있다.
```

![image](https://user-images.githubusercontent.com/84003381/122410244-b574d200-cfbe-11eb-8b49-3dad0dafe79b.png)


**<구현기능 점검을 위한 테스트 시나리오>**

![image](https://user-images.githubusercontent.com/84000853/124524739-288f9c80-de37-11eb-8bf8-35cc67aa43b0.png)


**1. MD가 연극 정보 등록**

- http POST http://localhost:8081/dramas drmaId="1" name="LionKing" reservableSeat="100"

![image](https://user-images.githubusercontent.com/84000853/124538785-2ccbb200-de57-11eb-9b64-18f6873f8d47.png)



**2. 사용자가 연극 좌석 예약**

2.1 정상예약 #1

- http POST http://localhost:8082/reservations dramaId="1" seats="10""


2.2 MD가 관리하는 정보상의 좌석수(잔여좌석수)를 초과한 예약 시도시에는 예약이 되지 않도록 처리함

- FeignClient를 이용한 Req/Resp 연동
- http POST http://localhost:8082/reservations dramaId="1" seats="200"

![image](https://user-images.githubusercontent.com/84000853/124538876-57b60600-de57-11eb-8151-009ca4a3669b.png)



**3. 예약 완료 후, 각 마이크로 서비스내 Pub/Sub을 통해 변경된 데이터 확인**

3.1 연극 정보 조회 (좌석수량 차감여부 확인)  --> 좌석수가 90으로 줄어듦
- http GET http://localhost:8081/dramas/1
![image](https://user-images.githubusercontent.com/84000853/124539115-ca26e600-de57-11eb-93fb-3c0d77541ee3.png)

   
3.2 배송 준비 내역 조회     --> 1 Row 생성
- http GET http://localhost:8082/deliveries
![image](https://user-images.githubusercontent.com/84000853/124539133-d317b780-de57-11eb-9122-7d6de57e314b.png)

       
3.3 마이페이지 조회        --> SeatReserved 로 업데이트됨
- http GET http://localhost:8045/myPages
![image](https://user-images.githubusercontent.com/84000853/124539146-db6ff280-de57-11eb-93b8-8836519af437.png)



**4. 사용자가 뮤지컬 예약 취소**

4.1 예약번호 #1을 취소함

- http DELETE http://localhost:8082/reservations/1

![image](https://user-images.githubusercontent.com/84000853/124539205-f7739400-de57-11eb-9863-f556198da009.png)

   
4.2 취소내역 확인

- http GET http://localhost:8082/reservations

![image](https://user-images.githubusercontent.com/84000853/124539217-ff333880-de57-11eb-9c71-86579c3bae6f.png)



**5. 예약 취소 후, 각 마이크로 서비스내 Pub/Sub을 통해 변경된 데이터 확인**

5.1 연극 정보 조회 (좌석수량 증가여부 확인)  --> 좌석수가 100으로 늘어남
- http GET http://localhost:8081/dramas/1
![image](https://user-images.githubusercontent.com/84000853/124539231-05291980-de58-11eb-8a07-2e26e4e7c9eb.png)

5.2 배송 준비 내역 조회    --> 1번 예약에 대한 배송건이 DeliveryCancelled 로 변경됨 (UPDATE)
- http GET http://localhost:8083/deveries
![image](https://user-images.githubusercontent.com/84000853/124539246-0c502780-de58-11eb-8e34-466bfd1bce7e.png)

5.3 마이페이지 조회       --> 1 Row 추가 생성 : DeliveryCancelled 생성 1건
- http GET http://localhost:8084/myPages
![image](https://user-images.githubusercontent.com/84000853/124539259-12460880-de58-11eb-9737-f1015cc63bd7.png)



## 3. 운영

### 3.1. Deploy


**네임스페이스 만들기**
```
kubectl create ns outerpark
kubectl get ns
```

![image](https://user-images.githubusercontent.com/84000853/124527422-78269600-de40-11eb-8d7e-915fc52ac313.png)


**소스가져오기**
```
git clone https://github.com/hyucksookwon/outerpark_dz.git
```

![image](https://user-images.githubusercontent.com/84000853/124527480-9be9dc00-de40-11eb-9942-af3cefe49b0a.png)

**빌드하기**
```
cd outerpark_dz/drama
mvn package
```
![image](https://user-images.githubusercontent.com/84000853/124527605-fb47ec00-de40-11eb-9606-a1c91c556072.png)

**도커라이징: Azure 레지스트리에 도커 이미지 빌드 후 푸시하기**
```
az acr build --registry user01skccacr --image user01skccacr.azurecr.io/drama:v1 .
```

![image](https://user-images.githubusercontent.com/84000853/124527863-ab1d5980-de41-11eb-8f3b-165f901c5345.png)

![image](https://user-images.githubusercontent.com/84000853/124527884-bb353900-de41-11eb-90a9-f389b8753835.png)

![image](https://user-images.githubusercontent.com/84000853/124527911-cc7e4580-de41-11eb-89a6-a27688d370df.png)

**컨테이너라이징: 디플로이 생성 확인**
```
kubectl create deploy drama --image=user01skccacr.azurecr.io/drama:v1 -n outerpark
kubectl get all -n outerpark
```

![image](https://user-images.githubusercontent.com/84000853/124528098-4c0c1480-de42-11eb-9712-3f5dd33ff7e1.png)


**컨테이너라이징: 서비스 생성 확인**

```
kubectl expose deploy drama --type="ClusterIP" --port=8080 -n outerpark
kubectl get all -n outerpark
```

![image](https://user-images.githubusercontent.com/84000853/124528203-968d9100-de42-11eb-880e-9e789b1c798b.png)


**reservation, customercenter, gateway에도 동일한 작업 반복**
*최종 결과

![image](https://user-images.githubusercontent.com/84000853/124528938-6b0ba600-de44-11eb-812e-bee17979c985.png)

- deployment.yml을 사용하여 배포 (reservation의 deployment.yml 추가)

![image](https://user-images.githubusercontent.com/84000848/122332320-2d1c1000-cf71-11eb-8766-b494f157f247.png)
- deployment.yml로 서비스 배포

```
kubectl apply -f kubernetes/deployment.yml
```


### 3.2. 동기식 호출 / 서킷 브레이킹 / 장애격리
- 시나리오는 예약(reservation)--> 연극공연(drama) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약이 과도할 경우 CB 를 통하여 장애격리.
- Hystrix 설정: 요청처리 쓰레드에서 처리시간이 250 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정


```
# circuit breaker 설정 start
feign:
  hystrix:
    enabled: true

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 250
# circuit breaker 설정 end
```
- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인: 동시사용자 100명, 60초 동안 실시
시즈 접속


```
kubectl exec -it pod/siege-d484db9c-9dkgd -c siege -n outerpark -- /bin/bash
```
- 부하테스트 동시사용자 100명 60초 동안 공연예약 수행


```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://reservation:8080/reservations POST {"dramaId": "1", "seats":1}'
```
- 부하 발생하여 CB가 발동하여 요청 실패처리하였고, 밀린 부하가 drama에서 처리되면서 다시 reservation 받기 시작


![image](https://user-images.githubusercontent.com/84000848/122355980-52b71280-cf8d-11eb-9d48-d9848d7189bc.png)

- 레포트결과


![image](https://user-images.githubusercontent.com/84000848/122356067-68c4d300-cf8d-11eb-9186-2dc33ebc806d.png)

서킷브레이킹 동작확인완료


### 3.3. Autoscale(HPA)

- 오토스케일 테스트를 위해 리소스 제한설정 함
- reservation/kubernetes/deployment.yml 설정

```
resources:
	limits:
		cpu : 500m
	requests: 
		cpu : 200m
```

- 예약 시스템에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다

```
kubectl autoscale deploy reservation --min=1 --max=10 --cpu-percent=15 -n outerpark
```

![image](https://user-images.githubusercontent.com/84000848/122361127-edb1eb80-cf91-11eb-93ff-2c386af48961.png)

- 부하테스트 동시사용자 200명 120초 동안 공연예약 수행

```
siege -c200 -t120S -r10 -v --content-type "application/json" 'http://reservation:8080/reservations POST {"musicalId": "1003", "seats":1}'
```

- 최초수행 결과

![image](https://user-images.githubusercontent.com/84000848/122360142-21d8dc80-cf91-11eb-9868-85dffcc21309.png)

- 오토스케일 모니터링 수행


```
kubectl get deploy reservation -w -n outerpark
```

![image](https://user-images.githubusercontent.com/84000848/122361571-55683680-cf92-11eb-802b-28f47fdada7b.png)

- 부하테스트 재수행 시 Availability가 높아진 것을 확인


![image](https://user-images.githubusercontent.com/84000848/122361773-86e10200-cf92-11eb-9ab7-c8f62b519174.png)

-  replica 를 10개 까지 늘어났다가 부하가 적어져서 다시 줄어드는걸 확인 가능 함


![image](https://user-images.githubusercontent.com/84000848/122361938-ad06a200-cf92-11eb-9a55-35f9b6ceefe0.png)

### 3.4. Self-healing (Liveness Probe)

- drama 서비스 정상 확인

![image](https://user-images.githubusercontent.com/84000848/122398259-adb03000-cfb4-11eb-9f49-5cf7018b81d4.png)


- drama의 deployment.yml 에 Liveness Probe 옵션 변경하여 계속 실패하여 재기동 되도록 yml 수정
```
          livenessProbe:
            tcpSocket:
              port: 8081
            initialDelaySeconds: 5
            periodSeconds: 5	
```
![image](https://user-images.githubusercontent.com/84000848/122398788-2dd69580-cfb5-11eb-91ce-bc82d7cf66a1.png)

-drama pod에 liveness가 적용된 부분 확인

![image](https://user-images.githubusercontent.com/84000848/122400529-c4578680-cfb6-11eb-8d06-a54f37ced872.png)

-musical 서비스의 liveness가 발동되어 7번 retry 시도 한 부분 확인

![image](https://user-images.githubusercontent.com/84000848/122401681-c66e1500-cfb7-11eb-9417-4ff189919f62.png)


### 3.5. Zero-downtime deploy(Readiness Probe)
- Zero-downtime deploy를 위해 Autoscale 및 CB 설정 제거 
- readiness 옵션이 없는 reservation 배포
- seige로 부하 준비 후 실행 
- seige로 부하 실행 중 reservation 새로운 버전의 이미지로 교체
- readiness 옵션이 없는 경우 배포 중 서비스 요청처리 실패

![image](https://user-images.githubusercontent.com/84000848/122414855-69c42780-cfc2-11eb-8955-30e623e721c6.png)

- deployment.yml에 readiness 옵션을 추가

![image](https://user-images.githubusercontent.com/84000848/122416039-5d8c9a00-cfc3-11eb-84b1-9eb4ce1b6e9d.png)

-readiness적용된 deployment.yml 적용

```
kubectl apply -f kubernetes/deployment.yml
```
-새로운 버전의 이미지로 교체

```
az acr build --registry outerparkskacr --image user01skccacr.azurecr.io/reservation:v3 .
kubectl set image deploy reservation reservation=user01skccacr.azurecr.io/reservation:v3 -n outerpark
```

-기존 버전과 새 버전의 reservation pod 공존 중

![image](https://user-images.githubusercontent.com/84000848/122417105-36829800-cfc4-11eb-9849-054cf58119f2.png)

-Availability: 100.00 % 확인

![image](https://user-images.githubusercontent.com/84000848/122417302-5a45de00-cfc4-11eb-87d1-cc7482113a33.png)

### 3.6. Config Map
application.yml 설정

-default부분

![image](https://user-images.githubusercontent.com/84000848/122422699-51570b80-cfc8-11eb-9cb9-f0fe332fb26a.png)

-docker 부분

![image](https://user-images.githubusercontent.com/84000848/122422842-70559d80-cfc8-11eb-8a0f-8bf10957140f.png)

Deployment.yml 설정

![image](https://user-images.githubusercontent.com/84000848/122423101-a3982c80-cfc8-11eb-821f-8c3aad8be16f.png)

config map 생성 후 조회
```
kubectl create configmap apiurl --from-literal=url=http://drama:8080 -n outerpark
```

![image](https://user-images.githubusercontent.com/84000848/122423850-346f0800-cfc9-11eb-90d8-9cb6c55bec21.png)

- 설정한 url로 주문 호출
```
http POST http://reservation:8080/reservations dramaId=1 seats=10
```
![image](https://user-images.githubusercontent.com/84000848/122424027-5a94a800-cfc9-11eb-8fa9-363b80e6b899.png)

-configmap 삭제 후 app 서비스 재시작

```
kubectl delete configmap apiurl -n outerpark
kubectl get pod/reservation-57d8f8c4fd-74csz -n outerpark -o yaml | kubectl replace --force -f-
```

![image](https://user-images.githubusercontent.com/84000848/122424266-89ab1980-cfc9-11eb-8683-ac313e971ed6.png)


-configmap 삭제된 상태에서 주문 호출

![image](https://user-images.githubusercontent.com/84000848/122423447-e3f7aa80-cfc8-11eb-8760-6df5eb08f039.png)

![image](https://user-images.githubusercontent.com/84000848/122423364-d3dfcb00-cfc8-11eb-8b35-9145c00659b9.png)

