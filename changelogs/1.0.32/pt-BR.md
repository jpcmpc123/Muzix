## Muzix v1.0.32

### Máxima Qualidade de Áudio Sem Premium (Opus 251 + Anti-DRC + Normalização Limpa)

Aprimoramentos profundos no motor de reprodução e na interface para entregar o máximo de fidelidade de áudio disponível no YouTube Music gratuito, sem depender de assinaturas ou fontes instáveis:

- **Qualidade Padrão Elevada para Opus 251 (`Medium - 129kps`):**
  - Tanto o streaming quanto os downloads agora partem por padrão de **Opus 251 (~129–160 kbps VBR)** em vez de `66 kbps` (`itag 250`).
  - Em contas gratuitas com a opção **Alta** selecionada, o fallback agora prioriza **Opus 251 → AAC 140 → maior bitrate sem DRC**, evitando seleções inferiores ou streams comprimidos.
- **Prioridade Anti-DRC (Faixas Sem Compressão Dinâmica):**
  - Detecção automática de faixas marcadas com `isDrc` ou parâmetro `drc=1` nos extrator e repositórios (`AudioTrackOrder`, `DirectAudioFastPath` e `StreamRepositoryImpl`).
  - Streams originais sem compressão de faixa dinâmica são priorizados sobre versões `drc=1` de mesmo idioma/faixa.
- **Correção na Normalização de Volume (`LoudnessEnhancer`):**
  - Corrigido o comportamento que enviava ganhos negativos ao `LoudnessEnhancer` do Android quando `loudnessDb > 0`.
  - Músicas mais altas agora são atenuadas suavemente via `player.volume` com o `LoudnessEnhancer` desativado (sem perda de clareza), enquanto faixas baixas recebem apenas reforço positivo controlado (`0` a `600 mB`).
- **Transparência na Interface do Player:**
  - O selo de qualidade no player (`toAudioQualityLabel`) agora exibe o codec amigável junto com bitrate e taxa de amostragem (ex.: `Opus · 134 kbps · 48.0 kHz`).
  - A folha **Informações da música** agora mostra o bitrate formatado em `kbps` (`bps`) e uma nova linha **Faixa dinâmica** indicando **Original (sem DRC)** ou **Comprimida (DRC)**.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
