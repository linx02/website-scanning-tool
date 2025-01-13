package com.leads.leadsgen.scanners;

import com.leads.leadsgen.models.Asset;
import com.leads.leadsgen.models.ScanReport;

public abstract class Scanner {

    private final String name;

    public Scanner(String name) {
        this.name = name;
    }

    /**
     * Scans the given asset and returns a report.
     * @param asset The asset to scan.
     * @return The scan report.
     */
    public abstract ScanReport scan(Asset asset);

    /**
     * Returns the name of the scanner.
     * @return The name of the scanner.
     */
    public String getName() {
        return name;
    }
}
