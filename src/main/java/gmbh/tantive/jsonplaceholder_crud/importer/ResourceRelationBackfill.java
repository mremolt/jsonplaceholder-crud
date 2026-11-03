package gmbh.tantive.jsonplaceholder_crud.importer;

import java.util.List;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceBulkService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "jsonplaceholder.relations.backfill.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ResourceRelationBackfill implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRelationBackfill.class);
    private static final int PAGE_SIZE = 500;

    private final List<ResourceBulkService<?>> resources;

    @Override
    public void run(ApplicationArguments args) {
        for (ResourceBulkService<?> resource : resources) {
            backfill(resource);
        }
    }

    private <T extends ResourceModel> void backfill(ResourceBulkService<T> service) {
        long afterId = 0L;
        long total = 0L;
        while (true) {
            List<T> page = service.findAfterId(afterId, PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            service.importAll(page);
            total += page.size();
            afterId = page.getLast().getId();
        }
        LOGGER.info("Backfilled {} {} relation rows", total, service.resourceName());
    }
}