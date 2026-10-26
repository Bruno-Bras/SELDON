# Trilha A — classificação por percentil da frota

> **Status:** experimento opcional, offline e não usado em produção. A regra
> oficial continua sendo a do `FleetAnomalyEngine`; esta trilha existe para
> comparar uma segunda implementação orientada por dados com a referência.

## 1. Pergunta

A Trilha A pede uma segunda implementação da interface de classificação,
orientada por dados em vez de limiares físicos fixos, e uma comparação explícita
contra a regra de referência: qual das duas erra menos, em quais casos e por quê.

A regra de referência é a especificação oficial:

- `INDETERMINADO` para dados que não permitem calcular as derivadas;
- `CRITICO` se dreno relativo ≥ 12 %/h, falhas ≥ 3 ou jank ≥ 15%;
- `ATENCAO` se dreno relativo ≥ 7 %/h, falhas ≥ 1 ou jank ≥ 8%;
- `OK` caso contrário.

## 2. Método pré-registrado

### 2.1 Candidato

O candidato é o `PercentileStatusClassifier`. Para cada dispositivo válido:

1. Calcular as métricas comuns (`dreno relativo`, `jank` e `falhas`) usando o
   mesmo helper de validação e derivação da referência.
2. Para cada sinal, calcular o rank percentual entre os dispositivos válidos da
   frota, **excluindo o próprio dispositivo** (*leave-one-out*).
3. Usar *midrank* para empates:
   `(quantidade abaixo + 0,5 × quantidade igual) / quantidade de pares`.
4. O score é o **máximo** dos três ranks. A escolha espelha a união da regra de
   referência: basta um sinal estar no extremo da frota.
5. Aplicar os cortes pre-registrados:
   - `CRITICO` se score ≥ **P90**;
   - `ATENCAO` se score ≥ **P60**;
   - `OK` caso contrário.
6. Registros inválidos permanecem `INDETERMINADO` e não entram na distribuição
   de pares. Sem pares válidos, o percentil não existe e o resultado também é
   `INDETERMINADO`.

O candidato não usa os limiares físicos da referência e não usa `riskScore`.

### 2.2 Por que percentil e por que máximo?

Com 28 registros válidos e apenas 10 por tier, clusterização ou um modelo
embarcado seriam instáveis e difíceis de justificar. Percentil é uma alternativa
simples, determinística e auditável.

O máximo foi escolhido em vez da média porque a referência é uma união de
sinais: um dreno muito alto não deveria ser diluído por `crashCount7d = 0`.
O custo dessa escolha aparece na comparação: ranks de sinais com pouca variação
podem promover variações absolutas pequenas.

### 2.3 Pré-registro

Os cortes P90/P60, o *leave-one-out*, o *midrank* e o máximo foram definidos
antes da execução final. Eles não foram ajustados para reproduzir a distribuição
`16 / 8 / 4 / 2` da referência. O candidato produziu uma distribuição diferente,
`11 / 11 / 6 / 2`, e essa diferença é parte do resultado.

## 3. Dados

O arquivo oficial tem 30 registros:

- **28 válidos**;
- **2 `INDETERMINADO`**: `DEV-0311` (capacidade 0) e `DEV-0745`
  (`screenOnMinutes = 0`);
- dreno relativo entre **2,529 e 19,444 %/h**;
- jank entre **0 e 11,2%** (9,8% entre os válidos);
- falhas entre **0 e 2**.

A referência classifica os 30 registros como **16 OK, 8 ATENCAO, 4 CRITICO e
2 INDETERMINADO**.

## 4. Resultado

### 4.1 Contagens

| Status | Referência | Candidato |
|---|---:|---:|
| `OK` | 16 | 11 |
| `ATENCAO` | 8 | 11 |
| `CRITICO` | 4 | 6 |
| `INDETERMINADO` | 2 | 2 |
| **Total** | **30** | **30** |

