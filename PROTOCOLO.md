# Protocolo de decisões — Seldon Fleet

Este documento registra a regra de classificação adotada, o tratamento dos
defeitos do `frota.json` e as ameaças à validade da conclusão. A intenção é que
um avaliador consiga prever o status de um registro lendo apenas este arquivo.

## 1. Fonte e escopo

- Fonte única: `app/src/main/assets/frota.json`.
- Nenhuma chamada de rede, login, banco relacional ou tela adicional.
- O arquivo é lido a cada abertura. Apenas os favoritos são persistidos.
- A classificação vive em `domain/` e não conhece JSON, `Context`, Android ou
  Compose. A normalização de unidades acontece antes de o domínio receber o
  registro.

## 2. Regra adotada

A regra é a regra de referência da especificação, sem ajuste nos limiares para
produzir uma distribuição desejada. A ordem de avaliação é:

1. `INDETERMINADO`
2. `CRITICO`
3. `ATENCAO`
4. `OK`

O primeiro estado que casar define o resultado.

### 2.1 Derivadas

Após normalizar a energia para mAh:

```text
drenoAbsolutoMahPorHora = energiaMah / (screenOnMinutes / 60)
drenoRelativoPctPorHora = (drenoAbsolutoMahPorHora / batteryCapacityMah) * 100
```

O dreno relativo é usado na classificação. O dreno absoluto é exibido no
detalhe apenas como informação auxiliar.

### 2.2 Limiares

| Status | Condição |
|---|---|
| `INDETERMINADO` | capacidade ausente, zero ou negativa; `screenOnMinutes` ausente, zero ou negativo; energia não normalizável |
| `CRITICO` | dreno relativo ≥ 12 %/h **ou** `crashCount7d` ≥ 3 **ou** `jankFramesPct` ≥ 15 |
| `ATENCAO` | dreno relativo ≥ 7 %/h **ou** `crashCount7d` ≥ 1 **ou** `jankFramesPct` ≥ 8 |
| `OK` | nenhuma condição acima |

A comparação usa `>=`: exatamente no limiar promove o status. Os testes cobrem
valores imediatamente abaixo, exatamente no limiar e imediatamente acima.

## 3. Defeitos do arquivo

### 3.1 Unidade de energia inconsistente

Registros com `energyUnit = "mWh"` são convertidos na camada de dados:

```text
energiaMah = energyConsumed / nominalVoltage
```

Exemplo do arquivo: `DEV-0204` tem 3773 mWh e 3,85 V; a conversão produz
980 mAh. O dreno relativo fica em aproximadamente 6,32 %/h e o dispositivo é
`OK`. Sem a conversão, o valor seria interpretado como 3773 mAh e o veredito
mudaria para `CRITICO`.

Se a unidade for `mWh` e não houver tensão nominal válida, não há conversão
segura: o registro vira `INDETERMINADO`. Se a unidade for desconhecida, o
registro também vira `INDETERMINADO`; ele nunca é tratado silenciosamente como
mAh.

### 3.2 Registros incompletos

Quando `batteryCapacityMah` é `0`, negativo ou ausente, ou quando
`screenOnMinutes` é `0`, negativo ou ausente, o registro é mantido na lista e
classificado como `INDETERMINADO`. A justificativa informa qual divisor não
pôde ser usado.

Essa escolha foi feita em vez de excluir o registro ou falhar a ingestão
inteira:

- excluir esconderia um dispositivo com problema de dados;
- falhar tudo por um registro impediria a visualização dos outros 29;
- inventar um valor, produzir `NaN`/`Infinity` ou encerrar o app seria pior.

No arquivo oficial, `DEV-0311` tem capacidade `0` e `DEV-0745` tem
`screenOnMinutes = 0`; ambos aparecem como `INDETERMINADO`.

### 3.3 Decisões defensivas adicionais

O parser adota uma postura conservadora para outros campos que também poderiam
esconder dados ruins:

