Atualização do **Muzix** com o início ultrarrápido de reprodução via `VISIONOS 1.04` sem bloquear a música enquanto o `WEB_REMIX` carrega em segundo plano, mantendo todas as proteções de compatibilidade, volume e qualidade a longo prazo.

### Principais Melhorias (`v1.0.25`)
- **Início de música quase 2x mais rápido (~280 ms):**
  - Quando a rota rápida `VISIONOS` responde com o áudio válido, o **Muzix** libera o player imediatamente sem esperar o `WEB_REMIX` terminar — reduzindo a espera mediana ao dar play ou pular faixas pela metade.
- **Enriquecimento em segundo plano sem travar nem gerar conflitos no banco:**
  - `playbackTracking` e `loudnessDb` do `WEB_REMIX` são buscados em paralelo nos bastidores (pulando a requisição extra quando `sendBackToGoogle` está desligado) e aplicados no banco Room via `UPDATE` parcial segmentado por `(videoId, cpn)`, cancelando tarefas antigas ao trocar de faixa.
- **Proteções de longo prazo preservadas:**
  - Classificação segura de `MediaType` evitando que thumbnails `4:3` do `VISIONOS` marquem músicas comuns como vídeo (preservando BPM/tonalidade do AutoMix e evitando atrasos na busca de áudio).
  - Contas **YouTube Premium** continuam aguardando os fluxos de `256 kbps` (`itag 774` / `141`) antes de iniciar.
  - Validação estrita de duração (`dur=`) e verificação inicial `HEAD` contra travamentos `403` mantidas integralmente.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.25/LICENSE
