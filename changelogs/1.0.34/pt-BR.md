## Muzix v1.0.34

### Otimizações de Rede Offline e Retomada Rápida do Fast Path

Ajustes focados em desempenho, resposta instantânea ao reconectar e economia de bateria quando o aparelho oscila ou fica temporariamente sem internet:

- **Fast Path `VISIONOS` Protegido Contra Cooldown Offline:**
  - Erros de ausência de DNS/rede (`UnknownHostException`, `NoRouteToHostException`, `UnresolvedAddressException`) não colocam mais o fast path `VISIONOS` em cooldown de 15 minutos.
  - Ao recuperar a conexão, a reprodução volta imediatamente a resolver faixas em ~350–400 ms em vez de cair para o fallback mais lento (~1,7–2,0 s).
- **Zero Loop de Pré-carregamento Sem Internet:**
  - `CrossfadeExoPlayerAdapter` agora verifica a conectividade antes de disparar pré-carregamento (`precache`) ou retries de erro de fonte, evitando dezenas de tentativas inúteis sem rede e preservando bateria, CPU e a confiança do extrator.
- **Logs Mais Limpos:**
  - Cancelamentos normais de telemetria (`CancellationException`) em `InitPlayback` ao pular rapidamente de faixa não são mais registrados como erros.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
