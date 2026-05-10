package com.lg.platform.loyalty.message.config;

import com.lg.platform.loyalty.message.mapper.InboundOrderEventAvroMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires plain-POJO mapper(s) (no Spring annotations on the mapper class
 * itself per RULE-005) as Spring beans for injection into Kafka
 * listeners.
 */
@Configuration
public class MessagingBeansConfig {

    @Bean
    public InboundOrderEventAvroMapper inboundOrderEventAvroMapper() {
        return new InboundOrderEventAvroMapper();
    }
}