O candidato concorda com a referência em **21 dos 30 registros** (70%). Entre os
28 válidos, a concordância é de **21/28 (75%)**. Há **9 divergências**.

### 4.2 Matriz de confusão

Linhas: referência. Colunas: candidato.

| referência \ candidato | `OK` | `ATENCAO` | `CRITICO` | `INDETERMINADO` | total |
|---|---:|---:|---:|---:|---:|
| `OK` | 11 | 5 | 0 | 0 | 16 |
| `ATENCAO` | 0 | 5 | 3 | 0 | 8 |
| `CRITICO` | 0 | 1 | 3 | 0 | 4 |
| `INDETERMINADO` | 0 | 0 | 0 | 2 | 2 |
| **total** | **11** | **11** | **6** | **2** | **30** |

Os dois registros inválidos continuam `INDETERMINADO` nas duas implementações;
o candidato não inventa status para eles.

### 4.3 Divergências

| ID | Dreno %/h | Jank % | Falhas | Referência | Candidato | Score | Dreno p | Jank p | Falhas p |
|---|---:|---:|---:|---|---|---:|---:|---:|---:|
| `DEV-0137` | 6,231 | 7,4 | 2 | `ATENCAO` | `CRITICO` | 1,000 | 0,519 | 0,963 | 1,000 |
| `DEV-0800` | 5,922 | 4,3 | 0 | `OK` | `ATENCAO` | 0,667 | 0,444 | 0,667 | 0,463 |
| `DEV-0854` | 6,070 | 5,2 | 0 | `OK` | `ATENCAO` | 0,741 | 0,481 | 0,741 | 0,463 |
| `DEV-1057` | 4,647 | 5,5 | 0 | `OK` | `ATENCAO` | 0,778 | 0,259 | 0,778 | 0,463 |
| `DEV-1231` | 4,660 | 4,8 | 0 | `OK` | `ATENCAO` | 0,704 | 0,296 | 0,704 | 0,463 |
| `DEV-1366` | 4,132 | 5,7 | 0 | `OK` | `ATENCAO` | 0,815 | 0,148 | 0,815 | 0,463 |
| `DEV-1546` | 7,491 | 7,1 | 0 | `ATENCAO` | `CRITICO` | 0,907 | 0,667 | 0,907 | 0,463 |
| `DEV-1582` | 10,232 | 7,1 | 0 | `ATENCAO` | `CRITICO` | 0,907 | 0,815 | 0,907 | 0,463 |
| `DEV-1924` | 13,379 | 3,0 | 0 | `CRITICO` | `ATENCAO` | 0,889 | 0,889 | 0,296 | 0,463 |

## 5. Adjudicação

Não existe ground truth independente no arquivo. A referência é a definição
operacional adotada, não uma verdade externa. Para cada divergência, o critério
de adjudicação foi:

- candidato `CRITICO` sem nenhum sinal no limiar crítico da especificação:
  **falso positivo**;
- candidato `ATENCAO` sem nenhum sinal no limiar de atenção: **falso positivo**;
- candidato abaixo de `CRITICO` quando a referência encontrou um sinal crítico:
  **falso negativo**.

Essa rubrica é operacional e não cega: o autor da entrega conhece a referência.
Ela serve para ordenar os casos, não para produzir uma medida de acurácia.

