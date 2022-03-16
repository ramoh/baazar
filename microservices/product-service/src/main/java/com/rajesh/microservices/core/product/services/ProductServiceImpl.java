package com.rajesh.microservices.core.product.services;

import com.rajesh.api.core.product.Product;
import com.rajesh.api.core.product.ProductService;
import com.rajesh.microservices.core.product.persistence.ProductEntity;
import com.rajesh.microservices.core.product.persistence.ProductRepository;
import com.rajesh.util.exceptions.InvalidInputException;
import com.rajesh.util.exceptions.NotFoundException;
import com.rajesh.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Autowired
    public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Product createProduct(Product body) {
        if (body.getProductId() < 1)
            throw new InvalidInputException("Invalid ProductId:" + body.getProductId());

        ProductEntity entity = mapper.apiToEntity(body);
        Mono<Product> newEntity = repository.save(entity)
                .log("Created a product with id " + body.getProductId())
                .onErrorMap(DuplicateKeyException.class,
                        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
                .map(e -> mapper.entityToApi(e));

        return newEntity.toProcessor().block();
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        if (productId < 1)
            throw new InvalidInputException("Invalid productId: " + productId);

        return repository.findByProductId(productId)
                .switchIfEmpty(Mono.error(new NotFoundException("No Product found for productId: " + productId)))
                .log("Found product with id " + productId)
                .map(e -> mapper.entityToApi(e))
                .map(e -> {
                    e.setServiceAddress(serviceUtil.getServiceAddress());
                    return e;
                });

    }

    @Override
    public void deleteProduct(int productId) {
        if (productId < 1)
            throw new InvalidInputException("Invalid ProductId: " + productId);

        LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        repository.findByProductId(productId)
                .log("Deleting the product with id" + productId)
                .map(e -> repository.delete(e)).flatMap(e -> e).toProcessor()
                .block();
    }
}
