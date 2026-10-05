package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.Vehicle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VehicleCatalog {
    private VehicleCatalog() {}
    public static List<Vehicle> filter(List<Vehicle> vehicles, String category, String search, String dealer) {
        String query = search.trim().toLowerCase(Locale.ROOT);
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : vehicles) {
            if (("Todos".equals(category) || category.equals(v.getCategoria()))
                    && v.getNome().toLowerCase(Locale.ROOT).contains(query)
                    && (dealer == null || dealer.equals(v.getConcessionariaId()))) result.add(v);
        }
        return result;
    }
}