| ID | Evidência | Julgamento |
|---|---|---|
| `DEV-0137` | Dreno 6,231; jank 7,4; 2 falhas. O candidato chega a crítico porque as 2 falhas são o topo da frota e o jank também é extremo em rank. | A referência é mais plausível: 2 falhas ficam abaixo do limiar crítico 3 e o dreno está em `ATENCAO`. Falso positivo do candidato. |
| `DEV-0800` | Dreno 5,922; jank 4,3; 0 falhas. Score 0,667 vem do jank. | Jank 4,3% e dreno 5,9% não atingem nem o limiar de atenção. Falso positivo. |
| `DEV-0854` | Dreno 6,070; jank 5,2; 0 falhas. Score 0,741 vem do jank. | Mesmo problema: percentil alto de um jank absoluto baixo. Falso positivo. |
| `DEV-1057` | Dreno 4,647; jank 5,5; 0 falhas. Score 0,778 vem do jank. | Falso positivo; nenhum sinal chega ao limiar de atenção. |
| `DEV-1231` | Dreno 4,660; jank 4,8; 0 falhas. Score 0,704 vem do jank. | Falso positivo; nenhum sinal chega ao limiar de atenção. |
| `DEV-1366` | Dreno 4,132; jank 5,7; 0 falhas. Score 0,815 vem do jank. | Falso positivo; a frota tem jank baixo e o rank transforma 5,7% em alerta. |
| `DEV-1546` | Dreno 7,491; jank 7,1; 0 falhas. Score 0,907 vem do jank. | A referência fica em `ATENCAO` pelo dreno ≥ 7, mas o jank 7,1 ainda está abaixo de 8. O candidato promove a crítico por rank. Falso positivo. |
| `DEV-1582` | Dreno 10,232; jank 7,1; 0 falhas. Score 0,907 vem do jank. | Referência em `ATENCAO` (dreno ≥ 7, mas < 12; jank < 8). O candidato cria um crítico sem sinal crítico. Falso positivo. |
| `DEV-1924` | Dreno 13,379; jank 3,0; 0 falhas. Rank de dreno 0,889. | O dreno passa do limiar crítico 12, mas o rank fica 0,011 abaixo de P90. Falso negativo do candidato. |

**Placar da adjudicação:** 8 falsos positivos e 1 falso negativo do candidato;
a referência foi considerada mais plausível nos 9 casos. O candidato não venceu
nenhum caso nesta amostra.

### 5.1 Por que o candidato erra

1. **Percentil não tem piso absoluto.** Um jank de 4,3% pode ser “alto para esta
   frota” e virar `ATENCAO` mesmo sem significado operacional. A referência
   ancora a decisão em valores físicos.
2. **Ranks de sinais esparsos são instáveis.** `crashCount7d` só assume 0, 1 e
   2; `crash = 2` é o topo da frota, mas ainda está abaixo do limiar crítico 3.
   O máximo promove esse rank a `CRITICO`.
3. **O corte P90 é um corte duro de posição.** `DEV-1924` tem dreno 13,379,
   acima do limiar crítico, mas é o quarto maior entre 28 e fica em 0,889. O
   candidato o rebaixa para `ATENCAO`.
4. **Máximo de percentis não compara escalas.** O rank de jank e o rank de dreno
   são tratados como equivalentes, embora um represente 4,3% e o outro 19,4%.

## 6. Veredito

**Na amostra, a referência erra menos.** O candidato diverge em 9 dos 30
registros e, pela rubrica operacional, todos os 9 são erros do candidato:
8 falsos positivos e 1 falso negativo. A referência é mais plausível em todos
os casos; o candidato não apresenta nenhum caso em que seja claramente melhor.

Essa conclusão é uma comparação contra a especificação adotada, **não uma
estimativa de acurácia**. Sem rótulos independentes, não é possível afirmar que
a referência está “certa” no mundo; apenas que ela é a definição operacional e
que o candidato se afasta dela de formas que a análise física não sustenta.

O candidato pode ser útil como sinal relativo de triagem em uma frota com
baseline conhecido, mas não como substituto da regra oficial. Por isso ele não
foi ligado à UI nem ao `FleetTelemetry`.

## 7. Estabilidade e sensibilidade

- O candidato é dependente da composição da frota: remover um par muda ranks e
  pode mudar o status. O *leave-one-out* reduz o efeito do próprio registro, mas
  não elimina a dependência dos demais.
