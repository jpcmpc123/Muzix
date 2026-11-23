Atualização do **Muzix** com aceleração extra na abertura de músicas novas (incluindo rede móvel lenta) e eliminação da extração duplicada em faixas de videoclipe (`OMV`), baseada nos logs reais da `v1.0.20`.

### Melhorias de Velocidade e Estabilidade no Player
- **Fim da extração dupla em faixas `OMV` (`~6,1 s` → `~1,8–2,5 s`):** faixas já identificadas como videoclipe (`MUSIC_VIDEO_TYPE_OMV`) agora resolvem a variante oficial de áudio de estúdio (`MUSIC_VIDEO_TYPE_ATV`) **antes** da primeira extração de stream, com timeout curto (`3,5 s`), fallback automático para o ID original caso o `ATV` falhe e cache negativo por sessão para faixas sem variante `ATV`.
- **Caminho ultrarrápido de áudio direto (`ANDROID_VR`) com fallback automático para `BravePipe`:** em reprodução somente áudio, o app consulta o cliente anônimo `ANDROID_VR` em paralelo ao `WEB_REMIX` (sem cookies de conta, com validação `HEAD`, janela de confiança de 5 minutos, validação de `dur=` e preservação de `playbackTracking`/`loudnessDb`), atrasando o `BravePipe` em `700 ms` (ou acionando-o imediatamente se o `ANDROID_VR` falhar) e entrando em cooldown automático de 15 minutos em caso de recusa ou 403.
- **Menos requisições descartadas no 4G:** tempo de hedge do `PipePipe` (`HEDGE_PIPE_FALLBACK_MS`) ajustado de `2,0 s` para `3,5 s`, evitando conexões abortadas contra `api.pipepipe.dev` em picos normais de latência móvel.
- **Pré-cache resiliente a oscilações breves de DNS:** adicionado backoff progressivo (`1,5 s * tentativa`) em `precacheRetryListener` antes de re-preparar faixas que falharam por erro de rede/DNS.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.21/LICENSE
