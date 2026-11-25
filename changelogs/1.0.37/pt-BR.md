## Muzix v1.0.37

### Remoção Definitiva de Créditos na Intro (ex.: `三浦徳子`) e Prioridade para LRCLIB

Ajuste cirúrgico baseado na análise direta dos payloads reais das APIs de letras:

- **Remoção garantida de créditos de compositor/letrista durante a introdução (`00:00`–`00:15`):**
  - Na faixa `夜空のイヤリング`, o nome da letrista (`<00:07.22>三 <00:08.12>浦 <00:09.03>徳 <00:09.93>子`) vinha cronometrado em `7,22s` (acima da janela anterior de `3,5s`).
  - Expandida a detecção de crédito inicial para até `15s` antes da entrada do vocal e adicionado **cruzamento determinístico** contra a letra limpa (`syncedLyrics`/`plainLyric`/`LRCLIB`/`YouTube Music Oficial`), removendo qualquer linha inicial não cantada em qualquer idioma.
- **LRCLIB como Provedor Padrão #1 e Tratamento de HTTP 401 do BetterLyrics:**
  - O servidor do BetterLyrics passou a exigir API key (HTTP 401) para músicas fora de cache; o app agora trata `401/403` imediatamente como indisponível (sem atrasos nem falhas falsas) e prioriza o **LRCLIB** (100% gratuito, ilimitado e catalogado) em 1º lugar.
- **Casamento Exato para Faixas `(Ao Vivo)`:**
  - Permite sincronia precisa em faixas `(Ao Vivo)` quando a duração bate em até `2s` com a gravação exata no catálogo (como *Flor De Lis (Ao Vivo)* — Emílio Santiago & Alcione), mantendo o bloqueio rigoroso contra versões de estúdio com duração diferente.
- **Revalidação automática do cache (`v3`):**
  - Músicas já salvas na `v1.0.36` são re-limpas automaticamente na próxima reprodução.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
