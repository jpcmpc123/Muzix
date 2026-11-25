## Muzix v1.0.38

### Novas Fontes de Letras de Alta Qualidade (YTM Oficial Sincronizado, Unison e NetEase)

Esta atualização amplia e melhora a precisão das letras sincronizadas no Muzix sem alterar tipografia ou desempenho do player:

- **YouTube Music Oficial Sincronizado (`ANDROID_MUSIC` `timedLyricsData`):**
  - Quando conectado com sua conta do YouTube Music, o app agora busca as **letras sincronizadas oficiais** (Musixmatch / LyricFind) vinculadas diretamente ao `videoId` da música — evitando letras erradas e desencontros de versão (`Ao Vivo` vs estúdio), com fallback automático para o modo estático quando necessário.
- **Unison (`unison.betterlyrics.org`):**
  - Integrada a base comunitária aberta em `TTML`/`LRC` (sincronia palavra por palavra / linha), consultada primeiro pelo `videoId` exato e exibindo atribuição clicável discreta no rodapé da letra.
- **NetEase Cloud Music (`music.163.com`):**
  - Integrado suporte direto (sem token e sem Cloudflare) com parser completo de **`YRC` palavra-por-palavra** e `LRC`, com excelente cobertura de músicas japonesas, coreanas e pop internacional, e filtragem rígida de título, artista, duração e remoção automática de créditos no topo (`作词`/`作曲`/`制作人`).
- **Orquestração e Seleção Inteligente:**
  - Prioridade automática quando logado: `YTM Oficial (Sincronizado)` → `LRCLIB` → `Unison` → `NetEase` → `Spotify` → `BetterLyrics` → `SimpMusic` → `YouTube Captions`, além de opções para seleção manual em **Configurações** e no menu **Reproduzindo Agora**.
  - Revalidação única automática do cache (`v4`) para atualizar músicas anteriormente salvas apenas como estáticas ou com fontes inferiores.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
