package jp.warau.bakari;

import jp.warau.bakari.ya.FileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@EnableScheduling
@Component
public class ScheduledSpammer {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledSpammer.class);

    private final FileLoader fileLoader;

    private Boolean ranOnce = false;

    public ScheduledSpammer(FileLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS, initialDelay = 10)
    public void loader() {
        if (!ranOnce) {
            try {
                ranOnce = true;
                fileLoader.fetchFileToDisk();
            } catch (BakariException e) {
                logger.info("We had something go very bad: ", e);
            }
        }
    }
}
