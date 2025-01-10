package com.leads.leadsgen.scanners;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.models.ScanReport;

public abstract class Scanner {

    private final String name;

    public Scanner(String name) {
        this.name = name;
    }

    public abstract ScanReport scan(Asset asset);

    public String getName() {
        return name;
    }
}
