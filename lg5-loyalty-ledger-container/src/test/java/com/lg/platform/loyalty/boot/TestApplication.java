package com.lg.platform.loyalty.boot;

import com.lg.platform.loyalty.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import(TestContainersLoader.class)
class TestApplication {

	static void main(String[] args) {
		SpringApplication.from(Application::main).with(TestApplication.class).run(args);
	}

}
