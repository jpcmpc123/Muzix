Atualização do **Muzix** fechando as 5 brechas residuais identificadas na auditoria do pipeline de letras e traduções para eliminar versos em branco, letras trocadas e sincronias cortadas.

### Correções e Melhorias
- **Tradução híbrida sem versos em branco (0% de lacunas):** além de ampliar a combinação de versos divididos no Letras/Vagalume para até 4 linhas, qualquer verso isolado que ainda não casar com a letra sincronizada passa a ser preenchido automaticamente em lote mantendo 100% da letra traduzida.
- **Consenso reforçado contra letras trocadas:** quando `LRCLIB` e `Spotify` não possuem a faixa, o `Letras.mus.br` passa a atuar como segunda opinião para validar ou rejeitar resultados errados do `BetterLyrics`; envios comunitários incorretos no `SimpMusic` também são substituídos ao haver conflito textual com fontes verificadas.
- **Prioridade PT-BR e busca multi-artistas:** para traduções em português, o `Letras.mus.br` e o `Vagalume` agora têm prioridade antes da base comunitária do `SimpMusic`, pesquisando todos os artistas/parcerias da música (com atualização automática de cache `#v7`).
- **Proteção contra LRC truncado no LRCLIB:** registros sincronizados que param cedo demais (antes de 60% da música) não encerram mais a busca antes de verificar versões completas no `/api/search`.
- **Polimento do Letras estático:** rótulos estruturais entre colchetes (como `[Refrão]`, `[Verso 1]`, `[Chorus]`, `[Outro]`) são removidos ao exibir letras estáticas do Letras.

### Qual APK baixar?
- Na maioria dos celulares Android modernos, baixe **`Muzix-full-arm64-v8a-release.apk`**.
- Em caso de dúvida, baixe **`Muzix-full-universal-release.apk`**.
- Atualiza diretamente por cima das versões anteriores do **Muzix** sem perder seus dados nem seu login.

### Créditos e código-fonte
**Baseado no SimpMusic, de maxrave-dev, licenciado sob a GPLv3.**

- Código-fonte do Muzix: https://github.com/jpcmpc123/Muzix
- Repositório original: https://github.com/maxrave-dev/SimpMusic
- Licença: https://github.com/jpcmpc123/Muzix/blob/v1.0.15/LICENSE
