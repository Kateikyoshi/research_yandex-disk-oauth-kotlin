package jp.warau.bakari;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@EnableScheduling
@Component
public class Based {

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void minuteSpam() {
        LoggerFactory.getLogger(Based)
        System.out.println("kek");
    }
}
