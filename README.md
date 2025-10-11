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