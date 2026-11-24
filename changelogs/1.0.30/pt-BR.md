## Muzix v1.0.30

### Blur Suave & Fluidez Estilo Apple Music (Zero-Lag)

Esta atualização implementa o desfoque progressivo de profundidade de campo nas letras estilo **Apple Music** de forma **100% automática**, calibrado para **60/120 FPS** sem travamentos nem bloqueios na UI:

- **Curva de Desfoque Suave e Progressiva:**
  - Linha ativa sempre nítida (`0 dp`, opacidade `100%`) e linhas vizinhas com desfoque gradual suave (`1.4 → 2.8 → 4.4 → 5.8 dp`, máximo de `6.0 dp`), congelando para linhas distantes (`>= 4`) para manter o GPU cache estável.
  - Desfoque é removido dinamicamente em `180 ms` ao rolar manualmente a letra para facilitar a leitura e restaurado suavemente em `320 ms` ao retomar a sincronização.
- **Zero-Lag na GPU e Quantização Inteligente:**
  - `AppleMusicBlurEffectPool` quantizado em passos de `0.5 dp`, reutilizando exatamente a mesma instância de `BlurEffect` entre frames sem invalidar `RenderNode`s.
  - Camada unificada em um único `graphicsLayer` por linha (eliminando 3 camadas extras por item do `LazyColumn`).
  - Fundo animado (`AppleMusicArtworkBackdrop`) e malha gradiente (`AppleMusicMeshBackdrop`) agora pré-processam o blur em resolução reduzida em background (`StackBlur` $O(1)$), eliminando o `.blur()` live em tela cheia sobre a GPU.
- **Suporte Automático em Todos os Dispositivos:**
  - **Android 12+ e Desktop:** aceleração nativa por hardware (`RenderEffect`).
  - **Android 8–11:** fallback automático em background via `StackBlur` em escala `0.5x` com cache LRU, sem depender de API 31+ e sem qualquer ajuste manual.
- **Sincronização Inteligente nas Configurações:**
  - Selecionar o estilo **Apple Music** no player agora ativa automaticamente o estilo de letras **Apple Music** caso esteja no modo Clássico, e as opções ficaram disponíveis sem restrição de versão.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
