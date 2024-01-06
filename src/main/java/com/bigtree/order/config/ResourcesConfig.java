package com.bigtree.order.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourcesConfig {

    @Value("classpath:/static/images/basket.png")
    Resource basketLogo;
}
