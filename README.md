# Spring Doc Swagger UI

- https://springdoc.org/
- Difference for mvc and webflux, read carefully

```xml
    <springdoc.openapi.version>2.8.13</springdoc.openapi.version>

    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.openapi.version}</version>
    </dependency>

    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        <version>${springdoc.openapi.version}</version>
    </dependency>
```

# Scalar API Documentation

- Access: http://localhost:4006/scalar (Product Service)
- Prettier UI for API documentation

```xml
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-scalar</artifactId>
      <version>${springdoc.openapi.version}</version>
    </dependency>
```

# Spring HATEOAS (Hypermedia as the Engine of Application State)

- Access: http://localhost:4006/explorer (Product Service)

- Generate link relations based on method names
- Easily create hypermedia-driven RESTful APIs
- Support for various media types (HAL, Collection+JSON, etc.)
- RESTful API best practices (Level 3 maturity)
- Using HAL Explorer for API exploration

```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-hateoas</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.data</groupId>
      <artifactId>spring-data-rest-hal-explorer</artifactId>
    </dependency>
```

# Spring Config Server

- Already setup at http://localhost:8080/viewer.html or http://localhost:8080/
- http://localhost:8888/product-service/configserver
  - product-service: load the product-service.yml file from native (local file) or git repo
  - configserver: profile's name of product-service.yml where ref from the product service directory

3. “Không cần restart” là nhờ Spring Cloud Bus

Đây mới là phần thần kỳ 🪄

spring-cloud-bus + spring-cloud-starter-actuator
cho phép broadcast sự kiện refresh config tới toàn bộ các service đang chạy.

Cụ thể:

Bạn gửi lệnh:

curl -X POST http://config-server:8888/actuator/busrefresh


Config Server publish một event (message) qua RabbitMQ.

Tất cả microservice client đang kết nối bus đều nhận event đó.

Mỗi client tự động gọi /actuator/refresh nội bộ,
reload lại các config trong Environment mà không restart JVM.

Các bean có annotation:

@RefreshScope


sẽ được tạo lại (re-initialize) với giá trị mới.