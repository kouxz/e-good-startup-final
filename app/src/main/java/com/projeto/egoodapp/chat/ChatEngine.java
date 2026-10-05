package com.projeto.egoodapp.chat;

import com.projeto.egoodapp.data.simulation.SimulationCalculations;
import com.projeto.egoodapp.data.simulation.SimulationCatalog;
import com.projeto.egoodapp.data.simulation.SimulationCatalog.Model;
import com.projeto.egoodapp.data.simulation.SimulationFormat;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Deterministic local conversation. No Android objects, network, or delayed callbacks. */
public final class ChatEngine {
    public static final int MAX_INPUT = 500, MAX_MESSAGES = 200;
    public enum Step { MENU, COMBUSTION, CUSTOM_NAME, CUSTOM_CONSUMPTION, ELECTRIC,
        DISTANCE, GAS_PRICE, ENERGY_PRICE, SOLAR_POWER, SOLAR_BILL, SOLAR_TARIFF }
    private Step step = Step.MENU;
    private boolean custom;
    private Model combustion, electric;
    private String customName = "";
    private double consumption, distance, gas, power, bill;
    private final List<ChatMessage> messages = new ArrayList<>();
    public ChatEngine() {
        bot("Olá! Sou o assistente local e-good. Posso comparar veículos, calcular os custos do seu carro e simular energia solar. "
                + "Os resultados são estimativas ilustrativas.\n\n" + prompt());
    }
    public Step step() { return step; }
    public List<ChatMessage> messages() { return java.util.Collections.unmodifiableList(new ArrayList<>(messages)); }
    public void clear() { messages.clear(); reset(); }
    public void start(String topic) { reset(); send(topic); }
    public void send(String input) {
        if (input == null || input.trim().isEmpty()) return;
        String text = input.trim();
        if (text.length() > MAX_INPUT) { bot("Use até 500 caracteres por mensagem."); return; }
        append(new ChatMessage(text, ChatMessage.TYPE_USER));
        String normalized = normalize(text);
        if (normalized.equals("voltar")) { back(); bot(prompt()); return; }
        if (normalized.equals("cancelar") || normalized.equals("reiniciar")) {
            reset(); bot((normalized.equals("cancelar") ? "Cálculo cancelado." : "Cálculo reiniciado.") + "\n\n" + prompt()); return;
        }
        if (normalized.equals("ajuda")) {
            bot("Use Comparar veículos, Meu carro ou Solar. Digite voltar para corrigir a etapa anterior, "
                    + "cancelar ou reiniciar para começar de novo.\n\n" + prompt()); return;
        }
        String answer = faq(normalized);
        if (answer != null) { bot(answer + "\n\n" + prompt()); return; }
        try {
            switch (step) {
                case MENU:
                    if (normalized.contains("solar")) step = Step.SOLAR_POWER;
                    else if (normalized.contains("meu carro") || normalized.contains("meu veiculo")) {
                        custom = true; step = Step.CUSTOM_NAME;
                    } else if (normalized.contains("compar") || normalized.equals("veiculos")
                            || normalized.equals("carros")) {
                        custom = false; step = Step.COMBUSTION;
                    } else { bot("Posso ajudar com cálculos e dúvidas preparados sobre Comparar e Solar.\n\n" + prompt()); return; }
                    break;
                case COMBUSTION:
                    combustion = select(text, SimulationCatalog.COMBUSTION);
                    consumption = combustion.consumption; step = Step.ELECTRIC; break;
                case CUSTOM_NAME:
                    customName = text; step = Step.CUSTOM_CONSUMPTION; break;
                case CUSTOM_CONSUMPTION:
                    double kmPerLiter = number(text);
                    if (kmPerLiter == 0) throw new IllegalArgumentException("O consumo em km/L deve ser maior que zero.");
                    consumption = 100 / kmPerLiter;
                    if (!Double.isFinite(consumption)) throw new IllegalArgumentException("Informe um consumo válido em km/L.");
                    step = Step.ELECTRIC; break;
                case ELECTRIC:
                    electric = select(text, SimulationCatalog.ELECTRIC); step = Step.DISTANCE; break;
                case DISTANCE: distance = number(text); step = Step.GAS_PRICE; break;
                case GAS_PRICE: gas = number(text); step = Step.ENERGY_PRICE; break;
                case ENERGY_PRICE:
                    String comparison = compare(number(text)); reset(); bot(comparison + "\n\n" + prompt()); return;
                case SOLAR_POWER: power = number(text); step = Step.SOLAR_BILL; break;
                case SOLAR_BILL: bill = number(text); step = Step.SOLAR_TARIFF; break;
                case SOLAR_TARIFF:
                    String solar = solar(number(text)); reset(); bot(solar + "\n\n" + prompt()); return;
            }
            bot(prompt());
        } catch (IllegalArgumentException | ArithmeticException invalid) {
            bot(invalid instanceof ArithmeticException ? "O valor é grande demais para esta simulação.\n\n" + prompt()
                    : invalid.getMessage() + "\n\n" + prompt());
        }
    }
    private String compare(double tariff) {
        SimulationCalculations.Comparison result = SimulationCalculations.compare(
                electric.consumption, consumption, distance, tariff, gas);
        boolean additional = result.difference.signum() < 0;
        String percent = result.combustionCost.signum() == 0 ? "Sem base percentual (custo de combustão zero)."
                : !Double.isFinite(result.differencePercent) ? "Percentual fora do intervalo calculável."
                : result.difference.signum() == 0 ? "Mesmo custo de energia/combustível."
                : SimulationFormat.integer(Math.abs(result.differencePercent)) + (additional ? "% mais caro." : "% mais barato.");
        String prices = custom ? "" : "\nPreço ilustrativo de compra: " + electric.name + " "
                + SimulationFormat.money(BigDecimal.valueOf(electric.price)) + "; " + combustion.name + " "
                + SimulationFormat.money(BigDecimal.valueOf(combustion.price)) + ".";
        return "Estimativa: " + (custom ? customName : combustion.name) + " × " + electric.name
                + "\nDistância mensal: " + SimulationFormat.number(distance) + " km"
                + "\nCombustão: " + SimulationFormat.money(result.combustionCost) + "/mês"
                + "\nElétrico: " + SimulationFormat.money(result.electricCost) + "/mês"
                + "\n" + (additional ? "Custo adicional mensal: " : "Economia mensal: ") + SimulationFormat.money(result.difference.abs())
                + "\nProjeção anual: " + SimulationFormat.money(result.annualDifference().abs())
                + (additional ? " de custo adicional." : " de economia.") + "\n" + percent + prices
                + "\n\nConsidera somente energia ou combustível; não inclui compra, manutenção, seguro ou impostos.";
    }
    private String solar(double tariff) {
        SimulationCalculations.Solar result = SimulationCalculations.solar(power, bill, tariff);
        return "Estimativa solar para " + SimulationFormat.number(power) + " kWp"
                + "\nGeração mensal: " + SimulationFormat.number(result.monthlyGeneration) + " kWh"
                + "\nGeração anual: " + SimulationFormat.number(result.annualGeneration) + " kWh"
                + "\nEconomia mensal: " + SimulationFormat.money(result.monthlySavings)
                + "\nEconomia anual: " + SimulationFormat.money(result.annualSavings)
                + "\nInvestimento ilustrativo: " + SimulationFormat.money(result.investment)
                + "\nRetorno: " + (result.paybackYears == null || !Double.isFinite(result.paybackYears)
                    ? "Sem retorno estimado" : String.format(SimulationFormat.BRAZIL, "%.1f anos", result.paybackYears))
                + "\n\nPremissas: 130 kWh/mês por kWp e R$ 4.000 por kWp instalado. A economia é limitada à conta informada. "
                + "Não considera clima, orientação, taxas ou regras de compensação locais.";
    }
    public String prompt() {
        switch (step) {
            case COMBUSTION: return "Escolha o veículo a combustão pelo número ou nome:\n" + list(SimulationCatalog.COMBUSTION, "L/100 km");
            case ELECTRIC: return "Escolha o veículo elétrico pelo número ou nome:\n" + list(SimulationCatalog.ELECTRIC, "kWh/100 km");
            case CUSTOM_NAME: return "Qual é o nome/modelo do seu carro?";
            case CUSTOM_CONSUMPTION: return "Qual é o consumo do seu carro em km/L? Ex.: 12,5.";
            case DISTANCE: return "Quantos quilômetros você percorre por mês? Ex.: 1500.";
            case GAS_PRICE: return "Qual é o preço da gasolina em R$/L? Ex.: 5,79.";
            case ENERGY_PRICE: return "Qual é a tarifa de energia em R$/kWh? Ex.: 0,85.";
            case SOLAR_POWER: return "Qual é a potência do sistema solar em kWp? Ex.: 5.";
            case SOLAR_BILL: return "Qual é o valor da conta de energia mensal em reais? Ex.: 400.";
            case SOLAR_TARIFF: return "Qual é a tarifa de energia em R$/kWh? Ex.: 0,85.";
            default: return "Escolha: Comparar veículos, Meu carro ou Solar. Para dúvidas, pergunte sobre consumo, tarifas, distância, economia ou preço de compra. Digite ajuda para ver os comandos.";
        }
    }
    private static String list(List<Model> models, String unit) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < models.size(); i++) {
            Model model = models.get(i);
            text.append(i + 1).append(". ").append(model.name).append(" — ")
                    .append(SimulationFormat.number(model.consumption)).append(' ').append(unit).append('\n');
        }
        return text.toString().trim();
    }
    private static Model select(String input, List<Model> models) {
        String name = normalize(input);
        if (name.matches("[0-9]+")) {
            try { int index = Integer.parseInt(name) - 1; if (index >= 0 && index < models.size()) return models.get(index); }
            catch (NumberFormatException ignored) {}
            throw new IllegalArgumentException("Escolha um número da lista.");
        }
        for (Model model : models) if (normalize(model.name).equals(name)) return model;
        List<Model> candidates = new ArrayList<>();
        for (Model model : models) {
            String full = normalize(model.name);
            String[] tokens = name.split(" ");
            if (name.length() >= 3 && java.util.Arrays.stream(tokens)
                    .allMatch(token -> java.util.Arrays.asList(full.split(" ")).contains(token))) candidates.add(model);
        }
        if (candidates.size() == 1) return candidates.get(0);
        if (candidates.size() > 1) throw new IllegalArgumentException("Nome ambíguo: "
                + String.join(", ", candidates.stream().map(model -> model.name).toArray(String[]::new))
                + ". Confirme pelo nome completo ou número da lista.");
        throw new IllegalArgumentException("Não encontrei esse modelo. Escolha um veículo da lista.");
    }
    static double number(String input) {
        String text = input.trim();
        // A single decimal separator: never silently delete minus signs, units or grouping.
        if (!text.matches("[0-9]+([.,][0-9]+)?")) throw new IllegalArgumentException("Informe apenas um número, sem separador de milhares ou unidade. Ex.: 1500 ou 0,85.");
        double value = Double.parseDouble(text.replace(',', '.'));
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Informe um número finito válido.");
        return value;
    }
    static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").trim();
    }
    private static String faq(String text) {
        boolean question = text.startsWith("o que") || text.startsWith("como") || text.startsWith("qual") || text.startsWith("quais")
                || text.startsWith("por que") || text.startsWith("quanto") || text.startsWith("explique") || text.startsWith("duvida")
                || text.equals("consumo") || text.equals("tarifas") || text.equals("economia")
                || text.equals("distancia") || text.equals("preco de compra") || text.equals("premissas solares");
        if (!question) return null;
        if (text.startsWith("quais") && (text.contains("veiculo") || text.contains("carro") || text.contains("modelo")))
            return "Modelos usados na tela Comparar:\n\nElétricos:\n" + list(SimulationCatalog.ELECTRIC, "kWh/100 km")
                    + "\n\nCombustão:\n" + list(SimulationCatalog.COMBUSTION, "L/100 km");
        if (text.contains("compra") || text.contains("preco do carro")) return "O preço de compra é um valor ilustrativo separado dos gastos de uso. Uma economia mensal de energia não significa recuperar automaticamente a diferença de preço entre os veículos.";
        if (text.contains("solar") || text.contains("kwp")) return "kWp mede a potência nominal do sistema solar. A tela Solar e este chat usam 130 kWh/mês por kWp e investimento de R$ 4.000/kWp; a economia não supera a conta mensal informada. São estimativas, não um orçamento.";
        if (text.contains("tarifa") || text.contains("gasolina")) return "Informe a tarifa elétrica em R$/kWh e a gasolina em R$/L. Consulte sua conta de energia e o preço do combustível; o chat não consulta preços em tempo real.";
        if (text.contains("consumo") || text.contains("km l") || text.contains("kwh") || text.contains("litro")) return "Elétricos usam kWh/100 km e carros a combustão usam L/100 km: quanto menor, menor o consumo para a mesma distância. Se seu carro informa km/L, convertemos por 100 ÷ km/L.";
        if (text.contains("distancia") || text.contains("quilometro")) return "Usamos sua distância mensal em km. O custo é consumo por 100 km × distância ÷ 100 × preço da energia ou combustível. A projeção anual considera 12 meses iguais.";
        if (text.contains("economia") || text.contains("compar") || text.contains("custo")) return "A economia é o custo de combustível menos o custo de energia. Se o elétrico custar mais, mostramos custo adicional. O percentual usa o custo de combustão como base; não inclui manutenção, seguro, impostos ou compra.";
        return null;
    }
    private void back() {
        switch (step) {
            case COMBUSTION: case CUSTOM_NAME: case SOLAR_POWER: reset(); break;
            case CUSTOM_CONSUMPTION: step = Step.CUSTOM_NAME; break;
            case ELECTRIC: step = custom ? Step.CUSTOM_CONSUMPTION : Step.COMBUSTION; break;
            case DISTANCE: step = Step.ELECTRIC; break;
            case GAS_PRICE: step = Step.DISTANCE; break;
            case ENERGY_PRICE: step = Step.GAS_PRICE; break;
            case SOLAR_BILL: step = Step.SOLAR_POWER; break;
            case SOLAR_TARIFF: step = Step.SOLAR_BILL; break;
            default: break;
        }
    }
    private void reset() {
        step = Step.MENU; custom = false; combustion = null; electric = null;
        customName = ""; consumption = distance = gas = power = bill = 0;
    }
    private void bot(String text) { append(new ChatMessage(text, ChatMessage.TYPE_BOT)); }
    private void append(ChatMessage message) {
        messages.add(message);
        while (messages.size() > MAX_MESSAGES) messages.remove(0);
    }
}
