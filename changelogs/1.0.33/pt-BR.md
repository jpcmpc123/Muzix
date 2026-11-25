## Muzix v1.0.33

### Qualidade de Áudio Sempre Visível + Atualizador Direto pelo App

Esta versão resolve de forma definitiva os casos em que a qualidade e os detalhes técnicos da faixa apareciam vazios ou como "Desconhecido" e traz download/instalação de atualizações diretamente pelo Muzix:

- **Qualidade de Áudio Preservada (Fim do "Desconhecido"):**
  - Trocas/oscilações de rede e mudanças nas configurações de qualidade agora apenas expiram as URLs de streaming (`expired_time`), preservando no banco `itag`, codec, bitrate, taxa de amostragem, normalização e metadados AutoMix de músicas já tocadas, em cache ou baixadas.
  - **Fallback em tempo real pelo ExoPlayer (`onTracksChanged`):** músicas antigas já armazenadas em cache ou baixadas que estavam sem registro técnico passam a ler `mimeType`, `codecs`, `bitrate` e `sampleRate` diretamente da faixa ativa do player, com zero requisições extras.
  - Logs `I/Stream` enriquecidos com bitrate em `kbps`, flag `drc` e `loudnessDb` sem expor URLs.
- **Atualizador Direto pelo App (Android):**
  - Ao tocar em **Baixar** no aviso de atualização, o Muzix agora seleciona automaticamente o APK exato da arquitetura do dispositivo (`arm64-v8a`, `armeabi-v7a` ou `x86_64`, com fallback `universal`) — economizando dados e tempo.
  - Download leve em streaming de `32 KB` sob demanda em segundo plano, com barra de progresso em tempo real, verificação de integridade **SHA-256** e abertura direta do instalador Android via `FileProvider` (incluindo retomada automática após conceder a permissão de instalação).
  - Limpeza automática de APKs antigos em cache ao concluir atualizações e opção secundária para abrir no GitHub caso prefira.

---

### Créditos e Licença
- **Muzix** — mantido por `jpcmpc123`.
- Baseado no projeto **SimpMusic** (`maxrave-dev`), licenciado sob a **GNU General Public License v3.0 (GPLv3)**.
