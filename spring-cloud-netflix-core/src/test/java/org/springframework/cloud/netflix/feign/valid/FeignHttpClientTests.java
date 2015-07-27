/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.feign.valid;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import feign.Client;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignHttpClientTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=feignclienttest" })
@DirtiesContext
public class FeignHttpClientTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@Autowired
	private Client feignClient;

	// @FeignClient(value = "http://localhost:9876", loadbalance = false)
	@FeignClient("localapp")
	protected static interface TestClient {
		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello();

		@RequestMapping(method = RequestMethod.PATCH, value = "/hellop")
		public ResponseEntity<Void> patchHello();
	}

	protected interface CrudClient<T> {

		@RequestMapping(method = RequestMethod.POST, value = "/users/")
		long save(T entity);

		@RequestMapping(method = RequestMethod.PUT, value = "/users/{id}")
		void update(@PathVariable("id") long id, T entity);

		@RequestMapping(method = RequestMethod.GET, value = "/users/{id}")
		T get(@PathVariable("id") long id);

		@RequestMapping(method = RequestMethod.DELETE, value = "/users/{id}")
		void delete(@PathVariable("id") long id);
	}

	@FeignClient("localapp")
	@RequestMapping(value = "/")
	protected interface UserClient extends CrudClient<User> {

	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	protected static class Application implements CrudClient {

		@RequestMapping(method = RequestMethod.GET, value = "/hello")
		public Hello getHello() {
			return new Hello("hello world 1");
		}

		@RequestMapping(method = RequestMethod.PATCH, value = "/hellop")
		public ResponseEntity<Void> patchHello() {
			return ResponseEntity.ok().header("X-Hello", "hello world patch").build();
		}

		public static void main(String[] args) {
			new SpringApplicationBuilder(Application.class).properties(
					"spring.application.name=feignclienttest",
					"management.contextPath=/admin").run(args);
		}

		@Override
		public long save(Object entity) {
			return 0;
		}

		@Override
		public void update(@PathVariable("id") long id, Object entity) {

		}

		@Override
		public Object get(@PathVariable("id") long id) {
			return null;
		}

		@Override
		public void delete(@PathVariable("id") long id) {

		}
	}

	@Test
	public void testSimpleType() {
		Hello hello = this.testClient.getHello();
		assertNotNull("hello was null", hello);
		assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
	}

	@Test
	public void testPatch() {
		ResponseEntity<Void> response = this.testClient.patchHello();
		assertThat(response, is(notNullValue()));
		String header = response.getHeaders().getFirst("X-Hello");
		assertThat(header, equalTo("hello world patch"));
	}

	@Test
	public void testFeignClientType() throws IllegalAccessException {
		assertThat(this.feignClient, is(instanceOf(feign.ribbon.RibbonClient.class)));
		Field field = ReflectionUtils.findField(feign.ribbon.RibbonClient.class,
				"delegate", Client.class);
		ReflectionUtils.makeAccessible(field);
		Client delegate = (Client) field.get(this.feignClient);
		assertThat(delegate, is(instanceOf(feign.httpclient.ApacheHttpClient.class)));
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Hello {
		private String message;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class User {
		private String message;
	}

	// Load balancer with fixed server list for "local" pointing to localhost
	@Configuration
	static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ILoadBalancer ribbonLoadBalancer() {
			BaseLoadBalancer balancer = new BaseLoadBalancer();
			balancer.setServersList(Arrays.asList(new Server("localhost", this.port)));
			return balancer;
		}

	}
}
