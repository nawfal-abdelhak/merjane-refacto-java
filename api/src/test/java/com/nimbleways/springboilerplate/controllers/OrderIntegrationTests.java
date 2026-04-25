package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.enums.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderIntegrationTests {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NotificationService notificationService;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private ProductRepository productRepository;

        @Test
        public void processOrderShouldDecreaseAvailableForInStockProducts() throws Exception {
                Product normalInStock = productRepository.save(new Product(null, 15, 30, ProductType.NORMAL, "Product1", null, null, null));
                Product normalOutOfStock = productRepository.save(new Product(null, 10, 0, ProductType.NORMAL, "Product2", null, null, null));
                Product expirableInStock = productRepository.save(new Product(null, 15, 30, ProductType.EXPIRABLE, "Product3", LocalDate.now().plusDays(26), null, null));
                Product expirableExpired = productRepository.save(new Product(null, 90, 6, ProductType.EXPIRABLE, "Product4", LocalDate.now().minusDays(2), null, null));
                Product seasonalInSeason = productRepository.save(new Product(null, 15, 30, ProductType.SEASONAL, "Product5", null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30)));
                Product seasonalOutOfSeason = productRepository.save(new Product(null, 15, 30, ProductType.SEASONAL, "Product6", null, LocalDate.now().plusDays(10), LocalDate.now().plusDays(60)));

                Order order = orderRepository.save(new Order(null, new HashSet<>(Set.of(
                        normalInStock, normalOutOfStock, expirableInStock, expirableExpired, seasonalInSeason, seasonalOutOfSeason
                ))));

                mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                        .contentType("application/json"))
                        .andExpect(status().isOk());

                assertEquals(29, (int) productRepository.findById(normalInStock.getId()).orElseThrow().getAvailable());
                assertEquals(0, (int) productRepository.findById(normalOutOfStock.getId()).orElseThrow().getAvailable());
                assertEquals(29, (int) productRepository.findById(expirableInStock.getId()).orElseThrow().getAvailable());
                assertEquals(0, (int) productRepository.findById(expirableExpired.getId()).orElseThrow().getAvailable());
                assertEquals(29, (int) productRepository.findById(seasonalInSeason.getId()).orElseThrow().getAvailable());
                assertEquals(30, (int) productRepository.findById(seasonalOutOfSeason.getId()).orElseThrow().getAvailable());
        }
}
