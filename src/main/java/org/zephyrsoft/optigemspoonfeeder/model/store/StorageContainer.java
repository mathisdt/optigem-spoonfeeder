package org.zephyrsoft.optigemspoonfeeder.model.store;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import org.zephyrsoft.optigemspoonfeeder.model.RulesResult;

import lombok.Getter;
import lombok.Setter;

/**
 * root node for EclipseStore
 */
@Getter
@Setter
public class StorageContainer {
    private Map<YearMonth, RulesResult> data = new HashMap<>();
}
