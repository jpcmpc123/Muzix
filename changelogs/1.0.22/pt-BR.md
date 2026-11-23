Atualização do **Muzix** com aceleração direta na abertura de músicas novas (`~2,1 s` → `~0,5–0,7 s` em média) usando o cliente anônimo `VISIONOS 1.04` em **1 única requisição**, além de correções de diagnóstico no `PlayTiming`.

### Principais Melhorias (`v1.0.22`)
- **Fast-Path Direto `VISIONOS 1.04` (1 POST em paralelo ao `WEB_REMIX`):**
  - Substitui o fast-path anterior pelo cliente anônimo `VISIONOS 1.04` (sem cookies de conta, com cache e pré-aquecimento do `visitorData` por 30 minutos).
  - Elimina as 3 chamadas HTTP extras seriais e descartadas que o extrator completo fazia por faixa (`visitor_id` repetido, `webMetadata` e `/next`), mantendo apenas a chamada que entrega as URLs diretas de áudio (`itag 251`, `140`, etc.).
  - Parsing defensivo via `JsonObject` (tolerante a campos opcionais ausentes), validação de duração `dur=`, ordenação por idioma preferido, mesclagem preservando `playbackTracking` e `loudnessDb` do `WEB_REMIX`, janela de confiança `HEAD` de 5 minutos e fallback/cooldown automático de 15 minutos para o `BravePipe` completo em caso de recusa.
- **Diagnóstico `PlayTiming` preciso:**
  - Evita falsos eventos `ready=`/`playing=` antes de a nova faixa ter duração e buffer reais carregados durante a troca de música, e registra explicitamente `visionos:cooldown` no `tier=[...]` quando em período de fallback.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.22/LICENSE
