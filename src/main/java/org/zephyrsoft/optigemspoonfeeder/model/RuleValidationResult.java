package org.zephyrsoft.optigemspoonfeeder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class RuleValidationResult {
    @Builder.Default
    private boolean error = false;
    /** 1-based !! */
    private long errorLine;
    private String errorMessage;
}
