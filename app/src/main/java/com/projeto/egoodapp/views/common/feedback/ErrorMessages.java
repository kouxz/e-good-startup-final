package com.projeto.egoodapp.views.common.feedback;

import java.util.Set;

/** Only known domain messages may be rendered; arbitrary IOException/server text is hidden. */
public final class ErrorMessages {
    private static final Set<String> APPROVED = Set.of(
            "Selecione uma foto válida.", "Selecione a foto novamente.", "Foto indisponível.",
            "O arquivo selecionado não é uma imagem válida.", "A foto deve ter no máximo 10 MB.",
            "A foto deve ter no máximo 20 megapixels.", "A foto selecionada está vazia.",
            "Use uma imagem JPEG, PNG, WebP, HEIC ou HEIF.", "Não foi possível salvar a foto.",
            "Complete o perfil da concessionária antes de publicar", "Complete os dados do perfil",
            "Veículo indisponível", "Apenas usuários Pessoa podem avaliar concessionárias",
            "Avaliação inválida", "O texto excede o limite permitido");
    private ErrorMessages() {}
    public static String safe(Exception failure) {
        String message = failure == null ? null : failure.getMessage();
        return APPROVED.contains(message == null ? "" : message) ? message
                : "Não foi possível concluir a operação. Tente novamente.";
    }
}
