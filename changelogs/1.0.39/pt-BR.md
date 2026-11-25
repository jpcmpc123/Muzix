## Muzix v1.0.39

### Correções na UI das Letras, Otimizações e Eliminação de Estalos no Equalizador

Esta atualização resolve o flash de código/formatação nas letras, corrige o erro HTTP 400 do YouTube Music no log e elimina pipocos/estalos nos agudos do equalizador:

- **Zero Flash de Tags/Código nas Letras:**
  - Corrigida a retenção de estado defasado (`produceState`) no `LyricsView` que exibia linhas `RICH_SYNCED` antigas (`<00:19.99>...`) ao trocar para músicas `LINE_SYNCED`, incluindo romanização e traduções defasadas.
  - Agora toda linha renderizada em modo estático ou traduzido passa por sanitização completa (`<mm:ss.xx>`, `[mm:ss.xx]`, marcadores `v1:` e tags HTML), preservando palavras com hífen (`mestre-sala`), pontuação e idiomas CJK.
  - Corrigido o ajuste de offset das letras (`lyricsOffsetMs`) no modo palavra-por-palavra, o reset de scroll e chaves únicas da lista por música, e o alinhamento por timestamp das traduções nos mini-previews do player.
- **Equalizador Sem Estalos/Pipocos (especialmente nos Agudos):**
  - Substituído o limiter reativo por um **Lookahead True-Peak Limiter de 2 ms** (teto seguro em `-0.8 dBFS`), evitando ceifamento duro (hard-clipping) e aliasing de transientes agudos.
  - Migrados os filtros Biquad para **slots fixos TDF-II com rampa suave de 10 ms**, eliminando estalos ao cruzar `0 dB` ou arrastar os controles de agudos.
  - Incluído o ganho de `Estéreo` e `Palco Espacial` no pré-amplificador automático (`AUTO_BALANCED`) e corrigida a fórmula RBJ das prateleiras de graves/agudos para evitar picos mesmo com agudos baixos.
- **Performance e Rede:**
  - Eliminado o erro `HTTP 400 INVALID_ARGUMENT` do cliente `ANDROID_MUSIC` ao consultar letras oficiais do YouTube Music, economizando uma chamada de rede por faixa, além de cache para `lyricsEndpoint`.
  - Reduzida drasticamente a carga na Main Thread com lookup `O(1)` no cache de preparação das letras, pré-limpeza em background e playhead único compartilhado.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
