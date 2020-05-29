package com.opentable.server.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

@Configuration
public class ReactiveServerJacksonConfiguration {

    @Bean
    public CodecCustomizer otCodecCustomizer(final ObjectMapper objectMapper) {
        return configurer -> {
            configurer.defaultCodecs().jackson2JsonEncoder(
                    new Jackson2JsonEncoder(objectMapper)
            );
            configurer.defaultCodecs().jackson2JsonDecoder(
                    new Jackson2JsonDecoder(objectMapper)
            );
        };
    }

}
