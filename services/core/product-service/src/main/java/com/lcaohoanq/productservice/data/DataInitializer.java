package com.lcaohoanq.productservice.data;

import com.lcaohoanq.productservice.domain.category.Category;
import com.lcaohoanq.productservice.domain.category.CategoryRepository;
import com.lcaohoanq.productservice.domain.product.Product;
import com.lcaohoanq.productservice.domain.product.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {

        // Initialize sample categories and products if needed
        if (categoryRepository.count() == 0) {

            var cat1 = Category.builder()
                .name("Electronics")
                .description("Electronic gadgets and devices")
                .build();

            var cat2 = Category.builder()
                .name("Books")
                .description("Various kinds of books and literature")
                .build();

            var cat3 = Category.builder()
                .name("Clothing")
                .description("Apparel and accessories")
                .build();

            var categoryList = List.of(cat1, cat2, cat3);

            categoryRepository.saveAll(categoryList);
            System.out.println("Sample categories initialized.");
        }

        if (productRepository.count() == 0) {

            var prod1 = Product.builder()
                .name("Smartphone")
                .description("Latest model smartphone with advanced features")
                .price(699.99)
                .active(true)
                .category(categoryRepository.findByName("Electronics").orElseThrow(
                    () -> new RuntimeException("Category not found")))
                .build();

            var prod2 = Product.builder()
                .name("Science Fiction Novel")
                .description("A thrilling science fiction novel")
                .price(19.99)
                .active(true)
                .category(categoryRepository.findByName("Books").orElseThrow(
                    () -> new RuntimeException("Category not found")))
                .build();

            var prod3 = Product.builder()
                .name("Jeans")
                .description("Comfortable and stylish jeans")
                .price(49.99)
                .active(false)
                .category(categoryRepository.findByName("Clothing").orElseThrow(
                    () -> new RuntimeException("Category not found")))
                .build();

            var productList = List.of(prod1, prod2, prod3);
            productRepository.saveAll(productList);
            System.out.println("Sample products initialized.");
        }

    }
}