### Liquid Glass (Otimizado e Estável — fora de BETA)
- **Mesmo design visual, muito mais fluido no Android**:
  - Eliminada a 3ª gravação offscreen de tela inteira por frame na Home, Biblioteca e Pesquisa (a barra superior agora reutiliza o mesmo backdrop dos chips em vez de duplicar com Haze).
  - Gravação de luminância sob demanda: `layer.record` agora roda apenas 1 vez a cada 2,5 s (em vez de todo frame) e pula `readPixels()` quando a tela está parada.
  - Luminância única compartilhada entre a barra inferior e o MiniPlayer, cortando pela metade os readbacks GPU→CPU.
  - Blur quantizado (1 dp) e limitado a 24 dp para evitar reconstruções de `RenderEffect` a cada frame da animação, mais fusão de `vibrancy()` + `colorControls()` em um único passe.
  - Desativados `lens` AGSL e `InnerShadow` no indicador da barra inferior quando em repouso (`pressProgress == 0f`).
  - Chips da Biblioteca e Pesquisa convertidos para `LazyRow` (chips fora da tela não executam shaders).
  - `layerBackdrop()` agora desativa gravações offscreen automaticamente em todas as telas quando o Liquid Glass está desligado.
- **Completude**:
  - Corrigido o formato do vidro do MiniPlayer (`CircleShape`) para alinhar com o recorte da pílula.
  - Verificação real de suporte a Android 13+ (API 33+) nas Configurações e fallback automático no modo Economia de Bateria.

### Crossfade (Estável — fora de BETA)
- **Zero buracos de silêncio**: aguarda a próxima faixa atingir `STATE_READY` mantendo a faixa atual em 100% de volume antes de trocar a notificação e iniciar o fade-out.
- **Rampa contínua (~30 Hz por relógio real)**: substitui os 50 passos fixos por interpolação contínua a cada 33 ms, eliminando degraus audíveis em crossfades longos (10–45 s).
- **AutoMix mais inteligente e leve**:
  - Usa o BPM local medido pelo `TrackStructureAnalyzer` quando os metadados do Tidal não estão disponíveis e reduz o fallback sem BPM de 30 s para 8 s.
  - Cache da resolução de duração/plano no loop de 50 ms, limite de 100 entradas em memória e proteção contra download duplicado em redes móveis (4G/5G).
- **Filtro Modo DJ sem estalos**: snapshot imutável thread-safe, topologia *Transposed Direct Form II* e rampa dry/wet de ~10 ms ao ligar/desligar.
- **Edge cases resolvidos**: permite crossfade em álbuns quando o modo Aleatório está ativo e encerra imediatamente transições em andamento se o usuário desligar o Crossfade.
