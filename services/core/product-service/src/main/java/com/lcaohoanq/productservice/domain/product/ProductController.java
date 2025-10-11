package com.lcaohoanq.productservice.domain.product;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

//    @GetMapping
//    public ResponseEntity<List<Product>> getAllProducts() {
//        return ResponseEntity.ok(productRepository.findAll());
//    }

//    @GetMapping("/{id}")
//    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
//        return productRepository.findById(id)
//            .map(ResponseEntity::ok)
//            .orElse(ResponseEntity.notFound().build());
//    }

//    @PostMapping
//    public ResponseEntity<Product> createProduct(
//        @Valid @RequestBody Product product
//    ) {
//        Product savedProduct = productRepository.save(product);
//        return ResponseEntity.ok(savedProduct);
//    }
//
//    @PatchMapping("/{id}")
//    public ResponseEntity<Product> updateProduct(@PathVariable Long id, Product product) {
//        return productRepository.findById(id)
//            .map(existingProduct -> {
//                existingProduct.setName(product.getName());
//                existingProduct.setDescription(product.getDescription());
//                existingProduct.setPrice(product.getPrice());
//                existingProduct.setActive(product.isActive());
//                existingProduct.setCategory(product.getCategory());
//                Product updatedProduct = productRepository.save(existingProduct);
//                return ResponseEntity.ok(updatedProduct);
//            })
//            .orElse(ResponseEntity.notFound().build());
//    }

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<Product>>> getAllProducts() {
        List<EntityModel<Product>> products = productRepository.findAll()
            .stream()
            .map(product -> EntityModel.of(product,
                                           WebMvcLinkBuilder.linkTo(
                                               WebMvcLinkBuilder.methodOn(ProductController.class).getProductById(product.getId())
                                           ).withSelfRel(),
                                           WebMvcLinkBuilder.linkTo(
                                               WebMvcLinkBuilder.methodOn(ProductController.class).updateProduct(product.getId(), null)
                                           ).withRel("update"),
                                           WebMvcLinkBuilder.linkTo(
                                               WebMvcLinkBuilder.methodOn(ProductController.class).deleteProduct(product.getId())
                                           ).withRel("delete")
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(
            CollectionModel.of(products,
                               WebMvcLinkBuilder.linkTo(
                                   WebMvcLinkBuilder.methodOn(ProductController.class).getAllProducts()
                               ).withSelfRel()
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Product>> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
            .map(product -> EntityModel.of(product,
                                           WebMvcLinkBuilder.linkTo(
                                               WebMvcLinkBuilder.methodOn(ProductController.class).getProductById(id)
                                           ).withSelfRel(),
                                           WebMvcLinkBuilder.linkTo(
                                               WebMvcLinkBuilder.methodOn(ProductController.class).getAllProducts()
                                           ).withRel("all-products"),
                                           WebMvcLinkBuilder.linkTo(
                                               WebMvcLinkBuilder.methodOn(ProductController.class).updateProduct(id, null)
                                           ).withRel("update"),
                                           WebMvcLinkBuilder.linkTo(
                                               WebMvcLinkBuilder.methodOn(ProductController.class).deleteProduct(id)
                                           ).withRel("delete")
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EntityModel<Product>> createProduct(@Valid @RequestBody Product product) {
        Product saved = productRepository.save(product);

        EntityModel<Product> model = EntityModel.of(saved,
                                                    WebMvcLinkBuilder.linkTo(
                                                        WebMvcLinkBuilder.methodOn(ProductController.class).getProductById(saved.getId())
                                                    ).withSelfRel(),
                                                    WebMvcLinkBuilder.linkTo(
                                                        WebMvcLinkBuilder.methodOn(ProductController.class).getAllProducts()
                                                    ).withRel("all-products")
        );

        return ResponseEntity.ok(model);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EntityModel<Product>> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productRepository.findById(id)
            .map(existing -> {
                existing.setName(product.getName());
                existing.setDescription(product.getDescription());
                existing.setPrice(product.getPrice());
                existing.setActive(product.isActive());
                existing.setCategory(product.getCategory());

                Product updated = productRepository.save(existing);

                EntityModel<Product> model = EntityModel.of(updated,
                                                            WebMvcLinkBuilder.linkTo(
                                                                WebMvcLinkBuilder.methodOn(ProductController.class).getProductById(id)
                                                            ).withSelfRel(),
                                                            WebMvcLinkBuilder.linkTo(
                                                                WebMvcLinkBuilder.methodOn(ProductController.class).getAllProducts()
                                                            ).withRel("all-products")
                );

                return ResponseEntity.ok(model);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        return productRepository.findById(id)
            .map(existing -> {
                productRepository.delete(existing);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

}
