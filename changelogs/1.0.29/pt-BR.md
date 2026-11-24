Atualização do **Muzix** com **blindagem definitiva contra silêncio inicial em videoclipes (`OMV -> ATV`)**, proteção do fast-path **`VISIONOS`** contra falsos cooldowns de 15 minutos e correções nos fallbacks de reprodução.

### Principais Melhorias (`v1.0.29`)
- **Fim Definitivo do Silêncio Inicial em Faixas com Videoclipe (`OMV -> ATV`):**
  - **Zero lista negra por queda/timeout de 4G:** a busca pela faixa oficial de estúdio (`MUSIC_VIDEO_TYPE_ATV`) agora diferencia **"não existe áudio de estúdio"** de **"timeout/erro de rede"** — oscilações no 4G não bloqueiam mais a faixa pelo resto da sessão, e o cache negativo confirmado agora expira em 10 minutos.
  - **Persistência em disco do mapeamento `OMV -> ATV`:** assim que uma música com clipe é convertida para a versão de estúdio (ex.: *I Can't Handle Change* de `03:26` com silêncio → `03:18` imediato), o par fica salvo no aparelho e abre em **`0 ms`** mesmo após fechar e reabrir o app.
  - **Recuperação automática em divergência de duração & upgrade mais forte nos Álbuns:** abertura de álbum usa `next` anônimo com orçamento maior para 4G (`2,5 s`) + busca paralela `FILTER_SONG`, e divergências reais de duração disparam re-tentativa imediata de `ATV`.
- **Fast-Path `VISIONOS` Muito Mais Consistente (`~0,4 s` sem travar por 15 min):**
  - Erros específicos de uma única faixa (`dur_mismatch`, `UNPLAYABLE`, `LOGIN_REQUIRED`) e oscilações isoladas de rede **não derrubam mais o `VISIONOS` por 15 minutos** para todas as músicas seguintes.
- **Fallbacks de Clientes YouTube e Correções Gerais:**
  - Clientes de fallback (`ANDROID`, `IOS`, `TVHTML5`, `TVHTML5_SIMPLY`) agora usam o endpoint correto (`youtubei.googleapis.com`) e `ANDROID_MUSIC`/`TVHTML5` autenticados para faixas com `LOGIN_REQUIRED`, eliminando erros `HTTP 400 Precondition check failed`.
  - Corrigido o erro `For input string: "Nenhum"` no `PlaylistParser` ao abrir playlists vazias em Português.
  - Métrica `PlayTiming: ready=` corrigida para não registrar tempos inflados após pausar/retomar, e remoção de logs ruidosos de evicção `formatDur=nulls`.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados, perfis salvos nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.29/LICENSE