- `jankFramesPct` ausente, não finito ou fora de `0..100`: `INDETERMINADO`;
- `crashCount7d` ausente, negativo ou não inteiro: `INDETERMINADO`;
- energia ausente, negativa ou não finita: `INDETERMINADO`;
- `id` duplicado: o segundo registro é renomeado com sufixo e recebe uma
  ressalva, em vez de sobrescrever o primeiro;
- registro que não é objeto JSON: é ignorado com aviso de frota;
- JSON inválido, vazio ou sem `devices`: erro controlado, sem exceção chegando
  à UI.

Essas regras são uma extensão conservadora da regra oficial. Elas não mudam o
status de nenhum dos 30 registros oficiais além dos dois defeitos previstos.

## 4. Índice de risco

O detalhe e a lista também mostram um índice de risco de 0 a 100. Ele é
**auxiliar e não participa da classificação oficial**. Seu cálculo usa os
mesmos sinais e limiares, com pesos heurísticos (dreno 55%, falhas 30%, quadros
15%) e piso por status. Ele não deve ser lido como uma segunda regra de negócio
nem como uma implementação da Trilha A.

A Trilha A é uma implementação separada, em
`domain/classifier/PercentileStatusClassifier.kt`, avaliada offline contra a
referência em [docs/trilha-a.md](docs/trilha-a.md). Ela não participa desta
regra, não é injetada na UI e não altera nenhum dos 30 resultados oficiais.

Para registros `INDETERMINADO`, o risco não é calculado e a interface mostra
“—” ou “risco não calculável”, em vez de um zero que poderia sugerir saúde.

## 5. Ameaças à validade

1. **O dreno relativo assume que todo o consumo ocorreu com a tela ligada.**
   Serviços em primeiro plano, sincronização ou uso em segundo plano podem
   inflar o dreno e produzir um falso `CRITICO`.
2. **A amostra é pequena e única.** São 30 registros de uma exportação; os
   limiares não foram calibrados com uma população maior. A classificação é
   descritiva daquele arquivo, não uma avaliação universal da frota.
3. **A conversão `mWh → mAh` usa a tensão nominal.** A tensão real varia com
   carga e consumo; a conversão é uma aproximação, não uma medição.
4. **A janela de tela pode ser curta.** Poucos minutos de tela amplificam
   erros de arredondamento e podem fazer um dreno pontual parecer estrutural.
5. **O arquivo não tem rótulos independentes.** A regra oficial é a referência
   adotada; não existe ground truth externo para afirmar que um status é
   “verdadeiro” em todos os casos.
6. **Dados ausentes viram `INDETERMINADO`.** Isso evita invenção, mas pode
   esconder um aparelho que precisaria de atenção se os campos estivessem
   disponíveis.
7. **O índice de risco é heurístico.** Os pesos e pisos não foram validados
   empiricamente e não alteram o status oficial.
8. **A Trilha A não é uma validação independente.** O classificador por
   percentil foi comparado com a própria referência, sem ground truth externo;
   a adjudicação das divergências é manual e não cega. O resultado não deve ser
   lido como acurácia de nenhum dos dois métodos.

## 6. Reprodutibilidade

```bash
# Testes JVM
./gradlew :app:testDebugUnitTest

# Apenas a comparação da Trilha A
./gradlew :app:testDebugUnitTest --tests "com.seldon.fleet.domain.comparison.*"

# Análise estática
./gradlew :app:lintDebug

# APK
./gradlew :app:assembleDebug
```

No Windows, use `gradlew.bat`. O CI repete lint e testes a cada push. Neste
workspace não há remote Git configurado nem credenciais do GitHub; a execução
remota deve ser confirmada depois do push para o repositório de entrega.
Localmente, os mesmos comandos estão verdes.

## 7. Declaração de uso de IA

O desenvolvimento contou com assistência de IA para scaffolding, implementação,
revisão e redação. As decisões de arquitetura, a regra de classificação, o
tratamento dos defeitos e as limitações registradas aqui foram revisados e
assumidos como parte da solução; o código deve ser compreendido e defendido na
entrevista técnica. Este protocolo não atribui à IA a autoria das decisões de
engenharia.
