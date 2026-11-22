Atualização do **Muzix** focada em **eliminar letras erradas em faixas instrumentais** e **bloquear legendas automáticas ruins do YouTube (ASR)**.

### Correções
- **Faixas instrumentais sem letra falsa:**
  - Reconhece quando o **LRCLIB** confirma que a faixa é `instrumental` ou quando o título indica explicitamente instrumental/karaokê/off-vocal/type beat — mantendo o card de **Letras** oculto em vez de puxar a letra de outro artista com o mesmo nome (como ocorria em *Bloom* de *j^p^n*).
  - Adicionada validação rigorosa de **título, artista e duração** no LRCLIB, Spotify e SimpMusic (também filtrando envios comunitários negativados).
- **Bloqueio de legendas automáticas ruins (ASR) do YouTube:**
  - O provedor do YouTube agora utiliza apenas legendas feitas por humanos (`kind != "asr"`), evitando transcrições quebradas e marcadores como `[Muzică]`/`[Music]`.
  - Adicionado filtro automático de qualidade de conteúdo (`LyricsContentQualityGuard`) que também **limpa do cache local** letras antigas contaminadas por ASR ou pertencentes a faixas confirmadas como instrumentais.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.8/LICENSE