- Dois casos ficam muito próximos dos cortes: `DEV-1924` em 0,889 (P90 = 0,90)
  e `DEV-0204` em 0,593 (P60 = 0,60). Pequenas mudanças de composição podem
  inverter esses casos.
- A referência é invariável à composição da frota, porque cada dispositivo é
  avaliado apenas contra limiares físicos.
- Não foi executado bootstrap: com 28 registros e ranks discretos, o resultado
  seria mais uma medida da instabilidade já visível do que uma validação.

## 8. Ameaças à validade

1. **Não há ground truth independente.** A referência é o árbitro; a adjudicação
   é operacional e não cega.
2. **A amostra é pequena.** São 28 registros válidos; P90 e P60 são cortes
   grosseiros, e 10 registros por tier impedem uma estratificação confiável.
3. **Sinais esparsos e empatados.** Falhas assumem poucos valores e jank tem
   baixa variância; o *midrank* reduz, mas não resolve, a degeneração.
4. **Máximo de ranks ignora magnitude.** Um rank alto de jank baixo pode vencer
   um rank menor de dreno alto.
5. **Registros inválidos foram excluídos da distribuição.** Se eles entrassem
   como zero, os ranks mudariam; a escolha de manter `INDETERMINADO` é
   deliberada.
6. **Uma única fotografia.** Não há série temporal, rótulo de intervenção ou
   confirmação de que um alerta evitou um problema.
7. **O dreno relativo compartilha a premissa de tela ligada** da referência;
   serviços em segundo plano podem inflar os dois métodos.
8. **O candidato não foi validado em produção.** Ele é um experimento offline,
   sem tela, sem persistência e sem impacto no fluxo do app.

## 9. Reprodutibilidade

