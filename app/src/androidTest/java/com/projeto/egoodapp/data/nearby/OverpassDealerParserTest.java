package com.projeto.egoodapp.data.nearby;

import com.projeto.egoodapp.data.model.Concessionaria;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class OverpassDealerParserTest {
    @Test
    public void parsesNodesAndCentersFromWaysAndRelations() throws Exception {
        JSONArray elements = new JSONArray("["
                + "{\"type\":\"node\",\"id\":10,\"lat\":-23.50,\"lon\":-47.45,"
                + "\"tags\":{\"name\":\"Loja A\",\"phone\":\"1111\"}},"
                + "{\"type\":\"way\",\"id\":20,\"center\":{\"lat\":-23.51,\"lon\":-47.46},"
                + "\"tags\":{\"name\":\"Loja B\",\"addr:street\":\"Rua B\","
                + "\"addr:housenumber\":\"20\",\"addr:city\":\"Sorocaba\"}},"
                + "{\"type\":\"relation\",\"id\":30,"
                + "\"center\":{\"lat\":-23.52,\"lon\":-47.47},\"tags\":{}},"
                + "{\"type\":\"way\",\"id\":40,\"tags\":{\"name\":\"Sem centro\"}}"
                + "]");

        List<Concessionaria> dealers = OverpassDealerParser.parse(
                elements, -23.5015, -47.4581);

        assertEquals(3, dealers.size());
        assertEquals("osm:node:10", dealers.get(0).getStableKey());
        assertEquals("Loja A", dealers.get(0).getNome());
        assertEquals("1111", dealers.get(0).getTelefone());
        assertEquals("osm:way:20", dealers.get(1).getStableKey());
        assertEquals("Rua B, 20 — Sorocaba", dealers.get(1).getEndereco());
        assertEquals("osm:relation:30", dealers.get(2).getStableKey());
        assertEquals("Concessionária", dealers.get(2).getNome());
    }

    @Test
    public void ignoresDuplicateAndInvalidElements() throws Exception {
        JSONArray elements = new JSONArray("["
                + "{\"type\":\"node\",\"id\":10,\"lat\":-23.50,\"lon\":-47.45},"
                + "{\"type\":\"node\",\"id\":10,\"lat\":-23.51,\"lon\":-47.46},"
                + "{\"type\":\"node\",\"id\":11,\"lat\":0,\"lon\":0},"
                + "{\"type\":\"node\",\"lat\":-23.50,\"lon\":-47.45}"
                + "]");

        List<Concessionaria> dealers = OverpassDealerParser.parse(
                elements, -23.5015, -47.4581);

        assertEquals(1, dealers.size());
        assertEquals("osm:node:10", dealers.get(0).getStableKey());
    }
}
