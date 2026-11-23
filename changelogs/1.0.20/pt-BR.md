Atualização do **Muzix** corrigindo o erro de silêncio inicial e duração divergente em faixas com videoclipe (`OMV`), acelerando a partida de músicas novas em internet móvel lenta e corrigindo bugs do player identificados em log real.

### Correções no Player e Áudio Oficial de Estúdio
- **Fim do silêncio inicial e da diferença de duração (`03:26` vs `03:19`):** em álbuns, filas e reprodução somente áudio, faixas devolvidas pelo YouTube como videoclipe (`MUSIC_VIDEO_TYPE_OMV`, que costumam ter créditos silenciosos no início) agora são resolvidas para a versão oficial de áudio de estúdio (`MUSIC_VIDEO_TYPE_ATV`).
- **Saneamento automático de faixas/álbuns já em cache:** o app invalida formatos ou trechos em cache cuja duração divirja dos metadados da música e atualiza as faixas de álbuns salvos ao reabrir, além de validar o parâmetro `dur=` da URL de stream e limpar cache ao renovar formato (`updateFormat`).

### Partida Mais Rápida em Internet Lenta (4G)
- **Extrator principal imediato (`BravePipe` em `t = 0 ms`):** elimina a espera artificial de `800–1800 ms` antes de acionar o extrator de streams no Android e coloca servidores `PipePipe` em cooldown também após timeouts/erros de rede.
- **Pré-cache sem roubar banda da faixa atual:** o pré-cache das próximas faixas agora aguarda a música atual atingir `STATE_READY` com buffer adiantado (`>= 8 s`), limita pré-cache em rede móvel/medida para 1 faixa e prepara os itens sequencialmente com buffers leves (`15–20 s`).
- **Pool HTTP compartilhado e buffers calibrados:** único pool de conexões no Media3 (`connect 8 s` / `read 15 s`, sem logging HTTP em release) reduzindo handshakes TLS repetidos, e `rebuffer` calibrado (`1,5 s`) para evitar micro-engasgos em rede instável.

### Outras Correções
- **`TrackCachePreloader` sem downloads repetidos:** pula trechos já salvos no cache e encerra quando o span está bloqueado pelo player, evitando baixar a mesma música 8 vezes seguidas em rede móvel.
- **Estabilidade e eficiência:** eliminada a reemissão periódica na thread principal (`Playback speed` a cada 5 s), `pause()` idempotente quando já pausado e suporte a paginação final da Biblioteca sem campo `items` (`GridContinuation`).

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.20/LICENSE
