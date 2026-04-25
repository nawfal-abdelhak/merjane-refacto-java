package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Slf4j
@Service
public class ProductService {


    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    public ProcessOrderResponse processOrder(Long orderId) {
        log.info("Processing order {}", orderId);
        Order order = orderRepository.findById(orderId).orElseThrow();
        for (Product product : order.getItems()) {
            processProduct(product);
        }
        return new ProcessOrderResponse(order.getId());
    }

    private void processProduct(Product product) {
        log.info("Processing product '{}' of type {}", product.getName(), product.getType());
        switch (product.getType()) {
            case NORMAL:
                handleNormalProduct(product);
                break;
            case SEASONAL:
                handleSeasonalProduct(product);
                break;
            case EXPIRABLE:
                handleExpirableProduct(product);
                break;
        }
    }

    private void handleNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else if (product.getLeadTime() > 0) {
            log.info("Product '{}' out of stock", product.getName());
            notifyDelay(product.getLeadTime(), product);
        }
    }

    private void handleSeasonalProduct(Product product) {
        LocalDate today = LocalDate.now();
        boolean inSeason = today.isAfter(product.getSeasonStartDate()) && today.isBefore(product.getSeasonEndDate());

        if (inSeason && product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else if (today.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
            log.info("Product '{}' out of stock and lead time exceeds season end", product.getName());
            notificationService.sendOutOfStockNotification(product.getName());
            product.setAvailable(0);
            productRepository.save(product);
        } else if (product.getSeasonStartDate().isAfter(today)) {
            log.info("Product '{}' season has not started yet", product.getName());
            notificationService.sendOutOfStockNotification(product.getName());
            productRepository.save(product);
        } else {
            log.info("Product '{}' out of stock in season", product.getName());
            notifyDelay(product.getLeadTime(), product);
        }
    }

    private void handleExpirableProduct(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            log.info("Product '{}' is expired or out of stock", product.getName());
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }

    private void notifyDelay(int leadTime, Product product) {
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }
}
