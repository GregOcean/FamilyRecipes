package com.familyrecipes.config;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class TimeConfig {
    @Bean public Clock businessClock(@Value("${app.time-zone:Asia/Shanghai}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
