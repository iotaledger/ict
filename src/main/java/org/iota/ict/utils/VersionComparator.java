package org.iota.ict.utils;

import java.util.Comparator;

public final class VersionComparator implements Comparator<String> {

    protected static final VersionComparator instance = new VersionComparator();

    @Override
    public int compare(String version1, String version2) {
        assertVersionFormat(version1);
        assertVersionFormat(version2);
        int[] segments1 = segmentateVersion(version1);
        int[] segments2 = segmentateVersion(version2);
        return compareSegments(segments1, segments2);
    }

    protected int compareSegments(int[] segments1, int[] segments2) {
        for(int i = 0; i < Math.max(segments1.length, segments2.length); i++) {
            int seg1 = segments1.length > i ? segments1[i] : 0;
            int seg2 = segments2.length > i ? segments2[i] : 0;
            int segmentCompare = Integer.compare(seg1, seg2);
            if(segmentCompare != 0)
                return segmentCompare;
        }
        return 0;
    }

    protected void assertVersionFormat(String version) {
        if(!version.matches("^[0-9]*(\\.[0-9]*)*(\\-SNAPSHOT)?$"))
            throw new IllegalArgumentException("Unexpected format for version '"+version+"'.");
    }

    protected int[] segmentateVersion(String version) {
        String[] strSegments = version.replace("-SNAPSHOT", ".-1").split("\\.");
        int[] intSegments = new int[strSegments.length];
        for(int i = 0; i < intSegments.length; i++)
            intSegments[i] = Integer.parseInt(strSegments[i]);
        return intSegments;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    public static VersionComparator getInstance() {
        return instance;
    }
}
