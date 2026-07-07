package com.ghanaride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic for group broadcasts (like driver location)
        // /queue for user specific messaging
        config.enableSimpleBroker("/topic", "/queue");
        // application endpoints prefix
        config.setApplicationDestinationPrefixes("/app");
        // user specific endpoint prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback options for browsers that don't support WebSockets
        registry.addEndpoint("/ws-ghanaride").withSockJS();
    }
}
