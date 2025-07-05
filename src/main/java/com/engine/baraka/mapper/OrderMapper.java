package com.engine.baraka.mapper;

import com.engine.baraka.dto.OrderResponse;
import com.engine.baraka.model.Order;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse orderToOrderResponse(Order order);

    List<OrderResponse> ordersToOrderResponses(List<Order> orders);
}