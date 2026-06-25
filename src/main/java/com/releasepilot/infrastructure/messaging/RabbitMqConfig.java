package com.releasepilot.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "promotion.events";
    public static final String AUDIT_QUEUE = "audit.queue";
    public static final String RELEASE_NOTES_QUEUE = "release-notes.queue";

    @Bean
    TopicExchange promotionEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE).build();
    }

    @Bean
    Queue releaseNotesQueue() {
        return QueueBuilder.durable(RELEASE_NOTES_QUEUE).build();
    }

    @Bean
    Binding auditBinding(@Qualifier("auditQueue") Queue auditQueue,
                         TopicExchange promotionEventsExchange) {
        return BindingBuilder.bind(auditQueue).to(promotionEventsExchange).with("#");
    }

    @Bean
    Binding releaseNotesBinding(@Qualifier("releaseNotesQueue") Queue releaseNotesQueue,
                                TopicExchange promotionEventsExchange) {
        return BindingBuilder.bind(releaseNotesQueue).to(promotionEventsExchange).with("PromotionApproved");
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
