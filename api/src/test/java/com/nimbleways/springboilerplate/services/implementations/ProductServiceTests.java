package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.enums.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@UnitTest
public class ProductServiceTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private ProductService productService;


    @Test
    public void whenNormalProductInStock_thenDecrementAvailable() {
        Product product = new Product(null, 15, 10, ProductType.NORMAL, "Product", null, null, null);
        processOrderWith(product);
        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService, never()).sendDelayNotification(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    public void whenNormalProductOutOfStock_thenSendDelayNotification() {
        Product product = new Product(null, 15, 0, ProductType.NORMAL, "Product", null, null, null);
        processOrderWith(product);
        verify(notificationService).sendDelayNotification(15, "Product");
        verify(productRepository).save(product);
    }


    @Test
    public void whenSeasonalProductInSeasonAndInStock_thenDecrementAvailable() {
        Product product = new Product(null, 15, 10, ProductType.SEASONAL, "Product",
                null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));
        processOrderWith(product);
        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService, never()).sendOutOfStockNotification(Mockito.anyString());
    }

    @Test
    public void whenSeasonalProductOutOfStockAndLeadTimeExceedsSeasonEnd_thenSendOutOfStockNotification() {
        Product product = new Product(null, 60, 0, ProductType.SEASONAL, "Product",
                null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));
        processOrderWith(product);
        assertEquals(0, product.getAvailable());
        verify(notificationService).sendOutOfStockNotification("Product");
    }

    @Test
    public void whenSeasonalProductSeasonNotStarted_thenSendOutOfStockNotification() {
        Product product = new Product(null, 15, 0, ProductType.SEASONAL, "Product",
                null, LocalDate.now().plusDays(10), LocalDate.now().plusDays(60));
        processOrderWith(product);
        verify(notificationService).sendOutOfStockNotification("Product");
    }

    @Test
    public void whenSeasonalProductOutOfStockInSeason_thenSendDelayNotification() {
        Product product = new Product(null, 5, 0, ProductType.SEASONAL, "Product",
                null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));
        processOrderWith(product);
        verify(notificationService).sendDelayNotification(5, "Product");
    }



    @Test
    public void whenExpirableProductInStockAndNotExpired_thenDecrementAvailable() {
        Product product = new Product(null, 15, 10, ProductType.EXPIRABLE, "Product",
                LocalDate.now().plusDays(10), null, null);
        processOrderWith(product);
        assertEquals(9, product.getAvailable());
        verify(productRepository).save(product);
        verify(notificationService, never()).sendExpirationNotification(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void whenExpirableProductExpired_thenSendExpirationNotification() {
        LocalDate expiryDate = LocalDate.now().minusDays(1);
        Product product = new Product(null, 15, 10, ProductType.EXPIRABLE, "Product", expiryDate, null, null);
        processOrderWith(product);
        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification("Product", expiryDate);
    }

    @Test
    public void whenExpirableProductOutOfStock_thenSendExpirationNotification() {
        LocalDate expiryDate = LocalDate.now().plusDays(5);
        Product product = new Product(null, 15, 0, ProductType.EXPIRABLE, "Product", expiryDate, null, null);
        processOrderWith(product);
        assertEquals(0, product.getAvailable());
        verify(notificationService).sendExpirationNotification("Product", expiryDate);
    }

    private void processOrderWith(Product product) {
        Order order = new Order(1L, Set.of(product));
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        Mockito.when(productRepository.save(product)).thenReturn(product);
        productService.processOrder(1L);
    }
}
