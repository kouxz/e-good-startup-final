package com.projeto.egoodapp.data.local;

import com.projeto.egoodapp.data.model.Interest;

import java.util.Arrays;
import java.util.List;

/** Canonical stages and outcomes for a dealership contact. */
public final class InterestWorkflow {
    public static final String STATUS_NEW = "Novo";
    public static final String STATUS_IN_CONTACT = "Em contato";
    public static final String STATUS_FINISHED = "Finalizado";
    public static final String OUTCOME_SALE = "SALE";
    public static final String OUTCOME_NO_SALE = "NO_SALE";
    public static final List<String> STATUSES = Arrays.asList(
            STATUS_NEW, STATUS_IN_CONTACT, STATUS_FINISHED);

    private InterestWorkflow() {}

    public static boolean update(Interest interest, String status, String outcome) {
        if (interest == null || !STATUSES.contains(status)) return false;
        if (STATUS_FINISHED.equals(status)) {
            if (!isOutcome(outcome)) return false;
            interest.outcome = outcome;
        } else {
            if (outcome != null && !outcome.isEmpty()) return false;
            interest.outcome = "";
        }
        interest.status = status;
        return true;
    }

    /** Normalizes old snapshots. Finished contacts historically counted as successful sales. */
    public static boolean migrateLegacy(Interest interest) {
        if (interest == null) return false;
        boolean changed = false;
        if (!STATUSES.contains(interest.status)) {
            interest.status = STATUS_NEW;
            changed = true;
        }
        String outcome = interest.outcome == null ? "" : interest.outcome;
        if (STATUS_FINISHED.equals(interest.status)) {
            if (!isOutcome(outcome)) {
                interest.outcome = OUTCOME_SALE;
                changed = true;
            }
        } else if (!outcome.isEmpty()) {
            interest.outcome = "";
            changed = true;
        } else if (interest.outcome == null) {
            interest.outcome = "";
            changed = true;
        }
        return changed;
    }

    public static boolean isSale(Interest interest) {
        return interest != null && STATUS_FINISHED.equals(interest.status)
                && OUTCOME_SALE.equals(interest.outcome);
    }

    public static String outcomeLabel(String outcome) {
        if (OUTCOME_SALE.equals(outcome)) return "Venda concluída";
        if (OUTCOME_NO_SALE.equals(outcome)) return "Sem venda";
        return "";
    }

    private static boolean isOutcome(String outcome) {
        return OUTCOME_SALE.equals(outcome) || OUTCOME_NO_SALE.equals(outcome);
    }
}
