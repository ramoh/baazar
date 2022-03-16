package com.rajesh.microservices.core.product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.rajesh.api.core.product.Product;
import com.rajesh.microservices.core.product.persistence.ProductEntity;
import com.rajesh.microservices.core.product.services.ProductMapper;

import org.junit.Test;
import org.mapstruct.factory.Mappers;

public class MapperTests {
    private ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

    @Test
    public void mapperTests() {

        assertNotNull(mapper);

        Product api = new Product(1, "n", 1, "sa");

        ProductEntity entity = mapper.apiToEntity(api);

        assertEquals(api.getProductId(), entity.getProductId());
        assertEquals(api.getProductId(), entity.getProductId());
        assertEquals(api.getName(), entity.getName());
        assertEquals(api.getWeight(), entity.getWeight());

        Product api2 = mapper.entityToApi(entity);

        assertEquals(api.getProductId(), api2.getProductId());
        assertEquals(api.getProductId(), api2.getProductId());
        assertEquals(api.getName(), api2.getName());
        assertEquals(api.getWeight(), api2.getWeight());
        assertNull(api2.getServiceAddress());
    }
}
