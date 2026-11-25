## Muzix v1.0.40

### Preservação dos Primeiros Versos e Início Sincronizado das Letras

Esta atualização corrige dois problemas reportados nas letras após a v1.0.39:

- **Primeiros Versos Preservados (ex: *Has To Be*, *Smile*):**
  - Endurecida a limpeza de créditos iniciais no `LyricsHeaderSanitizer` (`stripLeadingNonSungLinesAgainstReference` e remoção do falso positivo de nomes em Title Case) para remover apenas linhas de metadados/créditos reais (`Title - Artist`, `Lyrics by:`, nomes em kanji isolados e créditos da descrição), preservando ad-libs e versos iniciais reais mesmo quando ausentes ou com grafia diferente na letra de referência.
- **Letras com Tempo Sempre Começam Escurecidas (Sem Flash Todo em Branco):**
  - Corrigido o `LyricsResolver` para reter resultados estáticos (`UNSYNCED`, inclusive quando o `YTM_OFFICIAL` responde primeiro sem sincronia) enquanto os provedores sincronizados (`LRCLIB`, `Unison`, `NetEase`, `BetterLyrics`, `SimpMusic`) ainda estão em andamento, garantindo que músicas com tempo sempre iniciem com a prévia escurecida antes da primeira batida (e só exibindo letra estática se nenhum provedor tiver tempo).
- **Revalidação Automática do Cache:**
  - Atualizada a chave de revalidação para `lyrics_v5_revalidated_ids` para recuperar automaticamente faixas que haviam ficado cacheadas sem o primeiro verso ou como texto estático.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
