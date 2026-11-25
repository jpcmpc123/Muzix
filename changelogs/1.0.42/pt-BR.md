## Muzix v1.0.42

### Playlists Instantâneas com Capas Pré-Carregadas e Capas 56dp (Padrão YouTube Music)

Esta atualização elimina o spinner ao abrir a aba **Biblioteca** e ajusta o tamanho das capas da lista de playlists do YouTube para o padrão do YouTube Music:

- **Carregamento Instantâneo (Zero Spinner):**
  - `LibraryViewModel` promovido a singleton em memória mantendo as playlists e miniaturas vivas ao trocar de aba.
  - Adicionado snapshot persistente em disco com atualização silenciosa em background (SWR) e isolamento automático por conta Google (`accountHash`).
  - Pré-aquecimento da lista de playlists e prefetch de até 24 capas + **Músicas que Gostei** diretamente na inicialização do app (`App.kt`), com renderização imediata da RAM via `rememberRamFirstTransform`.
- **Capas `56.dp` no Padrão YouTube Music:**
  - Aumentado o tamanho das capas em `Suas Playlists do YouTube` (incluindo `Músicas que Gostei`) de `48.dp` para **`56.dp × 56.dp`** com `ContentScale.Crop` e alinhamento de `16.dp` alinhado ao título da seção.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
