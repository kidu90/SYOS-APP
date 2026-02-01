package com.syos.domain.repository;

import com.syos.domain.entity.Product;
import com.syos.domain.valueobject.ProductId;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    void save(Product product);
    Optional<Product> findById(ProductId id);
    List<Product> findAll();
    List<Product> findByCategory(String category);
    void delete(ProductId id);
}
