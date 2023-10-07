package org.zephyrsoft.optigemspoonfeeder.model;

import java.util.Comparator;

import lombok.Data;

@Data
public class IdAndName implements Comparable<IdAndName> {
    public static final Comparator<IdAndName> COMPARATOR = Comparator.comparing(IdAndName::getId)
        .thenComparing(IdAndName::getName);

    private final int id;
    private final String name;

    @Override
    public int compareTo(final IdAndName o) {
        return COMPARATOR.compare(this, o);
    }
}
