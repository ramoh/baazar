package com.rajesh.microservices.composite.product;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@SpringBootApplication
@ComponentScan("com.rajesh")
public class ProductCompositeServiceApplication {

    @Value("${api.common.version}")
    String apiVersion;

    @Value("${api.common.title}")
    String apiTitle;

    @Value("${api.common.description}")
    String apiDescription;

    @Value("${api.common.termsOfServiceUrl}")
    String apiTermsOfServiceUrl;

    @Value("${api.common.license}")
    String apiLicense;

    @Value("${api.common.licenseUrl}")
    String apiLicenseUrl;

    @Value("${api.common.contact.name}")
    String apiContactName;

    @Value("${api.common.contact.url}")
    String apiContactUrl;

    @Value("${api.common.contact.email}")
    String apiContactEmail;

    /**
     * Will be exposed on $HOST:$PORT/swagger-ui.html
     * 
     * @return
     */
    @Bean
    public Docket apiDopcumentation() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.rajesh.microservices.composite.product"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(new ApiInfo(
                        apiTitle,
                        apiDescription,
                        apiVersion,
                        apiTermsOfServiceUrl,
                        new Contact(apiContactName, apiContactUrl, apiContactEmail),
                        apiLicense,
                        apiLicenseUrl,
                        Collections.emptyList()));

    }

    public static void main(String[] args) {
        SpringApplication.run(ProductCompositeServiceApplication.class, args);
    }

}
