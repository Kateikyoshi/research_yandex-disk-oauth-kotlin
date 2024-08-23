package jp.warau.bakari;

import jp.warau.bakari.db.JdbcPoller;
import jp.warau.bakari.db.model.FunStuffMapper;
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

    private final JdbcPoller jdbcPoller;

    private final FunStuffMapper funStuffMapper;

    private Boolean ranOnce = false;

    public ScheduledSpammer(FileLoader fileLoader, JdbcPoller jdbcPoller, FunStuffMapper funStuffMapper) {
        this.fileLoader = fileLoader;
        this.jdbcPoller = jdbcPoller;
        this.funStuffMapper = funStuffMapper;
    }

    //TODO: Remove
    //@Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void minuteSpam() {
        logger.info("JDBC try: {}", jdbcPoller.fetchViolently());
        logger.info("MyBatis try: {}", funStuffMapper.getFunStuff(1L).getThing1());
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS, initialDelay = 10)
    public void loader() {
        if (!ranOnce) {
            try {
                //ranOnce = true;
                fileLoader.fetchFileToDisk();
            } catch (BakariException e) {
                logger.info("We had something go very bad: ", e);
            }
        }
    }
}
