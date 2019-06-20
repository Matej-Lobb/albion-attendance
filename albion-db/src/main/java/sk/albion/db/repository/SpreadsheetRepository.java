package sk.albion.db.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import sk.albion.db.entity.SpreadsheetEntity;

@Repository
public interface SpreadsheetRepository extends CrudRepository<SpreadsheetEntity, Long> {

    SpreadsheetEntity findByDiscordId(String discordId);
}
