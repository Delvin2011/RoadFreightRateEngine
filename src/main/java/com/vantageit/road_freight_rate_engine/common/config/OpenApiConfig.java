package com.vantageit.road_freight_rate_engine.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI roadFreightRateEngineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Road Freight Rate Engine API")
                        .description("Minimal CRUD API scaffold for the Road Freight Rate Engine")
                        .version("v0.0.1"));
    }
}
