package com.projeto.egoodapp.chat;

import com.projeto.egoodapp.data.simulation.SimulationCalculations;
import com.projeto.egoodapp.data.simulation.SimulationCatalog;
import com.projeto.egoodapp.data.simulation.SimulationFormat;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatEngineTest {
    private String last(ChatEngine engine) { return engine.messages().get(engine.messages().size() - 1).getText(); }
    private void send(ChatEngine engine, String... inputs) { for (String input : inputs) engine.send(input); }

    @Test public void everyPairUsesTheSameCostsAsComparison() {
        for (int c = 0; c < SimulationCatalog.COMBUSTION.size(); c++) {
            for (int e = 0; e < SimulationCatalog.ELECTRIC.size(); e++) {
                ChatEngine engine = new ChatEngine();
                send(engine, "Comparar veículos", String.valueOf(c + 1), String.valueOf(e + 1), "1500", "5,79", "0.85");
                SimulationCalculations.Comparison result = SimulationCalculations.compare(
                        SimulationCatalog.ELECTRIC.get(e).consumption, SimulationCatalog.COMBUSTION.get(c).consumption, 1500, .85, 5.79);
                assertTrue(last(engine), last(engine).contains(SimulationFormat.money(result.electricCost) + "/mês"));
                assertTrue(last(engine).contains(SimulationFormat.money(result.combustionCost) + "/mês"));
                assertTrue(last(engine).contains(SimulationFormat.money(result.annualDifference().abs())));
                assertEquals(ChatEngine.Step.MENU, engine.step());
            }
        }
    }
    @Test public void dolphinModelsAreNotConfusedAndAmbiguityRequiresConfirmation() {
        ChatEngine engine = new ChatEngine(); send(engine, "comparar", "onix", "dolphin");
        assertEquals(ChatEngine.Step.ELECTRIC, engine.step()); assertTrue(last(engine).contains("Nome ambíguo"));
        send(engine, "BYD Dolphin", "1500", "5,79", "0,85");
        assertTrue(last(engine).contains("Elétrico: R$ 170,85/mês"));
        assertTrue(last(engine).contains("× BYD Dolphin\n"));
        send(engine, "comparar", "1", "dolphin mini", "1500", "5.79", "0.85");
        assertTrue(last(engine).contains("Elétrico: R$ 164,48/mês"));
    }
    @Test public void allModelsCanBeChosenByFullNameAndAccentsAreIgnored() {
        ChatEngine engine = new ChatEngine();
        for (var electric : SimulationCatalog.ELECTRIC) {
            send(engine, "reiniciar", "COMPARAR VEÍCULOS", "CHEVROLET ONIX 1.0 TURBO", electric.name);
            assertEquals(ChatEngine.Step.DISTANCE, engine.step());
        }
        for (var combustion : SimulationCatalog.COMBUSTION) {
            send(engine, "reiniciar", "comparar", combustion.name);
            assertEquals(ChatEngine.Step.ELECTRIC, engine.step());
        }
    }
    @Test public void customConsumptionConvertsKmPerLiterInsteadOfUsingAverage() {
        ChatEngine engine = new ChatEngine(); send(engine, "meu carro", "Teste", "0");
        assertEquals(ChatEngine.Step.CUSTOM_CONSUMPTION, engine.step());
        send(engine, "12,5", "1", "1000", "6", "1");
        assertTrue(last(engine).contains("Combustão: R$ 480,00/mês"));
        assertTrue(last(engine).contains("Elétrico: R$ 129,00/mês"));
        assertTrue(last(engine).contains("Teste × BYD Dolphin Mini"));
    }
    @Test public void solarIsIdenticalToExistingScreenAndBillCapsSavings() {
        ChatEngine engine = new ChatEngine(); send(engine, "solar", "5", "400", "0,85");
        assertTrue(last(engine).contains("Geração mensal: 650 kWh"));
        assertTrue(last(engine).contains("Economia mensal: R$ 400,00"));
        assertTrue(last(engine).contains("Investimento ilustrativo: R$ 20.000,00"));
        assertTrue(last(engine).contains("Retorno: 4,2 anos"));
        send(engine, "solar", "7.5", "800", "0,40");
        assertTrue(last(engine).contains("Economia mensal: R$ 390,00"));
    }
    @Test public void solarAllowsZerosAndHandlesUnrepresentablePanelCounts() {
        ChatEngine engine = new ChatEngine(); send(engine, "solar", "0", "0", "0");
        assertTrue(last(engine).contains("Sem retorno estimado")); assertFalse(last(engine).contains("NaN"));
        send(engine, "solar", "999999999999", "100", "1");
        assertEquals(ChatEngine.Step.SOLAR_TARIFF, engine.step());
        assertTrue(last(engine).contains("grande demais"));
        send(engine, "voltar", "voltar", "5", "400", "0.85");
        assertEquals(ChatEngine.Step.MENU, engine.step());
    }
    @Test public void higherElectricCostsAreReportedAsAdditionalCosts() {
        ChatEngine engine = new ChatEngine(); send(engine, "comparar", "1", "1", "1000", "0,1", "2");
        assertTrue(last(engine).contains("Custo adicional mensal: R$ 245,50"));
        assertTrue(last(engine).contains("mais caro"));
    }
    @Test public void zeroBaseHasNoPercentageAndNoNonFiniteOutput() {
        ChatEngine engine = new ChatEngine(); send(engine, "comparar", "1", "1", "0", "0", "0");
        assertTrue(last(engine).contains("Sem base percentual"));
        assertFalse(last(engine).contains("NaN")); assertFalse(last(engine).contains("Infinity"));
    }
    @Test public void extremeFiniteValuesNeverExposeInfinitePercentagesOrPayback() {
        String tiny = "0." + "0".repeat(310) + "1";
        ChatEngine engine = new ChatEngine(); send(engine, "solar", "5", "400", tiny);
        assertEquals(ChatEngine.Step.MENU, engine.step());
        assertTrue(last(engine).contains("Sem retorno estimado")); assertFalse(last(engine).contains("Infinity"));
        send(engine, "comparar", "1", "1", "1500", tiny, "1");
        assertTrue(last(engine).contains("Percentual fora do intervalo calculável"));
        assertFalse(last(engine).contains("Infinity"));
    }
    @Test public void invalidNumbersNeverAdvanceOrSilentlyStripCharacters() {
        ChatEngine engine = new ChatEngine(); send(engine, "comparar", "1", "1");
        for (String invalid : new String[]{"-1", "NaN", "Infinity", "1.500,50", "1,500.50", "1500 km", "1e3", "1 500", "9".repeat(400)}) {
            engine.send(invalid); assertEquals(invalid, ChatEngine.Step.DISTANCE, engine.step());
        }
        engine.send("1,5"); assertEquals(ChatEngine.Step.GAS_PRICE, engine.step());
    }
    @Test public void backTracksCustomAndStandardFlowsCorrectly() {
        ChatEngine engine = new ChatEngine(); send(engine, "meu carro", "Teste", "12", "voltar");
        assertEquals(ChatEngine.Step.CUSTOM_CONSUMPTION, engine.step());
        send(engine, "voltar"); assertEquals(ChatEngine.Step.CUSTOM_NAME, engine.step());
        send(engine, "voltar"); assertEquals(ChatEngine.Step.MENU, engine.step());
        send(engine, "comparar", "1", "1", "1500", "6", "voltar");
        assertEquals(ChatEngine.Step.GAS_PRICE, engine.step());
        send(engine, "voltar", "voltar", "voltar"); assertEquals(ChatEngine.Step.COMBUSTION, engine.step());
    }
    @Test public void helpAndPreparedQuestionsPreserveCurrentStep() {
        ChatEngine engine = new ChatEngine(); send(engine, "comparar", "1", "1");
        for (String question : new String[]{"ajuda", "Como é calculada a economia?", "O que é kWh?", "Qual tarifa de energia usar?", "Como funciona o preço de compra?"}) {
            engine.send(question); assertEquals(ChatEngine.Step.DISTANCE, engine.step());
            assertTrue(last(engine).contains("Quantos quilômetros"));
        }
        send(engine, "1500", "6", "1"); assertEquals(ChatEngine.Step.MENU, engine.step());
    }
    @Test public void cancellationAndTopicShortcutsDoNotLeakPriorValues() {
        ChatEngine engine = new ChatEngine(); send(engine, "solar", "10", "cancelar");
        assertEquals(ChatEngine.Step.MENU, engine.step());
        engine.start("Meu carro"); assertEquals(ChatEngine.Step.CUSTOM_NAME, engine.step());
        engine.start("Solar"); assertEquals(ChatEngine.Step.SOLAR_POWER, engine.step());
    }
    @Test public void badModelsAndUnknownRequestsAreHandledWithoutAdvancing() {
        ChatEngine engine = new ChatEngine(); engine.send("previsão do tempo");
        assertEquals(ChatEngine.Step.MENU, engine.step()); assertTrue(last(engine).contains("Comparar"));
        send(engine, "comparar", "999", "Geely EX2");
        assertEquals(ChatEngine.Step.COMBUSTION, engine.step());
    }
    @Test public void messageLimitsDoNotResetActiveCalculation() {
        ChatEngine engine = new ChatEngine(); send(engine, "comparar", "1", "1");
        for (int i = 0; i < 150; i++) engine.send("ajuda");
        assertEquals(200, engine.messages().size()); assertEquals(ChatEngine.Step.DISTANCE, engine.step());
        engine.send("a".repeat(501)); assertEquals(ChatEngine.Step.DISTANCE, engine.step());
        assertTrue(last(engine).contains("500 caracteres"));
        send(engine, "1500", "5.79", "0.85"); assertTrue(last(engine).contains("R$ 164,48/mês"));
    }
}
