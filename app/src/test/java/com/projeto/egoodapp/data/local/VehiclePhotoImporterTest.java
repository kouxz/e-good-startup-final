package com.projeto.egoodapp.data.local;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VehiclePhotoImporterTest {
    @Test public void sampleSizeKeepsDecodedEdgeAtOrBelowLimit() {
        assertEquals(1, VehiclePhotoImporter.sampleSize(2048, 1200));
        assertEquals(2, VehiclePhotoImporter.sampleSize(4096, 2000));
        assertEquals(4, VehiclePhotoImporter.sampleSize(5000, 4000));
    }
}
