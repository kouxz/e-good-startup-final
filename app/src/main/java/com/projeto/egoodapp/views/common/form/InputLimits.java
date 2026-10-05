package com.projeto.egoodapp.views.common.form;

import android.app.Activity;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;
import com.projeto.egoodapp.R;
import com.projeto.egoodapp.security.InputRules;

/** Filters affect typing and paste; stored data is not silently truncated. */
public final class InputLimits {
    private InputLimits() {}
    public static void field(EditText field, int max) {
        field.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            String value = source.subSequence(start, end).toString();
            StringBuilder cleaned = new StringBuilder();
            value.codePoints().filter(c -> !Character.isISOControl(c)
                    && Character.getType(c) != Character.FORMAT).forEach(cleaned::appendCodePoint);
            return value.contentEquals(cleaned) ? null : cleaned.toString();
        }, new InputFilter.LengthFilter(max)});
    }
    public static void account(Activity activity) {
        int[][] limits = {{R.id.editNome, InputRules.NAME}, {R.id.editNomeEmpresa, InputRules.NAME},
            {R.id.editEmail, InputRules.EMAIL}, {R.id.editEmailEmpresa, InputRules.EMAIL},
            {R.id.editTelefoneUsuario, InputRules.PHONE}, {R.id.editTelefone, InputRules.PHONE},
            {R.id.editCnpj, InputRules.CNPJ}, {R.id.editEndereco, InputRules.ADDRESS},
            {R.id.editCidade, InputRules.CITY}, {R.id.editEstado, InputRules.STATE}, {R.id.editCep, InputRules.ZIP}};
        for (int[] limit : limits) {
            EditText field = activity.findViewById(limit[0]); if (field != null) field(field, limit[1]);
        }
    }
    public static void vehicle(Activity activity) {
        int[][] limits = {{R.id.editMarca, InputRules.VEHICLE_TEXT}, {R.id.editModelo, InputRules.VEHICLE_TEXT},
            {R.id.editCor, InputRules.VEHICLE_TEXT}, {R.id.editDescricao, InputRules.DESCRIPTION},
            {R.id.editAno, 4}, {R.id.editQuilometragem, 9}, {R.id.editPreco, 12},
            {R.id.editBateria, 5}, {R.id.editAutonomia, 5}, {R.id.editConsumo, 8},
            {R.id.editPotencia, 6}, {R.id.editRecarga, InputRules.CHARGING}};
        for (int[] limit : limits) {
            EditText field = activity.findViewById(limit[0]); if (field != null) field(field, limit[1]);
        }
    }
}