O experimento é reproduzido pelo teste:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.seldon.fleet.domain.comparison.*"
```

Arquivos principais:

- `app/src/main/java/com/seldon/fleet/domain/classifier/DeviceStatusClassifier.kt`
- `app/src/main/java/com/seldon/fleet/domain/classifier/PercentileStatusClassifier.kt`
- `app/src/main/java/com/seldon/fleet/domain/analysis/DeviceMetrics.kt`
- `app/src/main/java/com/seldon/fleet/domain/comparison/ClassificationComparison.kt`
- `app/src/test/java/com/seldon/fleet/domain/comparison/FleetComparisonTest.kt`

O teste fixa as contagens, a concordância e os IDs das divergências. Se o
arquivo oficial mudar, o snapshot precisa ser revisto.

## Apêndice — tabela completa

| ID | tier | Dreno %/h | Jank % | Falhas | Referência | Candidato | Score | Dreno p | Jank p | Falhas p |
|---|---|---:|---:|---:|---|---|---:|---:|---:|---:|
| `DEV-0137` | mid | 6,231 | 7,4 | 2 | `ATENCAO` | `CRITICO` | 1,000 | 0,519 | 0,963 | 1,000 |
| `DEV-0204` | mid | 6,323 | 4,1 | 0 | `OK` | `OK` | 0,593 | 0,593 | 0,574 | 0,463 |
| `DEV-0311` | low | N/D | 11,2 | 1 | `INDETERMINADO` | `INDETERMINADO` | N/D | N/D | N/D | N/D |
| `DEV-0418` | high | 5,558 | 2,3 | 0 | `OK` | `OK` | 0,463 | 0,333 | 0,148 | 0,463 |
| `DEV-0522` | low | 13,400 | 9,8 | 1 | `CRITICO` | `CRITICO` | 1,000 | 0,926 | 1,000 | 0,963 |
| `DEV-0630` | high | 6,313 | 3,6 | 0 | `OK` | `OK` | 0,556 | 0,556 | 0,444 | 0,463 |
| `DEV-0745` | low | N/D | 0,0 | 0 | `INDETERMINADO` | `INDETERMINADO` | N/D | N/D | N/D | N/D |
| `DEV-0800` | low | 5,922 | 4,3 | 0 | `OK` | `ATENCAO` | 0,667 | 0,444 | 0,667 | 0,463 |
| `DEV-0854` | mid | 6,070 | 5,2 | 0 | `OK` | `ATENCAO` | 0,741 | 0,481 | 0,741 | 0,463 |
| `DEV-0916` | mid | 4,405 | 3,5 | 0 | `OK` | `OK` | 0,463 | 0,185 | 0,407 | 0,463 |
| `DEV-0964` | mid | 4,628 | 0,9 | 0 | `OK` | `OK` | 0,463 | 0,222 | 0,019 | 0,463 |
| `DEV-1057` | low | 4,647 | 5,5 | 0 | `OK` | `ATENCAO` | 0,778 | 0,259 | 0,778 | 0,463 |
| `DEV-1142` | high | 5,584 | 0,9 | 0 | `OK` | `OK` | 0,463 | 0,370 | 0,019 | 0,463 |
| `DEV-1194` | mid | 2,529 | 3,2 | 0 | `OK` | `OK` | 0,463 | 0,000 | 0,333 | 0,463 |
| `DEV-1214` | high | 3,595 | 3,3 | 0 | `OK` | `OK` | 0,463 | 0,111 | 0,370 | 0,463 |
| `DEV-1231` | high | 4,660 | 4,8 | 0 | `OK` | `ATENCAO` | 0,704 | 0,296 | 0,704 | 0,463 |
| `DEV-1258` | low | 3,503 | 3,9 | 0 | `OK` | `OK` | 0,519 | 0,074 | 0,519 | 0,463 |
| `DEV-1277` | high | 2,989 | 2,1 | 0 | `OK` | `OK` | 0,463 | 0,037 | 0,111 | 0,463 |
| `DEV-1366` | high | 4,132 | 5,7 | 0 | `OK` | `ATENCAO` | 0,815 | 0,148 | 0,815 | 0,463 |
| `DEV-1422` | mid | 5,675 | 4,1 | 0 | `OK` | `OK` | 0,574 | 0,407 | 0,574 | 0,463 |
| `DEV-1514` | low | 7,418 | 2,4 | 0 | `ATENCAO` | `ATENCAO` | 0,630 | 0,630 | 0,185 | 0,463 |
| `DEV-1546` | mid | 7,491 | 7,1 | 0 | `ATENCAO` | `CRITICO` | 0,907 | 0,667 | 0,907 | 0,463 |
| `DEV-1559` | high | 10,062 | 3,7 | 0 | `ATENCAO` | `ATENCAO` | 0,778 | 0,778 | 0,481 | 0,463 |
| `DEV-1582` | low | 10,232 | 7,1 | 0 | `ATENCAO` | `CRITICO` | 0,907 | 0,815 | 0,907 | 0,463 |
| `DEV-1625` | low | 7,580 | 2,9 | 0 | `ATENCAO` | `ATENCAO` | 0,704 | 0,704 | 0,259 | 0,463 |
| `DEV-1711` | high | 9,680 | 4,2 | 0 | `ATENCAO` | `ATENCAO` | 0,741 | 0,741 | 0,630 | 0,463 |
| `DEV-1767` | mid | 10,784 | 2,7 | 0 | `ATENCAO` | `ATENCAO` | 0,852 | 0,852 | 0,222 | 0,463 |
| `DEV-1839` | mid | 19,444 | 5,9 | 0 | `CRITICO` | `CRITICO` | 1,000 | 1,000 | 0,852 | 0,463 |
| `DEV-1924` | high | 13,379 | 3,0 | 0 | `CRITICO` | `ATENCAO` | 0,889 | 0,889 | 0,296 | 0,463 |
| `DEV-1985` | low | 13,499 | 2,0 | 0 | `CRITICO` | `CRITICO` | 0,963 | 0,963 | 0,074 | 0,463 |
