package com.lg.platform.loyalty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point. The {@code scanBasePackages} explicitly includes the
 * framework's Kafka and Outbox config packages so their {@code @Configuration}
 * classes (notably {@code
 * com.lg5.spring.kafka.consumer.config.KafkaConsumerConfig} which contributes
 * the {@code kafkaListenerContainerFactory} bean) are registered in the
 * application context. Without this addition, {@code @KafkaListener}-annotated
 * methods silently fail to register because Spring Kafka cannot find a listener
 * container factory. Mirrors the convention from
 * {@code food-ordering-system/order-container/OrderServiceApplication}.
 */
@SpringBootApplication(scanBasePackages = {"com.lg.platform.loyalty", "com.lg5.spring.kafka", "com.lg5.spring.outbox"})
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
