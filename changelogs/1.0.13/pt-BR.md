Atualização do **Muzix** com blindagem em 4 camadas para impedir letras trocadas ou sem relação com a música tocando (como *Lamento — Antonio Carlos Jobim* aparecendo em *Onde Anda Você — Toquinho, Vinícius de Moraes*).

### Correções
- **Detector de identidade contraditória (`LyricsIdentityGuard`):** rejeita automaticamente letras cujo cabeçalho inicial (ex.: `Lamento - Antonio Carlos Jobim（安东尼奥·卡洛斯·乔宾）`) ou tags embutidas (`[ti:]`, `[ar:]`, `<ttm:title>`, `<ttm:agent>`, duração TTML) contradizem a faixa em reprodução.
- **Consenso textual contra provedores sem metadados (`BetterLyrics`):** provedores cegos no formato `RICH_SYNCED` não podem mais sobrescrever letras corretas nem vencer a corrida sem validação — quando uma fonte verificada (`LRCLIB` / `Spotify`) discorda da letra retornada, o app bloqueia a letra trocada e exibe a letra verificada da música certa.
- **Envio completo de todos os artistas da faixa:** músicas em parceria/dupla (como `Toquinho, Vinícius de Moraes`) agora enviam todos os artistas na busca e na validação em vez de apenas o primeiro.
- **Limpeza automática de cache contaminado:** letras erradas já salvas no cache local do celular são ignoradas na abertura e substituídas automaticamente pela versão correta da rede.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.13/LICENSE
